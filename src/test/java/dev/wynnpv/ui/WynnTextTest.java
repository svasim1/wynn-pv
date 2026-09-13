package dev.wynnpv.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

class WynnTextTest {
	@Test
	void keepsTextColourAndBold() {
		Component text = WynnText.parse("<span class='font-ascii' style='color:#87dd47'><span class='font-ascii' style='font-weight:bolder'>Arrow Bomb</span></span>");
		assertEquals("Arrow Bomb", text.getString());
		Style style = text.getSiblings().getFirst().getStyle();
		assertEquals(0x87DD47, style.getColor().getValue());
		assertTrue(style.isBold());
	}

	@Test
	void dropsIconFontGlyphs() {
		String line = "<span class='font-ascii' style='color:#66e6ff'><span class='font-common'>\ue007</span> "
			+ "<span class='font-ascii' style='color:#AAAAAA'>Mana Cost: </span><span class='font-ascii' style='color:#FFFFFF'>45</span></span>";
		assertEquals(" Mana Cost: 45", WynnText.plain(line));
	}

	@Test
	void lineBreakIsEmpty() {
		assertEquals("", WynnText.plain("</br>"));
	}
}
