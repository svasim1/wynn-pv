package dev.wynnpv.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** A guild as returned by {@code /v3/guild/uuid/{uuid}}, with its members by player UUID. */
public record GuildInfo(String uuid, String name, String prefix, int level, int xpPercent, int territories, int wars,
	int raids, @Nullable String created, int memberCount, int online, Map<String, Member> members) {

	/** One member: their guild rank, when they joined and how much XP they gave the guild. */
	public record Member(String rank, @Nullable String joined, long contributed, int contributionRank) {}

	public @Nullable Member member(String playerUuid) {
		return members.get(playerUuid);
	}

	public static GuildInfo parse(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		Map<String, Member> members = new HashMap<>();
		int memberCount = 0;
		if (root.get("members") instanceof JsonObject ranks) {
			for (Map.Entry<String, JsonElement> rank : ranks.entrySet()) {
				if (rank.getKey().equals("total")) {
					memberCount = rank.getValue().getAsInt();
				} else if (rank.getValue() instanceof JsonObject byName) {
					for (JsonElement entry : byName.asMap().values()) {
						if (entry instanceof JsonObject member && member.get("uuid") != null) {
							members.put(member.get("uuid").getAsString(), new Member(rank.getKey(), text(member, "joined"),
								number(member, "contributed"), (int) number(member, "contributionRank")));
						}
					}
				}
			}
		}
		return new GuildInfo(text(root, "uuid"), text(root, "name"), text(root, "prefix"), (int) number(root, "level"),
			(int) number(root, "xpPercent"), (int) number(root, "territories"), (int) number(root, "wars"),
			(int) number(root, "raids"), text(root, "created"), memberCount, (int) number(root, "online"), Map.copyOf(members));
	}

	private static @Nullable String text(JsonObject obj, String key) {
		JsonElement value = obj.get(key);
		return value == null || value.isJsonNull() ? null : value.getAsString();
	}

	private static long number(JsonObject obj, String key) {
		JsonElement value = obj.get(key);
		return value == null || value.isJsonNull() ? 0 : value.getAsLong();
	}
}
