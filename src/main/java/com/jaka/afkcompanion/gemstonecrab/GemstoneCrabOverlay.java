package com.jaka.afkcompanion.gemstonecrab;

import com.jaka.afkcompanion.AfkCompanionConfig;
import com.jaka.afkcompanion.util.Format;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class GemstoneCrabOverlay extends OverlayPanel
{
	private static final Color ACCENT = new Color(0x8A, 0xC7, 0xDB);

	private final GemstoneCrabTracker tracker;
	private final AfkCompanionConfig config;

	@Inject
	GemstoneCrabOverlay(GemstoneCrabTracker tracker, AfkCompanionConfig config)
	{
		this.tracker = tracker;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setPreferredSize(new Dimension(150, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.crabEnabled() || !config.crabOverlay())
		{
			return null;
		}

		final boolean crab = tracker.isCrabPresent();
		final boolean shell = tracker.isShellPresent();

		if (!crab && !shell)
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Gemstone Crab")
			.color(ACCENT)
			.build());

		if (crab)
		{
			final int remaining = tracker.getRemainingSeconds();
			final Color color = remaining < 0
				? Color.LIGHT_GRAY
				: remaining <= config.crabWarnSeconds() ? Color.RED : Color.GREEN;

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Burrows in")
				.right(Format.time(remaining) + (tracker.isCalibrated() ? "" : "?"))
				.rightColor(color)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Your damage")
				.right(String.valueOf(tracker.getDamageThisCrab()))
				.build());

			final int sinceHit = tracker.getSecondsSinceLastHit();
			if (sinceHit >= 5)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Not hitting")
					.right(Format.time(sinceHit))
					.rightColor(sinceHit >= config.crabIdleSeconds() ? Color.RED : Color.YELLOW)
					.build());
			}
		}

		if (shell)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Shell")
				.right(Format.time(tracker.getShellSecondsLeft()))
				.rightColor(Color.ORANGE)
				.build());
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Session")
			.right(tracker.getCrabsCompleted() + " crab / " + tracker.getDamageSession() + " dmg")
			.rightColor(Color.LIGHT_GRAY)
			.build());

		return super.render(graphics);
	}
}
