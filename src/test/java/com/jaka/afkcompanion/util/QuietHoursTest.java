package com.jaka.afkcompanion.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class QuietHoursTest
{
	@Test
	public void handlesWindowInsideOneDay()
	{
		assertFalse(QuietHours.isQuiet(8, 9, 17));
		assertTrue(QuietHours.isQuiet(9, 9, 17));
		assertTrue(QuietHours.isQuiet(16, 9, 17));
		assertFalse(QuietHours.isQuiet(17, 9, 17));
	}

	@Test
	public void handlesWindowWrappingPastMidnight()
	{
		assertTrue(QuietHours.isQuiet(23, 23, 8));
		assertTrue(QuietHours.isQuiet(0, 23, 8));
		assertTrue(QuietHours.isQuiet(7, 23, 8));
		assertFalse(QuietHours.isQuiet(8, 23, 8));
		assertFalse(QuietHours.isQuiet(22, 23, 8));
	}

	@Test
	public void emptyWindowIsNeverQuiet()
	{
		// A misconfigured window must not silence every notification.
		for (int hour = 0; hour < 24; hour++)
		{
			assertFalse(QuietHours.isQuiet(hour, 12, 12));
		}
	}
}
