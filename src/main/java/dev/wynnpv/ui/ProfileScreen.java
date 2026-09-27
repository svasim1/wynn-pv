package dev.wynnpv.ui;

import dev.wynnpv.api.PlayerProfile;
import dev.wynnpv.api.WynncraftApi;
import dev.wynnpv.ui.theme.ThemedScreen;
import dev.wynnpv.ui.theme.Ink;
import dev.wynnpv.ui.theme.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** A player's profile as an open book: who they are on the left, their characters or deeds on the right. */
public final class ProfileScreen extends ThemedScreen {
	private static final int CHARACTERS = 0;
	private static final int ROW_HEIGHT = 22;
	private static final int CARD_HEIGHT = 32;
	private static final int CARD_MIN_WIDTH = 105;
	private static final int CARD_PAD = 6;

	private final String query;
	private @Nullable PlayerProfile profile;
	private @Nullable String error;

	public ProfileScreen(String player) {
		super(Component.translatable("wynnpv.profile"));
		this.query = player;
		WynncraftApi.player(player).whenComplete((result, failure) -> Minecraft.getInstance().execute(() -> {
			if (failure != null) {
				error = Characters.lookupError(failure, "Could not find " + player + " in the archives.");
			} else {
				profile = result;
			}
		}));
	}

	/** The loaded profile, or null while loading or after an error. */
	public @Nullable PlayerProfile profile() {
		return profile;
	}

	@Override
	protected List<String> tabs() {
		return profile == null ? List.of() : List.of("Characters", "Deeds");
	}

	@Override
	protected void renderLeft(Page page) {
		if (profile == null) {
			page.gap(page.height / 2 - 16);
			if (error != null) {
				page.centered("Alas!", Ink.RUBRIC);
				for (String line : page.font.getSplitter().splitLines(error, page.width, net.minecraft.network.chat.Style.EMPTY)
					.stream().map(net.minecraft.network.chat.FormattedText::getString).toList()) {
					page.centered(line, Ink.FADED);
				}
			} else {
				page.centered("Searching the archives", Ink.FADED);
				page.centered("for " + query + "...", Ink.FADED);
			}
			return;
		}
		PlayerProfile p = profile;
		String rank = Format.rank(p.rank(), p.supportRank());
		page.centered(rank == null ? "ADVENTURER" : rank, Ink.RUBRIC);
		page.title(p.username(), Ink.TEXT);
		PlayerProfile.Guild guild = p.guild();
		if (guild == null) {
			page.centered("Wanders without a guild", Ink.FADED);
		} else {
			String role = guild.rank() == null ? "" : Format.capitalize(guild.rank()) + " of ";
			page.centeredFirstFitting(Ink.FADED, role + guild.name() + " [" + guild.prefix() + "]", role + guild.name(),
				guild.name() + " [" + guild.prefix() + "]", guild.name());
		}
		if (p.restricted("onlineStatus")) {
			page.centered("Whereabouts unknown", Ink.FADED);
		} else if (p.online()) {
			page.centered("Online now on " + p.server(), Ink.GREEN);
		} else {
			page.centered("Last seen " + Format.ago(Format.instant(p.lastJoin())), Ink.FADED);
		}
		page.divider();

		page.heading("Chronicle");
		Instant joined = Format.instant(p.firstJoin());
		page.ledger("Joined", Format.day(joined));
		long years = Format.yearsSince(joined);
		if (years > 0) {
			page.ledger("Veteran of", Format.plural(years, "year"));
		}
		page.ledger("Played", p.playtimeHours() == null ? "Hidden" : Format.hours(p.playtimeHours()));
		page.ledger("Characters", p.restricted("characterListAccess") ? "Hidden" : String.valueOf(p.characters().size()));

		// Highlights as far as they fit; small screens keep the essentials.
		if (page.remaining() < 2 * Page.LINE + 23) {
			return;
		}
		page.divider();
		page.heading("Highlights");
		p.characters().stream()
			.filter(c -> c.playtimeHours() != null)
			.max(Comparator.comparingDouble(PlayerProfile.Character::playtimeHours))
			.ifPresent(c -> page.ledger("Main class", c.className()));
		p.characters().stream()
			.max(Comparator.comparingInt(PlayerProfile.Character::level))
			.ifPresent(c -> page.ledger("Highest", c.className() + " " + c.level()));
		PlayerProfile.Global global = p.global();
		if (global != null) {
			if (page.remaining() >= Page.LINE) {
				global.dungeonList().entrySet().stream().max(Map.Entry.comparingByValue())
					.ifPresent(e -> page.ledger("Top dungeon", e.getKey()));
			}
			if (page.remaining() >= Page.LINE) {
				page.ledger("Total level", Format.number(global.totalLevel()));
			}
			if (p.playtimeHours() != null && p.playtimeHours() >= 1 && page.remaining() >= Page.LINE) {
				page.ledger("Mobs per hour", Format.rate(global.mobsKilled() / p.playtimeHours()));
			}
		}
	}

	@Override
	protected void renderRight(Page page, int tab) {
		if (profile == null) {
			return;
		}
		if (tab == CHARACTERS) {
			renderCharacters(page, profile);
		} else {
			renderDeeds(page, profile);
		}
	}

