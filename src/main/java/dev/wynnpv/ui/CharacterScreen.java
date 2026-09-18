package dev.wynnpv.ui;

import dev.wynnpv.api.AbilityTree;
import dev.wynnpv.api.PlayerProfile;
import dev.wynnpv.api.WynncraftApi;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/** One character of a profile, in tabs: stats, dungeons and raids, quests and the ability tree. */
public final class CharacterScreen extends Screen {
	private static final int HEADER = 0xFFFFD866;
	private static final int LABEL = 0xFFA0A8B0;
	private static final int VALUE = 0xFFFFFFFF;
	private static final int ERROR = 0xFFFF6B6B;
	private static final int LINE = 11;
	private static final int VALUE_OFFSET = 72;
	private static final int CONTENT_TOP = 56;
	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.ROOT);

	private static final Map<String, String> STAT_LABELS = Map.ofEntries(
		Map.entry("mobsKilled", "Mobs killed"),
		Map.entry("chestsFound", "Chests found"),
		Map.entry("discoveries", "Discoveries"),
		Map.entry("deaths", "Deaths"),
		Map.entry("logins", "Logins"),
		Map.entry("wars", "Wars"),
		Map.entry("worldEvents", "World events"),
		Map.entry("lootruns", "Lootruns"),
		Map.entry("caves", "Caves"),
		Map.entry("itemsIdentified", "Identified"),
		Map.entry("pvpKills", "PvP kills"),
		Map.entry("pvpDeaths", "PvP deaths"));
	private static final List<String> SKILLS = List.of("strength", "dexterity", "intelligence", "defence", "agility");
	private static final int[] SKILL_COLORS = {0xFF55C855, 0xFFFFFF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFFFFFF};
	private static final List<String> GATHERING = List.of("fishing", "woodcutting", "mining", "farming");
	private static final List<String> CRAFTING = List.of("alchemism", "armouring", "cooking", "jeweling", "scribing",
		"tailoring", "weaponsmithing", "woodworking");

	private enum Tab {
		STATS("Stats"), CONTENT("Dungeons & raids"), QUESTS("Quests"), ABILITIES("Ability tree");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private final Screen parent;
	private final PlayerProfile profile;
	private final PlayerProfile.Character character;
	private Tab tab = Tab.STATS;
	private double scroll;
	private @Nullable AbilityTreeView treeView;
	private @Nullable AbilityTree tree;
	private @Nullable String treeError;
	private boolean treeRequested;

	public CharacterScreen(Screen parent, PlayerProfile profile, PlayerProfile.Character character) {
		super(Component.literal(profile.username() + " · " + character.className()));
		this.parent = parent;
		this.profile = profile;
		this.character = character;
	}

	@Override
	protected void init() {
		int tabWidth = Math.min(100, (width - 20) / Tab.values().length);
		int tabsLeft = width / 2 - tabWidth * Tab.values().length / 2;
		for (Tab each : Tab.values()) {
			Button button = Button.builder(Component.literal(each.label), b -> select(each))
				.bounds(tabsLeft + each.ordinal() * tabWidth, 28, tabWidth - 2, 20)
				.build();
			button.active = each != tab;
			addRenderableWidget(button);
		}
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
			.bounds(width / 2 - 75, height - 28, 150, 20)
			.build());
	}

	private void select(Tab selected) {
		tab = selected;
		scroll = 0;
		if (tab == Tab.ABILITIES) {
			loadTree();
		}
		rebuildWidgets();
	}

	private void loadTree() {
		if (treeRequested || profile.restricted("characterBuildAccess")) {
			return;
		}
		treeRequested = true;
		WynncraftApi.abilities(profile.uuid(), character).whenComplete((result, failure) -> Minecraft.getInstance().execute(() -> {
			if (failure != null) {
				Throwable cause = failure;
				while (cause instanceof CompletionException && cause.getCause() != null) {
					cause = cause.getCause();
				}
				treeError = cause instanceof WynncraftApi.LookupException ? cause.getMessage() : "Could not load the ability tree.";
			} else {
				tree = result;
				treeView = new AbilityTreeView(result);
			}
		}));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, heading(), width / 2, 12, VALUE);
		switch (tab) {
			case STATS -> renderStats(graphics);
			case CONTENT -> renderContent(graphics);
			case QUESTS -> renderQuests(graphics);
			case ABILITIES -> renderAbilities(graphics, mouseX, mouseY);
		}
	}

	private String heading() {
		String name = character.nickname() != null ? " \"" + character.nickname() + "\"" : "";
		return profile.username() + " · " + character.className() + name + " · Lv. " + character.level();
	}

	private int contentBottom() {
		return height - 36;
	}

	// Stats tab: character stats, skill points, professions.

	private void renderStats(GuiGraphics graphics) {
		int columnWidth = Math.min(150, (width - 40) / 3);
		int left = width / 2 - columnWidth * 3 / 2 - 10;
		renderActivity(graphics, left, CONTENT_TOP);
		int middle = left + columnWidth + 10;
		int progressTop = renderSkillPoints(graphics, middle, CONTENT_TOP) + 6;
		renderProgress(graphics, middle, progressTop);
		renderProfessions(graphics, left + 2 * (columnWidth + 10), CONTENT_TOP);
	}

	private void renderActivity(GuiGraphics graphics, int x, int y) {
		graphics.drawString(font, "Activity", x, y, HEADER);
		y += 14;
		for (String key : List.of("mobsKilled", "chestsFound", "discoveries", "deaths", "logins", "wars",
			"worldEvents", "lootruns", "caves", "itemsIdentified")) {
			Integer value = character.stat(key);
			y = row(graphics, x, y, STAT_LABELS.get(key), value == null ? hidden() : NUMBERS.format(value));
		}
		Integer kills = character.stat("pvpKills");
		Integer deaths = character.stat("pvpDeaths");
		row(graphics, x, y, "PvP K/D", kills == null || deaths == null ? hidden() : kills + " / " + deaths);
	}

	private void renderProgress(GuiGraphics graphics, int x, int y) {
		graphics.drawString(font, "Progress", x, y, HEADER);
		y += 14;
		y = row(graphics, x, y, "Level", character.level() + " (" + character.xpPercent() + "%)");
		y = row(graphics, x, y, "Total level", NUMBERS.format(character.totalLevel()));
		Integer content = character.stat("contentCompletion");
		y = row(graphics, x, y, "Content done", content == null ? hidden() : NUMBERS.format(content));
		y = row(graphics, x, y, "Playtime", character.playtimeHours() == null ? hidden() : NUMBERS.format(Math.round(character.playtimeHours())) + " h");
		if (!character.gamemodes().isEmpty()) {
			row(graphics, x, y, "Gamemodes", String.join(", ", character.gamemodes().stream().map(CharacterScreen::capitalize).toList()));
		}
	}

	/** Returns the y below the section. */
	private int renderSkillPoints(GuiGraphics graphics, int x, int y) {
		Map<String, Integer> points = character.skillPoints();
		if (points == null || profile.restricted("characterBuildAccess")) {
			graphics.drawString(font, "Skill points", x, y, HEADER);
			graphics.drawString(font, hidden(), x, y + 14, LABEL);
			return y + 14 + LINE;
		}
		int total = points.values().stream().mapToInt(Integer::intValue).sum();
		graphics.drawString(font, "Skill points (" + total + ")", x, y, HEADER);
		y += 14;
		for (int i = 0; i < SKILLS.size(); i++) {
			graphics.drawString(font, capitalize(SKILLS.get(i)), x, y, SKILL_COLORS[i]);
			graphics.drawString(font, String.valueOf(points.getOrDefault(SKILLS.get(i), 0)), x + VALUE_OFFSET, y, VALUE);
			y += LINE;
		}
		return y;
	}

	private void renderProfessions(GuiGraphics graphics, int x, int y) {
		graphics.drawString(font, "Gathering", x, y, HEADER);
		y += 14;
		for (String name : GATHERING) {
			y = professionRow(graphics, x, y, name);
		}
		y += 4;
		graphics.drawString(font, "Crafting", x, y, HEADER);
		y += 14;
		for (String name : CRAFTING) {
			y = professionRow(graphics, x, y, name);
		}
	}

	private int professionRow(GuiGraphics graphics, int x, int y, String name) {
		PlayerProfile.Profession profession = character.professions().get(name);
		String value = profession == null ? hidden() : profession.level() + (profession.xpPercent() > 0 ? " (" + profession.xpPercent() + "%)" : "");
		// "Weaponsmithing" is wider than the other labels.
		graphics.drawString(font, capitalize(name), x, y, LABEL);
		graphics.drawString(font, value, x + font.width("Weaponsmithing") + 6, y, VALUE);
		return y + LINE;
	}

	// Dungeons & raids tab.

	private void renderContent(GuiGraphics graphics) {
		int columnWidth = Math.min(200, (width - 30) / 2);
		int left = width / 2 - columnWidth - 5;
		renderCounts(graphics, "Dungeons", character.dungeons(), left, CONTENT_TOP, columnWidth);
		renderCounts(graphics, "Raids", character.raids(), width / 2 + 5, CONTENT_TOP, columnWidth);
	}

	private void renderCounts(GuiGraphics graphics, String title, @Nullable Map<String, Integer> counts, int x, int y, int width) {
		if (counts == null) {
			graphics.drawString(font, title, x, y, HEADER);
			graphics.drawString(font, hidden(), x, y + 14, LABEL);
			return;
		}
		int total = counts.values().stream().mapToInt(Integer::intValue).sum();
		graphics.drawString(font, title + " (" + NUMBERS.format(total) + ")", x, y, HEADER);
		y += 14;
		if (counts.isEmpty()) {
			graphics.drawString(font, "None yet", x, y, LABEL);
			return;
		}
		List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
		sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
		for (Map.Entry<String, Integer> entry : sorted) {
			String count = NUMBERS.format(entry.getValue());
			graphics.drawString(font, font.plainSubstrByWidth(entry.getKey(), width - font.width(count) - 6), x, y,
				entry.getKey().startsWith("Corrupted") ? 0xFFE07070 : LABEL);
			graphics.drawString(font, count, x + width - font.width(count), y, VALUE);
			y += LINE;
		}
	}

	// Quests tab: every completed quest, in columns.

	private void renderQuests(GuiGraphics graphics) {
		List<String> quests = character.quests();
		int left = 20;
		if (quests == null) {
			graphics.drawCenteredString(font, hidden(), width / 2, CONTENT_TOP, LABEL);
			return;
		}
		graphics.drawCenteredString(font, NUMBERS.format(quests.size()) + " quests done", width / 2, CONTENT_TOP, HEADER);
		List<String> sorted = quests.stream().sorted(Comparator.comparing(q -> q.toLowerCase(Locale.ROOT))).toList();
		int columns = Math.max(1, (width - 2 * left) / 150);
		int columnWidth = (width - 2 * left) / columns;
		int top = CONTENT_TOP + 16;
		int rows = (sorted.size() + columns - 1) / columns;
		scroll = Mth.clamp(scroll, 0, Math.max(0, rows * LINE - (contentBottom() - top)));
		graphics.enableScissor(0, top, width, contentBottom());
		for (int i = 0; i < sorted.size(); i++) {
			int x = left + (i / rows) * columnWidth;
			int y = top + (i % rows) * LINE - (int) scroll;
			graphics.drawString(font, font.plainSubstrByWidth(sorted.get(i), columnWidth - 6), x, y, LABEL);
		}
		graphics.disableScissor();
	}

	// Ability tree tab.

	private void renderAbilities(GuiGraphics graphics, int mouseX, int mouseY) {
		if (profile.restricted("characterBuildAccess")) {
			graphics.drawCenteredString(font, hidden(), width / 2, CONTENT_TOP + 20, LABEL);
			return;
		}
		if (treeError != null) {
			graphics.drawCenteredString(font, treeError, width / 2, CONTENT_TOP + 20, ERROR);
			return;
		}
		if (treeView == null || tree == null) {
			graphics.drawCenteredString(font, "Loading the ability tree...", width / 2, CONTENT_TOP + 20, LABEL);
			return;
		}
		int left = width / 2 - AbilityTreeView.width() / 2;
		treeView.render(graphics, font, left, CONTENT_TOP, contentBottom() - CONTENT_TOP, mouseX, mouseY);
		int infoX = left + AbilityTreeView.width() + 16;
		if (infoX + 100 < width) {
			graphics.drawString(font, "Abilities taken", infoX, CONTENT_TOP, HEADER);
			graphics.drawString(font, String.valueOf(tree.takenAbilities()), infoX, CONTENT_TOP + 12, VALUE);
			graphics.drawString(font, "Hover an ability", infoX, CONTENT_TOP + 32, LABEL);
			graphics.drawString(font, "to read it.", infoX, CONTENT_TOP + 43, LABEL);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (tab == Tab.ABILITIES && treeView != null) {
			treeView.scroll(scrollY, contentBottom() - CONTENT_TOP);
		} else {
			scroll = Math.max(0, scroll - scrollY * LINE * 3);
		}
		return true;
	}

	private int row(GuiGraphics graphics, int x, int y, String label, String value) {
		graphics.drawString(font, label, x, y, LABEL);
		graphics.drawString(font, value, x + VALUE_OFFSET, y, VALUE);
		return y + LINE;
	}

	private static String hidden() {
		return Component.translatable("wynnpv.hidden").getString();
	}

	private static String capitalize(String text) {
		String spaced = text.replace('_', ' ');
		return spaced.isEmpty() ? spaced : java.lang.Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).toLowerCase(Locale.ROOT);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
