package com.skychest;

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

/**
 * Layout keeps to the centre column (the inventory's own width) because mods like Firmament use the
 * left and right of the screen. The search bar sits below the window, the chest panel above it.
 * S.edgeGap leaves room for quick-nav tab strips around the window.
 */
final class ChestPanel {
    static boolean open;
    static final List<AbstractWidget> panel = new ArrayList<>();

    /** {left, top, height} of the container window (176 wide, centred). */
    private static int[] win(Screen screen, boolean chest) {
        int rows = chest ? ((ChestMenu) ((AbstractContainerScreen<?>) screen).getMenu()).getRowCount() : 0;
        int h = chest ? 114 + rows * 18 : 166;
        return new int[]{(screen.width - 176) / 2, (screen.height - h) / 2, h};
    }

    static void attach(Screen screen, boolean chest) {
        int[] g = win(screen, chest);
        SearchBar.attach(screen, g[0], g[1] + g[2] + S.edgeGap);
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
        int[] g = win(screen, true);
        int x = g[0];
        int y = g[1] - S.edgeGap - 18;   // bottom row of the panel, just above the window

        add(screen, open ? "Chest info [-]" : "Chest info [+]", x, y, 176, true, () -> open = !open);
        if (!open) return;

        // name + area
        y -= 18;
        add(screen, cur != null ? cur.name : (curHalves != null ? "Not tracked yet" : "Not a chest"), x, y, 108, false, () -> { });
        add(screen, "Area: " + S.area, x + 110, y, 66, true, () ->
            Minecraft.getInstance().setScreen(new PromptScreen(screen, "Area (island) you are on", S.area, v -> {
                S.area = v; glow.clear(); S.save();
            })));

        if (cur == null) {
            if (curHalves != null) {
                y -= 18;
                add(screen, "Track this chest", x, y, 176, true, () -> {
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

        // new preset / rename / stop tracking
        y -= 18;
        add(screen, "+ Preset", x, y, 58, true, () ->
            Minecraft.getInstance().setScreen(new PromptScreen(screen, "New preset name", "", v -> {
                if (S.presets.stream().noneMatch(s -> s.equalsIgnoreCase(v))) S.presets.add(v);
                cur.category = v; S.save();
            })));
        add(screen, "Rename", x + 59, y, 58, true, () ->
            Minecraft.getInstance().setScreen(new PromptScreen(screen, "Chest name", cur.name, v -> { cur.name = v; S.save(); })));
        add(screen, cur.gui ? "Menu" : "Untrack", x + 118, y, 58, !cur.gui, () -> {
            S.stashes.remove(cur); cur = null; S.save();
        });

        // presets: 3-column grid above; if the screen is too short, one "next preset" button instead
        int n = S.presets.size(), prow = (n + 2) / 3;
        if (y - prow * 18 < 2) {
            y -= 18;
            add(screen, "Preset: " + cur.category + " (next)", x, y, 176, true, () -> {
                int i = S.presets.indexOf(cur.category);
                cur.category = S.presets.get((i + 1) % n); S.save();
            });
            return;
        }
        y -= prow * 18;
        for (int i = 0; i < n; i++) {
            String p = S.presets.get(i);
            add(screen, (p.equals(cur.category) ? "> " : "") + p, x + (i % 3) * 59, y + (i / 3) * 18, 58, true, () -> {
                cur.category = p; S.save();
            });
        }
    }
}
