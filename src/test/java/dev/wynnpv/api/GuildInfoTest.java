package dev.wynnpv.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** A real response from /v3/guild/uuid/{uuid}, saved on 2026-10-05. */
class GuildInfoTest {
	@Test
	void parsesGuildAndMembers() throws IOException {
		String json;
		try (InputStream in = GuildInfoTest.class.getResourceAsStream("/guild.json")) {
			json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		GuildInfo guild = GuildInfo.parse(json);

		assertEquals("Anime Lovers", guild.name());
		assertEquals("Zamn", guild.prefix());
		assertEquals(90, guild.memberCount());
		assertEquals(90, guild.members().size());
		GuildInfo.Member owner = guild.member("df60c736-b8a6-44a3-ba44-3ea40e727da4");
		assertNotNull(owner);
		assertEquals("owner", owner.rank());
		assertEquals(23_155_353_902L, owner.contributed());
		assertEquals(54, owner.contributionRank());
		assertNull(guild.member("00000000-0000-0000-0000-000000000000"));
	}
}
