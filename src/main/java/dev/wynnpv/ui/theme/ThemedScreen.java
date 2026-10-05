package dev.wynnpv.ui.theme;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * A profile screen drawn in a {@link Theme}: a fixed left panel and a right panel with tabs that
 * scrolls when its content is longer than the panel. All art is drawn at one texel per
 * GUI pixel, so it is pixel-perfect at every GUI scale.
 */
public abstract class ThemedScreen extends Screen {
	public record Hit(int x0, int y0, int x1, int y1, Runnable action) {}

	private final List<Hit> hits = new ArrayList<>();
	private final Theme theme = new BoardTheme();
	private Theme.Layout layout = new Theme.Layout(new Theme.Area(0, 0, 0, 0), new Theme.Area(0, 0, 0, 0), 0);
	private int selectedTab;
	private double scroll;
	private int contentHeight;

	protected ThemedScreen(Component title) {
		super(title);
	}

	protected Theme theme() {
		return theme;
	}

	/** Labels of the right panel's tabs; empty for none. */
	protected abstract List<String> tabs();

	/** A tab on the left that leads back, e.g. to the profile; null for none. */
	protected @Nullable String backLabel() {
		return null;
	}

	protected void onBack() {
		onClose();
	}

	protected abstract void renderLeft(Page page);

	/** Draws the selected tab's content; it may run longer than the panel, which then scrolls. */
	protected abstract void renderRight(Page page, int tab);

	/** Whether this tab shows only cards, which the theme may pin straight onto its background. */
	protected boolean isCardTab(int tab) {
		return false;
	}

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

	/** The sound of a page turning, for tabs and moving between screens. */
	public static void turnPage() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
	}

	@Override
	protected void init() {
		layout = theme.layout(width, height, font);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		hits.clear();

		List<String> labels = tabs();
		List<Theme.Tab> tabs = new ArrayList<>();
		for (int i = 0; i < labels.size(); i++) {
			int index = i;
			tabs.add(new Theme.Tab(labels.get(i), i == selectedTab, () -> selectTab(index)));
		}
		String backLabel = backLabel();
		Theme.Tab back = backLabel == null ? null : new Theme.Tab(backLabel, false, this::onBack);
		boolean bareRight = theme.cards() && isCardTab(selectedTab);
		theme.render(graphics, font, layout, tabs, back, bareRight, mouseX, mouseY, hits);

		Theme.Area l = layout.left();
		Page left = new Page(graphics, font, theme, l.x(), l.y(), l.w(), l.h(), 0, mouseX, mouseY, hits);
		graphics.enableScissor(l.x() - 4, l.y() - 2, l.right() + 4, l.bottom() + 2);
		renderLeft(left);
		graphics.disableScissor();

		Theme.Area r = layout.right();
		int maxScroll = Math.max(0, contentHeight - r.h());
		scroll = Mth.clamp(scroll, 0, maxScroll);
		Page right = new Page(graphics, font, theme, r.x(), r.y(), r.w(), r.h(), (int) scroll, mouseX, mouseY, hits);
		graphics.enableScissor(r.x() - 4, r.y() - 2, r.right() + 4, r.bottom() + 2);
		renderRight(right, selectedTab);
		graphics.disableScissor();
		contentHeight = right.written((int) scroll);
		if (maxScroll > 0) {
			renderScrollMarks(graphics, r, maxScroll, bareRight);
		}
	}

	/** Small arrows beside the right panel, showing there is more above or below. */
	private void renderScrollMarks(GuiGraphics graphics, Theme.Area area, int maxScroll, boolean onWood) {
		int color = onWood ? 0xFFE8D8B0 : Ink.FADED;
		if (scroll > 0) {
			arrow(graphics, layout.scrollMarkX(), area.y() + 1, true, color);
		}
		if (scroll < maxScroll) {
			arrow(graphics, layout.scrollMarkX(), area.bottom() - 4, false, color);
		}
	}

	private static void arrow(GuiGraphics graphics, int x, int y, boolean up, int color) {
		for (int row = 0; row < 3; row++) {
			int width = up ? row : 2 - row;
			graphics.fill(x + 2 - width, y + row, x + 3 + width, y + row + 1, color);
		}
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
		int page = layout.right().h() - Page.LINE;
		switch (event.key()) {
			case GLFW.GLFW_KEY_LEFT -> selectTab(selectedTab - 1);
			case GLFW.GLFW_KEY_RIGHT -> selectTab(selectedTab + 1);
			case GLFW.GLFW_KEY_UP -> scroll -= Page.LINE;
			case GLFW.GLFW_KEY_DOWN -> scroll += Page.LINE;
			case GLFW.GLFW_KEY_PAGE_UP -> scroll -= page;
			case GLFW.GLFW_KEY_PAGE_DOWN -> scroll += page;
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
