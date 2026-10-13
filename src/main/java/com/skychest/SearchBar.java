package com.skychest;

import com.skychest.ChestStore.Item;
import com.skychest.ChestStore.Stash;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

import static com.skychest.SkyChestMod.*;

/**
 * A slide-out search tab on the LEFT of the inventory/chest window.
 * Click the small "<" tab to open the panel: search box, filter, sort, results, pager.
 * Type to see matching items (Tab autocompletes, Enter picks), pick one to see every place it is stored.
 * Click an island chest to highlight it for 90s.
 */
final class SearchBar {
    static final int PW = 150;                 // panel width in GUI px
    static boolean open, sortAz;
    static String query = "", chosen, notice, top;
    static int fi, page, L, T, H, maxRows = 5;
    static EditBox box;
    static Screen scr, hooked;
    static Button prev, next, pageLabel;
    static final Button[] rows = new Button[12];
    static final Runnable[] acts = new Runnable[12];
    static final List<AbstractWidget> mine = new ArrayList<>();

    record Row(String text, boolean active, Runnable act) { }

    /**
     * draw=true: the dark panel with a light frame. It is added FIRST (drawn underneath) and is inactive,
     * so it never steals clicks from the box/buttons above it.
     * draw=false: an invisible, active catcher added LAST, so clicks on empty panel space are swallowed
     * and never fall through to the container (which would drop the item on your cursor).
     */
    static final class Pane extends AbstractWidget {
        final boolean draw;

        Pane(int x, int y, int w, int h, boolean draw) {
            super(x, y, w, h, Component.empty());
            this.draw = draw;
            this.active = !draw;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            if (!draw) return;
            int x = getX(), y = getY(), x2 = x + this.width, y2 = y + this.height;
            g.fillGradient(x, y, x2, y2, 0xFF000000, 0xFF000000);
            g.fillGradient(x + 1, y + 1, x2 - 1, y2 - 1, 0xFFC6C6C6, 0xFFC6C6C6);
            g.fillGradient(x + 4, y + 4, x2 - 4, y2 - 4, 0xFF373737, 0xFF373737);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) { }
    }

    static String filter() {
        return fi <= 0 || fi > S.presets.size() ? null : S.presets.get(fi - 1);
    }

    private static <W extends AbstractWidget> W add(List<AbstractWidget> w, W x) {
        w.add(x); mine.add(x); return x;
    }

