package com.jaka.afkcompanion.util;

/**
 * Decides whether a given hour falls inside the configured quiet window.
 * Kept free of any client state so it can be unit tested.
 */
public final class QuietHours
{
	private QuietHours()
	{
	}

	/**
	 * @param hour hour of day, 0-23
	 * @param from hour the quiet window opens
	 * @param to   hour the quiet window closes; may be smaller than {@code from} to wrap past midnight
	 * @return true while notifications should be held back
	 */
	public static boolean isQuiet(int hour, int from, int to)
	{
		if (from == to)
		{
			// An empty window is treated as "never quiet" rather than "always quiet",
			// so a misconfiguration cannot silence every notification.
			return false;
		}

		return from < to
			? hour >= from && hour < to
			: hour >= from || hour < to;
	}
}
