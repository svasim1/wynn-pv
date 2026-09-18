package dev.wynnpv.api;

import dev.wynnpv.WynnPv;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Util;

/**
 * Reads from the official Wynncraft API. Wynncraft caches player data for two minutes and ability
 * trees for ten, so answers are kept that long here too and asking again sooner costs no request
 * (the API allows 50 player requests a minute without a token). Class ability trees only change
 * with game updates and are kept until the game closes.
 */
public final class WynncraftApi {
	private static final String BASE = "https://api.wynncraft.com/v3/";
	private static final long PROFILE_CACHE_MS = 2 * 60_000;
	private static final long ABILITIES_CACHE_MS = 10 * 60_000;
	private static final HttpClient CLIENT = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.build();

	private record Cached<T>(long time, CompletableFuture<T> value) {}

	private static final Map<String, Cached<PlayerProfile>> PROFILES = new ConcurrentHashMap<>();
	private static final Map<String, Cached<AbilityTree>> ABILITIES = new ConcurrentHashMap<>();
	private static final Map<String, CompletableFuture<String>> CLASS_TREES = new ConcurrentHashMap<>();

	/** Why a lookup failed, worded for the player. */
	public static final class LookupException extends RuntimeException {
		public LookupException(String message) {
			super(message);
		}
	}

	private WynncraftApi() {}

	/** Looks a player up by username or UUID; failures complete with a {@link LookupException}. */
	public static CompletableFuture<PlayerProfile> player(String player) {
		return cached(PROFILES, player.toLowerCase(Locale.ROOT), PROFILE_CACHE_MS, () ->
			get("player/" + encode(player) + "?fullResult", Map.of(
				// Several players have had this name; asking by UUID picks one.
				300, "More than one player has been called " + player + ". Try their UUID.",
				404, "No Wynncraft player called " + player + "."))
				.thenApply(PlayerProfile::parse));
	}

	/** A character's ability tree, laid out like in game; hidden trees fail with a {@link LookupException}. */
	public static CompletableFuture<AbilityTree> abilities(String playerUuid, PlayerProfile.Character character) {
		String className = character.treeName();
		return cached(ABILITIES, character.uuid(), ABILITIES_CACHE_MS, () -> {
			CompletableFuture<String> chosen = get("player/" + playerUuid + "/characters/" + character.uuid() + "/abilities",
				Map.of(403, "This player hides their ability trees."));
			CompletableFuture<String> map = classTree("ability/map/" + className);
			CompletableFuture<String> tree = classTree("ability/tree/" + className);
			return CompletableFuture.allOf(chosen, map, tree)
				.thenApply(ignored -> AbilityTree.parse(map.join(), tree.join(), chosen.join()));
		});
	}

	private static CompletableFuture<String> classTree(String path) {
		CompletableFuture<String> tree = CLASS_TREES.computeIfAbsent(path, key -> get(key, Map.of()));
		if (tree.isCompletedExceptionally()) {
			CLASS_TREES.remove(path, tree);
			return classTree(path);
		}
		return tree;
	}

	private static <T> CompletableFuture<T> cached(Map<String, Cached<T>> cache, String key, long maxAgeMs,
		java.util.function.Supplier<CompletableFuture<T>> fetch) {
		long now = Util.getMillis();
		Cached<T> cached = cache.get(key);
		if (cached != null && now - cached.time() < maxAgeMs && !cached.value().isCompletedExceptionally()) {
			return cached.value();
		}
		CompletableFuture<T> value = fetch.get();
		cache.put(key, new Cached<>(now, value));
		return value;
	}

	/** GETs a path and returns the body of a 200 answer; other answers fail with a readable message. */
	private static CompletableFuture<String> get(String path, Map<Integer, String> messages) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + path))
			.timeout(Duration.ofSeconds(15))
			.header("User-Agent", "wynnpv-mod")
			.build();
		return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.handle((response, error) -> {
				if (error != null) {
					WynnPv.LOGGER.warn("Request for {} failed: {}", path, error.getMessage());
					throw new LookupException("Could not reach the Wynncraft API.");
				}
				int code = response.statusCode();
				if (code == 200) {
					return response.body();
				}
				if (messages.containsKey(code)) {
					throw new LookupException(messages.get(code));
				}
				if (code == 429) {
					throw new LookupException("Too many lookups, try again in a minute.");
				}
				WynnPv.LOGGER.warn("Request for {} failed with HTTP {}: {}", path, code, response.body());
				throw new LookupException("The Wynncraft API answered with an error (" + code + ").");
			});
	}

	private static String encode(String text) {
		return URLEncoder.encode(text, StandardCharsets.UTF_8);
	}
}
