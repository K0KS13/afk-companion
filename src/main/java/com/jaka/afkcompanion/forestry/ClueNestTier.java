package com.jaka.afkcompanion.forestry;

import net.runelite.api.gameval.ItemID;

/**
 * Clue nest tiers, lowest first, so a tier can be compared against the minimum you asked for.
 * Same tiers as the core Woodcutting plugin, but used the other way round: there the tier
 * filters clue nests out, here it lets them through even with the bird's nest switch off.
 */
public enum ClueNestTier
{
	BEGINNER,
	EASY,
	MEDIUM,
	HARD,
	ELITE,
	DISABLED;

	/**
	 * @return the tier of a clue nest item, or null when the item is not a clue nest
	 */
	public static ClueNestTier fromItem(int itemId)
	{
		switch (itemId)
		{
			case ItemID.WC_CLUE_NEST_BEGINNER:
				return BEGINNER;
			case ItemID.WC_CLUE_NEST_EASY:
				return EASY;
			case ItemID.WC_CLUE_NEST_MEDIUM:
				return MEDIUM;
			case ItemID.WC_CLUE_NEST_HARD:
				return HARD;
			case ItemID.WC_CLUE_NEST_ELITE:
				return ELITE;
			default:
				return null;
		}
	}
}
