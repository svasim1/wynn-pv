package dev.wynnpv.ui;

import dev.wynnpv.WynnPv;
import dev.wynnpv.api.AbilityTree;
import dev.wynnpv.api.PlayerProfile;
import dev.wynnpv.api.WynncraftApi;
import dev.wynnpv.ui.theme.ThemedScreen;
import dev.wynnpv.ui.theme.Ink;
import dev.wynnpv.ui.theme.Page;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** One character as an open book: who they are on the left, their deeds, crafts, quests and abilities on the right. */
public final class CharacterScreen extends ThemedScreen {
	private static final int DEEDS = 0;
	private static final int PROFESSIONS = 1;
	private static final int QUESTS = 2;
	private static final int ABILITIES = 3;

	/**
	 * Wynncraft's skills with the symbol and colour of their element. The symbols are our own
	 * sprites: Minecraft's font lacks Wynncraft's characters and draws them at half resolution.
	 */
	private record Skill(String key, String name, Identifier symbol, int color) {}

	private static final List<Skill> SKILLS = List.of(
		new Skill("strength", "Strength", WynnPv.id("element/earth"), 0xFF2E7A2A),
		new Skill("dexterity", "Dexterity", WynnPv.id("element/thunder"), 0xFFB08A10),
		new Skill("intelligence", "Intelligence", WynnPv.id("element/water"), 0xFF1E78A8),
		new Skill("defence", "Defence", WynnPv.id("element/fire"), 0xFFB0301C),
		new Skill("agility", "Agility", WynnPv.id("element/air"), 0xFF6A7480));
	/** The most skill points a character can have, over all five skills. */
	private static final int SKILL_POINTS = 200;
	private static final List<String> GATHERING = List.of("fishing", "woodcutting", "mining", "farming");
	private static final List<String> CRAFTING = List.of("alchemism", "armouring", "cooking", "jeweling", "scribing",
		"tailoring", "weaponsmithing", "woodworking");
	private static final int PROFESSION_CAP = 132;

	private final Screen parent;
	private final PlayerProfile profile;
	private final PlayerProfile.Character character;
	private @Nullable AbilityTreeView treeView;
	private @Nullable String treeError;
	private boolean treeRequested;

	public CharacterScreen(Screen parent, PlayerProfile profile, PlayerProfile.Character character) {
		super(Component.literal(profile.username() + " · " + character.className()));
		this.parent = parent;
		this.profile = profile;
		this.character = character;
	}

	@Override
	protected List<String> tabs() {
		return List.of("Deeds", "Crafts", "Quests", "Abilities");
	}

	@Override
	protected @Nullable String backLabel() {
		return "« " + profile.username();
	}

	@Override
	public void onClose() {
		turnPage();
		minecraft.setScreen(parent);
	}

	@Override
	protected void onTabSelected(int tab) {
		if (tab == ABILITIES) {
			loadTree();
		}
	}

	private void loadTree() {
		if (treeRequested || profile.restricted("characterBuildAccess")) {
			return;
		}
		treeRequested = true;
		WynncraftApi.abilities(profile.uuid(), character).whenComplete((result, failure) -> Minecraft.getInstance().execute(() -> {
			if (failure != null) {
				treeError = Characters.lookupError(failure, "Could not load the ability tree.");
			} else {
				treeView = new AbilityTreeView(result);
			}
		}));
	}

	// Left page: the character and their skill points.

