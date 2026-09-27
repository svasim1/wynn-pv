package dev.wynnpv.ui.theme;

import dev.wynnpv.WynnPv;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/**
 * A town quest board: dark planks in an iron-bound frame, with the two panels as parchment notes
 * nailed or sealed onto it, wooden signs nailed along the top as tabs, and characters as small
 * cards pinned straight onto the wood.
 */
public final class BoardTheme implements Theme {
	private static final Identifier PLANKS = WynnPv.id("board/planks");
	private static final Identifier FRAME = WynnPv.id("board/frame");
	private static final Identifier NOTE = WynnPv.id("board/note");
	private static final Identifier SIGN = WynnPv.id("board/sign");
	private static final Identifier NAIL = WynnPv.id("board/nail");
	private static final Identifier SEAL = WynnPv.id("board/seal");

	private static final int MAX_WIDTH = 500;
	private static final int MAX_HEIGHT = 300;
	private static final int NARROW = 380;
	private static final int MARGIN = 4;
	/** The frame around the planks. */
	private static final int BORDER = 9;
	/** Signs are nailed across the top of the frame. */
	private static final int SIGN_TOP = 3;
	private static final int SIGN_HEIGHT = 15;
	private static final int NOTES_TOP = 26;
	private static final int NOTES_BOTTOM = 15;
	private static final int SHADOW = 0x55000000;
	private static final int SIGN_TEXT = 0xFFF6E7C4;
	private static final int SIGN_TEXT_SELECTED = 0xFFFFD45C;

	private int boardX;
	private int boardY;
	private int boardWidth;
	private int boardHeight;
	/** The two notes. */
	private Area leftNote = new Area(0, 0, 0, 0);
	private Area rightNote = new Area(0, 0, 0, 0);

	@Override
	public String name() {
		return "Board";
	}

	@Override
	public boolean cards() {
		return true;
	}

	@Override
	public Layout layout(int screenWidth, int screenHeight, Font font) {
		boardWidth = Math.min(MAX_WIDTH, screenWidth - 2 * MARGIN);
		boardHeight = Math.min(MAX_HEIGHT, screenHeight - 2 * MARGIN);
		boardX = (screenWidth - boardWidth) / 2;
		boardY = (screenHeight - boardHeight) / 2;
		boolean narrow = boardWidth < NARROW;
		int side = BORDER + (narrow ? 5 : 9);
		int gap = narrow ? 7 : 12;
		int innerWidth = boardWidth - 2 * side - gap;
		int leftWidth = Math.round(innerWidth * (narrow ? 0.48f : 0.45f));
		int top = boardY + NOTES_TOP;
		int height = boardHeight - NOTES_TOP - NOTES_BOTTOM;
		leftNote = new Area(boardX + side, top, leftWidth, height);
		rightNote = new Area(leftNote.right() + gap, top, boardX + boardWidth - side - leftNote.right() - gap, height);
		int padX = narrow ? 8 : 11;
		return new Layout(inset(leftNote, padX), inset(rightNote, padX), rightNote.right() - padX + 3);
	}

	/** The text area of a note: clear of its nails at the top and its torn edges. */
	private static Area inset(Area note, int padX) {
		return new Area(note.x() + padX, note.y() + 12, note.w() - 2 * padX, note.h() - 12 - 10);
	}

	@Override
	public void render(GuiGraphics graphics, Font font, Layout layout, List<Tab> tabs, @Nullable Tab back, boolean bareRight,
		int mouseX, int mouseY, List<ThemedScreen.Hit> hits) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PLANKS, boardX + 4, boardY + 4, boardWidth - 8, boardHeight - 8);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, boardX, boardY, boardWidth, boardHeight);

		note(graphics, leftNote, false);
		nail(graphics, leftNote.x() + 5, leftNote.y() + 3);
		nail(graphics, leftNote.right() - 10, leftNote.y() + 3);
		if (!bareRight) {
			note(graphics, rightNote, false);
			// Sealed with red wax at the top, nailed at the bottom corners.
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SEAL, rightNote.x() + rightNote.w() / 2 - 7, rightNote.y() - 5, 15, 15);
			nail(graphics, rightNote.x() + 5, rightNote.bottom() - 9);
			nail(graphics, rightNote.right() - 10, rightNote.bottom() - 9);
		}
		renderSigns(graphics, font, tabs, back, mouseX, mouseY, hits);
	}

	/** A parchment note with its shadow on the wood; lifted a little further when {@code raised}. */
	private static void note(GuiGraphics graphics, Area area, boolean raised) {
		int drop = raised ? 3 : 2;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NOTE, area.x() + drop, area.y() + drop, area.w(), area.h(), SHADOW);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NOTE, area.x(), area.y() - (raised ? 1 : 0), area.w(), area.h());
	}

	private static void nail(GuiGraphics graphics, int x, int y) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NAIL, x, y, 5, 5);
	}

	@Override
	public void drawCard(GuiGraphics graphics, int x, int y, int w, int h, boolean hovered) {
		note(graphics, new Area(x, y, w, h), hovered);
		nail(graphics, x + w / 2 - 2, y + 2 - (hovered ? 1 : 0));
	}

	private void renderSigns(GuiGraphics graphics, Font font, List<Tab> tabs, @Nullable Tab back, int mouseX, int mouseY,
		List<ThemedScreen.Hit> hits) {
		int x = rightNote.x();
		int room = boardX + boardWidth - BORDER - x;
		int textWidth = tabs.stream().mapToInt(tab -> font.width(tab.label())).sum() + 3 * Math.max(0, tabs.size() - 1);
		int pad = tabs.isEmpty() ? 8 : Math.clamp((room - textWidth) / (2 * tabs.size()), 4, 8);
		if (textWidth + 2 * pad * tabs.size() > room) {
			int backRight = back == null ? boardX + BORDER : leftNote.x() + font.width(back.label()) + 16 + 3;
			x = Math.max(backRight, boardX + boardWidth - BORDER - textWidth - 2 * pad * tabs.size());
		}
		for (Tab tab : tabs) {
			int w = font.width(tab.label()) + 2 * pad;
			sign(graphics, font, x, tab, w, pad, mouseX, mouseY, hits);
			x += w + 3;
		}
		if (back != null) {
			sign(graphics, font, leftNote.x(), back, font.width(back.label()) + 16, 8, mouseX, mouseY, hits);
		}
	}

	/** A wooden sign nailed onto the frame; the selected one is lit with gold lettering. */
	private void sign(GuiGraphics graphics, Font font, int x, Tab tab, int w, int pad, int mouseX, int mouseY,
		List<ThemedScreen.Hit> hits) {
		int y = boardY + SIGN_TOP;
		boolean hovered = !tab.selected() && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + SIGN_HEIGHT;
		int tint = tab.selected() ? 0xFFFFFFFF : hovered ? 0xFFE0E0E0 : 0xFFB4B4B4;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SIGN, x + 1, y + 1, w, SIGN_HEIGHT, 0x66000000);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SIGN, x, y, w, SIGN_HEIGHT, ARGB.opaque(tint));
		graphics.drawString(font, tab.label(), x + pad, y + 4, tab.selected() ? SIGN_TEXT_SELECTED : hovered ? SIGN_TEXT : 0xFFD8C8A4, true);
		if (!tab.selected()) {
			hits.add(new ThemedScreen.Hit(x, y, x + w, y + SIGN_HEIGHT, tab.action()));
		}
	}
}
