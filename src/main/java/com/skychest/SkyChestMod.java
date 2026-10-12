package com.skychest;

import com.skychest.ChestStore.Item;
import com.skychest.ChestStore.Stash;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;

import java.util.*;

/**
 * Open a tracked chest: its contents are saved when you close it, and a "Chest info" panel
 * lets you pick a preset (Farming, Mining...). The "Search" button under your inventory finds items.
 */
public class SkyChestMod implements ClientModInitializer {
    static final ChestStore S = ChestStore.load();
    static BlockPos pending;
    static long pendingAt;

    // the chest/menu screen that is currently open
    static Screen curScreen;
    static Stash cur;                 // tracked entry for it (null = not tracked yet)
    static List<BlockPos> curHalves;  // block positions if it is a real chest/barrel

    static final List<AABB> glow = new ArrayList<>();
    static long glowUntil;

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide() && isStorage(level.getBlockState(hit.getBlockPos()))) {
                pending = halves(level, hit.getBlockPos()).get(0);
                pendingAt = System.currentTimeMillis();
            }
            return InteractionResult.PASS;
        });
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> onScreen(screen));
        // If chests don't highlight on your build, try LevelRenderEvents.DEBUG_RENDER instead.
        LevelRenderEvents.END_EXTRACTION.register(ctx -> drawGlow());
    }

    // ---------- helpers ----------
    static boolean isStorage(BlockState s) {
        return s.getBlock() instanceof ChestBlock || s.getBlock() instanceof BarrelBlock;
    }

    /** One position for a single chest, two (sorted) for a double chest. */
    static List<BlockPos> halves(Level l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        if (!(s.getBlock() instanceof ChestBlock) || s.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return List.of(p);
        BlockPos o = p.relative(ChestBlock.getConnectedDirection(s));
        return p.asLong() <= o.asLong() ? List.of(p, o) : List.of(o, p);
    }

    static boolean onHypixel() {
        if (S.anyServer) return true;
        ServerData sd = Minecraft.getInstance().getCurrentServer();
        return sd != null && sd.ip != null && sd.ip.toLowerCase().contains("hypixel.net");
    }

    static String fmt(int n) {
        return n >= 1_000_000 ? String.format("%.1fM", n / 1e6) : n >= 10_000 ? String.format("%.1fk", n / 1e3) : String.valueOf(n);
    }

    /** Reads "Area: Hub" from the tab list. Falls back to whatever you set by hand in the panel. */
    static void detectArea() {
        var conn = Minecraft.getInstance().getConnection();
        if (conn == null) return;
        for (PlayerInfo pi : conn.getListedOnlinePlayers()) {
            Component n = pi.getTabListDisplayName();
            if (n == null) continue;
            String t = ChatFormatting.stripFormatting(n.getString());
            if (t != null && t.trim().startsWith("Area:")) {
                String a = t.trim().substring(5).trim();
                if (!a.isEmpty() && !a.equals(S.area)) { S.area = a; glow.clear(); S.save(); }
                return;
            }
        }
    }

    // ---------- screens ----------
    static void onScreen(Screen screen) {
        if (!onHypixel()) return;
        boolean chest = screen instanceof AbstractContainerScreen<?> a && a.getMenu() instanceof ChestMenu;
        if (!chest && !(screen instanceof InventoryScreen)) return;
        detectArea();
        if (chest && screen != curScreen) resolve(screen);
        ChestPanel.attach(screen, chest);
    }

    static void resolve(Screen screen) {
        curScreen = screen; cur = null; curHalves = null;
        ChestMenu cm = (ChestMenu) ((AbstractContainerScreen<?>) screen).getMenu();
        String title = screen.getTitle().getString();
        Level level = Minecraft.getInstance().level;
        if (pending != null && System.currentTimeMillis() - pendingAt < 3000 && level != null) {
            curHalves = halves(level, pending);
            BlockPos p = curHalves.get(0);
            cur = S.at(S.area, p.getX(), p.getY(), p.getZ());
        }
        pending = null;
        if (curHalves == null) {
            for (String p : S.tracked) {
                if (title.toLowerCase().contains(p.toLowerCase())) {
                    cur = S.gui(title);
                    if (cur == null) { cur = new Stash(); cur.name = title; cur.area = "Menus"; cur.gui = true; S.stashes.add(cur); }
                    break;
                }
            }
        }
        // save contents when this screen closes (or another screen opens on top of it)
        ScreenEvents.remove(screen).register(s -> {
            if (curScreen == s && cur != null) {
                cur.items = read(cm.getContainer());
                cur.updated = System.currentTimeMillis();
                S.save();
            }
        });
    }

    static List<Item> read(net.minecraft.world.Container c) {
        Map<String, Item> m = new LinkedHashMap<>();
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack st = c.getItem(i);
            if (st.isEmpty()) continue;
            String name = ChatFormatting.stripFormatting(st.getHoverName().getString());
            name = name == null ? "" : name.trim();
            if (name.isEmpty()) continue; // Hypixel filler panes have blank names
            CustomData cd = st.get(DataComponents.CUSTOM_DATA);
            String id = cd == null ? "" : cd.copyTag().getStringOr("id", "");
            Item it = m.computeIfAbsent(id.isEmpty() ? name.toLowerCase() : id, k -> new Item());
            if (it.name.isEmpty()) { it.name = name; it.id = id; }
            it.count += st.getCount();
        }
        return new ArrayList<>(m.values());
    }

    // ---------- highlighting ----------
    static void drawGlow() {
        if (glow.isEmpty()) return;
        if (System.currentTimeMillis() > glowUntil) { glow.clear(); return; }
        for (AABB b : glow) Gizmos.cuboid(b, GizmoStyle.stroke(0xFFFFB02E)).setAlwaysOnTop();
    }

    /** @return true if the chest is in the current area and now highlighted */
    static boolean highlight(Stash s) {
        if (s.gui || !s.area.equalsIgnoreCase(S.area)) return false;
        glow.clear();
        glow.add(new AABB(new BlockPos(s.x, s.y, s.z)).inflate(0.003));
        if (s.split) glow.add(new AABB(new BlockPos(s.x2, s.y2, s.z2)).inflate(0.003));
        glowUntil = System.currentTimeMillis() + 90_000;
        return true;
    }
}
