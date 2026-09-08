package com.jaka.afkcompanion;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.jaka.afkcompanion.gemstonecrab.CrabInfoBox;
import com.jaka.afkcompanion.gemstonecrab.GemstoneCrabOverlay;
import com.jaka.afkcompanion.gemstonecrab.GemstoneCrabTracker;
import com.jaka.afkcompanion.push.NtfyControl;
import com.jaka.afkcompanion.push.PushSender;
import com.jaka.afkcompanion.stats.SessionStats;
import com.jaka.afkcompanion.util.Format;
import com.jaka.afkcompanion.util.QuietHours;
import com.jaka.afkcompanion.watch.AfkWatchdog;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "AFK Companion",
	description = "Gemstone Crab burrow timer plus phone notifications for everything that ends an AFK",
	tags = {"afk", "gemstone", "crab", "notification", "push", "ntfy", "timer", "idle"}
)
public class AfkCompanionPlugin extends Plugin
{
	/** If the next frame does not arrive in this time, send without a screenshot. */
	private static final int SCREENSHOT_TIMEOUT_SECONDS = 2;

	/** How long after spawning we keep checking whether a random event NPC is talking to you. */
	private static final int RANDOM_EVENT_GRACE_TICKS = 4;

	/** At or above this priority a notification survives quiet hours. */
	private static final int URGENT_PRIORITY = 5;

	/** Lifetime totals are flushed to the config this often. */
	private static final int STATS_SAVE_TICKS = 100;

	private static final Set<String> RANDOM_EVENT_NPCS = ImmutableSet.of(
		"bee keeper",
		"capt' arnav",
		"sergeant damien",
		"drunken dwarf",
		"freaky forester",
		"genie",
		"evil bob",
		"postie pete",
		"leo",
		"mysterious old man",
		"quiz master",
		"dunce",
		"rick turpentine",
		"sandwich lady",
		"strange plant",
		"flippa",
		"niles",
		"miles",
		"giles",
		"molly",
		"mime",
		"pillory guard",
		"dr jekyll"
	);

	private static final Set<ChatMessageType> TRIGGERABLE_CHAT = ImmutableSet.of(
		ChatMessageType.GAMEMESSAGE,
		ChatMessageType.SPAM,
		ChatMessageType.ENGINE,
		ChatMessageType.BROADCAST,
		ChatMessageType.NPC_EXAMINE
	);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientUI clientUI;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private AfkCompanionConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private GemstoneCrabOverlay crabOverlay;

	@Inject
	private GemstoneCrabTracker tracker;

	@Inject
	private AfkWatchdog watchdog;

	@Inject
	private AfkCompanionPanel panel;

	@Inject
	private SessionStats stats;

	@Inject
	private PushSender pushSender;

	@Inject
	private NtfyControl ntfyControl;

	@Inject
	private Notifier notifier;

	@Inject
	private ConfigManager configManager;

	private final Map<NPC, Integer> pendingRandomEvents = new HashMap<>();

	private NavigationButton navButton;
	private CrabInfoBox burrowInfoBox;
	private CrabInfoBox shellInfoBox;

	private boolean warnedBurrowSoon;
	private boolean warnedNotAttacking;
	private int ticksSinceStatsSave;

	private String chatPatternSource;
	private Pattern chatPattern;

	@Override
	protected void startUp()
	{
		migrateSettings();

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");

		panel.setActions(this::sendTestNotification, this::resetSession);
		navButton = NavigationButton.builder()
			.tooltip("AFK Companion")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		overlayManager.add(crabOverlay);

		burrowInfoBox = new CrabInfoBox(icon, this, tracker, config, CrabInfoBox.Kind.BURROW);
		shellInfoBox = new CrabInfoBox(icon, this, tracker, config, CrabInfoBox.Kind.SHELL);
		infoBoxManager.addInfoBox(burrowInfoBox);
		infoBoxManager.addInfoBox(shellInfoBox);

		stats.load();
		stats.startSession();
		tracker.resetSession();
		watchdog.reset();
		pushSender.resetCounters();
		warnedBurrowSoon = false;
		warnedNotAttacking = false;

		ntfyControl.start(this::onRemoteCommand);

		// Picks up a crab that is already in the scene if you enable the plugin mid fight.
		clientThread.invokeLater(this::scanForCrab);
		log.debug("AFK Companion started");
	}

