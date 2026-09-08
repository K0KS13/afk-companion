package com.jaka.afkcompanion;

/**
 * What a notification is about. Used to decide whether a screenshot is worth attaching, so
 * that can be chosen per kind of event rather than all or nothing.
 * <p>
 * The values line up with the settings sections, so the choice reads the same way in the
 * settings panel as it does here.
 */
public enum NotificationCategory
{
	/** Gemstone Crab: burrow countdown, burrow, not landing hits. */
	CRAB,

	/** AFK safeguards: logout, hitpoints, prayer, poison, drops, random events, chat triggers. */
	AFK,

	/** Skilling and combat: idle animation, inventory, supplies, aggression, special attack. */
	SKILLING,

	/** Account events: level ups, death, Grand Exchange. */
	ACCOUNT
}
