# Wynn PV

Look up any Wynncraft player's profile without leaving the game, pinned up on a town quest board.

- Type **/pv &lt;player&gt;** to open their profile, or just **/pv** for your own.
- See their rank, guild, playtime, when they joined and when they were last online.
- Totals for quests, dungeons, raids, wars, world events, lootruns and caves.
- All their characters, with class, level, quests and gamemodes like hardcore or ironman.
- Click a character to see its skill points, professions, dungeons, raids and every quest done.
- Their ability tree, drawn like the one in game; hover an ability to read what it does.

Players can hide parts of their profile on wynncraft.com; hidden parts show as hidden.

For Minecraft 1.21.11 with Fabric.

## What it downloads
Wynn PV needs the internet to look players up. It only downloads; it doesn't send anything about
you or your game.

- Player profiles, characters, guilds and ability trees from the
  [Wynncraft API](https://docs.wynncraft.com) (`api.wynncraft.com`).

A profile is kept for a couple of minutes, so opening it again is instant.

## License
LGPL-3.0, see [LICENSE](LICENSE). To build it yourself, run `./gradlew build` (needs Java 25).
