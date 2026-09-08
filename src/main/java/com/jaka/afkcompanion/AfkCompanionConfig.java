package com.jaka.afkcompanion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(AfkCompanionConfig.GROUP)
public interface AfkCompanionConfig extends Config
{
	String GROUP = "afkcompanion";

	@ConfigSection(
		name = "Phone notifications",
		description = "Where notifications are delivered",
		position = 0
	)
	String pushSection = "push";

	@ConfigSection(
		name = "Gemstone Crab",
		description = "Burrow countdown and crab notifications",
		position = 1
	)
	String crabSection = "crab";

	@ConfigSection(
		name = "AFK safeguards",
		description = "General warnings while you are away",
		position = 2
	)
	String afkSection = "afk";

	@ConfigSection(
		name = "Skilling and combat",
		description = "Warnings for the moment your AFK actually ends",
		position = 3
	)
	String skillSection = "skill";

	@ConfigSection(
		name = "Account events",
		description = "Level ups, death, Grand Exchange",
		position = 4
	)
	String accountSection = "account";

	// ---------------------------------------------------------------- notifications

	@ConfigItem(
		keyName = "sendNtfy",
		name = "Send to ntfy",
		description = "Deliver notifications through ntfy.sh. The easiest option: free app, no account. "
			+ "Several services can be enabled at once.",
		position = 0,
		section = pushSection
	)
	default boolean sendNtfy()
	{
		return false;
	}

	@ConfigItem(
		keyName = "ntfyTopic",
		name = "ntfy topic",
		description = "The topic you subscribed to in the ntfy app. Pick something unguessable - anyone who knows the name can read your notifications.",
		position = 1,
		section = pushSection
	)
	default String ntfyTopic()
	{
		return "";
	}

	@ConfigItem(
		keyName = "ntfyControlTopic",
		name = "ntfy control topic",
		description = "Optional second topic the plugin listens on. Publish 'status', 'crab', 'stats' or 'help' to it "
			+ "from your phone and the client answers on your notification topic. Read-only: it never acts in game. "
			+ "Anyone who knows this topic can query your status, so use a separate unguessable name. Leave empty to disable.",
		position = 2,
		section = pushSection
	)
	default String ntfyControlTopic()
	{
		return "";
	}

	@ConfigItem(
		keyName = "ntfyServer",
		name = "ntfy server",
		description = "Leave as is for https://ntfy.sh, or point it at your own self-hosted server.",
		position = 3,
		section = pushSection
	)
	default String ntfyServer()
	{
		return "https://ntfy.sh";
	}

	@ConfigItem(
		keyName = "sendDiscord",
		name = "Send to Discord",
		description = "Deliver notifications to a Discord channel webhook, on its own or alongside the others.",
		position = 4,
		section = pushSection
	)
	default boolean sendDiscord()
	{
		return false;
	}

