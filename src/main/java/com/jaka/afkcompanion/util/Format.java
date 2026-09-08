package com.jaka.afkcompanion.util;

import java.util.Locale;

/**
 * Small formatting helpers shared by the overlay, the side panel and notifications.
 */
public final class Format
{
	private Format()
	{
	}

	/**
	 * Formats a coin amount the way the game does: 1.2M, 340.5K, 812 gp.
	 */
	public static String gp(long value)
	{
		if (value >= 1_000_000)
		{
			return String.format(Locale.ROOT, "%.1fM", value / 1_000_000d);
		}

		if (value >= 1_000)
		{
			return String.format(Locale.ROOT, "%.1fK", value / 1_000d);
		}

		return value + " gp";
	}

	/**
	 * Formats a duration as m:ss, or h:mm:ss once it passes an hour.
	 *
	 * @return "?" for a negative (unknown) duration
	 */
	public static String time(int seconds)
	{
		if (seconds < 0)
		{
			return "?";
		}

		if (seconds >= 3600)
		{
			return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
		}

		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}

	/**
	 * Formats a per-hour rate: 41.2k/h.
	 */
	public static String perHour(long amount, long elapsedSeconds)
	{
		if (elapsedSeconds <= 0)
		{
			return "-";
		}

		final long hourly = Math.round(amount * 3600d / elapsedSeconds);

		if (hourly >= 1_000_000)
		{
			return String.format(Locale.ROOT, "%.1fm/h", hourly / 1_000_000d);
		}

		if (hourly >= 1_000)
		{
			return String.format(Locale.ROOT, "%.1fk/h", hourly / 1_000d);
		}

		return hourly + "/h";
	}
}
