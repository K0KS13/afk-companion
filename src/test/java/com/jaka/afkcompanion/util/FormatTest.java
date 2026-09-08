package com.jaka.afkcompanion.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FormatTest
{
	@Test
	public void formatsCoinsByMagnitude()
	{
		assertEquals("812 gp", Format.gp(812));
		assertEquals("1.0K", Format.gp(1_000));
		assertEquals("50.0K", Format.gp(50_000));
		assertEquals("2.4M", Format.gp(2_400_000));
	}

	@Test
	public void formatsDurations()
	{
		assertEquals("?", Format.time(-1));
		assertEquals("0:00", Format.time(0));
		assertEquals("0:09", Format.time(9));
		assertEquals("4:44", Format.time(284));
		assertEquals("1:00:00", Format.time(3600));
	}

	@Test
	public void formatsHourlyRates()
	{
		assertEquals("-", Format.perHour(1000, 0));
		// 10k over ten minutes is 60k an hour.
		assertEquals("60.0k/h", Format.perHour(10_000, 600));
		assertEquals("1.2m/h", Format.perHour(300_000, 900));
	}
}
