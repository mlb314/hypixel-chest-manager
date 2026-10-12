# Hypixel Chest Manager

Client-side Fabric mod for Minecraft **26.1.2**. Remembers what is in your Hypixel SkyBlock chests, sorts them into presets (Farming, Mining, Minions...), and lets you search for any item. Nothing is sent to the server and it only works on `hypixel.net`.

## In game
**Chest info panel.** Open any chest or barrel. A *Chest info [+]* button sits at the top right of the chest window.
- **Track this chest** starts remembering it (contents are saved when you close it, and shown live in the panel).
- Pick a **preset** with one click, or **+ New preset** to make your own (it is assigned to this chest straight away).
- **Rename**, **Area**, and **Stop tracking** are in the panel too.

**Search.** A *Search* button sits at the bottom left of your inventory (also under chest windows). Type an item name: matching item names autofill, and the places that hold it are listed with amount, preset and area.
- Island chest in your current area: click it and the chest is highlighted through walls for 90 seconds.
- Ender Chest / Backpack: listed as "in menu"; they are remembered automatically whenever you close them.

**Area.** Same coordinates exist on every island, so chests are filed by area. The mod reads `Area: ...` from your tab list; if your tab list does not show it, click the **Area** button in the panel and type it.

Data is in `config/hypixel-chest-manager.json`. You can edit the `presets` and `tracked` (menu titles to auto-remember, matched by "contains") lists there.

## Build (no tools needed on your PC)
1. Upload this folder to a new GitHub repo.
2. Open the repo's **Actions** tab. The `build` workflow runs automatically (or press *Run workflow*).
3. When it turns green, open the **Releases** page (right-hand side of the repo's front page) and download the jar from **Latest build**. It is replaced on every new build.
   (The same jar is also attached to the run itself under *Artifacts*.)
4. Put it in your `mods` folder with Fabric Loader 0.19.1+ and Fabric API 0.145.4+26.1.2.

If the run is red, open it and copy the error text; that is all that is needed to fix it.

Local build (optional): JDK 25, then `./gradlew build`.

## If it fails to compile (26.1 is new)
- `Screens.getWidgets(screen)` in `ChestPanel` (adds widgets to existing screens)
- `Gizmos`/`GizmoStyle` and `LevelRenderEvents.END_EXTRACTION` in `SkyChestMod` (highlight; try `DEBUG_RENDER`)
- `CompoundTag.getStringOr`, `CustomData.copyTag` (SkyBlock id)
- `PlayerInfo.getTabListDisplayName`, `getListedOnlinePlayers` (area detection)
