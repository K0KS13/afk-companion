package com.jaka.afkcompanion.forestry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ForestryWatchTest
{
	private final ForestryWatch watch = new ForestryWatch();

	@Test
	public void reportsAnEventOnceForAllOfItsParts()
	{
		final Object root1 = new Object();
		final Object root2 = new Object();

		assertTrue(watch.onSpawn(root1, ForestryEvent.RISING_ROOTS));
		assertFalse(watch.onSpawn(root2, ForestryEvent.RISING_ROOTS));
	}

	@Test
	public void reArmsOnceTheLastPartIsGone()
	{
		final Object root1 = new Object();
		final Object root2 = new Object();
		watch.onSpawn(root1, ForestryEvent.RISING_ROOTS);
		watch.onSpawn(root2, ForestryEvent.RISING_ROOTS);

		watch.onDespawn(root1);
		assertFalse(watch.onSpawn(new Object(), ForestryEvent.RISING_ROOTS));

		watch.clear();
		ticks(ForestryWatch.REARM_TICKS);
		assertTrue(watch.onSpawn(new Object(), ForestryEvent.RISING_ROOTS));
	}

	@Test
	public void aRootThatMovesDoesNotCountAsANewEvent()
	{
		// Rising roots despawn and reappear somewhere else while the event runs.
		final Object root = new Object();
		final Object stillThere = new Object();
		watch.onSpawn(root, ForestryEvent.RISING_ROOTS);
		watch.onSpawn(stillThere, ForestryEvent.RISING_ROOTS);
		watch.onDespawn(root);

		assertFalse(watch.onSpawn(new Object(), ForestryEvent.RISING_ROOTS));
	}

	@Test
	public void differentEventsAreIndependent()
	{
		assertTrue(watch.onSpawn(new Object(), ForestryEvent.RISING_ROOTS));
		assertTrue(watch.onSpawn(new Object(), ForestryEvent.LEPRECHAUN));
	}

	@Test
	public void theSameEventAfterItEndedIsNews()
	{
		final Object leprechaun = new Object();
		assertTrue(watch.onSpawn(leprechaun, ForestryEvent.LEPRECHAUN));
		watch.onDespawn(leprechaun);
		ticks(ForestryWatch.REARM_TICKS);

		assertTrue(watch.onSpawn(new Object(), ForestryEvent.LEPRECHAUN));
	}

	@Test
	public void aPartSwappedForAnotherIsNotNews()
	{
		// A sapling changes object as it grows and withers: despawn and spawn on the same tick.
		final Object sapling = new Object();
		watch.onSpawn(sapling, ForestryEvent.STRUGGLING_SAPLING);
		watch.onDespawn(sapling);

		assertFalse(watch.onSpawn(new Object(), ForestryEvent.STRUGGLING_SAPLING));
	}

	@Test
	public void aLoneRootThatMovesIsNotNews()
	{
		final Object root = new Object();
		watch.onSpawn(root, ForestryEvent.RISING_ROOTS);
		watch.onDespawn(root);
		ticks(ForestryWatch.REARM_TICKS - 1);

		assertFalse(watch.onSpawn(new Object(), ForestryEvent.RISING_ROOTS));
	}

	@Test
	public void despawningSomethingUntrackedIsHarmless()
	{
		watch.onDespawn(new Object());
		assertTrue(watch.onSpawn(new Object(), ForestryEvent.BEE_HIVE));
	}

	@Test
	public void aSceneReloadForgetsObjectsButKeepsNpcs()
	{
		final Object root = "root";
		final Object leprechaun = 42;
		watch.onSpawn(root, ForestryEvent.RISING_ROOTS);
		watch.onSpawn(leprechaun, ForestryEvent.LEPRECHAUN);

		watch.clear(part -> part instanceof String);

		// The same roots come back right after the reload; that is not a new event.
		assertFalse(watch.onSpawn("root again", ForestryEvent.RISING_ROOTS));
		assertFalse(watch.onSpawn(43, ForestryEvent.LEPRECHAUN));
	}

	@Test
	public void hoppingForgetsEverything()
	{
		final Object root = new Object();
		watch.onSpawn(root, ForestryEvent.RISING_ROOTS);
		watch.onDespawn(root);

		watch.clear();

		assertTrue(watch.onSpawn(new Object(), ForestryEvent.RISING_ROOTS));
	}

	private void ticks(int n)
	{
		for (int i = 0; i < n; i++)
		{
			watch.onTick();
		}
	}
}
