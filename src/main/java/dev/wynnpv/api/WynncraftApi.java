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
 * Reads player profiles from the official Wynncraft API. Wynncraft caches player data for two
 * minutes, so profiles are kept that long here too and asking again sooner costs no request (the
 * API allows 50 requests a minute without a token).
 */
public final class WynncraftApi {
	private static final String BASE = "https://api.wynncraft.com/v3/";
	private static final long CACHE_MS = 2 * 60_000;
	private static final HttpClient CLIENT = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.build();

	private record Cached(long time, CompletableFuture<PlayerProfile> profile) {}

	private static final Map<String, Cached> PROFILES = new ConcurrentHashMap<>();

	/** Why a lookup failed, worded for the player. */
	public static final class LookupException extends RuntimeException {
		public LookupException(String message) {
			super(message);
		}
	}

	private WynncraftApi() {}

	/** Looks a player up by username or UUID; failures complete with a {@link LookupException}. */
	public static CompletableFuture<PlayerProfile> player(String player) {
		String key = player.toLowerCase(Locale.ROOT);
		long now = Util.getMillis();
		Cached cached = PROFILES.get(key);
		if (cached != null && now - cached.time() < CACHE_MS && !cached.profile().isCompletedExceptionally()) {
			return cached.profile();
		}
		CompletableFuture<PlayerProfile> profile = fetchPlayer(player);
		PROFILES.put(key, new Cached(now, profile));
		return profile;
	}

	private static CompletableFuture<PlayerProfile> fetchPlayer(String player) {
		URI uri = URI.create(BASE + "player/" + URLEncoder.encode(player, StandardCharsets.UTF_8) + "?fullResult");
		HttpRequest request = HttpRequest.newBuilder(uri)
			.timeout(Duration.ofSeconds(15))
			.header("User-Agent", "wynnpv-mod")
			.build();
		return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.handle((response, error) -> {
				if (error != null) {
					WynnPv.LOGGER.warn("Looking up {} failed: {}", player, error.getMessage());
					throw new LookupException("Could not reach the Wynncraft API.");
				}
				return switch (response.statusCode()) {
					case 200 -> PlayerProfile.parse(response.body());
					// Several players have had this name; asking by UUID picks one.
					case 300 -> throw new LookupException("More than one player has been called " + player + ". Try their UUID.");
					case 404 -> throw new LookupException("No Wynncraft player called " + player + ".");
					case 429 -> throw new LookupException("Too many lookups, try again in a minute.");
					default -> {
						WynnPv.LOGGER.warn("Looking up {} failed with HTTP {}: {}", player, response.statusCode(), response.body());
						throw new LookupException("The Wynncraft API answered with an error (" + response.statusCode() + ").");
					}
				};
			});
	}
}
