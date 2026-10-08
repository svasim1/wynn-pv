package dev.wynnpv.ui;

import dev.wynnpv.WynnPv;
import dev.wynnpv.api.AbilityTree;
import dev.wynnpv.ui.theme.Ink;
import dev.wynnpv.ui.theme.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	private static final int PATH_TAKEN = Ink.TEXT;
	private static final int PATH = 0xFFCDB487;
	/** Untaken abilities are drawn in the colour of faint ink. */
	private static final int UNTAKEN = 0xFFB89A68;
	private static final int UNTAKEN_WASH = 0xA0EDDBB2;
	private static final Set<String> TIERS = Set.of("nodeWhite", "nodeYellow", "nodePurple", "nodeBlue", "nodeRed");
	/** Sprite sizes for 18 and 14 pixel cells. */
	private static final Map<String, Integer> LARGE = Map.of("gem", 12, "spell", 14, "star", 16);
	private static final Map<String, Integer> SMALL = Map.of("gem", 10, "spell", 12, "star", 12);
	private static final Map<String, ItemStack> ITEMS = new HashMap<>();
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
		String kind = node.style().startsWith("ultimate") ? "star" : TIERS.contains(node.style()) ? "gem" : "spell";
		int size = cell >= MAX_CELL ? LARGE.get(kind) : SMALL.get(kind);
		String sprite = "tree/" + kind + (cell >= MAX_CELL ? "_large" : "_small") + (taken ? "" : "_empty");
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WynnPv.id(sprite), x + (cell - size) / 2, y + (cell - size) / 2,
			size, size, ARGB.opaque(taken ? color(node.style()) : UNTAKEN));
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
