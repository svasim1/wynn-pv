package dev.wynnpv.ui;

import dev.wynnpv.api.AbilityTree;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Draws an ability tree like the in-game one, all pages below each other: taken abilities and the
 * paths between them are lit, the rest is dimmed. Hovering an ability shows its description.
 */
final class AbilityTreeView {
	private static final int CELL = 18;
	private static final int PAGE_GAP = 12;
	private static final int PATH_TAKEN = 0xFFF2F2F2;
	private static final int PATH = 0xFF4A4F57;
	private static final int NODE_EMPTY = 0xFF1C1F24;
	private static final int LABEL = 0xFFA0A8B0;

	private final AbilityTree tree;
	private double scroll;

	AbilityTreeView(AbilityTree tree) {
		this.tree = tree;
	}

	static int width() {
		return AbilityTree.COLUMNS * CELL;
	}

	private int pages() {
		return (tree.rows() - 1) / AbilityTree.ROWS_PER_PAGE + 1;
	}

	private int contentHeight() {
		return tree.rows() * CELL + pages() * PAGE_GAP;
	}

	/** Top of a grid row (1-based, counted over all pages) relative to the top of the tree. */
	private static int rowTop(int row) {
		int page = (row - 1) / AbilityTree.ROWS_PER_PAGE;
		return (page + 1) * PAGE_GAP + (row - 1) * CELL;
	}

	void scroll(double amount, int height) {
		scroll = Mth.clamp(scroll - amount * CELL * 2, 0, Math.max(0, contentHeight() - height));
	}

	void render(GuiGraphics graphics, Font font, int left, int top, int height, int mouseX, int mouseY) {
		scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight() - height));
		graphics.enableScissor(left - 60, top, left + width() + 4, top + height);
		int originY = top - (int) scroll;
		for (int page = 1; page <= pages(); page++) {
			int labelY = originY + rowTop((page - 1) * AbilityTree.ROWS_PER_PAGE + 1) - PAGE_GAP + 2;
			String label = "Page " + page;
			graphics.drawString(font, label, left - font.width(label) - 8, labelY + PAGE_GAP / 2, LABEL);
		}

		AbilityTree.Node hovered = null;
		// Paths first so abilities are drawn over their ends.
		for (AbilityTree.Node node : tree.nodes()) {
			if (!node.ability()) {
				drawConnector(graphics, node, left, originY);
			}
		}
		for (AbilityTree.Node node : tree.nodes()) {
			if (node.ability()) {
				int x = left + (node.cell().x() - 1) * CELL;
				int y = originY + rowTop(node.cell().y());
				drawAbility(graphics, node, x, y);
				if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL
					&& mouseY >= top && mouseY < top + height) {
					hovered = node;
				}
			}
		}
		graphics.disableScissor();

		if (hovered != null) {
			graphics.setComponentTooltipForNextFrame(font, tooltip(hovered), mouseX, mouseY);
		}
	}

	private void drawConnector(GuiGraphics graphics, AbilityTree.Node node, int left, int originY) {
		int x = left + (node.cell().x() - 1) * CELL;
		int y = originY + rowTop(node.cell().y());
		int cx = x + CELL / 2;
		int cy = y + CELL / 2;
		// A path leaving the last row of a page runs on through the gap to the next page.
		boolean lastRowOfPage = node.cell().y() % AbilityTree.ROWS_PER_PAGE == 0;
		for (AbilityTree.Direction direction : AbilityTree.Direction.values()) {
			if (!node.connects(direction)) {
				continue;
			}
			int color = tree.isTakenTowards(node, direction) ? PATH_TAKEN : PATH;
			switch (direction) {
				case UP -> graphics.fill(cx - 1, y, cx + 1, cy + 1, color);
				case DOWN -> graphics.fill(cx - 1, cy - 1, cx + 1, y + CELL + (lastRowOfPage ? PAGE_GAP : 0), color);
				case LEFT -> graphics.fill(x, cy - 1, cx + 1, cy + 1, color);
				case RIGHT -> graphics.fill(cx - 1, cy - 1, x + CELL, cy + 1, color);
			}
		}
	}

	private void drawAbility(GuiGraphics graphics, AbilityTree.Node node, int x, int y) {
		boolean ultimate = node.style().startsWith("ultimate");
		int size = ultimate ? 16 : 12;
		int x0 = x + (CELL - size) / 2;
		int y0 = y + (CELL - size) / 2;
		int color = color(node.style());
		if (tree.isTaken(node)) {
			graphics.fill(x0 - 1, y0 - 1, x0 + size + 1, y0 + size + 1, 0xFFFFFFFF);
			graphics.fill(x0, y0, x0 + size, y0 + size, color);
		} else {
			graphics.fill(x0, y0, x0 + size, y0 + size, dim(color));
			graphics.fill(x0 + 2, y0 + 2, x0 + size - 2, y0 + size - 2, NODE_EMPTY);
		}
	}

	/** Matches the colours of Wynncraft's node icons: white, yellow, purple, blue and red tiers. */
	private static int color(String style) {
		return switch (style) {
			case "nodeWhite" -> 0xFFE4E4E4;
			case "nodeYellow" -> 0xFFF2CF4A;
			case "nodePurple" -> 0xFFB66BEA;
			case "nodeBlue" -> 0xFF5BA4F2;
			case "nodeRed" -> 0xFFE85B5B;
			// Ultimates, archetype starts and the class's own spells.
			default -> style.startsWith("ultimate") ? 0xFFFFAA00 : 0xFF6FD08C;
		};
	}

	private static int dim(int color) {
		return 0xFF000000 | ((color >> 1) & 0x7F7F7F);
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
