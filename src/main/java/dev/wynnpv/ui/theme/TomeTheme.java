package dev.wynnpv.ui.theme;

import dev.wynnpv.WynnPv;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/** An open leather-bound book: the two panels are its pages and the tabs are cloth ribbons. */
public final class TomeTheme implements Theme {
	private static final Identifier COVER = WynnPv.id("book/cover");
	private static final Identifier PAGE_LEFT = WynnPv.id("book/page_left");
	private static final Identifier PAGE_RIGHT = WynnPv.id("book/page_right");
	private static final Identifier RIBBON = WynnPv.id("book/ribbon");

	private static final int MAX_WIDTH = 480;
	private static final int MAX_HEIGHT = 300;
	/** Below this book width the page margins shrink so the text keeps its room. */
	private static final int NARROW = 380;
	private static final int MARGIN = 6;
	/** How far ribbons stick up above the cover; the selected one a little further. */
	private static final int RIBBON_RISE = 13;
	private static final int RIBBON_RAISED = 3;
	/** Leather visible around the pages. */
	private static final int COVER_SIDE = 7;
	private static final int COVER_TOP = 6;
	private static final int PAD_TOP = 11;
	private static final int PAD_BOTTOM = 13;
	private static final int[] RIBBON_COLORS = {0xB0352E, 0x34609E, 0x3C7D3E, 0xB8862C, 0x6E4496};
	private static final int BACK_RIBBON = 0x7A7468;

	// The book, from the last layout.
	private int bookX;
	private int bookY;
	private int bookWidth;
	private int bookHeight;
	/** Space between the page art's edge and the text: the outer edge (page stack) and the gutter. */
	private int padOuter;
	private int padGutter;

	@Override
	public String name() {
		return "Tome";
	}

	@Override
	public boolean cards() {
		return false;
	}

	@Override
	public Layout layout(int screenWidth, int screenHeight, Font font) {
		bookWidth = Math.min(MAX_WIDTH, screenWidth - 2 * MARGIN) & ~1;
		bookHeight = Math.min(MAX_HEIGHT, screenHeight - 2 * MARGIN - RIBBON_RISE - RIBBON_RAISED);
		bookX = (screenWidth - bookWidth) / 2;
		bookY = (screenHeight - bookHeight + RIBBON_RISE + RIBBON_RAISED) / 2;
		boolean narrow = bookWidth < NARROW;
		padOuter = narrow ? 9 : 13;
		padGutter = narrow ? 10 : 15;
		int textTop = pageTop() + PAD_TOP;
		int textHeight = pageBottom() - PAD_BOTTOM - textTop;
		int leftX = bookX + COVER_SIDE + padOuter;
		int rightX = spine() + padGutter;
		return new Layout(
			new Area(leftX, textTop, spine() - padGutter - leftX, textHeight),
			new Area(rightX, textTop, bookX + bookWidth - COVER_SIDE - padOuter - rightX, textHeight),
			bookX + bookWidth - COVER_SIDE - padOuter + 4);
	}

	private int spine() {
		return bookX + bookWidth / 2;
	}

	private int pageTop() {
		return bookY + COVER_TOP;
	}

	private int pageBottom() {
		return bookY + bookHeight - COVER_SIDE;
	}

	@Override
	public void render(GuiGraphics graphics, Font font, Layout layout, List<Tab> tabs, @Nullable Tab back, boolean bareRight,
		int mouseX, int mouseY, List<ThemedScreen.Hit> hits) {
		// Ribbons first: the cover hides their lower ends, as if they were tucked between the pages.
		renderRibbons(graphics, font, layout, tabs, back, mouseX, mouseY, hits);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COVER, bookX, bookY, bookWidth, bookHeight);
		int pageHeight = pageBottom() - pageTop();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAGE_LEFT, bookX + COVER_SIDE, pageTop(), spine() - bookX - COVER_SIDE, pageHeight);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAGE_RIGHT, spine(), pageTop(), bookX + bookWidth - COVER_SIDE - spine(), pageHeight);
		// The fold between the pages.
		graphics.fill(spine() - 1, pageTop(), spine() + 1, pageBottom(), 0xFF6E5236);
	}

	private void renderRibbons(GuiGraphics graphics, Font font, Layout layout, List<Tab> tabs, @Nullable Tab back,
		int mouseX, int mouseY, List<ThemedScreen.Hit> hits) {
		int x = layout.right().x() - 4;
		int room = bookX + bookWidth - 4 - x;
		int textWidth = tabs.stream().mapToInt(tab -> font.width(tab.label())).sum() + 2 * Math.max(0, tabs.size() - 1);
		int pad = tabs.isEmpty() ? 7 : Math.clamp((room - textWidth) / (2 * tabs.size()), 3, 7);
		if (textWidth + 2 * pad * tabs.size() > room) {
			// Not enough room over the right page: let the ribbons reach over the left one.
			int backRight = back == null ? bookX + 4 : layout.left().x() - 4 + font.width(back.label()) + 14 + 4;
			x = Math.max(backRight, bookX + bookWidth - 4 - textWidth - 2 * pad * tabs.size());
		}
		for (int i = 0; i < tabs.size(); i++) {
			Tab tab = tabs.get(i);
			int w = font.width(tab.label()) + 2 * pad;
			x += ribbon(graphics, font, x, tab, w, pad, RIBBON_COLORS[i % RIBBON_COLORS.length], mouseX, mouseY, hits) + 2;
		}
		if (back != null) {
			ribbon(graphics, font, layout.left().x() - 4, back, font.width(back.label()) + 14, 7, BACK_RIBBON, mouseX, mouseY, hits);
		}
	}

	/** Draws one ribbon and returns its width. */
	private int ribbon(GuiGraphics graphics, Font font, int x, Tab tab, int w, int pad, int color, int mouseX, int mouseY,
		List<ThemedScreen.Hit> hits) {
		boolean selected = tab.selected();
		int top = bookY - RIBBON_RISE - (selected ? RIBBON_RAISED : 0);
		boolean hovered = !selected && mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < bookY;
		if (hovered) {
			top -= 1;
		}
		int tint = selected || hovered ? color : ARGB.scaleRGB(color, 0.78f);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, RIBBON, x, top, w, bookY + COVER_TOP - top, ARGB.opaque(tint));
		graphics.drawString(font, tab.label(), x + pad, top + 4, selected ? 0xFFFFF4D6 : 0xFFE8DCC0, true);
		if (!selected) {
			hits.add(new ThemedScreen.Hit(x, top, x + w, bookY, tab.action()));
		}
		return w;
	}
}
