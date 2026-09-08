package com.jaka.afkcompanion.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Comma separated item names as typed into the settings, matched case-insensitively against
 * whatever the game calls an item.
 * <p>
 * Matching is on substrings so "prayer potion" catches every dose, and "dragon" catches the
 * whole family. That is deliberate: a list meant to be typed by hand is more useful loose
 * than exact.
 */
public final class NameList
{
	private NameList()
	{
	}

	/**
	 * @param csv comma separated names, may be null or blank
	 * @return the names, lowercased and trimmed, without blanks; never null
	 */
	public static Set<String> parse(String csv)
	{
		if (csv == null || csv.trim().isEmpty())
		{
			return Collections.emptySet();
		}

		final Set<String> names = new LinkedHashSet<>();
		for (String part : csv.split(","))
		{
			final String name = part.trim().toLowerCase(Locale.ROOT);
			if (!name.isEmpty())
			{
				names.add(name);
			}
		}

		return names;
	}

	/**
	 * @return the first entry the item name contains, or null when nothing matches
	 */
	public static String firstMatch(String itemName, Set<String> names)
	{
		if (itemName == null || names.isEmpty())
		{
			return null;
		}

		final String haystack = itemName.toLowerCase(Locale.ROOT);
		for (String name : names)
		{
			if (haystack.contains(name))
			{
				return name;
			}
		}

		return null;
	}
}
