package dev.wynnpv.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Turns the website markup in Wynncraft API text into chat components, e.g.
 * {@code <span class='font-ascii' style='color:#AAAAAA'>Mana Cost: </span>}. Only colour and bold
 * are kept. Text in Wynncraft's own icon fonts (anything but {@code font-ascii}) is dropped, as it
 * is private-use glyphs that only Wynncraft's resource pack can draw.
 */
public final class WynnText {
	private static final Pattern TAG = Pattern.compile("<(/?)(span|br)([^>]*)>");
	private static final Pattern COLOR = Pattern.compile("color:\\s*#([0-9a-fA-F]{6})");
	private static final Pattern FONT_CLASS = Pattern.compile("class='font-([a-z-]+)'");

	private record Frame(Style style, boolean hidden) {}

	private WynnText() {}

	public static Component parse(String markup) {
		MutableComponent result = Component.empty();
		Deque<Frame> stack = new ArrayDeque<>();
		stack.push(new Frame(Style.EMPTY, false));
		Matcher tag = TAG.matcher(markup);
		int last = 0;
		while (tag.find()) {
			append(result, markup.substring(last, tag.start()), stack.peek());
			last = tag.end();
			if (tag.group(2).equals("br")) {
				continue;
			}
			if (tag.group(1).equals("/")) {
				if (stack.size() > 1) {
					stack.pop();
				}
				continue;
			}
			Frame parent = stack.peek();
			String attributes = tag.group(3);
			Style style = parent.style();
			Matcher color = COLOR.matcher(attributes);
			if (color.find()) {
				style = style.withColor(TextColor.fromRgb(Integer.parseInt(color.group(1), 16)));
			}
			if (attributes.contains("font-weight:bolder") || attributes.contains("font-weight:bold")) {
				style = style.withBold(true);
			}
			Matcher font = FONT_CLASS.matcher(attributes);
			boolean hidden = parent.hidden() || (font.find() && !font.group(1).equals("ascii"));
			stack.push(new Frame(style, hidden));
		}
		append(result, markup.substring(last), stack.peek());
		return result;
	}

	/** The text without markup, e.g. for searching or sorting. */
	public static String plain(String markup) {
		return parse(markup).getString();
	}

	private static void append(MutableComponent into, String text, Frame frame) {
		if (text.isEmpty() || frame.hidden()) {
			return;
		}
		into.append(Component.literal(unescape(text)).withStyle(frame.style()));
	}

	private static String unescape(String text) {
		return text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
	}
}