	@Override
	protected boolean isCardTab(int tab) {
		return tab == CHARACTERS && profile != null && !hidesCharacters(profile);
	}

	private static boolean hidesCharacters(PlayerProfile p) {
		return p.restricted("characterListAccess") || p.restricted("characterDataAccess");
	}

	private void renderCharacters(Page page, PlayerProfile p) {
		if (hidesCharacters(p)) {
			page.heading("Characters");
			page.text("Hidden by the player", page.left, page.y, Ink.FAINT);
			page.gap(Page.LINE);
			return;
		}
		if (page.theme.cards()) {
			renderCharacterCards(page, p);
			return;
		}
		page.heading("Characters", String.valueOf(p.characters().size()));
		for (PlayerProfile.Character character : p.characters()) {
			int top = page.y;
			boolean hovered = page.clickable(page.left - 3, top - 2, page.right() + 3, top + ROW_HEIGHT - 2, () -> open(p, character));
			if (hovered) {
				page.graphics.fill(page.left - 3, top - 2, page.right() + 3, top + ROW_HEIGHT - 2, Ink.WASH);
			}
			String level = "Lv. " + character.level();
			String name = page.fit(Characters.title(character), page.width - page.font.width(level) - 6);
			page.text(name, page.left, top, hovered ? Ink.RUBRIC : Ink.TEXT);
			page.text(level, page.right() - page.font.width(level), top, Characters.levelColor(character));
			page.text(page.fit(details(character), page.width), page.left, top + 10, Ink.FADED);
			page.gap(ROW_HEIGHT);
		}
	}

	/** Characters as small notes pinned onto the board, as many to a row as fit. */
	private void renderCharacterCards(Page page, PlayerProfile p) {
		int gap = 6;
		int columns = Math.max(1, (page.width + gap) / (CARD_MIN_WIDTH + gap));
		int cardWidth = (page.width - (columns - 1) * gap) / columns;
		List<PlayerProfile.Character> characters = p.characters();
		// Room above the first row for the cards' nails.
		page.gap(2);
		for (int i = 0; i < characters.size(); i += columns) {
			int top = page.y;
			for (int column = 0; column < columns && i + column < characters.size(); column++) {
				PlayerProfile.Character character = characters.get(i + column);
				int x = page.left + column * (cardWidth + gap);
				// Like lines of text, a card the panel edge would cut is left out.
				if (!page.fullyVisible(top, CARD_HEIGHT)) {
					continue;
				}
				boolean hovered = page.clickable(x, top, x + cardWidth, top + CARD_HEIGHT, () -> open(p, character));
				int lift = hovered ? 1 : 0;
				page.theme.drawCard(page.graphics, x, top, cardWidth, CARD_HEIGHT, hovered);
				int textX = x + CARD_PAD;
				int textWidth = cardWidth - 2 * CARD_PAD;
				page.text(page.fit(Characters.title(character), textWidth), textX, top + 9 - lift, hovered ? Ink.RUBRIC : Ink.TEXT);
				String level = "Lv. " + character.level();
				page.text(level, textX, top + 20 - lift, Characters.levelColor(character));
				String total = " · Total " + character.totalLevel();
				page.text(page.fit(total, textWidth - page.font.width(level)), textX + page.font.width(level), top + 20 - lift, Ink.FADED);
			}
			page.gap(CARD_HEIGHT + gap);
		}
	}

	private static String details(PlayerProfile.Character character) {
		List<String> details = new ArrayList<>();
		details.add("Total " + character.totalLevel());
		if (character.quests() != null) {
			details.add(character.quests().size() + " quests");
		}
		details.addAll(character.gamemodes().stream().map(Characters::gamemode).toList());
		return String.join(" · ", details);
	}

	private void open(PlayerProfile p, PlayerProfile.Character character) {
		turnPage();
		minecraft.setScreen(new CharacterScreen(this, p, character));
	}

	private void renderDeeds(Page page, PlayerProfile p) {
		PlayerProfile.Global g = p.global();
		if (g == null) {
			page.heading("Deeds");
			page.text("Hidden by the player", page.left, page.y, Ink.FAINT);
			page.gap(Page.LINE);
			return;
		}
		page.heading("Deeds");
		page.ledger("Quests completed", Format.number(g.completedQuests()));
		page.ledger("Content completed", Format.number(g.contentCompletion()));
		page.ledger("Mobs slain", Format.number(g.mobsKilled()));
		page.ledger("Chests opened", Format.number(g.chestsFound()));
		page.ledger("Caves explored", Format.number(g.caves()));
		page.ledger("World events", Format.number(g.worldEvents()));
		page.ledger("Lootruns", Format.number(g.lootruns()));
		page.ledger("Wars fought", Format.number(g.wars()));
		page.ledger("Guild raids", Format.number(g.guildRaids()));
		page.ledger("PvP K/D", g.pvpKills() + " / " + g.pvpDeaths());
		page.divider();
		Characters.counts(page, "Dungeons", g.dungeonList());
		page.divider();
		Characters.counts(page, "Raids", g.raidList());
	}

	private static String hidden() {
		return "Hidden";
	}
}
