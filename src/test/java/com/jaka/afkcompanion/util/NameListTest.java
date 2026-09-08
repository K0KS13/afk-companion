package com.jaka.afkcompanion.util;

import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NameListTest
{
	@Test
	public void parsesTrimsAndLowercases()
	{
		final Set<String> names = NameList.parse("  Shark , Prayer potion(4),, DRAGON warhammer ");
		assertEquals(3, names.size());
		assertTrue(names.contains("shark"));
		assertTrue(names.contains("prayer potion(4)"));
		assertTrue(names.contains("dragon warhammer"));
	}

	@Test
	public void blankInputIsAnEmptyList()
	{
		assertTrue(NameList.parse(null).isEmpty());
		assertTrue(NameList.parse("").isEmpty());
		assertTrue(NameList.parse("   ,  , ").isEmpty());
	}

	@Test
	public void matchesOnSubstringSoDosesAndFamiliesWork()
	{
		final Set<String> names = NameList.parse("prayer potion, dragon");

		assertEquals("prayer potion", NameList.firstMatch("Prayer potion(3)", names));
		assertEquals("dragon", NameList.firstMatch("Dragon warhammer", names));
		assertEquals("dragon", NameList.firstMatch("Uncut dragonstone", names));
	}

	@Test
	public void reportsNothingWhenItDoesNotMatch()
	{
		final Set<String> names = NameList.parse("shark");

		assertNull(NameList.firstMatch("Lobster", names));
		assertNull(NameList.firstMatch(null, names));
		assertNull(NameList.firstMatch("Shark", NameList.parse("")));
	}
}
