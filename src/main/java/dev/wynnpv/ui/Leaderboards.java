package dev.wynnpv.ui;

import java.util.Map;

/** Readable names for Wynncraft's leaderboards, e.g. "combatSoloLevel" becomes "Combat level". */
final class Leaderboards {
	private static final Map<String, String> RAIDS = Map.of(
		"grootslang", "Grootslangs", "colossus", "Colossus", "orphion", "Orphion", "nameless", "Anomaly",
		"fruma", "Wartorn");
	private static final Map<String, String> NAMES = Map.ofEntries(
		// "Solo" boards count one character; "global" ones all of them.
		Map.entry("combatSoloLevel", "Combat level"),
		Map.entry("combatGlobalLevel", "Combat level (all)"),
		Map.entry("professionsSoloLevel", "Professions"),
		Map.entry("professionsGlobalLevel", "Professions (all)"),
		Map.entry("totalSoloLevel", "Total level"),
		Map.entry("totalGlobalLevel", "Total level (all)"),
		Map.entry("playerContent", "Content"),
		Map.entry("globalPlayerContent", "Content (all)"),
		Map.entry("warsCompletion", "Wars"),
		Map.entry("huntedContent", "Hunted content"),
		Map.entry("craftsmanContent", "Craftsman content"),
		Map.entry("ironmanContent", "Ironman content"),
		Map.entry("ultimateIronmanContent", "Ultimate ironman"),
		Map.entry("hardcoreContent", "Hardcore content"),
		Map.entry("hardcoreLegacyLevel", "Hardcore legacy"),
		// Gamemode combinations, by the initials players use: hardcore, (ultimate) ironman, craftsman, hunted.
		Map.entry("hicContent", "HIC content"),
		Map.entry("hichContent", "HICH content"),
		Map.entry("huicContent", "HUIC content"),
		Map.entry("huichContent", "HUICH content"));

	private Leaderboards() {}

	static String name(String key) {
		String known = NAMES.get(key);
		if (known != null) {
			return known;
		}
		if (key.endsWith("Level")) {
			// Professions, e.g. "woodcuttingLevel".
			return Format.capitalize(key.substring(0, key.length() - "Level".length()));
		}
		for (Map.Entry<String, String> raid : RAIDS.entrySet()) {
			if (key.startsWith(raid.getKey())) {
				String rest = key.substring(raid.getKey().length());
				return switch (rest) {
					case "Completion" -> raid.getValue() + " clears";
					case "SrPlayers" -> raid.getValue() + " speedrun";
					case "SrGPlayers" -> raid.getValue() + " guild speedrun";
					default -> raid.getValue() + " " + words(rest);
				};
			}
		}
		return Format.capitalize(words(key));
	}

	/** "someCamelCase" becomes "some camel case". */
	private static String words(String key) {
		return key.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(java.util.Locale.ROOT);
	}
}
