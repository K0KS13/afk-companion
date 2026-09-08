package com.jaka.afkcompanion.gemstonecrab;

import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.client.util.Text;

/**
 * Tracks the Gemstone Crab.
 * <p>
 * The crab's health bar does not drop from damage - it drops with time. When it empties the
 * crab burrows, sheds its shell and moves to the next of three mines, on a roughly ten minute
 * cycle. The health bar is therefore a countdown, and reading it tells us how long is left.
 * <p>
 * The bar is coarse (a few dozen steps), so instead of trusting a fixed cycle length we learn
 * how many ticks one step actually lasts and interpolate between steps. That turns a value
 * that jumps every ~20 seconds into a smooth per-second estimate.
 */
@Slf4j
@Singleton
public class GemstoneCrabTracker
{
	private static final String CRAB_NAME = "gemstone crab";
	private static final String SHELL_KEYWORD = "shell";

	/** Ids are more reliable than names; the name check stays as a fallback. */
	private static final int CRAB_ID = 14779;
	private static final int SHELL_ID = 14780;

	/** Fallback cycle length: 10 minutes, or 1000 game ticks. */
	private static final int DEFAULT_CYCLE_TICKS = 1000;

	/** The shed shell can be mined for 90 seconds, or 150 ticks. */
	private static final int SHELL_TICKS = 150;

	@Getter
	private NPC crab;
	@Getter
	private NPC shell;

	/** Estimated ticks until the crab burrows; -1 while unknown. */
	@Getter
	private int remainingTicks = -1;
	@Getter
	private int shellTicksLeft = -1;

	/** Damage you have dealt to the crab currently in the scene. */
	@Getter
	private int damageThisCrab;
	@Getter
	private int damageSession;
	@Getter
	private int crabsCompleted;
	/** Ticks since you last landed a hit on the crab; -1 if you never have. */
	@Getter
	private int ticksSinceLastHit = -1;

	private int lastRatio = -1;
	private int ticksSinceRatioChange;
	/** Learned length of one health bar step, in ticks. */
	private double stepTicks = -1;

	public boolean isCrabPresent()
	{
		return crab != null;
	}

	public boolean isShellPresent()
	{
		return shell != null && shellTicksLeft > 0;
	}

	public int getRemainingSeconds()
	{
		return remainingTicks < 0 ? -1 : (int) Math.round(remainingTicks * 0.6d);
	}

	public int getShellSecondsLeft()
	{
		return shellTicksLeft < 0 ? -1 : (int) Math.round(shellTicksLeft * 0.6d);
	}

	public int getSecondsSinceLastHit()
	{
		return ticksSinceLastHit < 0 ? -1 : (int) Math.round(ticksSinceLastHit * 0.6d);
	}

	/**
	 * @return true once the countdown has been calibrated against an observed step change
	 */
	public boolean isCalibrated()
	{
		return stepTicks > 0;
	}

	public void reset()
	{
		crab = null;
		shell = null;
		remainingTicks = -1;
		shellTicksLeft = -1;
		damageThisCrab = 0;
		ticksSinceLastHit = -1;
		lastRatio = -1;
		ticksSinceRatioChange = 0;
		stepTicks = -1;
	}

	public void resetSession()
	{
		reset();
		damageSession = 0;
		crabsCompleted = 0;
	}

	public void onNpcSpawned(NPC npc)
	{
		if (isCrab(npc))
		{
			crab = npc;
			lastRatio = -1;
			ticksSinceRatioChange = 0;
			damageThisCrab = 0;
			ticksSinceLastHit = -1;
			remainingTicks = -1;
			log.debug("Gemstone Crab spawned (id {})", npc.getId());
		}
		else if (isShell(npc))
		{
			shell = npc;
			shellTicksLeft = SHELL_TICKS;
			log.debug("Crab shell spawned (id {})", npc.getId());
		}
	}

	public void onNpcDespawned(NPC npc)
	{
		if (npc == crab)
		{
			if (damageThisCrab > 0)
			{
				crabsCompleted++;
			}
			crab = null;
			remainingTicks = 0;
			lastRatio = -1;
		}
		else if (npc == shell)
		{
			shell = null;
			shellTicksLeft = -1;
		}
	}

	public void onDamage(Actor target, int damage)
	{
		if (target != crab)
		{
			return;
		}

		if (damage > 0)
		{
			damageThisCrab += damage;
			damageSession += damage;
		}

		ticksSinceLastHit = 0;
	}

	public void onGameTick()
	{
		if (shellTicksLeft > 0)
		{
			shellTicksLeft--;
		}

		if (crab == null)
		{
			return;
		}

		if (ticksSinceLastHit >= 0)
		{
			ticksSinceLastHit++;
		}

		final int ratio = crab.getHealthRatio();
		final int scale = crab.getHealthScale();

		if (ratio < 0 || scale <= 0)
		{
			// No health bar in view (you are not in combat with it) - keep counting down
			// from the last estimate rather than throwing it away.
			if (remainingTicks > 0)
			{
				remainingTicks--;
			}
			return;
		}

		if (lastRatio < 0)
		{
			// First reading: a rough estimate straight from the fraction shown.
			lastRatio = ratio;
			ticksSinceRatioChange = 0;
			remainingTicks = (int) Math.round(ratio / (double) scale * DEFAULT_CYCLE_TICKS);
			return;
		}

		if (ratio != lastRatio)
		{
			if (ratio == lastRatio - 1 && ticksSinceRatioChange > 3)
			{
				learnStep(ticksSinceRatioChange);
			}
			lastRatio = ratio;
			ticksSinceRatioChange = 0;
			remainingTicks = (int) Math.round(ratio * stepTicks(scale));
			return;
		}

		ticksSinceRatioChange++;
		remainingTicks = Math.max(0, (int) Math.round(ratio * stepTicks(scale)) - ticksSinceRatioChange);
	}

	private double stepTicks(int scale)
	{
		if (stepTicks > 0)
		{
			return stepTicks;
		}

		return DEFAULT_CYCLE_TICKS / (double) Math.max(1, scale);
	}

	private void learnStep(int observed)
	{
		if (observed < 5 || observed > 300)
		{
			// Nonsense reading - a hop, a reload, or a bar that was hidden part of the time.
			return;
		}

		stepTicks = stepTicks < 0 ? observed : (stepTicks * 0.7d + observed * 0.3d);
		log.debug("Learned health bar step length: {} ticks", String.format("%.1f", stepTicks));
	}

	static boolean isCrab(NPC npc)
	{
		if (npc == null)
		{
			return false;
		}

		if (npc.getId() == CRAB_ID)
		{
			return true;
		}

		final String name = name(npc);
		return name.contains(CRAB_NAME) && !name.contains(SHELL_KEYWORD);
	}

	static boolean isShell(NPC npc)
	{
		if (npc == null)
		{
			return false;
		}

		if (npc.getId() == SHELL_ID)
		{
			return true;
		}

		final String name = name(npc);
		return name.contains(CRAB_NAME) && name.contains(SHELL_KEYWORD);
	}

	private static String name(NPC npc)
	{
		final String name = npc == null ? null : npc.getName();
		return name == null ? "" : Text.standardize(name);
	}
}
