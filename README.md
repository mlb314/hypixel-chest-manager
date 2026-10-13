# Hypixel Chest Manager

Client-side Fabric mod for Minecraft **26.1.2**. Remembers what is in your Hypixel SkyBlock chests, sorts them into presets (Farming, Mining, Minions...), and searches every chest for an item. Nothing is sent to the server and it only works on `hypixel.net`.

## Layout
- **Search tab:** a small `<` tab on the left edge of the inventory/chest window. Click it and a panel slides out to the left (click `>` to close). If Firmament's favourites are showing on the left, they may draw over this panel; hide them while you search.
- **Chest info panel:** above the chest window.

## In game
**First, give it something to search.** Open a chest, press **Chest info [+]** above the window, then **Track this chest**. Open your Ender Chest and backpacks once and close them. The search only knows about chests you have tracked.

**Search tab** (inventory and chest windows). Open it; with an empty box it lists everything tracked. Click the box and type to narrow it down.
- Matching item names appear with total amounts. **Tab** completes the top one, **Enter** picks it, or click one.
- Then you see every place it is stored. Island chest in your current area: click it and the chest glows through walls for 90s. Ender Chest / Backpack are listed as "in menu".
- **Filter** cycles a preset filter (All, Farming, Mining...). **Sort** switches Most / A-Z. `<` `>` at the bottom page through results.
- Click `< item` to go back.

**Chest info panel** (chest windows only). Press **Chest info [+]**.
- **Track this chest** starts remembering it; contents are saved when you close it.
- Click a **preset** to assign it, **+ Preset** to make your own, **Rename**, **Untrack**, or set the **Area**.

**Area.** The same coordinates exist on every island, so chests are filed by area. The mod reads `Area: ...` from your tab list; otherwise click **Area** in the panel and type it.

## Settings
`config/hypixel-chest-manager.json` after first run:
- `presets`: your preset names
- `tracked`: menu titles to remember automatically (matched by "contains"), default `Ender Chest`, `Backpack`
- `tabY`: how far down (GUI pixels) the search tab sits from the top of the window (default 36). Raise it if it overlaps another mod's tab.
- `edgeGap`: GUI pixels kept clear above the window for the chest info panel (default 28, room for quick-nav tab strips). Raise it if the panel overlaps something.

## Build (no tools needed on your PC)
1. Upload this folder to a GitHub repo.
2. The `build` workflow runs on the **Actions** tab and publishes **Latest build** on the **Releases** page.
3. Download the jar and put it in `mods` with Fabric Loader 0.19.1+ and Fabric API 0.145.4+26.1.2.

If the run is red, copy the error text from it.
