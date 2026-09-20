package dev.wynnpv.ui.book;

/** Ink colours for writing on parchment; all text on the pages is drawn without a shadow. */
public final class Ink {
	public static final int TEXT = 0xFF3A2614;
	public static final int FADED = 0xFF7D6142;
	public static final int FAINT = 0xFFB59872;
	/** Red ink for headings, like the rubrics in an illuminated manuscript. */
	public static final int RUBRIC = 0xFF8E2A1E;
	public static final int GOLD = 0xFF8C6416;
	public static final int GREEN = 0xFF3A6B26;
	public static final int BLUE = 0xFF2E5A8C;
	public static final int PURPLE = 0xFF6A3A8C;
	/** Translucent wash behind a hovered row. */
	public static final int WASH = 0x22603818;

	/** Dark leather tooltips with a gold edge; Wynncraft's own text colours are made for dark backgrounds. */
	public static final net.minecraft.resources.Identifier TOOLTIP = dev.wynnpv.WynnPv.id("leather");

	private Ink() {}
}
