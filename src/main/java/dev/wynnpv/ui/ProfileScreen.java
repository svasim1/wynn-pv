package dev.wynnpv.ui;

import dev.wynnpv.WynnPv;
import dev.wynnpv.api.GuildInfo;
import dev.wynnpv.api.PlayerProfile;
import dev.wynnpv.api.WynncraftApi;
import dev.wynnpv.ui.theme.ThemedScreen;
import dev.wynnpv.ui.theme.Ink;
import dev.wynnpv.ui.theme.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** A player's profile as an open book: who they are on the left, their characters or deeds on the right. */
public final class ProfileScreen extends ThemedScreen {
	private static final int CHARACTERS = 0;
	private static final int DEEDS = 1;
	private static final int RENOWN = 2;
	private static final int GUILD = 3;
	private static final Identifier ARROW_UP = WynnPv.id("ui/arrow_up");
	private static final Identifier ARROW_DOWN = WynnPv.id("ui/arrow_down");
	/** At most this many former guilds are looked up, to go easy on the API. */
	private static final int FORMER_GUILDS = 8;
	private static final int ROW_HEIGHT = 22;
	private static final int CARD_HEIGHT = 32;
	private static final int CARD_MIN_WIDTH = 105;
	private static final int CARD_PAD = 6;

	private final String query;
	private @Nullable PlayerProfile profile;
	private @Nullable String error;
	// The Guild tab's data, looked up the first time it is opened.
	private boolean guildsRequested;
	private @Nullable GuildInfo guild;
	private @Nullable String guildError;
	private final Map<String, GuildInfo> formerGuilds = new LinkedHashMap<>();
	private final Map<String, String> formerGuildErrors = new HashMap<>();

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
		return profile == null ? List.of() : List.of("Characters", "Deeds", "Renown", "Guild");
	}

	@Override
	protected void onTabSelected(int tab) {
		if (tab == GUILD) {
			loadGuilds();
		}
	}

	private void loadGuilds() {
		if (guildsRequested || profile == null) {
			return;
		}
		guildsRequested = true;
		PlayerProfile.Guild current = profile.guild();
		if (current != null && current.uuid() != null) {
			WynncraftApi.guild(current.uuid()).whenComplete((result, failure) -> Minecraft.getInstance().execute(() -> {
				if (failure != null) {
					guildError = Characters.lookupError(failure, "Could not reach the guild hall.");
				} else {
					guild = result;
				}
			}));
		}
		for (String uuid : formerGuildIds(profile)) {
			WynncraftApi.guild(uuid).whenComplete((result, failure) -> Minecraft.getInstance().execute(() -> {
				if (failure != null) {
					formerGuildErrors.put(uuid, Characters.lookupError(failure, "Unknown guild"));
				} else {
					formerGuilds.put(uuid, result);
				}
			}));
		}
	}

	/** The guilds the player was in before, newest first; the history also lists the current guild. */
	private static List<String> formerGuildIds(PlayerProfile p) {
		String current = p.guild() == null ? null : p.guild().uuid();
		List<String> former = new ArrayList<>(p.guildHistory().stream().filter(uuid -> !uuid.equals(current)).toList());
		java.util.Collections.reverse(former);
		return former.subList(0, Math.min(FORMER_GUILDS, former.size()));
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
		renderRanks(page, p);
		page.title(p.username(), Ink.TEXT);
		if (p.nickname() != null && !p.nickname().equalsIgnoreCase(p.username())) {
			page.centered("\"" + p.nickname() + "\"", Ink.FADED);
		}
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
		page.ledger("Joined", joined == null ? "Hidden" : Format.day(joined));
		long years = Format.yearsSince(joined);
		if (years > 0) {
			page.ledger("Playing for", Format.plural(years, "year"));
		}
		page.ledger("Played", p.playtimeHours() == null ? "Hidden" : Format.hours(p.playtimeHours()));
		page.ledger("Characters", hidesCharacters(p) ? "Hidden" : String.valueOf(p.characters().size()));
		PlayerProfile.Character active = activeCharacter(p);
		if (active != null) {
			page.ledgerFirstFitting("Last played", active.className() + " " + active.level(), active.className());
		}

		// Highlights as far as they fit; small screens keep the essentials. They come from the
		// characters and totals, so a player hiding both has none.
		if (page.remaining() < 2 * Page.LINE + 23 || (p.characters().isEmpty() && p.global() == null)) {
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
			.ifPresent(c -> page.ledgerFirstFitting("Highest", c.className() + " " + c.level(), c.className()));
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

	/**
	 * The ranks above the name: staff rank, bought rank and veteran, as many as fit. Players with
	 * none are simply adventurers.
	 */
	private static void renderRanks(Page page, PlayerProfile p) {
		List<String> ranks = new ArrayList<>();
		boolean staff = p.rank() != null && !p.rank().equalsIgnoreCase("Player");
		if (staff) {
			ranks.add(p.rank().toUpperCase(java.util.Locale.ROOT));
		}
		String support = Format.rank(null, p.supportRank());
		if (support != null) {
			ranks.add(support);
		}
		if (p.veteran()) {
			ranks.add("VETERAN");
		}
		if (ranks.isEmpty()) {
			ranks.add("ADVENTURER");
		}
		String[] options = new String[ranks.size()];
		for (int i = ranks.size(); i > 0; i--) {
			options[ranks.size() - i] = String.join(" · ", ranks.subList(0, i));
		}
		page.centeredFirstFitting(Ink.RUBRIC, options);
	}

	private static PlayerProfile.@Nullable Character activeCharacter(PlayerProfile p) {
		return p.activeCharacter() == null ? null
			: p.characters().stream().filter(c -> c.uuid().equals(p.activeCharacter())).findFirst().orElse(null);
	}

	@Override
	protected void renderRight(Page page, int tab) {
		if (profile == null) {
			return;
		}
		switch (tab) {
			case CHARACTERS -> renderCharacters(page, profile);
			case DEEDS -> renderDeeds(page, profile);
			case RENOWN -> renderRenown(page, profile);
			default -> renderGuild(page, profile);
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
				boolean active = character.uuid().equals(p.activeCharacter());
				page.theme.drawCard(page.graphics, x, top, cardWidth, CARD_HEIGHT, hovered, active);
				if (hovered && active) {
					page.tooltip(List.of(Component.literal("Last played").withColor(0xFFFFD45C)));
				}
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
		page.ledger("PvP K/D", g.pvpKills() + " / " + g.pvpDeaths());
		page.divider();
		Characters.counts(page, "Dungeons", g.dungeonList());
		page.divider();
		Characters.counts(page, "Raids", g.raidList());
		if (!g.guildRaidList().isEmpty()) {
			page.divider();
			Characters.counts(page, "Guild raids", g.guildRaidList());
		}
		PlayerProfile.RaidStats raids = g.raidStats();
		if (raids != null) {
			page.divider();
			page.heading("Raid combat");
			page.ledger("Damage dealt", Format.compact(raids.damageDealt()));
			page.ledger("Damage taken", Format.compact(raids.damageTaken()));
			page.ledger("Health healed", Format.compact(raids.healthHealed()));
			page.ledger("Deaths in raids", Format.number(raids.deaths()));
			page.ledger("Buffs taken", Format.number(raids.buffsTaken()));
			page.ledger("Gambits used", Format.number(raids.gambitsUsed()));
		}
	}

	/**
	 * Leaderboard places, best first. An arrow shows the change since Wynncraft last stored the
	 * previous place; hovering a row tells what it was.
	 */
	private void renderRenown(Page page, PlayerProfile p) {
		Map<String, Integer> ranking = p.ranking();
		page.heading("Leaderboards", ranking.isEmpty() ? "" : String.valueOf(ranking.size()));
		if (ranking.isEmpty()) {
			page.text(p.restricted("mainAccess") ? "Hidden by the player" : "Not on any leaderboard yet", page.left, page.y, Ink.FAINT);
			page.gap(Page.LINE);
			return;
		}
		List<Map.Entry<String, Integer>> places = new ArrayList<>(ranking.entrySet());
		places.sort(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()));
		for (Map.Entry<String, Integer> place : places) {
			int y = page.y;
			Integer before = p.previousRanking().get(place.getKey());
			String value = "#" + Format.number(place.getValue());
			int valueX = page.right() - page.font.width(value);
			int top = place.getValue() <= 10 ? Ink.GOLD : Ink.TEXT;
			int arrowSpace = 0;
			if (before != null && !before.equals(place.getValue())) {
				boolean up = place.getValue() < before;
				arrowSpace = 8;
				// Only with its row: the arrow is shorter than the text and would show alone at the page edge.
				if (page.fullyVisible(y, 8)) {
					page.icon(up ? ARROW_UP : ARROW_DOWN, valueX - 7, y + 2, 5, 3, up ? Ink.GREEN : Ink.RUBRIC);
				}
			}
			String label = page.fit(Leaderboards.name(place.getKey()), valueX - arrowSpace - page.left - 6);
			page.text(label, page.left, y, Ink.FADED);
			page.leaderLine(page.left + page.font.width(label) + 3, valueX - arrowSpace - 3, y + 7);
			page.text(value, valueX, y, top);
			if (page.fullyVisible(y, 8) && page.isMouseOver(page.left, y - 1, page.right(), y + Page.LINE - 1)) {
				page.graphics.fill(page.left - 2, y - 1, page.right() + 2, y + Page.LINE - 1, Ink.WASH);
				page.tooltip(List.of(
					Component.literal(Leaderboards.name(place.getKey())).withColor(0xFFFFD45C),
					before == null ? Component.literal("No earlier place stored").withColor(0xFFAAAAAA)
						: Component.literal("Previously #" + Format.number(before)).withColor(0xFFAAAAAA)));
			}
			page.gap(Page.LINE);
		}
	}

	/** The player's guild and their part in it, then the guilds they were in before. */
	private void renderGuild(Page page, PlayerProfile p) {
		loadGuilds();
		PlayerProfile.Guild current = p.guild();
		if (current == null) {
			page.heading("Guild");
			page.text("Wanders without a guild", page.left, page.y, Ink.FAINT);
			page.gap(Page.LINE);
		} else {
			page.heading(page.fit(current.name() + " [" + current.prefix() + "]", page.width));
			GuildInfo info = guild;
			GuildInfo.Member member = info == null ? null : info.member(p.uuid());
			page.ledger("Rank", current.rank() == null ? "-" : Format.capitalize(current.rank()));
			if (info == null) {
				page.text(guildError != null ? guildError : "Asking the guild hall...", page.left, page.y,
					guildError != null ? Ink.RUBRIC : Ink.FADED);
				page.gap(Page.LINE);
			} else {
				if (member != null) {
					page.ledger("Joined", Format.day(Format.instant(member.joined())));
					page.ledger("Contributed", Format.compact(member.contributed()) + " XP");
					page.ledger("Contribution", "#" + member.contributionRank() + " of " + info.memberCount());
				}
				page.gap(4);
				page.subheading("The guild");
				String level = "Level " + info.level();
				page.text(level, page.left, page.y, Ink.FADED);
				String percent = info.xpPercent() + "%";
				page.text(percent, page.right() - page.font.width(percent), page.y, Ink.TEXT);
				page.gap(Page.LINE);
				page.bar(page.left, page.y, page.width, info.xpPercent() / 100f, 0xFFC08A3A);
				page.gap(9);
				page.ledger("Members", info.memberCount() + " (" + info.online() + " online)");
				page.ledger("Territories", Format.number(info.territories()));
				page.ledger("Wars", Format.number(info.wars()));
				page.ledger("Guild raids", Format.number(info.raids()));
				page.ledger("Founded", Format.day(Format.instant(info.created())));
			}
		}
		if (p.restricted("guildHistoryAccess")) {
			page.divider();
			page.heading("Former guilds");
			page.text("Hidden by the player", page.left, page.y, Ink.FAINT);
			page.gap(Page.LINE);
			return;
		}
		List<String> former = formerGuildIds(p);
		if (former.isEmpty()) {
			return;
		}
		page.divider();
		page.heading("Former guilds", String.valueOf(former.size()));
		for (String uuid : former) {
			GuildInfo info = formerGuilds.get(uuid);
			if (info != null) {
				page.ledger(info.name() + " [" + info.prefix() + "]", Ink.FADED, "Lv. " + info.level(), Ink.TEXT);
			} else if (formerGuildErrors.containsKey(uuid)) {
				page.ledger("A disbanded guild", Ink.FAINT, "-", Ink.FAINT);
			} else {
				page.ledger("...", Ink.FAINT, "", Ink.FAINT);
			}
		}
	}

	private static String hidden() {
		return "Hidden";
	}
}