	@ConfigItem(
		keyName = "discordWebhookUrl",
		name = "Discord webhook",
		description = "Webhook URL of a Discord channel (Channel settings -> Integrations -> Webhooks).",
		position = 5,
		section = pushSection
	)
	default String discordWebhookUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "discordMention",
		name = "Discord mention",
		description = "Who to ping when a notification arrives. Paste a user id, a role id prefixed with &, "
			+ "or @everyone / @here. To copy an id, enable Developer Mode in Discord, then right click the "
			+ "user or role and Copy ID. Leave empty for no ping.",
		position = 6,
		section = pushSection
	)
	default String discordMention()
	{
		return "";
	}

	@ConfigItem(
		keyName = "discordMentionUrgentOnly",
		name = "Only ping when urgent",
		description = "Ping only for the most urgent notifications - death, low hitpoints, poison, running out "
			+ "of something, valuable drops - instead of every one.",
		position = 7,
		section = pushSection
	)
	default boolean discordMentionUrgentOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sendPushover",
		name = "Send to Pushover",
		description = "Deliver notifications through Pushover.",
		position = 8,
		section = pushSection
	)
	default boolean sendPushover()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pushoverUserKey",
		name = "Pushover user key",
		description = "Your Pushover user key.",
		position = 9,
		section = pushSection
	)
	default String pushoverUserKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "pushoverAppToken",
		name = "Pushover app token",
		description = "API token of the application you created on Pushover.",
		position = 10,
		section = pushSection
	)
	default String pushoverAppToken()
	{
		return "";
	}

	@ConfigItem(
		keyName = "sendTelegram",
		name = "Send to Telegram",
		description = "Deliver notifications through a Telegram bot.",
		position = 11,
		section = pushSection
	)
	default boolean sendTelegram()
	{
		return false;
	}

	@ConfigItem(
		keyName = "telegramBotToken",
		name = "Telegram bot token",
		description = "The token @BotFather gives you.",
		position = 12,
		section = pushSection
	)
	default String telegramBotToken()
	{
		return "";
	}

	@ConfigItem(
		keyName = "telegramChatId",
		name = "Telegram chat id",
		description = "Id of your conversation with the bot.",
		position = 13,
		section = pushSection
	)
	default String telegramChatId()
	{
		return "";
	}

	@ConfigItem(
		keyName = "sendWebhook",
		name = "Send to custom webhook",
		description = "Deliver notifications to a URL of your own.",
		position = 14,
		section = pushSection
	)
	default boolean sendWebhook()
	{
		return false;
	}

	@ConfigItem(
		keyName = "webhookUrl",
		name = "Custom webhook",
		description = "Any URL, which receives a POST with JSON {source,title,message,priority}.",
		position = 15,
		section = pushSection
	)
	default String webhookUrl()
	{
		return "";
	}

	@Range(min = 0, max = 600)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "pushCooldownSeconds",
		name = "Cooldown",
		description = "Minimum gap between two notifications, so a busy moment does not spam your phone.",
		position = 16,
		section = pushSection
	)
	default int pushCooldownSeconds()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "alsoDesktopNotify",
		name = "Also notify on desktop",
		description = "Fire the normal RuneLite desktop notification alongside the phone one.",
		position = 17,
		section = pushSection
	)
	default boolean alsoDesktopNotify()
	{
		return true;
	}

	@ConfigItem(
		keyName = "onlyWhenUnfocused",
		name = "Only when unfocused",
		description = "Notify only while the RuneLite window is not active, so nothing interrupts you at the keyboard.",
		position = 18,
		section = pushSection
	)
	default boolean onlyWhenUnfocused()
	{
		return true;
	}

	@ConfigItem(
		keyName = "attachScreenshot",
		name = "Attach screenshot",
		description = "Attach a picture of the game so you can see what happened. ntfy and Discord only. "
			+ "The image shows your username, chat and inventory - use a private topic.",
		position = 19,
		section = pushSection
	)
	default boolean attachScreenshot()
	{
		return false;
	}

	@ConfigItem(
		keyName = "quietHours",
		name = "Quiet hours",
		description = "Hold back notifications during the configured window.",
		position = 20,
		section = pushSection
	)
	default boolean quietHours()
	{
		return false;
	}

	@Range(min = 0, max = 23)
	@ConfigItem(
		keyName = "quietFrom",
		name = "Quiet from (hour)",
		description = "Hour the quiet window opens.",
		position = 21,
		section = pushSection
	)
	default int quietFrom()
	{
		return 23;
	}

	@Range(min = 0, max = 23)
	@ConfigItem(
		keyName = "quietTo",
		name = "Quiet until (hour)",
		description = "Hour the quiet window closes. The window may wrap past midnight.",
		position = 22,
		section = pushSection
	)
	default int quietTo()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "quietAllowUrgent",
		name = "Urgent still gets through",
		description = "During quiet hours, still deliver the most urgent notifications: death, low HP, poison, valuable drops.",
		position = 23,
		section = pushSection
	)
	default boolean quietAllowUrgent()
	{
		return true;
	}

	// ---------------------------------------------------------------- crab

	@ConfigItem(
		keyName = "crabEnabled",
		name = "Enable Gemstone Crab",
		description = "Track the crab and send its notifications.",
		position = 0,
		section = crabSection
	)
	default boolean crabEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "crabOverlay",
		name = "Show overlay",
		description = "Shows time until the crab burrows, your damage and the shell timer.",
		position = 1,
		section = crabSection
	)
	default boolean crabOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInfoboxes",
		name = "Show infobox timers",
		description = "Also show the burrow and shell countdowns as infoboxes.",
		position = 2,
		section = crabSection
	)
	default boolean showInfoboxes()
	{
		return true;
	}

	@Range(min = 5, max = 300)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "crabWarnSeconds",
		name = "Warn before burrow",
		description = "How many seconds before the crab burrows to notify you.",
		position = 3,
		section = crabSection
	)
	default int crabWarnSeconds()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "crabNotifyBurrow",
		name = "Notify on burrow",
		description = "Notify when the crab burrows and drops its shell, which lasts 90 seconds.",
		position = 4,
		section = crabSection
	)
	default boolean crabNotifyBurrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "crabNotifyIdle",
		name = "Notify if not attacking",
		description = "Notify when you have not landed a hit on the crab for a while.",
		position = 5,
		section = crabSection
	)
	default boolean crabNotifyIdle()
	{
		return true;
	}

	@Range(min = 5, max = 300)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "crabIdleSeconds",
		name = "Not attacking threshold",
		description = "How many seconds without a hit before warning you.",
		position = 6,
		section = crabSection
	)
	default int crabIdleSeconds()
	{
		return 30;
	}

	// ---------------------------------------------------------------- afk

	@ConfigItem(
		keyName = "idleLogoutWarning",
		name = "Warn before logout",
		description = "Notify just before the game logs you out for inactivity.",
		position = 0,
		section = afkSection
	)
	default boolean idleLogoutWarning()
	{
		return true;
	}

	@Range(min = 5, max = 120)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "idleLogoutWarnSeconds",
		name = "Seconds before logout",
		description = "How many seconds before the automatic logout to warn you.",
		position = 1,
		section = afkSection
	)
	default int idleLogoutWarnSeconds()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "lowHitpointsWarning",
		name = "Low hitpoints",
		description = "Notify when your hitpoints drop below the threshold.",
		position = 2,
		section = afkSection
	)
	default boolean lowHitpointsWarning()
	{
		return true;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(
		keyName = "lowHitpointsThreshold",
		name = "Hitpoints threshold",
		description = "Hitpoints level at which to warn.",
		position = 3,
		section = afkSection
	)
	default int lowHitpointsThreshold()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "lowPrayerWarning",
		name = "Low prayer",
		description = "Notify when your prayer points drop below the threshold.",
		position = 4,
		section = afkSection
	)
	default boolean lowPrayerWarning()
	{
		return true;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(
		keyName = "lowPrayerThreshold",
		name = "Prayer threshold",
		description = "Prayer points at which to warn.",
		position = 5,
		section = afkSection
	)
	default int lowPrayerThreshold()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "poisonNotify",
		name = "Poison and venom",
		description = "Notify when you become poisoned or envenomed.",
		position = 6,
		section = afkSection
	)
	default boolean poisonNotify()
	{
		return true;
	}

	@ConfigItem(
		keyName = "valuableDropNotify",
		name = "Valuable drop",
		description = "Notify when an NPC drops loot worth more than the threshold.",
		position = 7,
		section = afkSection
	)
	default boolean valuableDropNotify()
	{
		return true;
	}

	@Range(min = 1000, max = 100_000_000)
	@ConfigItem(
		keyName = "valuableDropThreshold",
		name = "Drop threshold (gp)",
		description = "Total Grand Exchange value of a drop at which to notify. Untradeable items count as zero.",
		position = 8,
		section = afkSection
	)
	default int valuableDropThreshold()
	{
		return 50_000;
	}

	@ConfigItem(
		keyName = "randomEventNotify",
		name = "Random event",
		description = "Notify when a random event NPC (Genie, Drunken dwarf, Strange plant, ...) comes to talk to you.",
		position = 9,
		section = afkSection
	)
	default boolean randomEventNotify()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatTriggerRegex",
		name = "Chat trigger",
		description = "Regular expression matched against game messages; a match sends a notification. "
			+ "Example: pet|You have run out of|Congratulations. Leave empty to disable.",
		position = 10,
		section = afkSection
	)
	default String chatTriggerRegex()
	{
		return "";
	}

	// ---------------------------------------------------------------- skilling and combat

	@ConfigItem(
		keyName = "idleAnimationNotify",
		name = "Stopped working",
		description = "Notify when your animation stops - you stopped chopping, fishing, mining and so on.",
		position = 0,
		section = skillSection
	)
	default boolean idleAnimationNotify()
	{
		return true;
	}

	@Range(min = 3, max = 300)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "idleAnimationSeconds",
		name = "Stopped working threshold",
		description = "How many seconds without an animation before warning you.",
		position = 1,
		section = skillSection
	)
	default int idleAnimationSeconds()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "combatIdleNotify",
		name = "Out of combat",
		description = "Notify when you have had no opponent for a while. At the Gemstone Crab the crab warning takes over.",
		position = 2,
		section = skillSection
	)
	default boolean combatIdleNotify()
	{
		return false;
	}

	@Range(min = 5, max = 300)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "combatIdleSeconds",
		name = "Out of combat threshold",
		description = "How many seconds without an opponent before warning you.",
		position = 3,
		section = skillSection
	)
	default int combatIdleSeconds()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "inventoryFullNotify",
		name = "Inventory full",
		description = "Notify once all 28 slots are taken.",
		position = 4,
		section = skillSection
	)
	default boolean inventoryFullNotify()
	{
		return true;
	}

	@ConfigItem(
		keyName = "watchedItems",
		name = "Warn when out of",
		description = "Comma separated item names. When one runs out of your inventory you get a notification. "
			+ "Example: shark, prayer potion, varrock teleport. Leave empty to disable.",
		position = 5,
		section = skillSection
	)
	default String watchedItems()
	{
		return "";
	}

	@Range(min = 0, max = 10000)
	@ConfigItem(
		keyName = "ammoThreshold",
		name = "Low ammo threshold",
		description = "Warn when fewer than this many pieces remain in your ammo slot. 0 disables it.",
		position = 6,
		section = skillSection
	)
	default int ammoThreshold()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "specRestoredNotify",
		name = "Special attack full",
		description = "Notify when your special attack energy is back to 100%.",
		position = 7,
		section = skillSection
	)
	default boolean specRestoredNotify()
	{
		return false;
	}

	@ConfigItem(
		keyName = "aggroNotify",
		name = "Aggression expiring",
		description = "NPCs stop attacking after roughly ten minutes in one area. This estimates that timer from how "
			+ "long you have stood near the same spot, so it is an approximation, not a value read from the game.",
		position = 8,
		section = skillSection
	)
	default boolean aggroNotify()
	{
		return false;
	}

	@Range(min = 10, max = 300)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "aggroWarnSeconds",
		name = "Seconds before aggression ends",
		description = "How many seconds before aggression expires to warn you.",
		position = 9,
		section = skillSection
	)
	default int aggroWarnSeconds()
	{
		return 60;
	}

	// ---------------------------------------------------------------- account

	@ConfigItem(
		keyName = "levelUpNotify",
		name = "Level up",
		description = "Notify on every new level.",
		position = 0,
		section = accountSection
	)
	default boolean levelUpNotify()
	{
		return true;
	}

	@ConfigItem(
		keyName = "deathNotify",
		name = "Death",
		description = "Notify when your character dies.",
		position = 1,
		section = accountSection
	)
	default boolean deathNotify()
	{
		return true;
	}

	@ConfigItem(
		keyName = "geOfferNotify",
		name = "Grand Exchange offer done",
		description = "Notify when an offer finishes buying or selling.",
		position = 2,
		section = accountSection
	)
	default boolean geOfferNotify()
	{
		return true;
	}
}