	@Override
	protected void renderLeft(Page page) {
		PlayerProfile.Character c = character;
		String owner = profile.username().toUpperCase(Locale.ROOT);
		page.centered(owner + (owner.endsWith("S") ? "'" : "'S"), Ink.RUBRIC);
		page.title(c.className(), Ink.TEXT);
		if (c.nickname() != null) {
			page.centered("\"" + c.nickname() + "\"", Ink.FADED);
		}
		String base = Format.capitalize(c.type());
		String kind = base.equals(c.className()) ? "" : base + " · ";
		page.centered(kind + "Level " + c.level() + " · Total " + Format.number(c.totalLevel()), Ink.FADED);
		if (!c.gamemodes().isEmpty()) {
			page.centered(String.join(" · ", c.gamemodes().stream().map(Characters::gamemode).toList()), Ink.PURPLE);
		}
		page.gap(3);

		if (c.level() >= Characters.LEVEL_CAP) {
			page.text("Experience", page.left, page.y, Ink.FADED);
			page.text("Max level", page.right() - page.font.width("Max level"), page.y, Ink.GOLD);
			page.gap(Page.LINE);
			page.bar(page.left, page.y, page.width, 1f, 0xFFE0B040);
		} else {
			String percent = c.xpPercent() + "% to " + (c.level() + 1);
			page.text("Experience", page.left, page.y, Ink.FADED);
			page.text(percent, page.right() - page.font.width(percent), page.y, Ink.TEXT);
			page.gap(Page.LINE);
			page.bar(page.left, page.y, page.width, c.xpPercent() / 100f, 0xFF7FB04A);
		}
		page.gap(8);
		page.divider();

		Map<String, Integer> points = c.skillPoints();
		if (points == null || profile.restricted("characterBuildAccess")) {
			page.heading("Skill points");
			page.text("Hidden by the player", page.left, page.y, Ink.FAINT);
			return;
		}
		int total = points.values().stream().mapToInt(Integer::intValue).sum();
		page.heading("Skill points", total + " / " + SKILL_POINTS);
		int labelWidth = 10 + page.font.width("Intelligence") + 6;
		int valueWidth = page.font.width("150") + 4;
		for (Skill skill : SKILLS) {
			int value = points.getOrDefault(skill.key(), 0);
			page.icon(skill.symbol(), page.left, page.y, 7, 7, skill.color());
			page.text(skill.name(), page.left + 10, page.y, skill.color());
			String shown = String.valueOf(value);
			page.text(shown, page.right() - page.font.width(shown), page.y, value == 0 ? Ink.FAINT : Ink.TEXT);
			int barX = page.left + labelWidth;
			page.bar(barX, page.y, page.right() - valueWidth - barX, value / (float) SKILL_POINTS, skill.color());
			page.gap(Page.LINE);
		}
		renderFeats(page);
	}

	/** A few numbers worked out from the stats, when the page has room for them. */
	private void renderFeats(Page page) {
		Double hours = character.playtimeHours();
		if (hours == null || hours < 1 || page.remaining() < 4 * Page.LINE + 23) {
			return;
		}
		page.divider();
		page.heading("Feats");
		page.ledger("Played", Format.hours(hours));
		Integer mobs = character.stat("mobsKilled");
		if (mobs != null) {
			page.ledger("Mobs per hour", Format.rate(mobs / hours));
		}
		Integer logins = character.stat("logins");
		if (logins != null && logins > 0) {
			page.ledger("Average visit", Math.round(hours * 60 / logins) + " min");
		}
		Integer deaths = character.stat("deaths");
		if (deaths != null && page.remaining() >= Page.LINE) {
			page.ledger("Deaths", deaths == 0 ? "Never!" : Format.number(deaths));
		}
	}

	// Right page.

	@Override
	protected void renderRight(Page page, int tab) {
		switch (tab) {
			case DEEDS -> renderDeeds(page);
			case PROFESSIONS -> renderProfessions(page);
			case QUESTS -> renderQuests(page);
			default -> renderAbilities(page);
		}
	}