    /** left/winTop/h describe the container window (176 wide). */
    static void attach(Screen screen, int left, int winTop, int h) {
        scr = screen; L = left; T = winTop; H = h;
        rebuild(screen);

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
                        if (rows[i] != null && rows[i].visible && rows[i].active && acts[i] != null) { acts[i].run(); break; }
                    return false;
                }
                box.keyPressed(ev);
                return false;
            });
        }
    }

    static void rebuild(Screen screen) {
        List<AbstractWidget> w = Screens.getWidgets(screen);
        w.removeAll(mine); mine.clear(); box = null;

        Button tab = Button.builder(Component.literal(open ? ">" : "<"), b -> {
            open = !open;
            Minecraft.getInstance().execute(() -> rebuild(screen)); // rebuild after the click finishes
        }).bounds(L - 16, T + S.tabY, 14, 30).build();

        if (!open) { add(w, tab); return; }

        int px = Math.max(2, L - 18 - PW);
        add(w, new Pane(px, T, PW, H, true));
        add(w, tab);
        int ix = px + 5, iw = PW - 10;

        box = add(w, new EditBox(Screens.getFont(screen), ix, T + 5, iw, 16, Component.literal("Search")));
        box.setMaxLength(40);
        box.setHint(Component.literal("Search all chests"));
        box.setValue(query);
        box.setResponder(s -> { query = s; chosen = null; notice = null; page = 0; refresh(); });

        add(w, Button.builder(Component.literal("Filter: " + (filter() == null ? "All" : filter())), b -> {
            fi = (fi + 1) % (S.presets.size() + 1);
            page = 0;
            b.setMessage(Component.literal("Filter: " + (filter() == null ? "All" : filter())));
            refresh();
        }).bounds(ix, T + 23, iw, 16).build());

        add(w, Button.builder(Component.literal("Sort: " + (sortAz ? "A-Z" : "Most")), b -> {
            sortAz = !sortAz;
            page = 0;
            b.setMessage(Component.literal("Sort: " + (sortAz ? "A-Z" : "Most")));
            refresh();
        }).bounds(ix, T + 41, iw, 16).build());

        int ry = T + 61;
        maxRows = Math.max(3, Math.min(rows.length, (H - 61 - 22 - 4) / 14));
        for (int i = 0; i < rows.length; i++) {
            final int idx = i;
            rows[i] = Button.builder(Component.empty(), b -> { if (acts[idx] != null) acts[idx].run(); })
                    .bounds(ix, ry + i * 14, iw, 14).build();
            rows[i].visible = false;
            if (i < maxRows) add(w, rows[i]);
        }

        int by = T + H - 21;
        prev = add(w, Button.builder(Component.literal("<"), b -> { page--; refresh(); }).bounds(ix, by, 20, 16).build());
        pageLabel = add(w, Button.builder(Component.literal("1/1"), b -> { }).bounds(ix + 22, by, iw - 44, 16).build());
        pageLabel.active = false;
        next = add(w, Button.builder(Component.literal(">"), b -> { page++; refresh(); }).bounds(ix + iw - 20, by, 20, 16).build());
        add(w, new Pane(px, T, PW, H, false)); // last: click catcher
        refresh();
    }

    private static boolean matches(Item i, String q) {
        return i.name.toLowerCase().contains(q) || i.id.toLowerCase().contains(q.replace(' ', '_'));
    }

    private record Ent(String label, int count, Runnable act) { }

    static void refresh() {
        if (box == null) return;
        for (int i = 0; i < rows.length; i++) { if (rows[i] != null) rows[i].visible = false; acts[i] = null; }
        top = null;
        String q = query.trim().toLowerCase(), cat = filter();
        List<Row> pinned = new ArrayList<>();
        List<Ent> ents = new ArrayList<>();
        String emptyMsg = null;

        if (chosen != null) {
            pinned.add(new Row("< " + chosen, true, () -> { chosen = null; notice = null; page = 0; refresh(); }));
            if (notice != null) pinned.add(new Row(notice, false, null));
            for (Stash s : S.stashes) {
                if (cat != null && !cat.equals(s.category)) continue;
                int t = 0;
                for (Item i : s.items) if (i.name.equalsIgnoreCase(chosen)) t += i.count;
                if (t > 0) ents.add(new Ent(s.gui ? s.name : s.name + " [" + s.area + "]", t, () -> pick(s)));
            }
            if (ents.isEmpty()) emptyMsg = "Not in this filter";
        } else {
            Map<String, Integer> names = new HashMap<>();
            for (Stash s : S.stashes) {
                if (cat != null && !cat.equals(s.category)) continue;
                for (Item i : s.items) if (q.isEmpty() || matches(i, q)) names.merge(i.name, i.count, Integer::sum);
            }
            int best = -1;
            for (var e : names.entrySet()) {
                String n = e.getKey();
                if (e.getValue() > best) { best = e.getValue(); top = n; }
                ents.add(new Ent(n, e.getValue(), () -> { chosen = n; notice = null; page = 0; refresh(); }));
            }
            if (ents.isEmpty()) emptyMsg = S.stashes.isEmpty() ? "No chests tracked yet" : "Nothing found in tracked chests";
        }

        if (sortAz) ents.sort(Comparator.comparing(e -> e.label().toLowerCase()));
        else ents.sort((a, b) -> b.count() - a.count());

        List<Row> list = new ArrayList<>();
        for (Ent e : ents) list.add(new Row(fmt(e.count()) + "x " + e.label(), true, e.act()));
        if (emptyMsg != null) list.add(new Row(emptyMsg, false, null));

        int per = Math.max(1, maxRows - pinned.size());
        int pages = Math.max(1, (list.size() + per - 1) / per);
        page = Math.max(0, Math.min(page, pages - 1));

        List<Row> show = new ArrayList<>(pinned);
        show.addAll(list.subList(page * per, Math.min(list.size(), page * per + per)));
        for (int i = 0; i < Math.min(show.size(), maxRows); i++) {
            Row r = show.get(i);
            rows[i].setMessage(Component.literal(r.text()));
            rows[i].active = r.active();
            rows[i].visible = true;
            acts[i] = r.act();
        }
        pageLabel.setMessage(Component.literal((page + 1) + "/" + pages));
        prev.active = page > 0;
        next.active = page < pages - 1;
    }

    static void pick(Stash s) {
        if (s.gui) notice = "In menu: " + s.name + " (open it in game)";
        else if (highlight(s)) { if (scr != null) scr.onClose(); return; } // close so the glow is visible
        else notice = "On " + s.area + ", you are on " + S.area;
        refresh();
    }
}