	@Override
	protected void shutDown()
	{
		ntfyControl.stop();
		stats.save();

		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(crabOverlay);
		infoBoxManager.removeInfoBox(burrowInfoBox);
		infoBoxManager.removeInfoBox(shellInfoBox);

		tracker.resetSession();
		watchdog.reset();
		pendingRandomEvents.clear();
	}

	@Provides
	AfkCompanionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AfkCompanionConfig.class);
	}

	/**
	 * Settings that changed shape between versions are carried over here, once, at startup.
	 */
	private void migrateSettings()
	{
		migrateSingleProviderSetting();
		migrateGlobalScreenshotSetting();
	}

	/**
	 * The screenshot used to be one switch covering every notification. Carry an existing
	 * "on" over to all four categories, which is what that setting meant.
	 */
	private void migrateGlobalScreenshotSetting()
	{
		final String legacy = configManager.getConfiguration(AfkCompanionConfig.GROUP, "attachScreenshot");
		if (legacy == null)
		{
			return;
		}

		if (Boolean.parseBoolean(legacy))
		{
			configManager.setConfiguration(AfkCompanionConfig.GROUP, "screenshotCrab", true);
			configManager.setConfiguration(AfkCompanionConfig.GROUP, "screenshotAfk", true);
			configManager.setConfiguration(AfkCompanionConfig.GROUP, "screenshotSkilling", true);
			configManager.setConfiguration(AfkCompanionConfig.GROUP, "screenshotAccount", true);
			log.debug("Migrated the old attachScreenshot setting to every category");
		}

		configManager.unsetConfiguration(AfkCompanionConfig.GROUP, "attachScreenshot");
	}

	/**
	 * Delivery used to be a single choice. Anyone upgrading has that old value stored, so switch
	 * on the matching target once and drop the dead key, rather than silently going quiet on them.
	 */
	private void migrateSingleProviderSetting()
	{
		final String legacy = configManager.getConfiguration(AfkCompanionConfig.GROUP, "pushProvider");
		if (legacy == null)
		{
			return;
		}

		final String key;
		switch (legacy)
		{
			case "NTFY":
				key = "sendNtfy";
				break;
			case "DISCORD":
				key = "sendDiscord";
				break;
			case "PUSHOVER":
				key = "sendPushover";
				break;
			case "TELEGRAM":
				key = "sendTelegram";
				break;
			case "WEBHOOK":
				key = "sendWebhook";
				break;
			default:
				key = null;
				break;
		}

		if (key != null)
		{
			configManager.setConfiguration(AfkCompanionConfig.GROUP, key, true);
			log.debug("Migrated the old pushProvider setting {} to {}", legacy, key);
		}

		configManager.unsetConfiguration(AfkCompanionConfig.GROUP, "pushProvider");
	}

	// ------------------------------------------------------------------ events

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!AfkCompanionConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		final String key = event.getKey();
		if ("ntfyControlTopic".equals(key) || "ntfyServer".equals(key) || "sendNtfy".equals(key))
		{
			ntfyControl.restart();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		final GameState state = event.getGameState();

		if (state == GameState.LOGGED_IN)
		{
			watchdog.onLogin();
			return;
		}

		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
		{
			tracker.reset();
			warnedBurrowSoon = false;
			warnedNotAttacking = false;
			pendingRandomEvents.clear();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		final NPC npc = event.getNpc();
		final boolean hadCrab = tracker.isCrabPresent();
		tracker.onNpcSpawned(npc);

		if (!hadCrab && tracker.isCrabPresent())
		{
			warnedBurrowSoon = false;
			warnedNotAttacking = false;
		}

		if (config.randomEventNotify() && RANDOM_EVENT_NPCS.contains(Text.standardize(npc.getName())))
		{
			// On spawn the NPC does not necessarily show who it is talking to yet.
			pendingRandomEvents.put(npc, RANDOM_EVENT_GRACE_TICKS);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		final NPC npc = event.getNpc();
		final boolean wasCrab = npc == tracker.getCrab();
		final int damage = tracker.getDamageThisCrab();

		tracker.onNpcDespawned(npc);
		pendingRandomEvents.remove(npc);

		if (wasCrab && config.crabEnabled() && config.crabNotifyBurrow() && damage > 0)
		{
			notify(NotificationCategory.CRAB, "Gemstone Crab", "The crab burrowed away. The shell is minable for 90s - then follow it through the tunnel.", 4);
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!event.getHitsplat().isMine())
		{
			return;
		}

		watchdog.noteCombat();

		final boolean crabHit = event.getActor() == tracker.getCrab();
		tracker.onDamage(event.getActor(), event.getHitsplat().getAmount());

		if (crabHit)
		{
			warnedNotAttacking = false;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!TRIGGERABLE_CHAT.contains(event.getType()))
		{
			return;
		}

		final Pattern pattern = chatPattern();
		if (pattern == null)
		{
			return;
		}

		final String message = Text.removeTags(event.getMessage());
		if (pattern.matcher(message).find())
		{
			notify(NotificationCategory.AFK, "Chat trigger", message, 4);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		watchdog.onStatChanged(event, this::notify);
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		watchdog.onActorDeath(event, this::notify);
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		watchdog.onGrandExchangeOfferChanged(event, this::notify);
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		watchdog.onNpcLootReceived(event, this::notify);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!tracker.isCrabPresent())
		{
			scanForCrab();
		}

		tracker.onGameTick();

		final boolean crabHandlesCombat = config.crabEnabled() && config.crabNotifyIdle() && tracker.isCrabPresent();

		if (config.crabEnabled())
		{
			checkBurrowWarning();
			checkNotAttackingWarning();
		}

		checkRandomEvents();
		watchdog.onGameTick(this::notify, crabHandlesCombat);

		stats.syncCrabProgress(tracker.getCrabsCompleted(), tracker.getDamageSession());
		updatePanel();

		if (++ticksSinceStatsSave >= STATS_SAVE_TICKS)
		{
			ticksSinceStatsSave = 0;
			stats.save();
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		final String command = event.getCommand();

		if ("afktest".equalsIgnoreCase(command))
		{
			sendTestNotification();
		}
		else if ("afkreset".equalsIgnoreCase(command))
		{
			resetSession();
		}
		else if ("afkstatus".equalsIgnoreCase(command))
		{
			chat("AFK Companion: " + buildStatus());
		}
	}

	// ------------------------------------------------------------------ crab

	private void checkBurrowWarning()
	{
		if (warnedBurrowSoon || !tracker.isCrabPresent())
		{
			return;
		}

		final int remaining = tracker.getRemainingSeconds();
		if (remaining < 0 || remaining > config.crabWarnSeconds())
		{
			return;
		}

		warnedBurrowSoon = true;
		notify(NotificationCategory.CRAB, "Gemstone Crab", "The crab burrows in " + remaining + "s. Get your pickaxe ready for the shell.", 4);
	}

	private void checkNotAttackingWarning()
	{
		if (!config.crabNotifyIdle() || warnedNotAttacking || !tracker.isCrabPresent())
		{
			return;
		}

		final int sinceHit = tracker.getSecondsSinceLastHit();
		if (sinceHit < 0 || sinceHit < config.crabIdleSeconds())
		{
			return;
		}

		warnedNotAttacking = true;
		notify(NotificationCategory.CRAB, "Gemstone Crab", "You have not hit the crab for " + sinceHit + "s - you are probably out of the fight.", 4);
	}

	private void checkRandomEvents()
	{
		if (pendingRandomEvents.isEmpty())
		{
			return;
		}

		final Iterator<Map.Entry<NPC, Integer>> it = pendingRandomEvents.entrySet().iterator();
		while (it.hasNext())
		{
			final Map.Entry<NPC, Integer> entry = it.next();
			final NPC npc = entry.getKey();

			if (npc.getInteracting() == client.getLocalPlayer())
			{
				it.remove();
				notify(NotificationCategory.AFK, "Random event", npc.getName() + " came to visit you.", 4);
				continue;
			}

			final int ticksLeft = entry.getValue() - 1;
			if (ticksLeft <= 0)
			{
				it.remove();
			}
			else
			{
				entry.setValue(ticksLeft);
			}
		}
	}

	private void scanForCrab()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (NPC npc : client.getNpcs())
		{
			tracker.onNpcSpawned(npc);
			if (tracker.isCrabPresent())
			{
				break;
			}
		}
	}

	// ------------------------------------------------------------------ panel and remote

	private void updatePanel()
	{
		final String service = pushSender.hasAnyProvider()
			? pushSender.enabledLabel() + " (" + pushSender.getSentCount() + " ok / " + pushSender.getFailedCount() + " fail)"
			: "off";

		final String control = !ntfyControl.isEnabled()
			? "off"
			: ntfyControl.isConnected() ? "connected" : "reconnecting";

		final long seconds = stats.getSessionSeconds();
		final long xp = stats.getSessionXp(client.getOverallExperience());

		panel.update(
			service,
			control,
			tracker.isCrabPresent() ? Format.time(tracker.getRemainingSeconds()) : "-",
			tracker.isShellPresent() ? Format.time(tracker.getShellSecondsLeft()) : "-",
			Format.time((int) seconds),
			xp <= 0 ? "-" : Format.perHour(xp, seconds),
			String.valueOf(stats.getNotifications()),
			String.valueOf(stats.getLifetimeCrabs()),
			String.valueOf(stats.getLifetimeNotifications()));
	}

	private void sendTestNotification()
	{
		if (!pushSender.hasAnyProvider())
		{
			chat("AFK Companion: no delivery service is switched on, see the plugin settings.");
			return;
		}

		pushSender.resetCooldown();
		pushSender.send("AFK Companion", "Test notification - if this reached your phone, it works.", 3, null,
			result -> chat("AFK Companion: test notification " + result + "."));
	}

	private void resetSession()
	{
		tracker.resetSession();
		watchdog.reset();
		watchdog.onLogin();
		stats.startSession();
		pushSender.resetCounters();
		panel.clearHistory();
		chat("AFK Companion: session reset.");
	}

	/**
	 * Handles a command published to the control topic. Arrives on a WebSocket thread, so any
	 * game state is read back on the client thread. Commands only ever report - they never act.
	 */
	private void onRemoteCommand(String command)
	{
		log.debug("Remote command: {}", command);

		clientThread.invokeLater(() ->
		{
			switch (command)
			{
				case "status":
					pushSender.sendImmediate("Status", buildStatus(), 3);
					break;
				case "crab":
					pushSender.sendImmediate("Gemstone Crab", buildCrabStatus(), 3);
					break;
				case "stats":
					pushSender.sendImmediate("Stats", buildStats(), 3);
					break;
				case "help":
					pushSender.sendImmediate("AFK Companion", "Commands: status, crab, stats, help.", 3);
					break;
				default:
					log.debug("Unknown remote command: {}", command);
					break;
			}
		});
	}

	private String buildStatus()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return "Not logged in.";
		}

		final StringBuilder sb = new StringBuilder();
		sb.append("HP ").append(client.getBoostedSkillLevel(Skill.HITPOINTS))
			.append("/").append(client.getRealSkillLevel(Skill.HITPOINTS));
		sb.append(", prayer ").append(client.getBoostedSkillLevel(Skill.PRAYER))
			.append("/").append(client.getRealSkillLevel(Skill.PRAYER));

		if (tracker.isCrabPresent())
		{
			sb.append(", crab burrows in ").append(Format.time(tracker.getRemainingSeconds()));
		}

		final int aggro = watchdog.getAggroSecondsLeft();
		if (aggro >= 0)
		{
			sb.append(", aggression ").append(Format.time(aggro));
		}

		final long seconds = stats.getSessionSeconds();
		sb.append(", session ").append(Format.time((int) seconds));

		final long xp = stats.getSessionXp(client.getOverallExperience());
		if (xp > 0)
		{
			sb.append(" at ").append(Format.perHour(xp, seconds));
		}

		return sb.append('.').toString();
	}

	private String buildCrabStatus()
	{
		if (!tracker.isCrabPresent())
		{
			return tracker.isShellPresent()
				? "Crab burrowed, shell available for " + Format.time(tracker.getShellSecondsLeft()) + "."
				: "No crab in the scene.";
		}

		return "Burrows in " + Format.time(tracker.getRemainingSeconds())
			+ ", your damage " + tracker.getDamageThisCrab()
			+ ", session " + tracker.getCrabsCompleted() + " crabs.";
	}

	private String buildStats()
	{
		final long seconds = stats.getSessionSeconds();
		return "Session " + Format.time((int) seconds)
			+ ", " + stats.getNotifications() + " notifications, "
			+ tracker.getCrabsCompleted() + " crabs. All time: "
			+ stats.getLifetimeCrabs() + " crabs, "
			+ stats.getLifetimeNotifications() + " notifications.";
	}

	// ------------------------------------------------------------------ delivery

	private Pattern chatPattern()
	{
		final String source = config.chatTriggerRegex() == null ? "" : config.chatTriggerRegex().trim();

		if (source.isEmpty())
		{
			chatPatternSource = "";
			chatPattern = null;
			return null;
		}

		if (source.equals(chatPatternSource))
		{
			return chatPattern;
		}

		chatPatternSource = source;
		try
		{
			chatPattern = Pattern.compile(source, Pattern.CASE_INSENSITIVE);
		}
		catch (PatternSyntaxException e)
		{
			chatPattern = null;
			log.warn("Invalid chat trigger pattern: {}", e.getMessage());
			chat("AFK Companion: the chat trigger is not a valid regular expression.");
		}

		return chatPattern;
	}

	private void notify(NotificationCategory category, String title, String message, int priority)
	{
		if (config.onlyWhenUnfocused() && clientUI.isFocused())
		{
			log.debug("Notification skipped, window is focused: {}", title);
			return;
		}

		if (config.quietHours()
			&& QuietHours.isQuiet(LocalTime.now().getHour(), config.quietFrom(), config.quietTo())
			&& !(config.quietAllowUrgent() && priority >= URGENT_PRIORITY))
		{
			log.debug("Notification skipped, quiet hours: {}", title);
			return;
		}

		log.debug("Notification: {} - {}", title, message);
		stats.noteNotification();
		panel.addHistory(title, message);

		if (config.alsoDesktopNotify())
		{
			notifier.notify(title + ": " + message);
		}

		if (!screenshotWanted(category)
			|| !pushSender.anySupportsScreenshots()
			|| client.getGameState() != GameState.LOGGED_IN)
		{
			pushSender.send(title, message, priority);
			return;
		}

		// The image only exists once the next frame is drawn; if that never happens - a
		// minimised window, for instance - fall back to a plain notification.
		final AtomicBoolean sent = new AtomicBoolean();

		drawManager.requestNextFrameListener(image ->
		{
			if (sent.compareAndSet(false, true))
			{
				pushSender.send(title, message, priority, ImageUtil.bufferedImageFromImage(image));
			}
		});

		executor.schedule(() ->
		{
			if (sent.compareAndSet(false, true))
			{
				pushSender.send(title, message, priority);
			}
		}, SCREENSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}

	/**
	 * A screenshot is worth its bandwidth for some notifications and pure noise for others,
	 * so it is chosen per kind of event rather than once for everything.
	 */
	private boolean screenshotWanted(NotificationCategory category)
	{
		switch (category)
		{
			case CRAB:
				return config.screenshotCrab();
			case AFK:
				return config.screenshotAfk();
			case SKILLING:
				return config.screenshotSkilling();
			case ACCOUNT:
				return config.screenshotAccount();
			default:
				return false;
		}
	}

	/**
	 * Writes a line to the game chat. Delivery results arrive on an HTTP thread and the pattern
	 * warning can come from anywhere, so this always hops to the client thread first -
	 * {@code invoke} runs straight away when it is already there.
	 */
	private void chat(String message)
	{
		clientThread.invoke(() -> client.addChatMessage(ChatMessageType.CONSOLE, "", message, null));
	}
}