	private void renderDeeds(Page page) {
		page.heading("Deeds");
		stat(page, "Content completed", "contentCompletion");
		stat(page, "Mobs slain", "mobsKilled");
		stat(page, "Chests opened", "chestsFound");
		stat(page, "Places discovered", "discoveries");
		stat(page, "Caves explored", "caves");
		stat(page, "World events", "worldEvents");
		stat(page, "Lootruns", "lootruns");
		stat(page, "Wars fought", "wars");
		stat(page, "Items identified", "itemsIdentified");
		stat(page, "Deaths", "deaths");
		stat(page, "Logins", "logins");
		Integer kills = character.stat("pvpKills");
		Integer deaths = character.stat("pvpDeaths");
		page.ledger("PvP K/D", kills == null || deaths == null ? "Hidden" : kills + " / " + deaths);
		page.divider();
		countsOrHidden(page, "Dungeons", character.dungeons());
		page.divider();
		countsOrHidden(page, "Raids", character.raids());
	}

	private void stat(Page page, String label, String key) {
		Integer value = character.stat(key);
		page.ledger(label, value == null ? "Hidden" : Format.number(value));
	}

	private static void countsOrHidden(Page page, String title, @Nullable Map<String, Integer> counts) {
		if (counts == null) {
			page.heading(title);
			page.text("Hidden by the player", page.left, page.y, Ink.FAINT);
			page.gap(Page.LINE);
		} else {
			Characters.counts(page, title, counts);
		}
	}

	private void renderProfessions(Page page) {
		int total = character.professions().values().stream().mapToInt(PlayerProfile.Profession::level).sum();
		page.heading("Professions", "Total " + total);
		page.subheading("Gathering");
		GATHERING.forEach(name -> profession(page, name));
		page.gap(3);
		page.subheading("Crafting");
		CRAFTING.forEach(name -> profession(page, name));
	}

	private void profession(Page page, String name) {
		PlayerProfile.Profession profession = character.professions().get(name);
		int labelWidth = page.font.width("Weaponsmithing") + 6;
		page.text(Format.capitalize(name), page.left, page.y, Ink.FADED);
		if (profession == null) {
			page.text("Hidden", page.right() - page.font.width("Hidden"), page.y, Ink.FAINT);
		} else {
			String level = String.valueOf(profession.level());
			int valueWidth = page.font.width("132") + 4;
			page.text(level, page.right() - page.font.width(level), page.y, profession.level() <= 1 ? Ink.FAINT : Ink.TEXT);
			int barX = page.left + labelWidth;
			// The bar shows the level; its last stretch the progress towards the next one.
			float progress = (profession.level() - 1 + profession.xpPercent() / 100f) / (PROFESSION_CAP - 1);
			page.bar(barX, page.y, page.right() - valueWidth - barX, progress, 0xFFC08A3A);
		}
		page.gap(Page.LINE);
	}

	private void renderQuests(Page page) {
		List<String> quests = character.quests();
		if (quests == null) {
			page.heading("Quests");
			page.text("Hidden by the player", page.left, page.y, Ink.FAINT);
			return;
		}
		page.heading("Quests completed", String.valueOf(quests.size()));
		if (quests.isEmpty()) {
			page.text("None yet", page.left, page.y, Ink.FAINT);
			return;
		}
		for (String quest : quests.stream().sorted(Comparator.comparing(q -> q.toLowerCase(Locale.ROOT))).toList()) {
			page.text("✔", page.left, page.y, Ink.GREEN);
			page.text(page.fit(quest, page.width - 10), page.left + 10, page.y, Ink.TEXT);
			page.gap(Page.LINE);
		}
	}

	private void renderAbilities(Page page) {
		if (profile.restricted("characterBuildAccess")) {
			page.heading("Abilities");
			page.text("Hidden by the player", page.left, page.y, Ink.FAINT);
			return;
		}
		if (treeError != null) {
			page.heading("Abilities");
			page.text(page.fit(treeError, page.width), page.left, page.y, Ink.RUBRIC);
			return;
		}
		if (treeView == null) {
			loadTree();
			page.heading("Abilities");
			page.text("Unrolling the tree...", page.left, page.y, Ink.FADED);
			return;
		}
		page.heading("Abilities", treeView.tree().takenAbilities() + " taken");
		treeView.render(page);
	}
}
