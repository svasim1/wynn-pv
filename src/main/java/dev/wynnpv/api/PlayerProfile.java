package dev.wynnpv.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * A player as returned by {@code /v3/player/{player}?fullResult}. Anything the player has hidden
 * with Wynncraft's access rules comes back missing or null, so most fields are nullable; see
 * {@link #restricted}.
 */
public record PlayerProfile(
	String username,
	String uuid,
	@Nullable String rank,
	@Nullable String supportRank,
	boolean online,
	@Nullable String server,
	@Nullable String firstJoin,
	@Nullable String lastJoin,
	@Nullable Double playtimeHours,
	@Nullable Guild guild,
	@Nullable Global global,
	List<Character> characters,
	Map<String, Boolean> restrictions,
	@Nullable String nickname,
	boolean veteran,
	@Nullable String activeCharacter,
	Map<String, Integer> ranking,
	Map<String, Integer> previousRanking,
	List<String> guildHistory) {

	public record Guild(@Nullable String uuid, String name, String prefix, @Nullable String rank) {}

	/** Combat totals over every raid run. */
	public record RaidStats(long damageDealt, long damageTaken, long healthHealed, int deaths, int buffsTaken, int gambitsUsed) {}

	/** Totals over all characters ({@code globalData}). */
	public record Global(int totalLevel, int completedQuests, int contentCompletion, int dungeons, int raids,
		int guildRaids, int wars, int mobsKilled, int chestsFound, int worldEvents, int lootruns, int caves,
		int pvpKills, int pvpDeaths, Map<String, Integer> dungeonList, Map<String, Integer> raidList,
		Map<String, Integer> guildRaidList, @Nullable RaidStats raidStats) {}

	/**
	 * One character. Stats the player removed from their character page are missing from
	 * {@link #stats}; build data (skill points) is null when hidden by an access rule.
	 */
	public record Character(String uuid, String type, @Nullable String reskin, @Nullable String nickname,
		int level, long xp, int xpPercent, int totalLevel, List<String> gamemodes, boolean preEconomy, @Nullable Double playtimeHours,
		Map<String, Integer> stats, @Nullable Map<String, Integer> skillPoints, Map<String, Profession> professions,
		@Nullable Map<String, Integer> dungeons, @Nullable Map<String, Integer> raids, @Nullable List<String> quests) {

		/** The class as players call it, e.g. "Ninja" for a reskinned Assassin. */
		public String className() {
			return capitalize(reskin != null ? reskin : type);
		}

		/** The class the ability tree belongs to, e.g. "assassin" for a Ninja. */
		public String treeName() {
			return type.toLowerCase(java.util.Locale.ROOT);
		}

		public @Nullable Integer stat(String key) {
			return stats.get(key);
		}
	}

	public record Profession(int level, int xpPercent) {}

	/** Plain number stats of a character, in the order they are shown. */
	public static final List<String> CHARACTER_STATS = List.of("contentCompletion", "mobsKilled", "chestsFound",
		"discoveries", "deaths", "logins", "wars", "worldEvents", "lootruns", "caves", "itemsIdentified");

	/** True when the player hides the field group behind this access rule, e.g. "characterDataAccess". */
	public boolean restricted(String rule) {
		return restrictions.getOrDefault(rule, false);
	}

	public static PlayerProfile parse(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		return new PlayerProfile(
			text(root, "username"),
			text(root, "uuid"),
			text(root, "rank"),
			text(root, "supportRank"),
			bool(root, "online"),
			text(root, "server"),
			text(root, "firstJoin"),
			text(root, "lastJoin"),
			decimal(root, "playtime"),
			parseGuild(object(root, "guild")),
			parseGlobal(object(root, "globalData")),
			parseCharacters(object(root, "characters")),
			parseRestrictions(object(root, "restrictions")),
			text(root, "nickname"),
			bool(root, "veteran"),
			text(root, "activeCharacter"),
			ranks(object(root, "ranking")),
			ranks(object(root, "previousRanking")),
			strings(root.get("guildHistory")));
	}

	/** Leaderboard name to position, e.g. "combatSoloLevel" to 4. */
	private static Map<String, Integer> ranks(@Nullable JsonObject ranking) {
		Map<String, Integer> counts = counts(ranking);
		return counts == null ? Map.of() : Map.copyOf(counts);
	}

	private static @Nullable Guild parseGuild(@Nullable JsonObject guild) {
		if (guild == null || text(guild, "name") == null) {
			return null;
		}
		return new Guild(text(guild, "uuid"), text(guild, "name"), text(guild, "prefix"), text(guild, "rank"));
	}

	private static @Nullable Global parseGlobal(@Nullable JsonObject global) {
		if (global == null) {
			return null;
		}
		JsonObject pvp = object(global, "pvp");
		Map<String, Integer> dungeons = listCounts(object(global, "dungeons"));
		Map<String, Integer> raids = listCounts(object(global, "raids"));
		Map<String, Integer> guildRaids = listCounts(object(global, "guildRaids"));
		return new Global(
			integer(global, "totalLevel", 0),
			integer(global, "completedQuests", 0),
			integer(global, "contentCompletion", 0),
			total(global, "dungeons"),
			total(global, "raids"),
			total(global, "guildRaids"),
			integer(global, "wars", 0),
			integer(global, "mobsKilled", 0),
			integer(global, "chestsFound", 0),
			integer(global, "worldEvents", 0),
			integer(global, "lootruns", 0),
			integer(global, "caves", 0),
			pvp == null ? 0 : integer(pvp, "kills", 0),
			pvp == null ? 0 : integer(pvp, "deaths", 0),
			dungeons == null ? Map.of() : dungeons,
			raids == null ? Map.of() : raids,
			guildRaids == null ? Map.of() : guildRaids,
			raidStats(object(global, "raidStats")));
	}

	private static @Nullable RaidStats raidStats(@Nullable JsonObject stats) {
		if (stats == null) {
			return null;
		}
		return new RaidStats(whole(stats, "damageDealt"), whole(stats, "damageTaken"), whole(stats, "healthHealed"),
			integer(stats, "deaths", 0), integer(stats, "buffsTaken", 0), integer(stats, "gambitsUsed", 0));
	}

	private static long whole(JsonObject obj, String key) {
		JsonElement value = obj.get(key);
		return value == null || value.isJsonNull() ? 0 : value.getAsLong();
	}

	private static List<Character> parseCharacters(@Nullable JsonObject characters) {
		if (characters == null) {
			return List.of();
		}
		List<Character> result = new ArrayList<>();
		for (Map.Entry<String, JsonElement> entry : characters.entrySet()) {
			if (!(entry.getValue() instanceof JsonObject c)) {
				continue;
			}
			result.add(parseCharacter(entry.getKey(), c));
		}
		// Highest total level first, like the in-game character selector sorts by progress.
		result.sort(Comparator.comparingInt(Character::totalLevel).reversed());
		return List.copyOf(result);
	}

	private static Character parseCharacter(String uuid, JsonObject c) {
		Map<String, Integer> stats = new LinkedHashMap<>();
		for (String key : CHARACTER_STATS) {
			if (c.get(key) != null && c.get(key).isJsonPrimitive()) {
				stats.put(key, c.get(key).getAsInt());
			}
		}
		JsonObject pvp = object(c, "pvp");
		if (pvp != null) {
			stats.put("pvpKills", integer(pvp, "kills", 0));
			stats.put("pvpDeaths", integer(pvp, "deaths", 0));
		}

		Map<String, Profession> professions = new LinkedHashMap<>();
		JsonObject professionData = object(c, "professions");
		if (professionData != null) {
			for (Map.Entry<String, JsonElement> entry : professionData.entrySet()) {
				if (entry.getValue() instanceof JsonObject profession) {
					professions.put(entry.getKey(), new Profession(integer(profession, "level", 1), integer(profession, "xpPercent", 0)));
				}
			}
		}

		List<String> quests = null;
		if (c.get("quests") instanceof JsonArray array) {
			quests = new ArrayList<>();
			for (JsonElement quest : array) {
				quests.add(quest.getAsString());
			}
			quests = List.copyOf(quests);
		}

		return new Character(uuid, text(c, "type"), text(c, "reskin"), text(c, "nickname"),
			integer(c, "level", 0), whole(c, "xp"), integer(c, "xpPercent", 0), integer(c, "totalLevel", 0), strings(c.get("gamemode")),
			bool(c, "preEconomy"), decimal(c, "playtime"), Map.copyOf(stats), counts(object(c, "skillPoints")), Map.copyOf(professions),
			listCounts(object(c, "dungeons")), listCounts(object(c, "raids")), quests);
	}

	private static List<String> strings(@Nullable JsonElement element) {
		List<String> result = new ArrayList<>();
		if (element instanceof JsonArray array) {
			array.forEach(value -> result.add(value.getAsString()));
		}
		return List.copyOf(result);
	}

	private static @Nullable Map<String, Integer> counts(@Nullable JsonObject obj) {
		if (obj == null) {
			return null;
		}
		Map<String, Integer> result = new LinkedHashMap<>();
		obj.entrySet().forEach(e -> result.put(e.getKey(), e.getValue().getAsInt()));
		return result;
	}

	/** Dungeons and raids per character: {@code {"total": n, "list": {"name": count}}}. */
	private static @Nullable Map<String, Integer> listCounts(@Nullable JsonObject obj) {
		return obj == null ? null : counts(object(obj, "list"));
	}

	private static Map<String, Boolean> parseRestrictions(@Nullable JsonObject restrictions) {
		Map<String, Boolean> result = new TreeMap<>();
		if (restrictions != null) {
			restrictions.entrySet().forEach(e -> result.put(e.getKey(), e.getValue().getAsBoolean()));
		}
		return result;
	}

	// Reskinned classes whose API name is not just the name in capitals.
	private static final Map<String, String> CLASS_NAMES = Map.of("DARKWIZARD", "Dark Wizard");

	static String capitalize(String text) {
		String known = CLASS_NAMES.get(text);
		if (known != null) {
			return known;
		}
		return text.isEmpty() ? text : java.lang.Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase(java.util.Locale.ROOT);
	}

	private static @Nullable JsonObject object(JsonObject obj, String key) {
		return obj.get(key) instanceof JsonObject value ? value : null;
	}

	private static @Nullable String text(JsonObject obj, String key) {
		JsonElement value = obj.get(key);
		return value == null || value.isJsonNull() ? null : value.getAsString();
	}

	private static boolean bool(JsonObject obj, String key) {
		JsonElement value = obj.get(key);
		return value != null && !value.isJsonNull() && value.getAsBoolean();
	}

	private static @Nullable Double decimal(JsonObject obj, String key) {
		JsonElement value = obj.get(key);
		return value == null || value.isJsonNull() ? null : value.getAsDouble();
	}

	private static int integer(JsonObject obj, String key, int fallback) {
		JsonElement value = obj.get(key);
		return value == null || value.isJsonNull() ? fallback : value.getAsInt();
	}

	/** Dungeons and raids come as {@code {"total": n, "list": {...}}}. */
	private static int total(JsonObject obj, String key) {
		JsonObject counts = object(obj, key);
		return counts == null ? 0 : integer(counts, "total", 0);
	}
}
