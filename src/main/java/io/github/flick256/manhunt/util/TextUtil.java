package io.github.flick256.manhunt.util;

import java.util.Locale;

/** Small pure-Java text helpers (no Minecraft types). */
public final class TextUtil {
	private TextUtil() {
	}

	/**
	 * Formats a tick count as a clock. Partial seconds round UP, so a countdown never
	 * shows "0:00" while time is still left (1 tick => "0:01").
	 */
	public static String ticksToClock(int ticks) {
		return secondsToClock((Math.max(0, ticks) + 19) / 20);
	}

	/** Formats whole seconds as "m:ss" (minutes are not wrapped into hours). Negative => "0:00". */
	public static String secondsToClock(int seconds) {
		int s = Math.max(0, seconds);
		return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
	}
}
