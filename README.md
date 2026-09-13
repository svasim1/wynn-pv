# Wynn PV

Look up any Wynncraft player's profile without leaving the game.

- Type **/pv &lt;player&gt;** to open their profile, or just **/pv** for your own.
- See their rank, guild, playtime, when they joined and when they were last online.
- Totals for quests, dungeons, raids, wars, world events, lootruns and caves.
- All their characters, with class, level, quests and gamemodes like hardcore or ironman.

Players can hide parts of their profile on wynncraft.com; hidden parts show as hidden.

For Minecraft 1.21.11 with Fabric.

## What it downloads
Wynn PV only downloads; it doesn't send anything about you or your game. Profiles come from the
[Wynncraft API](https://docs.wynncraft.com) (`api.wynncraft.com`), which allows 50 lookups a
minute, so a profile is reused for two minutes before it is asked for again.

## License
LGPL-3.0, see [LICENSE](LICENSE). To build it yourself, run `./gradlew build` (needs Java 25).
