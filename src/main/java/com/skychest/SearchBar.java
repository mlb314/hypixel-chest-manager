package com.skychest;

import com.skychest.ChestStore.Item;
import com.skychest.ChestStore.Stash;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

import static com.skychest.SkyChestMod.*;

/**
 * Inline search bar under the inventory (same idea as Firmament's bar).
 * Type to see matching items (Tab autocompletes, Enter picks the top one), pick an item to see every
 * place it is stored. Click an island chest to highlight it for 90s. The button on the right
 * cycles a preset filter (All, Farming, Mining...).
 */
final class SearchBar {
    static String query = "", chosen, notice, top;
    static int fi;
    static EditBox box;
    static Screen scr, hooked;
    static int maxRows = 5;
    static final Button[] rows = new Button[8];
    static final Runnable[] acts = new Runnable[8];

    record Row(String text, boolean active, Runnable act) { }

    static String filter() {
        return fi <= 0 || fi > S.presets.size() ? null : S.presets.get(fi - 1);
    }

    static void attach(Screen screen, int x, int y) {
        scr = screen;
        List<AbstractWidget> w = Screens.getWidgets(screen);
        box = new EditBox(Screens.getFont(screen), x, y, 118, 16, Component.literal("Search"));
        box.setMaxLength(40);
        box.setHint(Component.literal("Search all chests"));
        box.setValue(query);
        box.setResponder(s -> { query = s; chosen = null; notice = null; refresh(); });
        w.add(box);

        Button fb = Button.builder(Component.literal(filter() == null ? "All" : filter()), b -> {
            fi = (fi + 1) % (S.presets.size() + 1);
            b.setMessage(Component.literal(filter() == null ? "All" : filter()));
            refresh();
        }).bounds(x + 120, y, 56, 16).build();
        w.add(fb);

        maxRows = Math.max(2, Math.min(rows.length, (screen.height - (y + 18) - 24) / 14));
        for (int i = 0; i < rows.length; i++) {
            final int idx = i;
            rows[i] = Button.builder(Component.empty(), b -> { if (acts[idx] != null) acts[idx].run(); })
                    .bounds(x, y + 18 + i * 14, 176, 14).build();
            rows[i].visible = false;
            if (i < maxRows) w.add(rows[i]);
        }

        // Without this, typing "e" would close the inventory. Keys go to our box while it is focused.
        if (hooked != screen) {
            hooked = screen;
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, ev) -> {
                if (box == null || !box.isFocused()) return true;
                int k = ev.key();
                if (k == 256) return true;                              // Esc: normal behaviour
                if (k == 258) {                                         // Tab: autocomplete
                    if (top != null && chosen == null) box.setValue(top);
                    return false;
                }
                if (k == 257 || k == 335) {                             // Enter: pick first row
                    for (int i = 0; i < maxRows; i++)
                        if (rows[i].visible && rows[i].active && acts[i] != null) { acts[i].run(); break; }
                    return false;
                }
                box.keyPressed(ev);
                return false;
            });
        }
        refresh();
    }

    private static boolean matches(Item i, String q) {
        return i.name.toLowerCase().contains(q) || i.id.toLowerCase().contains(q.replace(' ', '_'));
    }

    static void refresh() {
        for (int i = 0; i < rows.length; i++) { if (rows[i] != null) rows[i].visible = false; acts[i] = null; }
        top = null;
        String q = query.trim().toLowerCase(), cat = filter();
        List<Row> list = new ArrayList<>();

        if (chosen != null) {
            list.add(new Row("< " + chosen, true, () -> { chosen = null; notice = null; refresh(); }));
            if (notice != null) list.add(new Row(notice, false, null));
            Map<Stash, Integer> tot = new HashMap<>();
            for (Stash s : S.stashes) {
                if (cat != null && !cat.equals(s.category)) continue;
                int t = 0;
                for (Item i : s.items) if (i.name.equalsIgnoreCase(chosen)) t += i.count;
                if (t > 0) tot.put(s, t);
            }
            List<Stash> where = new ArrayList<>(tot.keySet());
            where.sort((a, b) -> tot.get(b) - tot.get(a));
            for (Stash s : where)
                list.add(new Row(fmt(tot.get(s)) + "x " + (s.gui ? s.name : s.name + " [" + s.area + "]"), true, () -> pick(s)));
            if (where.isEmpty()) list.add(new Row("Not in this filter", false, null));
        } else if (!q.isEmpty()) {
            Map<String, Integer> names = new HashMap<>();
            for (Stash s : S.stashes) {
                if (cat != null && !cat.equals(s.category)) continue;
                for (Item i : s.items) if (matches(i, q)) names.merge(i.name, i.count, Integer::sum);
            }
            List<Map.Entry<String, Integer>> es = new ArrayList<>(names.entrySet());
            es.sort((a, b) -> b.getValue() - a.getValue());
            for (var e : es) {
                String n = e.getKey();
                if (top == null) top = n;
                list.add(new Row(fmt(e.getValue()) + "x " + n, true, () -> { chosen = n; notice = null; refresh(); }));
            }
            if (es.isEmpty()) list.add(new Row("Nothing found in tracked chests", false, null));
        } else {
            list.add(new Row("Search every tracked chest (Tab completes)", false, null));
        }

        for (int i = 0; i < Math.min(list.size(), maxRows); i++) {
            Row r = list.get(i);
            rows[i].setMessage(Component.literal(r.text()));
            rows[i].active = r.active();
            rows[i].visible = true;
            acts[i] = r.act();
        }
    }

    static void pick(Stash s) {
        if (s.gui) notice = "In menu: " + s.name + " (open it in game)";
        else if (highlight(s)) { if (scr != null) scr.onClose(); return; } // close so the glow is visible
        else notice = "On " + s.area + ", you are on " + S.area;
        refresh();
    }
}
