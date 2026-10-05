package dev.wynnpv.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PlayerProfileTest {
	private static String fixture(String name) throws IOException {
		try (InputStream in = PlayerProfileTest.class.getResourceAsStream("/" + name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** A real response from /v3/player/Salted?fullResult, saved on 2026-10-04. */
	@Test
	void parsesPublicProfile() throws IOException {
		PlayerProfile profile = PlayerProfile.parse(fixture("player-public.json"));

		assertEquals("Salted", profile.username());
		assertEquals("Administrator", profile.rank());
		assertNotNull(profile.guild());
		assertEquals("WYNN", profile.guild().prefix());
		assertNotNull(profile.global());
		assertEquals(1497, profile.global().totalLevel());
		assertEquals(36, profile.global().completedQuests());
		assertEquals(15, profile.characters().size());
		assertFalse(profile.restricted("characterDataAccess"));

		PlayerProfile.Character best = profile.characters().getFirst();
		assertEquals("Warrior", best.className());
		assertEquals(101, best.level());
		assertEquals(326, best.totalLevel());
		assertEquals(9, best.quests().size());
		assertNotNull(best.skillPoints());
		assertEquals(12, best.professions().size());
		assertNotNull(best.dungeons());
	}

	/** Reskinned classes show the reskin name. */
	@Test
	void usesReskinName() throws IOException {
		PlayerProfile profile = PlayerProfile.parse(fixture("player-public.json"));
		assertTrue(profile.characters().stream().anyMatch(c -> c.className().equals("Ninja")));
		assertTrue(profile.characters().stream().anyMatch(c -> c.className().equals("Dark Wizard")));
	}

	/** A real response from /v3/player/muffinsko?fullResult, saved on 2026-10-05: a top player. */
	@Test
	void parsesRankingsHistoryAndRaidStats() throws IOException {
		PlayerProfile profile = PlayerProfile.parse(fixture("player-ranked.json"));

		assertEquals(38, profile.ranking().size());
		assertEquals(149, profile.ranking().get("miningLevel"));
		assertEquals(1506, profile.previousRanking().get("miningLevel"));
		assertEquals(2, profile.guildHistory().size());
		assertEquals("266c9cf8-df07-41b0-b927-ce3402b88abe", profile.guild().uuid());
		assertEquals("c6462273-35f8-4093-9198-3220f4d91087", profile.activeCharacter());
		assertNotNull(profile.global().raidStats());
		assertEquals(176_851_091_098L, profile.global().raidStats().damageDealt());
		assertEquals(5, profile.global().guildRaidList().size());
	}

	/**
	 * A real response for a player hiding their stats, characters and online status, saved on
	 * 2026-10-05: the hidden fields are left out entirely rather than sent as null.
	 */
	@Test
	void parsesRealRestrictedProfile() throws IOException {
		PlayerProfile profile = PlayerProfile.parse(fixture("player-restricted.json"));

		assertEquals("fusianasan_", profile.username());
		assertTrue(profile.restricted("mainAccess"));
		assertTrue(profile.restricted("characterDataAccess"));
		assertTrue(profile.restricted("onlineStatus"));
		assertNull(profile.global());
		assertNull(profile.firstJoin());
		assertNull(profile.playtimeHours());
		assertTrue(profile.characters().isEmpty());
		assertFalse(profile.ranking().isEmpty());
	}

	/** Hand-written to match the documented shape of a profile with everything hidden. */
	@Test
	void parsesHiddenProfile() throws IOException {
		PlayerProfile profile = PlayerProfile.parse(fixture("player-hidden.json"));

		assertEquals("HiddenPlayer", profile.username());
		assertNull(profile.global());
		assertNull(profile.playtimeHours());
		assertNull(profile.guild());
		assertTrue(profile.characters().isEmpty());
		assertTrue(profile.restricted("mainAccess"));
		assertTrue(profile.restricted("onlineStatus"));
		assertFalse(profile.restricted("guildHistoryAccess"));
		assertFalse(profile.restricted("someRuleNotInTheResponse"));
	}
}
