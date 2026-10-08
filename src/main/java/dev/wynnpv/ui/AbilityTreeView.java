package dev.wynnpv.ui;

import dev.wynnpv.WynnPv;
import dev.wynnpv.api.AbilityTree;
import dev.wynnpv.ui.theme.Ink;
import dev.wynnpv.ui.theme.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Draws an ability tree onto a page, inked like the in-game one with all pages below each other:
 * taken abilities are coloured in and their paths inked; the rest are only sketched. Hovering an
 * ability shows its description.
 */
final class AbilityTreeView {
	private static final int MAX_CELL = 18;
	private static final int PAGE_GAP = 14;
	// Carved into dark slate like Wynncraft's own tree: dark stone paths, and taken paths glowing cyan.
	private static final Identifier SLATE = WynnPv.id("tree/slate");
	private static final int SLATE_MARGIN = 6;
	private static final int PATH = 0xFF3C3D47;
	private static final int PATH_TAKEN = 0xFF6CF0FF;
	private static final int PATH_GLOW = 0xFF1CA6C2;
	private static final int LABEL = 0xFF8A8C99;
	private static final int LABEL_PATCH = 0xFF22232A;
	/** The glyph of an untaken rune stays dark; official icons fade under a dark wash instead. */
	private static final int GLYPH_UNLIT = 0xFF5C5D6A;
	private static final int UNTAKEN_WASH = 0xB0222329;
	private static final Map<String, String> TIERS = Map.of(
		"nodeWhite", "white", "nodeYellow", "yellow", "nodePurple", "purple", "nodeBlue", "blue", "nodeRed", "red");
	/** The colour each glyph lights up in. */
	private static final Map<String, Integer> GLYPH_COLORS = Map.of("white", 0xFFF4F4F4, "yellow", 0xFFFFC935,
		"purple", 0xFFE05CD8, "blue", 0xFF5FA8FF, "red", 0xFFFF4A4A, "spell", 0xFFFF4A4A, "ultimate", 0xFFFFB020);
	private static final Map<String, ItemStack> ITEMS = new HashMap<>();

	private final AbilityTree tree;

	AbilityTreeView(AbilityTree tree) {
		this.tree = tree;
	}

	AbilityTree tree() {
		return tree;
	}

	private int pages() {
		return (tree.rows() - 1) / AbilityTree.ROWS_PER_PAGE + 1;
	}

	/** Cell size in GUI pixels, set per frame so the tree's 9 columns fit the page. */
	private int cell = MAX_CELL;

	/** Top of a grid row (1-based, counted over all pages) relative to the top of the tree. */
	private int rowTop(int row) {
		int page = (row - 1) / AbilityTree.ROWS_PER_PAGE;
		return (page + 1) * PAGE_GAP + (row - 1) * cell;
	}

