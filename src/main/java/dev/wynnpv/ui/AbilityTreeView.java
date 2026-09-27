package dev.wynnpv.ui;

import dev.wynnpv.api.AbilityTree;
import dev.wynnpv.ui.book.Ink;
import dev.wynnpv.ui.book.Page;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Draws an ability tree onto a page, inked like the in-game one with all pages below each other:
 * taken abilities are coloured in and their paths inked; the rest are only sketched. Hovering an
 * ability shows its description.
 */
final class AbilityTreeView {
	private static final int MAX_CELL = 18;
	private static final int PAGE_GAP = 14;
	private static final int PATH_TAKEN = Ink.TEXT;
	private static final int PATH = 0xFFCDB487;
	private static final int OUTLINE = 0xFF2A1A0C;
	private static final int PARCHMENT = 0xFFEDDBB2;

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
		for (AbilityTree.Node node : tree.nodes()) {
			if (!node.ability()) {
				drawConnector(graphics, node, left, top);
			}
		}
		// Page numbers sit on a patch of clean parchment over the paths running past them.
		for (int p = 1; p <= pages(); p++) {
			int labelY = top + rowTop((p - 1) * AbilityTree.ROWS_PER_PAGE + 1) - PAGE_GAP + 3;
			String label = "Page " + p;
			int labelX = page.left + (page.width - page.font.width(label)) / 2;
			if (page.fullyVisible(labelY, 8)) {
				graphics.fill(labelX - 3, labelY - 2, labelX + page.font.width(label) + 3, labelY + 9, PARCHMENT);
			}
			page.text(label, labelX, labelY, Ink.FADED);
		}
		AbilityTree.Node hovered = null;
		for (AbilityTree.Node node : tree.nodes()) {
			if (node.ability()) {
				int x = left + (node.cell().x() - 1) * cell;
				int y = top + rowTop(node.cell().y());
				drawAbility(graphics, node, x, y);
				if (page.isMouseOver(x, y, x + cell, y + cell)) {
					hovered = node;
				}
			}
		}
		if (hovered != null) {
			graphics.setComponentTooltipForNextFrame(page.font, tooltip(hovered), page.mouseX(), page.mouseY(), Ink.TOOLTIP);
		}
		page.gap(rowTop(tree.rows()) + cell);
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
			int color = tree.isTakenTowards(node, direction) ? PATH_TAKEN : PATH;
			switch (direction) {
				case UP -> graphics.fill(cx - 1, y, cx + 1, cy + 1, color);
				case DOWN -> graphics.fill(cx - 1, cy - 1, cx + 1, y + cell + (lastRowOfPage ? PAGE_GAP : 0), color);
				case LEFT -> graphics.fill(x, cy - 1, cx + 1, cy + 1, color);
				case RIGHT -> graphics.fill(cx - 1, cy - 1, x + cell, cy + 1, color);
			}
		}
	}

	private void drawAbility(GuiGraphics graphics, AbilityTree.Node node, int x, int y) {
		boolean ultimate = node.style().startsWith("ultimate");
		int size = cell - (ultimate ? 4 : 6);
		int x0 = x + (cell - size) / 2;
		int y0 = y + (cell - size) / 2;
		int color = color(node.style());
		if (tree.isTaken(node)) {
			graphics.fill(x0, y0, x0 + size, y0 + size, OUTLINE);
			graphics.fill(x0 + 1, y0 + 1, x0 + size - 1, y0 + size - 1, color);
			// Light from the top left.
			graphics.fill(x0 + 1, y0 + 1, x0 + size - 1, y0 + 2, 0x60FFFFFF);
			graphics.fill(x0 + 1, y0 + size - 2, x0 + size - 1, y0 + size - 1, 0x40000000);
		} else {
			graphics.fill(x0, y0, x0 + size, y0 + size, PATH);
			graphics.fill(x0 + 1, y0 + 1, x0 + size - 1, y0 + size - 1, 0xFFEADAB4);
			graphics.fill(x0 + 3, y0 + 3, x0 + size - 3, y0 + size - 3, net.minecraft.util.ARGB.multiplyAlpha(color, 0.35f));
		}
	}

	/** Matches the colours of Wynncraft's node icons: white, yellow, purple, blue and red tiers. */
	private static int color(String style) {
		return switch (style) {
			case "nodeWhite" -> 0xFFF4F0E6;
			case "nodeYellow" -> 0xFFE8BE3A;
			case "nodePurple" -> 0xFFA45ED8;
			case "nodeBlue" -> 0xFF4C92E0;
			case "nodeRed" -> 0xFFD84A4A;
			// Ultimates, archetype starts and the class's own spells.
			default -> style.startsWith("ultimate") ? 0xFFFF9A1A : 0xFF5EBE7A;
		};
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
