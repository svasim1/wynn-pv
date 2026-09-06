package dev.wynnpv.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Comparator;
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
	Map<String, Boolean> restrictions) {

	public record Guild(String name, String prefix, @Nullable String rank) {}

	/** Totals over all characters ({@code globalData}). */
	public record Global(int totalLevel, int completedQuests, int dungeons, int raids, int wars,
		int mobsKilled, int chestsFound, int worldEvents, int lootruns, int caves) {}

	public record Character(String uuid, String type, @Nullable String reskin, @Nullable String nickname,
		int level, int totalLevel, List<String> gamemodes, @Nullable Double playtimeHours, @Nullable Integer deaths,
		@Nullable Integer completedQuests) {

		/** The class as players call it, e.g. "Ninja" for a reskinned Assassin. */
		public String className() {
			return capitalize(reskin != null ? reskin : type);
		}
	}

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
			parseRestrictions(object(root, "restrictions")));
	}

	private static @Nullable Guild parseGuild(@Nullable JsonObject guild) {
		if (guild == null || text(guild, "name") == null) {
			return null;
		}
		return new Guild(text(guild, "name"), text(guild, "prefix"), text(guild, "rank"));
	}

	private static @Nullable Global parseGlobal(@Nullable JsonObject global) {
		if (global == null) {
			return null;
		}
		return new Global(
			integer(global, "totalLevel", 0),
			integer(global, "completedQuests", 0),
			total(global, "dungeons"),
			total(global, "raids"),
			integer(global, "wars", 0),
			integer(global, "mobsKilled", 0),
			integer(global, "chestsFound", 0),
			integer(global, "worldEvents", 0),
			integer(global, "lootruns", 0),
			integer(global, "caves", 0));
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
			List<String> gamemodes = new ArrayList<>();
			if (c.get("gamemode") != null && c.get("gamemode").isJsonArray()) {
				c.getAsJsonArray("gamemode").forEach(mode -> gamemodes.add(mode.getAsString()));
			}
			Integer quests = c.get("quests") != null && c.get("quests").isJsonArray() ? c.getAsJsonArray("quests").size() : null;
			result.add(new Character(entry.getKey(), text(c, "type"), text(c, "reskin"), text(c, "nickname"),
				integer(c, "level", 0), integer(c, "totalLevel", 0), List.copyOf(gamemodes), decimal(c, "playtime"),
				c.has("deaths") && !c.get("deaths").isJsonNull() ? c.get("deaths").getAsInt() : null, quests));
		}
		// Highest total level first, like the in-game character selector sorts by progress.
		result.sort(Comparator.comparingInt(Character::totalLevel).reversed());
		return List.copyOf(result);
	}

	private static Map<String, Boolean> parseRestrictions(@Nullable JsonObject restrictions) {
		Map<String, Boolean> result = new TreeMap<>();
		if (restrictions != null) {
			restrictions.entrySet().forEach(e -> result.put(e.getKey(), e.getValue().getAsBoolean()));
		}
		return result;
	}

	static String capitalize(String text) {
		return text.isEmpty() ? text : text.charAt(0) + text.substring(1).toLowerCase(java.util.Locale.ROOT);
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
