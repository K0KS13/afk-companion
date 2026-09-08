package com.jaka.afkcompanion.gemstonecrab;

import com.jaka.afkcompanion.AfkCompanionConfig;
import com.jaka.afkcompanion.util.Format;
import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/**
 * Countdown infobox for the crab. Rather than being added and removed as the fight moves on,
 * it stays registered and simply declines to render when there is nothing to show.
 */
public class CrabInfoBox extends InfoBox
{
	public enum Kind
	{
		BURROW,
		SHELL
	}

	private final GemstoneCrabTracker tracker;
	private final AfkCompanionConfig config;
	private final Kind kind;

	public CrabInfoBox(BufferedImage image, Plugin plugin, GemstoneCrabTracker tracker,
		AfkCompanionConfig config, Kind kind)
	{
		super(image, plugin);
		this.tracker = tracker;
		this.config = config;
		this.kind = kind;
		setTooltip(kind == Kind.BURROW ? "Time until the Gemstone Crab burrows" : "Time left to mine the shell");
	}

	@Override
	public String getText()
	{
		return Format.time(kind == Kind.BURROW ? tracker.getRemainingSeconds() : tracker.getShellSecondsLeft());
	}

	@Override
	public Color getTextColor()
	{
		if (kind == Kind.SHELL)
		{
			return Color.ORANGE;
		}

		final int remaining = tracker.getRemainingSeconds();
		if (remaining < 0)
		{
			return Color.LIGHT_GRAY;
		}

		return remaining <= config.crabWarnSeconds() ? Color.RED : Color.WHITE;
	}

	@Override
	public boolean render()
	{
		if (!config.crabEnabled() || !config.showInfoboxes())
		{
			return false;
		}

		return kind == Kind.BURROW ? tracker.isCrabPresent() : tracker.isShellPresent();
	}
}
