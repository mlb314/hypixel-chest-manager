package com.skychest;

import com.skychest.ChestStore.Item;
import com.skychest.ChestStore.Stash;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

import static com.skychest.SkyChestMod.*;

/** Item search with autofill. Click a result: island chests are highlighted for 90s. */
final class SearchScreen extends Screen {
    private final Screen parent;
    private EditBox box;
    private final Button[] sug = new Button[6];
    private final Button[] res = new Button[8];
    private final Stash[] resStash = new Stash[8];
    private Button status;

    SearchScreen(Screen parent) {
        super(Component.literal("Search chests"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 130, y = height / 5;
        box = new EditBox(font, x, y, 260, 20, Component.literal("Search"));
        box.setHint(Component.literal("Search items (e.g. Hyperion, Wheat)"));
        box.setMaxLength(40);
        box.setResponder(s -> refresh());
        addRenderableWidget(box);
        setInitialFocus(box);

        for (int i = 0; i < sug.length; i++) {
            sug[i] = Button.builder(Component.empty(), b -> { box.setValue(b.getMessage().getString()); })
                    .bounds(x, y + 22 + i * 14, 260, 14).build();
            addRenderableWidget(sug[i]);
        }
        int ry = y + 22 + sug.length * 14 + 6;
        status = Button.builder(Component.literal("Type to search your tracked chests"), b -> { })
                .bounds(x, ry, 260, 16).build();
        status.active = false;
        addRenderableWidget(status);
        for (int i = 0; i < res.length; i++) {
            final int idx = i;
            res[i] = Button.builder(Component.empty(), b -> pick(idx)).bounds(x, ry + 20 + i * 18, 260, 16).build();
            addRenderableWidget(res[i]);
        }
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(x, ry + 24 + res.length * 18, 80, 16).build());
        refresh();
    }

    private static boolean matches(Item i, String q) {
        return i.name.toLowerCase().contains(q) || i.id.toLowerCase().contains(q.replace(' ', '_'));
    }

    private void refresh() {
        String q = box.getValue().trim().toLowerCase();
        for (Button b : sug) b.visible = false;
        for (int i = 0; i < res.length; i++) { res[i].visible = false; resStash[i] = null; }
        if (q.isEmpty()) return;

        // autofill: distinct item names, biggest total first
        Map<String, Integer> names = new HashMap<>();
        for (Stash s : S.stashes) for (Item i : s.items) if (matches(i, q)) names.merge(i.name, i.count, Integer::sum);
        List<String> top = names.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue()).map(Map.Entry::getKey)
                .filter(n -> !n.equalsIgnoreCase(q)).limit(sug.length).toList();
        for (int i = 0; i < top.size(); i++) { sug[i].setMessage(Component.literal(top.get(i))); sug[i].visible = true; }

        // where it is
        record Hit(Stash s, int total, String name) { }
        List<Hit> hits = new ArrayList<>();
        for (Stash s : S.stashes) {
            int total = 0; String nm = null;
            for (Item i : s.items) if (matches(i, q)) { total += i.count; if (nm == null) nm = i.name; }
            if (nm != null) hits.add(new Hit(s, total, nm));
        }
        hits.sort((a, b) -> b.total - a.total);
        status.setMessage(Component.literal(hits.isEmpty() ? "Not found in your tracked chests" : "Found in " + hits.size() + " place(s). Click one."));
        for (int i = 0; i < Math.min(hits.size(), res.length); i++) {
            Hit h = hits.get(i);
            String where = h.s.gui ? h.s.name : h.s.name + " [" + h.s.category + ", " + h.s.area + "]";
            String t = fmt(h.total) + "x " + h.name + " - " + where;
            if (t.length() > 44) t = t.substring(0, 43) + "...";
            res[i].setMessage(Component.literal(t));
            res[i].visible = true;
            resStash[i] = h.s;
        }
    }

    private void pick(int i) {
        Stash s = resStash[i];
        if (s == null) return;
        if (s.gui) {
            status.setMessage(Component.literal("In menu: " + s.name + ". Open it in-game to take it."));
        } else if (highlight(s)) {
            if (parent != null) parent.onClose(); else minecraft.setScreen(null); // close so you can see the glow
        } else {
            status.setMessage(Component.literal("That chest is on " + s.area + ". You are on " + S.area + "."));
        }
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
