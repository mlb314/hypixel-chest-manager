# Hypixel Chest Manager

Client-side Fabric mod for Minecraft **26.1.2**. Remembers what is in your Hypixel SkyBlock chests, sorts them into presets (Farming, Mining, Minions...), and searches every chest for an item. Nothing is sent to the server and it only works on `hypixel.net`.

## Layout
Designed to share the screen with Firmament-style item lists: everything stays in the centre column (the width of the inventory). Nothing is drawn at the far left or right.
- **Search bar:** below the inventory/chest window, with the results list under it.
- **Chest info panel:** above the chest window.

## In game
**Chest info panel** (chest windows only). Press **Chest info [+]**.
- **Track this chest** starts remembering it; contents are saved when you close it.
- Click a **preset** to assign it, **+ Preset** to make your own, **Rename**, **Untrack**, or set the **Area**.

**Search bar** (inventory and chest windows). Click the box and type.
- Matching item names appear with total amounts. **Tab** completes the top one, **Enter** picks it, or click one.
- Then you see every place it is stored. Island chest in your current area: click it and the chest glows through walls for 90s. Ender Chest / Backpack are listed as "in menu".
- The button on the right cycles a filter: All, Farming, Mining...
- Click `< item` to go back.

**Area.** The same coordinates exist on every island, so chests are filed by area. The mod reads `Area: ...` from your tab list; otherwise click **Area** in the panel and type it.

## Settings
`config/hypixel-chest-manager.json` after first run:
- `presets`: your preset names
- `tracked`: menu titles to remember automatically (matched by "contains"), default `Ender Chest`, `Backpack`
- `edgeGap`: GUI pixels kept clear above/below the window for tab strips (default 28). Raise it if the bar overlaps something.

## Build (no tools needed on your PC)
1. Upload this folder to a GitHub repo.
2. The `build` workflow runs on the **Actions** tab and publishes **Latest build** on the **Releases** page.
3. Download the jar and put it in `mods` with Fabric Loader 0.19.1+ and Fabric API 0.145.4+26.1.2.

If the run is red, copy the error text from it.
