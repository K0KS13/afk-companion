package com.jaka.afkcompanion.gemstonecrab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.NPC;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GemstoneCrabTrackerTest
{
	private static final int CRAB_ID = 14779;
	private static final int SHELL_ID = 14780;
	private static final int HEALTH_SCALE = 30;

	private GemstoneCrabTracker tracker;
	private NPC crab;

	@Before
	public void setUp()
	{
		tracker = new GemstoneCrabTracker();
		crab = mock(NPC.class);
		when(crab.getId()).thenReturn(CRAB_ID);
		when(crab.getHealthScale()).thenReturn(HEALTH_SCALE);
	}

	@Test
	public void recognisesCrabAndShellById()
	{
		final NPC shell = mock(NPC.class);
		when(shell.getId()).thenReturn(SHELL_ID);

		tracker.onNpcSpawned(crab);
		assertTrue(tracker.isCrabPresent());

		tracker.onNpcSpawned(shell);
		assertTrue(tracker.isShellPresent());
		// The shell lasts 90 seconds.
		assertEquals(90, tracker.getShellSecondsLeft());
	}

	@Test
	public void firstReadingEstimatesFromTheHealthFraction()
	{
		when(crab.getHealthRatio()).thenReturn(15);

		tracker.onNpcSpawned(crab);
		tracker.onGameTick();

		// Half the bar of a ten minute cycle is roughly five minutes.
		assertEquals(300, tracker.getRemainingSeconds());
		assertFalse(tracker.isCalibrated());
	}

	@Test
	public void learnsStepLengthAndUsesItForTheEstimate()
	{
		when(crab.getHealthRatio()).thenReturn(15);
		tracker.onNpcSpawned(crab);

		// Hold at 15 for 33 ticks, which is one step of a 1000 tick cycle over 30 steps.
		for (int i = 0; i < 34; i++)
		{
			tracker.onGameTick();
		}

		when(crab.getHealthRatio()).thenReturn(14);
		tracker.onGameTick();

		assertTrue(tracker.isCalibrated());
		// 14 steps left at ~33 ticks each is ~462 ticks, or ~277 seconds.
		final int remaining = tracker.getRemainingSeconds();
		assertTrue("expected ~277s but got " + remaining, remaining > 265 && remaining < 290);
	}

	@Test
	public void keepsCountingWhileTheHealthBarIsHidden()
	{
		when(crab.getHealthRatio()).thenReturn(15);
		tracker.onNpcSpawned(crab);
		tracker.onGameTick();

		final int before = tracker.getRemainingTicks();

		// A hidden bar reports -1; the estimate should keep ticking down rather than reset.
		when(crab.getHealthRatio()).thenReturn(-1);
		tracker.onGameTick();
		tracker.onGameTick();

		assertEquals(before - 2, tracker.getRemainingTicks());
	}

	@Test
	public void countsOnlyDamageDealtToTheCrab()
	{
		final NPC other = mock(NPC.class);
		when(other.getId()).thenReturn(1234);
		when(crab.getHealthRatio()).thenReturn(20);

		tracker.onNpcSpawned(crab);
		tracker.onDamage(crab, 12);
		tracker.onDamage(other, 99);

		assertEquals(12, tracker.getDamageThisCrab());
		assertEquals(12, tracker.getDamageSession());
		assertEquals(0, tracker.getSecondsSinceLastHit());
	}

	@Test
	public void countsACrabOnlyWhenYouDamagedIt()
	{
		when(crab.getHealthRatio()).thenReturn(20);

		tracker.onNpcSpawned(crab);
		tracker.onNpcDespawned(crab);
		assertEquals(0, tracker.getCrabsCompleted());

		tracker.onNpcSpawned(crab);
		tracker.onDamage(crab, 5);
		tracker.onNpcDespawned(crab);
		assertEquals(1, tracker.getCrabsCompleted());
	}
}
