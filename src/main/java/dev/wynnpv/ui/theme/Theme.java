package dev.wynnpv.ui.theme;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

/**
 * How a profile looks: where its two text panels sit and the art around them. The left panel
 * holds who the player or character is; the right panel holds the selected tab and scrolls.
 */
public interface Theme {
	/** Where the text goes, for a screen of this size; recomputed when the screen resizes. */
	Layout layout(int screenWidth, int screenHeight, Font font);

	/**
	 * Draws everything but the text: background, panels and tabs. Tabs register their click
	 * areas in {@code hits}.
	 *
	 * @param bareRight the right panel shows cards that the theme can pin straight onto its
	 *     background, if it has one, instead of on a panel
	 */
	void render(GuiGraphics graphics, Font font, Layout layout, List<Tab> tabs, @Nullable Tab back, boolean bareRight,
		int mouseX, int mouseY, List<ThemedScreen.Hit> hits);

	/** Whether character lists are shown as cards (see {@link #drawCard}) rather than rows. */
	boolean cards();

	/** A card in a card list, e.g. one character; {@code marked} singles one out, like the last played. */
	default void drawCard(GuiGraphics graphics, int x, int y, int w, int h, boolean hovered, boolean marked) {}

	/** A text area in GUI pixels. */
	record Area(int x, int y, int w, int h) {
		public int right() {
			return x + w;
		}

		public int bottom() {
			return y + h;
		}
	}

	/** The two text areas, and where the right one's scroll arrows go. */
	record Layout(Area left, Area right, int scrollMarkX) {}

	/** A tab, or the link back, with its label and what clicking it does. */
	record Tab(String label, boolean selected, Runnable action) {}
}
