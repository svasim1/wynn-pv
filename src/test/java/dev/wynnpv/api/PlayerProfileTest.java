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
		assertEquals(9, best.completedQuests());
	}

	/** Reskinned classes show the reskin name. */
	@Test
	void usesReskinName() throws IOException {
		PlayerProfile profile = PlayerProfile.parse(fixture("player-public.json"));
		assertTrue(profile.characters().stream().anyMatch(c -> c.className().equals("Ninja")));
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
