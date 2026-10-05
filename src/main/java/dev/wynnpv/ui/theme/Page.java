package dev.wynnpv.ui.theme;

import dev.wynnpv.WynnPv;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Writes onto one page of the book, top to bottom: each call draws at the cursor {@link #y} and
 * moves it down. Coordinates are GUI pixels; the page clips anything outside its text area.
 */
public final class Page {
	public static final int LINE = 10;
	private static final Identifier DIVIDER = WynnPv.id("page/divider");
	private static final Identifier DIVIDER_GEM = WynnPv.id("page/divider_gem");
	private static final Identifier BAR = WynnPv.id("page/bar");
	private static final Identifier BAR_FILL = WynnPv.id("page/bar_fill");

	public final GuiGraphics graphics;
	public final Font font;
	/** The theme the page is drawn in, e.g. for drawing cards. */
	public final Theme theme;
	/** The text area. */
	public final int left;
	public final int top;
	public final int width;
	public final int height;
	private final int mouseX;
	private final int mouseY;
	private final List<ThemedScreen.Hit> hits;
	/** Where the next line is written. */
	public int y;

	Page(GuiGraphics graphics, Font font, Theme theme, int left, int top, int width, int height, int scroll, int mouseX, int mouseY,
		List<ThemedScreen.Hit> hits) {
		this.graphics = graphics;
		this.font = font;
		this.theme = theme;
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.hits = hits;
		this.y = top - scroll;
	}

	public int right() {
		return left + width;
	}

	/** How far down the page has been written, including what is scrolled away. */
	int written(int scroll) {
		return y - (top - scroll);
	}

	public void gap(int pixels) {
		y += pixels;
	}

	public void text(String text, int x, int y, int color) {
		if (fullyVisible(y, 8)) {
			graphics.drawString(font, text, x, y, color, false);
		}
	}

	public void text(Component text, int x, int y, int color) {
		if (fullyVisible(y, 8)) {
			graphics.drawString(font, text, x, y, color, false);
		}
	}

	/** Whether something {@code h} pixels high at {@code y} fits on the visible page; half lines are not drawn. */
	public boolean fullyVisible(int y, int h) {
		return y >= top - 1 && y + h <= top + height + 1;
	}

	/** Pixels left below the cursor on the visible page. */
	public int remaining() {
		return top + height - y;
	}

	/** A title in double-size letters, or normal size when it would not fit. */
	public void title(String text, int color) {
		if (font.width(text) * 2 > width) {
			centered(text, color);
			return;
		}
		if (fullyVisible(y, 16)) {
			var pose = graphics.pose();
			pose.pushMatrix();
			// Whole GUI pixels only, so the doubled letters stay on the pixel grid.
			pose.translate(left + (width - font.width(text) * 2) / 2, y);
			pose.scale(2, 2);
			graphics.drawString(font, text, 0, 0, color, false);
			pose.popMatrix();
		}
		y += 2 * LINE;
	}

	/** Text cut to fit {@code maxWidth}, ending in "..." when it was too long. */
	public String fit(String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		return font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
	}

	/** The first of {@code options} that fits on one line, centred; the last one is cut to fit. */
	public void centeredFirstFitting(int color, String... options) {
		for (String option : options) {
			if (font.width(option) <= width) {
				centered(option, color);
				return;
			}
		}
		centered(options[options.length - 1], color);
	}

	public void centered(String text, int color) {
		String fitted = fit(text, width);
		text(fitted, left + (width - font.width(fitted)) / 2, y, color);
		y += LINE;
	}

	public void centered(Component text, int color) {
		text(text, left + (width - font.width(text)) / 2, y, color);
		y += LINE;
	}

	/** A section heading in red ink with the page's flourish under it. */
	public void heading(String text) {
		text(text, left, y, Ink.RUBRIC);
		y += LINE + 1;
	}

	/** A heading with a note on the right, e.g. a count. */
	public void heading(String text, String note) {
		text(text, left, y, Ink.RUBRIC);
		text(note, right() - font.width(note), y, Ink.FADED);
		y += LINE + 1;
	}

	/** A smaller heading inside a section, in gold ink. */
	public void subheading(String text) {
		text(text, left, y, Ink.GOLD);
		y += LINE;
	}

	/** A ledger line: label on the left, value on the right, joined by a dotted leader. */
	public void ledger(String label, String value) {
		ledger(label, Ink.FADED, value, Ink.TEXT);
	}

	/** A ledger line with the first of {@code values} that fits beside the whole label. */
	public void ledgerFirstFitting(String label, String... values) {
		int room = width - Math.min(font.width(label), width / 2) - 9;
		for (String value : values) {
			if (font.width(value) <= room) {
				ledger(label, value);
				return;
			}
		}
		ledger(label, values[values.length - 1]);
	}

	public void ledger(String label, int labelColor, String value, int valueColor) {
		// Short labels stay whole and the value gives way; only a label wider than half the page is cut.
		int labelRoom = Math.min(font.width(label), width / 2);
		String shownValue = fit(value, width - labelRoom - 9);
		int valueX = right() - font.width(shownValue);
		String shownLabel = fit(label, valueX - left - 6);
		text(shownLabel, left, y, labelColor);
		leader(left + font.width(shownLabel) + 3, valueX - 3, y + 7);
		text(shownValue, valueX, y, valueColor);
		y += LINE;
	}

	/** A dotted leader between {@code from} and {@code to}, for rows laid out by hand. */
	public void leaderLine(int from, int to, int baseline) {
		leader(from, to, baseline);
	}

	/** Dots every third pixel on the text baseline, starting on a multiple of three so rows line up. */
	void leader(int from, int to, int baseline) {
		if (!fullyVisible(baseline - 7, 8)) {
			return;
		}
		for (int x = from + Math.floorMod(-from, 3); x < to; x += 3) {
			graphics.fill(x, baseline, x + 1, baseline + 1, Ink.FAINT);
		}
	}

	/** The ink flourish across the page with a red gem in its middle. */
	public void divider() {
		y += 2;
		int w = width - 8;
		if (!fullyVisible(y, 5)) {
			y += 10;
			return;
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DIVIDER, left + 4, y, w, 5);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DIVIDER_GEM, left + 4 + w / 2 - 4, y, 9, 5);
		y += 5 + 5;
	}

	/** A groove filled to {@code progress} (0 to 1) in {@code color}; 7 pixels high. */
	public void bar(int x, int y, int w, float progress, int color) {
		if (!fullyVisible(y, 7)) {
			return;
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR, x, y, w, 7);
		int filled = Math.round((w - 2) * Math.clamp(progress, 0f, 1f));
		if (filled >= 2) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FILL, x + 1, y + 1, filled, 5, ARGB.opaque(color));
		}
	}

	/** Shows a leather tooltip at the mouse this frame. */
	public void tooltip(List<Component> lines) {
		graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, Ink.TOOLTIP);
	}

	/** A small sprite tinted {@code color}, left out like text when the page edge would cut it. */
	public void icon(Identifier sprite, int x, int y, int w, int h, int color) {
		if (fullyVisible(y, h)) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h, ARGB.opaque(color));
		}
	}

	/**
	 * Registers a clickable area and returns whether the mouse is over it, so the caller can draw a
	 * hover state. Only the visible part of the page counts.
	 */
	public boolean clickable(int x0, int y0, int x1, int y1, Runnable action) {
		int clippedTop = Math.max(y0, top);
		int clippedBottom = Math.min(y1, top + height);
		if (clippedTop >= clippedBottom) {
			return false;
		}
		hits.add(new ThemedScreen.Hit(x0, clippedTop, x1, clippedBottom, action));
		return mouseX >= x0 && mouseX < x1 && mouseY >= clippedTop && mouseY < clippedBottom;
	}

	public boolean isMouseOver(int x0, int y0, int x1, int y1) {
		return mouseX >= x0 && mouseX < x1 && mouseY >= Math.max(y0, top) && mouseY < Math.min(y1, top + height);
	}

	public int mouseX() {
		return mouseX;
	}

	public int mouseY() {
		return mouseY;
	}
}
