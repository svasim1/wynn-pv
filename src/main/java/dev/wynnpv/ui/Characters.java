package dev.wynnpv.ui;

import dev.wynnpv.api.PlayerProfile;
import dev.wynnpv.api.WynncraftApi;
import dev.wynnpv.ui.theme.Ink;
import dev.wynnpv.ui.theme.Page;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/** Wording and page sections shared by the profile and character books. */
final class Characters {
	private static final String CORRUPTED = "Corrupted ";

	private Characters() {}

	/** "Ninja" or "Ninja "Shadow"" with a nickname. */
	static String title(PlayerProfile.Character character) {
		return character.className() + (character.nickname() != null ? " \"" + character.nickname() + "\"" : "");
	}

	/**
	 * The combat level cap. Past it lies one bonus level, 121, which takes as much XP as all levels
	 * before it together (wynncraft.wiki.gg/wiki/Experience_Points).
	 */
	static final int LEVEL_CAP = 120;
	static final int BONUS_LEVEL = 121;

	/** Gold for a character at the level cap. */
	static int levelColor(PlayerProfile.Character character) {
		return character.level() >= LEVEL_CAP ? Ink.GOLD : Ink.TEXT;
	}

	/** "ultimate_ironman" becomes "Ultimate Ironman". */
	static String gamemode(String mode) {
		return Arrays.stream(mode.toLowerCase(Locale.ROOT).split("_"))
			.map(Format::capitalize).collect(Collectors.joining(" "));
	}

	/** A heading with the total, then each entry by name with its corrupted version right below it in red. */
	static void counts(Page page, String title, Map<String, Integer> counts) {
		int total = counts.values().stream().mapToInt(Integer::intValue).sum();
		page.heading(title, Format.number(total));
		if (counts.isEmpty()) {
			page.text("None yet", page.left, page.y, Ink.FAINT);
			page.gap(Page.LINE);
			return;
		}
		List<String> names = new ArrayList<>(counts.keySet());
		names.sort(Comparator.comparing((String name) -> base(name)).thenComparing(name -> name.startsWith(CORRUPTED)));
		for (String name : names) {
			String count = Format.number(counts.get(name));
			if (name.startsWith(CORRUPTED)) {
				page.ledger("  Corrupted", Ink.RUBRIC, count, Ink.RUBRIC);
			} else {
				page.ledger(name, Ink.FADED, count, Ink.TEXT);
			}
		}
	}

	private static String base(String name) {
		return name.startsWith(CORRUPTED) ? name.substring(CORRUPTED.length()) : name;
	}

	/** The message to show for a failed API call. */
	static String lookupError(Throwable failure, String fallback) {
		Throwable cause = failure;
		while (cause instanceof CompletionException && cause.getCause() != null) {
			cause = cause.getCause();
		}
		return cause instanceof WynncraftApi.LookupException ? cause.getMessage() : fallback;
	}
}
