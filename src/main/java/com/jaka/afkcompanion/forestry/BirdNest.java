package com.jaka.afkcompanion.forestry;

import java.util.Locale;

/**
 * Decides what to say when a bird's nest falls. The game announces every nest with the same
 * line, so the clue tier comes from the nest item that spawned on the same tick.
 */
public final class BirdNest
{
	private BirdNest()
	{
	}

	/**
	 * @param anyNest the "every nest" switch
	 * @param minClueTier clue nests at or above this tier notify even with {@code anyNest} off
	 * @param clueTier tier of the nest that fell, or null for an ordinary nest
	 * @return the notification text, or null when this nest is not wanted
	 */
	public static String message(boolean anyNest, ClueNestTier minClueTier, ClueNestTier clueTier)
	{
		final boolean wantedClue = clueTier != null
			&& minClueTier != ClueNestTier.DISABLED
			&& clueTier.ordinal() >= minClueTier.ordinal();

		if (!anyNest && !wantedClue)
		{
			return null;
		}

		if (clueTier == null)
		{
			return "A bird's nest fell out of the tree.";
		}

		final String tier = clueTier.name().toLowerCase(Locale.ROOT);
		final String article = tier.charAt(0) == 'e' ? "An " : "A ";
		return article + tier + " clue nest fell out of the tree.";
	}

	/** A clue nest is worth a ping; an ordinary one is not. */
	public static int priority(ClueNestTier clueTier)
	{
		return clueTier != null ? 5 : 4;
	}
}
