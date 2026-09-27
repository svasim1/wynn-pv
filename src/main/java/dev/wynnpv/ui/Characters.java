package dev.wynnpv.ui;

import dev.wynnpv.api.PlayerProfile;
import dev.wynnpv.api.WynncraftApi;
import dev.wynnpv.ui.book.Ink;
import dev.wynnpv.ui.book.Page;
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
