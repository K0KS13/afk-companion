package com.jaka.afkcompanion.watch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CannonWarningTest
{
	private final CannonWarning warning = new CannonWarning();

	@Test
	public void warnsOnceOnTheWayDown()
	{
		assertFalse(warning.update(30, 10));
		assertFalse(warning.update(11, 10));
		assertTrue(warning.update(10, 10));
		assertFalse(warning.update(9, 10));
		assertFalse(warning.update(1, 10));
	}

	@Test
	public void reArmsOnAnyTopUpLikeTheCorePlugin()
	{
		warning.update(2, 10);
		assertTrue(warning.update(8, 10));
	}

	@Test
	public void staysQuietWhenEmptyOrSwitchedOff()
	{
		assertFalse(warning.update(0, 10));
		assertFalse(warning.update(5, 0));
	}

	@Test
	public void resetForgetsTheLastWarning()
	{
		warning.update(5, 10);
		warning.reset();
		assertTrue(warning.update(5, 10));
	}

	@Test
	public void reportsTheCannonDecayingOnlyWhenWeSawItHappen()
	{
		// The first value arrives on login; an already decayed cannon is not news.
		assertFalse(warning.decayed(1));
		assertFalse(warning.decayed(0));
		assertTrue(warning.decayed(1));
		assertFalse(warning.decayed(1));
	}

	@Test
	public void resetForgetsTheDecayState()
	{
		warning.decayed(0);
		warning.reset();
		assertFalse(warning.decayed(1));
	}
}
