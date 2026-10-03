package dev.wynnpv.ui;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Numbers, dates and names worded the way players say them. */
public final class Format {
	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.ROOT);
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ROOT).withZone(ZoneOffset.UTC);
	private static final Map<String, String> SUPPORT_RANKS = Map.of(
		"vip", "VIP", "vipplus", "VIP+", "hero", "HERO", "heroplus", "HERO+", "champion", "CHAMPION");

	private Format() {}

	public static String number(long value) {
		return NUMBERS.format(value);
	}

	/** "1,204", or "0.4" for small numbers so they don't round to nothing. */
	public static String rate(double value) {
		return value >= 10 ? number(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
	}

	/** "999", "12.3K", "36.9M", "176.9B": short enough for a ledger line. */
	public static String compact(long value) {
		if (value < 10_000) {
			return number(value);
		}
		String[] units = {"K", "M", "B", "T"};
		double scaled = value;
		int unit = -1;
		while (scaled >= 1000 && unit < units.length - 1) {
			scaled /= 1000;
			unit++;
		}
		return String.format(Locale.ROOT, scaled >= 100 ? "%.0f%s" : "%.1f%s", scaled, units[unit]);
	}

	/** "13,938 h" */
	public static String hours(@Nullable Double hours) {
		return hours == null ? "-" : number(Math.round(hours)) + " h";
	}

	public static @Nullable Instant instant(@Nullable String timestamp) {
		if (timestamp == null) {
			return null;
		}
		try {
			return Instant.parse(timestamp);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/** "27 Mar 2013" */
	public static String day(@Nullable Instant instant) {
		return instant == null ? "-" : DAY.format(instant);
	}

	/** "just now", "5 minutes ago", "3 days ago", "2 years ago". */
	public static String ago(@Nullable Instant instant) {
		if (instant == null) {
			return "-";
		}
		long minutes = Math.max(0, Duration.between(instant, Instant.now()).toMinutes());
		if (minutes < 2) {
			return "just now";
		}
		if (minutes < 60) {
			return minutes + " minutes ago";
		}
		long hours = minutes / 60;
		if (hours < 24) {
			return plural(hours, "hour") + " ago";
		}
		long days = hours / 24;
		if (days < 31) {
			return plural(days, "day") + " ago";
		}
		if (days < 365) {
			return plural(days / 30, "month") + " ago";
		}
		return plural(days / 365, "year") + " ago";
	}

	/** Whole years since {@code instant}. */
	public static long yearsSince(@Nullable Instant instant) {
		return instant == null ? 0 : Duration.between(instant, Instant.now()).toDays() / 365;
	}

	public static String plural(long count, String word) {
		return number(count) + " " + word + (count == 1 ? "" : "s");
	}

	/** The rank shown above a name: staff rank first, then the bought rank. */
	public static @Nullable String rank(@Nullable String rank, @Nullable String supportRank) {
		if (rank != null && !rank.equalsIgnoreCase("Player")) {
			return rank.toUpperCase(Locale.ROOT);
		}
		return supportRank == null ? null : SUPPORT_RANKS.getOrDefault(supportRank.toLowerCase(Locale.ROOT), supportRank.toUpperCase(Locale.ROOT));
	}

	/** "WEAPON_SMITHING" or "weaponsmithing" becomes "Weaponsmithing". */
	public static String capitalize(String text) {
		String spaced = text.replace('_', ' ');
		return spaced.isEmpty() ? spaced : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).toLowerCase(Locale.ROOT);
	}
}
