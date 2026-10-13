package com.skychest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Everything the mod remembers. Saved as config/hypixel-chest-manager.json. */
public final class ChestStore {
    public static final class Item {
        public String id = "";    // SkyBlock internal id, e.g. HYPERION (empty for vanilla items)
        public String name = "";
        public int count;
    }

    /** A chest/barrel in the world (gui=false) or a menu such as Ender Chest (gui=true). */
    public static final class Stash {
        public String name = "";
        public String area = "";
        public String category = "Unsorted"; // preset such as Farming, Mining, Minions
        public boolean gui;
        public int x, y, z;
        public boolean split;     // double chest: second half
        public int x2, y2, z2;
        public long updated;
        public List<Item> items = new ArrayList<>();
    }

    public List<Stash> stashes = new ArrayList<>();
    public List<String> tracked = new ArrayList<>(List.of("Ender Chest", "Backpack")); // menu titles (contains) to remember automatically
    public List<String> presets = new ArrayList<>(List.of("Farming", "Mining", "Minions", "Slayers", "Dragons", "Fishing", "Foraging", "Combat", "Misc"));
    public String area = "Private Island";
    public int edgeGap = 28;                           // GUI px kept free above/below the inventory for other mods' tab strips
    public int tabY = 36;                              // how far down the search tab sits from the window top (GUI px)
    public boolean anyServer;                          // debug: work outside Hypixel

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("hypixel-chest-manager.json");
    }

    public static ChestStore load() {
        try {
            if (Files.exists(file())) {
                ChestStore s = GSON.fromJson(Files.readString(file()), ChestStore.class);
                if (s != null) return s;
            }
        } catch (Exception ignored) { }
        return new ChestStore();
    }

    public void save() {
        try { Files.writeString(file(), GSON.toJson(this)); } catch (Exception ignored) { }
    }

    public Stash byName(String n) {
        for (Stash s : stashes) if (s.name.equalsIgnoreCase(n.trim())) return s;
        return null;
    }

    public Stash at(String area, int x, int y, int z) {
        for (Stash s : stashes)
            if (!s.gui && s.area.equalsIgnoreCase(area) && s.x == x && s.y == y && s.z == z) return s;
        return null;
    }

    public Stash gui(String title) {
        for (Stash s : stashes) if (s.gui && s.name.equalsIgnoreCase(title)) return s;
        return null;
    }
}
