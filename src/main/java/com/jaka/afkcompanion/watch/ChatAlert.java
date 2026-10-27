package com.jaka.afkcompanion.watch;

import java.util.regex.Pattern;

/**
 * Game messages that end an AFK on their own, with nothing else in the client to watch for.
 * The texts are the same ones the core Slayer, Cannon, Item Charges and Woodcutting plugins
 * listen for.
 */
public enum ChatAlert
{
	SUPERIOR,
	CANNON_EMPTY,
	SLAUGHTER_BROKE,
	EXPEDITIOUS_BROKE,
	TASK_COMPLETE,
	BIRD_NEST;

	/*
	 * A bracelet reports every activation in one line and only breaks when that line ends in
	 * "crumbles to dust" - "regenerates itself to full charge" is the same line on a lucky roll.
	 */
	private static final Pattern SLAUGHTER_BROKE_PATTERN = Pattern.compile(
		"Your bracelet of slaughter prevents your slayer count from decreasing\\. It then crumbles to dust\\.");
	private static final Pattern EXPEDITIOUS_BROKE_PATTERN = Pattern.compile(
		"Your expeditious bracelet helps you progress your slayer (?:task )?faster\\. It then crumbles to dust\\.");

	private static final Pattern TASK_COMPLETE_PATTERN = Pattern.compile(
		"^You've completed [\\d,]+ (?:Wilderness )?tasks?.*; return to a Slayer master\\.$");

	/**
	 * @param message a game message with its formatting tags already removed
	 * @return what the message means, or null when it is none of these
	 */
	public static ChatAlert of(String message)
	{
		if (message == null || message.isEmpty())
		{
			return null;
		}

		if (message.contains("A superior foe has appeared..."))
		{
			return SUPERIOR;
		}

		if (message.contains("Your cannon is out of ammo!"))
		{
			return CANNON_EMPTY;
		}

		if (message.contains("A bird's nest falls out of the tree"))
		{
			return BIRD_NEST;
		}

		if (TASK_COMPLETE_PATTERN.matcher(message).find())
		{
			return TASK_COMPLETE;
		}

		if (SLAUGHTER_BROKE_PATTERN.matcher(message).find())
		{
			return SLAUGHTER_BROKE;
		}

		if (EXPEDITIOUS_BROKE_PATTERN.matcher(message).find())
		{
			return EXPEDITIOUS_BROKE;
		}

		return null;
	}
}
