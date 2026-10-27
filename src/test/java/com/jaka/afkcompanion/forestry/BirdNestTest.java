package com.jaka.afkcompanion.forestry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class BirdNestTest
{
	@Test
	public void readsTheClueTierFromTheNestItem()
	{
		assertEquals(ClueNestTier.BEGINNER, ClueNestTier.fromItem(ItemID.WC_CLUE_NEST_BEGINNER));
		assertEquals(ClueNestTier.EASY, ClueNestTier.fromItem(ItemID.WC_CLUE_NEST_EASY));
		assertEquals(ClueNestTier.MEDIUM, ClueNestTier.fromItem(ItemID.WC_CLUE_NEST_MEDIUM));
		assertEquals(ClueNestTier.HARD, ClueNestTier.fromItem(ItemID.WC_CLUE_NEST_HARD));
		assertEquals(ClueNestTier.ELITE, ClueNestTier.fromItem(ItemID.WC_CLUE_NEST_ELITE));
		assertNull(ClueNestTier.fromItem(ItemID.BIRD_NEST_EMPTY));
	}

	@Test
	public void anyNestWhenNestsAreOn()
	{
		assertEquals("A bird's nest fell out of the tree.", BirdNest.message(true, ClueNestTier.DISABLED, null));
		assertEquals("A hard clue nest fell out of the tree.", BirdNest.message(true, ClueNestTier.DISABLED, ClueNestTier.HARD));
	}

	@Test
	public void nothingWhenEverythingIsOff()
	{
		assertNull(BirdNest.message(false, ClueNestTier.DISABLED, null));
		assertNull(BirdNest.message(false, ClueNestTier.DISABLED, ClueNestTier.ELITE));
	}

	@Test
	public void clueNestsAtOrAboveTheTierGetThroughOnTheirOwn()
	{
		assertEquals("A hard clue nest fell out of the tree.", BirdNest.message(false, ClueNestTier.HARD, ClueNestTier.HARD));
		assertEquals("An elite clue nest fell out of the tree.",
			BirdNest.message(false, ClueNestTier.HARD, ClueNestTier.ELITE));
		assertNull(BirdNest.message(false, ClueNestTier.HARD, ClueNestTier.MEDIUM));
		assertNull(BirdNest.message(false, ClueNestTier.HARD, null));
	}

	@Test
	public void clueNestsAreUrgentPlainNestsAreNot()
	{
		assertEquals(5, BirdNest.priority(ClueNestTier.BEGINNER));
		assertEquals(4, BirdNest.priority(null));
	}
}
