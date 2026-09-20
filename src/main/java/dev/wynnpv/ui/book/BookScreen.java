package dev.wynnpv.ui.book;

import dev.wynnpv.WynnPv;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * A screen drawn as an open leather-bound book. The left page is fixed; the right page has cloth
 * ribbon tabs along the top and scrolls when its content is longer than the page. All art is drawn
 * at one texel per GUI pixel, so it is pixel-perfect at every GUI scale.
 */
public abstract class BookScreen extends Screen {
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
	/** Space between the page art's edge and the text: outer edge (page stack), gutter, top, bottom. */
	private int padOuter = 13;
	private int padGutter = 15;
	private static final int PAD_TOP = 11;
	private static final int PAD_BOTTOM = 13;
	private static final int[] RIBBON_COLORS = {0xB0352E, 0x34609E, 0x3C7D3E, 0xB8862C, 0x6E4496};
	private static final int BACK_RIBBON = 0x7A7468;

	public record Hit(int x0, int y0, int x1, int y1, Runnable action) {}

	private final List<Hit> hits = new ArrayList<>();
	private int selectedTab;
	private double scroll;
	private int contentHeight;

	// Layout, recomputed in init().
	private int bookX;
	private int bookY;
	private int bookWidth;
	private int bookHeight;

	protected BookScreen(Component title) {
		super(title);
	}

	/** Labels of the right page's ribbons; empty for none. */
	protected abstract List<String> tabs();

	/** A ribbon on the left that leads back, e.g. to the profile; null for none. */
	protected @Nullable String backLabel() {
		return null;
	}

	protected void onBack() {
		onClose();
	}

	protected abstract void renderLeft(Page page);

	/** Draws the selected tab's content; it may run longer than the page, which then scrolls. */
	protected abstract void renderRight(Page page, int tab);

	protected int selectedTab() {
		return selectedTab;
	}

	protected void selectTab(int tab) {
		if (tab != selectedTab && tab >= 0 && tab < tabs().size()) {
			selectedTab = tab;
			scroll = 0;
			turnPage();
			onTabSelected(tab);
		}
	}

	protected void onTabSelected(int tab) {}

