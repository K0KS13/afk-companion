package com.jaka.afkcompanion.stats;

import com.jaka.afkcompanion.AfkCompanionConfig;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.client.config.ConfigManager;

/**
 * Session counters, plus lifetime totals that survive a restart.
 * <p>
 * Lifetime values live in the plugin's own config group under keys that have no
 * {@code @ConfigItem}, so they persist with the profile without cluttering the settings panel.
 */
@Singleton
public class SessionStats
{
	private static final String KEY_LIFETIME_CRABS = "lifetimeCrabs";
	private static final String KEY_LIFETIME_DAMAGE = "lifetimeDamage";
	private static final String KEY_LIFETIME_NOTIFICATIONS = "lifetimeNotifications";
	private static final String KEY_LIFETIME_SECONDS = "lifetimeSeconds";

	private final ConfigManager configManager;

	private Instant sessionStart = Instant.now();
	private long xpAtStart = -1;

	@Getter
	private int notifications;
	@Getter
	private int lifetimeCrabs;
	@Getter
	private long lifetimeDamage;
	@Getter
	private int lifetimeNotifications;
	@Getter
	private long lifetimeSeconds;

	private int savedCrabs;
	private long savedDamage;

	@Inject
	private SessionStats(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	public void load()
	{
		lifetimeCrabs = readInt(KEY_LIFETIME_CRABS);
		lifetimeDamage = readLong(KEY_LIFETIME_DAMAGE);
		lifetimeNotifications = readInt(KEY_LIFETIME_NOTIFICATIONS);
		lifetimeSeconds = readLong(KEY_LIFETIME_SECONDS);
	}

	public void startSession()
	{
		sessionStart = Instant.now();
		xpAtStart = -1;
		notifications = 0;
		savedCrabs = 0;
		savedDamage = 0;
	}

	public long getSessionSeconds()
	{
		return Duration.between(sessionStart, Instant.now()).getSeconds();
	}

	/**
	 * @param overallExperience the account's total XP right now
	 * @return XP gained since the session started, or 0 before the first reading
	 */
	public long getSessionXp(long overallExperience)
	{
		if (overallExperience <= 0)
		{
			return 0;
		}

		if (xpAtStart < 0)
		{
			xpAtStart = overallExperience;
			return 0;
		}

		return Math.max(0, overallExperience - xpAtStart);
	}

	public void noteNotification()
	{
		notifications++;
		lifetimeNotifications++;
	}

	/**
	 * Folds the crab tracker's running totals into the lifetime counters. Safe to call every
	 * tick: only the increase since the previous call is added.
	 */
	public void syncCrabProgress(int crabsCompleted, int sessionDamage)
	{
		if (crabsCompleted > savedCrabs)
		{
			lifetimeCrabs += crabsCompleted - savedCrabs;
			savedCrabs = crabsCompleted;
		}

		if (sessionDamage > savedDamage)
		{
			lifetimeDamage += sessionDamage - savedDamage;
			savedDamage = sessionDamage;
		}
	}

	public void save()
	{
		lifetimeSeconds += getSessionSeconds();
		sessionStart = Instant.now();

		write(KEY_LIFETIME_CRABS, lifetimeCrabs);
		write(KEY_LIFETIME_DAMAGE, lifetimeDamage);
		write(KEY_LIFETIME_NOTIFICATIONS, lifetimeNotifications);
		write(KEY_LIFETIME_SECONDS, lifetimeSeconds);
	}

	public void resetLifetime()
	{
		lifetimeCrabs = 0;
		lifetimeDamage = 0;
		lifetimeNotifications = 0;
		lifetimeSeconds = 0;
		save();
	}

	private int readInt(String key)
	{
		final Integer value = configManager.getConfiguration(AfkCompanionConfig.GROUP, key, Integer.class);
		return value == null ? 0 : value;
	}

	private long readLong(String key)
	{
		final Long value = configManager.getConfiguration(AfkCompanionConfig.GROUP, key, Long.class);
		return value == null ? 0 : value;
	}

	private void write(String key, Object value)
	{
		configManager.setConfiguration(AfkCompanionConfig.GROUP, key, value);
	}
}
