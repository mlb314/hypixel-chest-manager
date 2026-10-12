package com.skychest;

import com.skychest.ChestStore.Item;
import com.skychest.ChestStore.Stash;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;

import java.util.ArrayList;
import java.util.List;

import static com.skychest.SkyChestMod.*;

/** Search button (bottom-left of the inventory) and the "Chest info" side panel. Vanilla widgets only. */
final class ChestPanel {
    static boolean open;
    static final List<AbstractWidget> panel = new ArrayList<>();

    static void attach(Screen screen, boolean chest) {
        int rows = chest ? ((ChestMenu) ((AbstractContainerScreen<?>) screen).getMenu()).getRowCount() : 0;
        int imgH = chest ? 114 + rows * 18 : 166;
        int left = (screen.width - 176) / 2, top = (screen.height - imgH) / 2;
        Button search = Button.builder(Component.literal("Search"),
                b -> Minecraft.getInstance().setScreen(new SearchScreen(screen)))
                .bounds(left, top + imgH + 4, 52, 16).build();
        Screens.getWidgets(screen).add(search);
        if (chest) build(screen);
    }

    private static Button add(Screen screen, String text, int x, int y, int w, boolean enabled, Runnable r) {
        Button b = Button.builder(Component.literal(text), btn -> {
            r.run();
            Minecraft.getInstance().execute(() -> build(screen)); // rebuild after the click finishes
        }).bounds(x, y, w, 16).build();
        b.active = enabled;
        Screens.getWidgets(screen).add(b);
        panel.add(b);
        return b;
    }

    static void build(Screen screen) {
        Screens.getWidgets(screen).removeAll(panel);
        panel.clear();
        ChestMenu cm = (ChestMenu) ((AbstractContainerScreen<?>) screen).getMenu();
        int imgH = 114 + cm.getRowCount() * 18;
        int x = Math.min((screen.width - 176) / 2 + 176 + 6, screen.width - 126);
        int y = (screen.height - imgH) / 2;

        add(screen, open ? "Chest info [-]" : "Chest info [+]", x, y, 120, true, () -> open = !open);
        if (!open) return;
        y += 18;

        add(screen, cur != null ? cur.name : (curHalves != null ? "Not tracked yet" : "Not a chest"), x, y, 120, false, () -> { });
        y += 18;
        add(screen, "Area: " + S.area, x, y, 120, true, () ->
            Minecraft.getInstance().setScreen(new PromptScreen(screen, "Area (island) you are on", S.area, v -> {
                S.area = v; glow.clear(); S.save();
            })));
        y += 18;

        if (cur == null) {
            if (curHalves != null) {
                add(screen, "Track this chest", x, y, 120, true, () -> {
                    BlockPos p = curHalves.get(0);
                    Stash st = new Stash();
                    st.name = "Chest " + p.getX() + " " + p.getY() + " " + p.getZ();
                    st.area = S.area; st.x = p.getX(); st.y = p.getY(); st.z = p.getZ();
                    st.split = curHalves.size() == 2;
                    if (st.split) { BlockPos q = curHalves.get(1); st.x2 = q.getX(); st.y2 = q.getY(); st.z2 = q.getZ(); }
                    st.items = read(cm.getContainer());
                    st.updated = System.currentTimeMillis();
                    S.stashes.add(st); cur = st; S.save();
                });
            }
            return;
        }

        add(screen, "Rename", x, y, 120, true, () ->
            Minecraft.getInstance().setScreen(new PromptScreen(screen, "Chest name", cur.name, v -> { cur.name = v; S.save(); })));
        y += 18;

        add(screen, "Preset: " + cur.category, x, y, 120, false, () -> { });
        y += 18;
        for (int i = 0; i < S.presets.size(); i++) {
            String p = S.presets.get(i);
            add(screen, (p.equals(cur.category) ? "> " : "") + p, x + (i % 2) * 61, y + (i / 2) * 18, 59, true, () -> {
                cur.category = p; S.save();
            });
        }
        y += ((S.presets.size() + 1) / 2) * 18;
        add(screen, "+ New preset", x, y, 120, true, () ->
            Minecraft.getInstance().setScreen(new PromptScreen(screen, "New preset name", "", v -> {
                if (S.presets.stream().noneMatch(s -> s.equalsIgnoreCase(v))) S.presets.add(v);
                cur.category = v; S.save();
            })));
        y += 18;

        // live view of what is inside right now (top few items)
        List<Item> now = read(cm.getContainer());
        now.sort((a, b) -> b.count - a.count);
        add(screen, "Inside: " + now.size() + " item types", x, y, 120, false, () -> { });
        y += 18;
        for (Item it : now.stream().limit(4).toList()) {
            add(screen, fmt(it.count) + "x " + it.name, x, y, 120, false, () -> { });
            y += 18;
        }
        if (!cur.gui) {
            add(screen, "Stop tracking", x, y, 120, true, () -> { S.stashes.remove(cur); cur = null; S.save(); });
        }
    }
}