	/** The sound of a book's page turning, for tabs and moving between books. */
	public static void turnPage() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
	}

	@Override
	protected void init() {
		bookWidth = Math.min(MAX_WIDTH, width - 2 * MARGIN) & ~1;
		bookHeight = Math.min(MAX_HEIGHT, height - 2 * MARGIN - RIBBON_RISE - RIBBON_RAISED);
		bookX = (width - bookWidth) / 2;
		bookY = (height - bookHeight + RIBBON_RISE + RIBBON_RAISED) / 2;
		boolean narrow = bookWidth < NARROW;
		padOuter = narrow ? 9 : 13;
		padGutter = narrow ? 10 : 15;
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

	private int rightTextLeft() {
		return spine() + padGutter;
	}

	private int rightTextWidth() {
		return bookX + bookWidth - COVER_SIDE - padOuter - rightTextLeft();
	}

	private int textTop() {
		return pageTop() + PAD_TOP;
	}

	private int textHeight() {
		return pageBottom() - PAD_BOTTOM - textTop();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		hits.clear();

		// Ribbons first: the cover hides their lower ends, as if they were tucked between the pages.
		renderRibbons(graphics, mouseX, mouseY);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COVER, bookX, bookY, bookWidth, bookHeight);
		int pageHeight = pageBottom() - pageTop();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAGE_LEFT, bookX + COVER_SIDE, pageTop(), spine() - bookX - COVER_SIDE, pageHeight);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAGE_RIGHT, spine(), pageTop(), bookX + bookWidth - COVER_SIDE - spine(), pageHeight);
		// The fold between the pages.
		graphics.fill(spine() - 1, pageTop(), spine() + 1, pageBottom(), 0xFF6E5236);

		int leftTextLeft = bookX + COVER_SIDE + padOuter;
		int leftTextWidth = spine() - padGutter - leftTextLeft;
		Page left = new Page(graphics, font, leftTextLeft, textTop(), leftTextWidth, textHeight(), 0, mouseX, mouseY, hits);
		graphics.enableScissor(leftTextLeft - 4, textTop() - 2, leftTextLeft + leftTextWidth + 4, textTop() + textHeight() + 2);
		renderLeft(left);
		graphics.disableScissor();

		int maxScroll = Math.max(0, contentHeight - textHeight());
		scroll = Mth.clamp(scroll, 0, maxScroll);
		Page right = new Page(graphics, font, rightTextLeft(), textTop(), rightTextWidth(), textHeight(), (int) scroll, mouseX, mouseY, hits);
		graphics.enableScissor(rightTextLeft() - 4, textTop() - 2, rightTextLeft() + rightTextWidth() + 4, textTop() + textHeight() + 2);
		renderRight(right, selectedTab);
		graphics.disableScissor();
		contentHeight = right.written((int) scroll);
		if (maxScroll > 0) {
			renderScrollMarks(graphics, maxScroll);
		}
	}

	/** Small ink arrows in the page's outer margin, showing there is more above or below. */
	private void renderScrollMarks(GuiGraphics graphics, int maxScroll) {
		int x = bookX + bookWidth - COVER_SIDE - padOuter + 4;
		if (scroll > 0) {
			arrow(graphics, x, textTop() + 1, true);
		}
		if (scroll < maxScroll) {
			arrow(graphics, x, textTop() + textHeight() - 4, false);
		}
	}

	private static void arrow(GuiGraphics graphics, int x, int y, boolean up) {
		for (int row = 0; row < 3; row++) {
			int width = up ? row : 2 - row;
			graphics.fill(x + 2 - width, y + row, x + 3 + width, y + row + 1, Ink.FADED);
		}
	}

	private void renderRibbons(GuiGraphics graphics, int mouseX, int mouseY) {
		List<String> tabs = tabs();
		int x = rightTextLeft() - 4;
		int room = bookX + bookWidth - 4 - x;
		int textWidth = tabs.stream().mapToInt(font::width).sum() + 2 * Math.max(0, tabs.size() - 1);
		int pad = tabs.isEmpty() ? 7 : Math.clamp((room - textWidth) / (2 * tabs.size()), 3, 7);
		if (textWidth + 2 * pad * tabs.size() > room) {
			// Not enough room over the right page: let the ribbons reach over the left one.
			String back = backLabel();
			int backRight = back == null ? bookX + 4 : bookX + COVER_SIDE + padOuter - 4 + font.width(back) + 14 + 4;
			x = Math.max(backRight, bookX + bookWidth - 4 - textWidth - 2 * pad * tabs.size());
		}
		for (int i = 0; i < tabs.size(); i++) {
			int w = font.width(tabs.get(i)) + 2 * pad;
			boolean selected = i == selectedTab;
			int color = RIBBON_COLORS[i % RIBBON_COLORS.length];
			int index = i;
			x += ribbon(graphics, x, tabs.get(i), w, pad, color, selected, mouseX, mouseY, () -> selectTab(index)) + 2;
		}
		String back = backLabel();
		if (back != null) {
			int w = font.width(back) + 14;
			ribbon(graphics, bookX + COVER_SIDE + padOuter - 4, back, w, 7, BACK_RIBBON, false, mouseX, mouseY, this::onBack);
		}
	}

	/** Draws one ribbon and returns its width. */
	private int ribbon(GuiGraphics graphics, int x, String label, int w, int pad, int color, boolean selected, int mouseX, int mouseY,
		Runnable action) {
		int top = bookY - RIBBON_RISE - (selected ? RIBBON_RAISED : 0);
		boolean hovered = !selected && mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < bookY;
		if (hovered) {
			top -= 1;
		}
		int tint = selected || hovered ? color : ARGB.scaleRGB(color, 0.78f);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, RIBBON, x, top, w, bookY + COVER_TOP - top, ARGB.opaque(tint));
		graphics.drawString(font, label, x + pad, top + 4, selected ? 0xFFFFF4D6 : 0xFFE8DCC0, true);
		if (!selected) {
			hits.add(new Hit(x, top, x + w, bookY, action));
		}
		return w;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			for (Hit hit : List.copyOf(hits)) {
				if (event.x() >= hit.x0() && event.x() < hit.x1() && event.y() >= hit.y0() && event.y() < hit.y1()) {
					hit.action().run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, isDoubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= scrollY * Page.LINE * 3;
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		switch (event.key()) {
			case GLFW.GLFW_KEY_LEFT -> selectTab(selectedTab - 1);
			case GLFW.GLFW_KEY_RIGHT -> selectTab(selectedTab + 1);
			case GLFW.GLFW_KEY_UP -> scroll -= Page.LINE;
			case GLFW.GLFW_KEY_DOWN -> scroll += Page.LINE;
			case GLFW.GLFW_KEY_PAGE_UP -> scroll -= textHeight() - Page.LINE;
			case GLFW.GLFW_KEY_PAGE_DOWN -> scroll += textHeight() - Page.LINE;
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (backLabel() != null) {
					onBack();
				}
			}
			default -> {
				return super.keyPressed(event);
			}
		}
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