	/** Draws the tree at the page's cursor, centred, and moves the cursor below it. */
	void render(Page page) {
		GuiGraphics graphics = page.graphics;
		// Even sizes keep the 2 pixel paths centred in their cells.
		cell = Math.min(MAX_CELL, page.width / AbilityTree.COLUMNS) & ~1;
		int width = AbilityTree.COLUMNS * cell;
		int left = page.left + (page.width - width) / 2;
		int top = page.y;
		int height = rowTop(tree.rows()) + cell + SLATE_MARGIN;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLATE, left - SLATE_MARGIN, top, width + 2 * SLATE_MARGIN, height);
		for (AbilityTree.Node node : tree.nodes()) {
			if (!node.ability()) {
				drawConnector(graphics, node, left, top);
			}
		}
		// Page numbers sit on a patch of clean slate over the paths running past them.
		for (int p = 1; p <= pages(); p++) {
			int labelY = top + rowTop((p - 1) * AbilityTree.ROWS_PER_PAGE + 1) - PAGE_GAP + 3;
			String label = "Page " + p;
			int labelX = page.left + (page.width - page.font.width(label)) / 2;
			if (page.fullyVisible(labelY, 8)) {
				graphics.fill(labelX - 3, labelY - 2, labelX + page.font.width(label) + 3, labelY + 9, LABEL_PATCH);
			}
			page.text(label, labelX, labelY, LABEL);
		}
		AbilityTree.Node hovered = null;
		boolean officialIcons = WynncraftPack.loaded();
		for (AbilityTree.Node node : tree.nodes()) {
			if (node.ability()) {
				int x = left + (node.cell().x() - 1) * cell;
				int y = top + rowTop(node.cell().y());
				drawAbility(graphics, node, x, y, officialIcons);
				if (page.isMouseOver(x, y, x + cell, y + cell)) {
					hovered = node;
				}
			}
		}
		if (hovered != null) {
			graphics.setComponentTooltipForNextFrame(page.font, tooltip(hovered), page.mouseX(), page.mouseY(), Ink.TOOLTIP);
		}
		page.gap(height);
	}

	private void drawConnector(GuiGraphics graphics, AbilityTree.Node node, int left, int top) {
		int x = left + (node.cell().x() - 1) * cell;
		int y = top + rowTop(node.cell().y());
		int cx = x + cell / 2;
		int cy = y + cell / 2;
		// A path leaving the last row of a page runs on through the gap to the next page.
		boolean lastRowOfPage = node.cell().y() % AbilityTree.ROWS_PER_PAGE == 0;
		for (AbilityTree.Direction direction : AbilityTree.Direction.values()) {
			if (!node.connects(direction)) {
				continue;
			}
			boolean taken = tree.isTakenTowards(node, direction);
			// A taken path is a bright core with a darker glow a pixel either side.
			int glow = taken ? 1 : 0;
			for (int pass = taken ? 0 : 1; pass < 2; pass++) {
				int color = pass == 0 ? PATH_GLOW : taken ? PATH_TAKEN : PATH;
				int w = pass == 0 ? 1 + glow : 1;
				switch (direction) {
					case UP -> graphics.fill(cx - w, y, cx + w, cy + 1, color);
					case DOWN -> graphics.fill(cx - w, cy - 1, cx + w, y + cell + (lastRowOfPage ? PAGE_GAP : 0), color);
					case LEFT -> graphics.fill(x, cy - w, cx + 1, cy + w, color);
					case RIGHT -> graphics.fill(cx - 1, cy - w, x + cell, cy + w, color);
				}
			}
		}
	}

	/**
	 * Draws an ability: Wynncraft's own icon when its resource pack is loaded and the cell fits a
	 * 16 pixel item, otherwise our pixel-art node in the tier's colour. Abilities not taken are only
	 * outlined, or for Wynncraft's icons, faded under a parchment wash.
	 */
	private void drawAbility(GuiGraphics graphics, AbilityTree.Node node, int x, int y, boolean officialIcons) {
		boolean taken = tree.isTaken(node);
		ItemStack item = officialIcons && cell >= MAX_CELL ? item(node) : null;
		if (item != null) {
			int x0 = x + (cell - 16) / 2;
			int y0 = y + (cell - 16) / 2;
			graphics.renderItem(item, x0, y0);
			if (!taken) {
				graphics.fill(x0, y0, x0 + 16, y0 + 16, UNTAKEN_WASH);
			}
			return;
		}
		// A rune stone, with a glyph for its tier; spells and ultimates get the bigger studded stone.
		boolean large = cell >= MAX_CELL;
		String glyph = node.style().startsWith("ultimate") ? "ultimate" : TIERS.getOrDefault(node.style(), "spell");
		boolean major = glyph.equals("spell") || glyph.equals("ultimate");
		int size = large ? (major ? 18 : 16) : (major ? 14 : 12);
		String scale = large ? "_large" : "_small";
		int x0 = x + (cell - size) / 2;
		int y0 = y + (cell - size) / 2;
		String stone = "tree/" + (major ? "major" : "rune") + scale;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WynnPv.id(stone), x0, y0, size, size);
		if (taken) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WynnPv.id(stone + "_glow"), x0 - 1, y0 - 1, size + 2, size + 2);
		}
		int glyphSize = large ? 7 : 5;
		int g = (size - glyphSize) / 2;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WynnPv.id("tree/glyph_" + glyph + scale), x0 + g, y0 + g,
			glyphSize, glyphSize, taken ? GLYPH_COLORS.get(glyph) : GLYPH_UNLIT);
	}

	/** The item Wynncraft's resource pack draws this ability's icon on, or null when the API gave none. */
	private ItemStack item(AbilityTree.Node node) {
		if (node.item() == null || node.model() == null) {
			return null;
		}
		return ITEMS.computeIfAbsent(node.item() + "#" + node.model(), key -> {
			Item type = BuiltInRegistries.ITEM.getValue(Identifier.parse(node.item()));
			ItemStack stack = new ItemStack(type);
			stack.set(DataComponents.CUSTOM_MODEL_DATA,
				new CustomModelData(List.of((float) node.model()), List.of(), List.of(), List.of()));
			return stack;
		});
	}

	private List<Component> tooltip(AbilityTree.Node node) {
		List<Component> lines = new ArrayList<>();
		AbilityTree.Ability ability = node.id() == null ? null : tree.abilities().get(node.id());
		if (ability == null) {
			lines.add(Component.literal(node.id() == null ? "Unknown ability" : node.id()));
		} else {
			lines.add(WynnText.parse(ability.name()));
			for (String line : ability.description()) {
				lines.add(WynnText.parse(line));
			}
		}
		lines.add(Component.empty());
		lines.add(tree.isTaken(node)
			? Component.literal("Taken").withColor(0xFF55FF55)
			: Component.literal("Not taken").withColor(0xFF888888));
		return lines;
	}
}
