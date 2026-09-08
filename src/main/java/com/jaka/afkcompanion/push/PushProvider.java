package com.jaka.afkcompanion.push;

/**
 * A delivery target. Several can be enabled at once, in which case every notification goes
 * to all of them.
 */
public enum PushProvider
{
	NTFY("ntfy"),
	DISCORD("Discord"),
	PUSHOVER("Pushover"),
	TELEGRAM("Telegram"),
	WEBHOOK("webhook");

	private final String label;

	PushProvider(String label)
	{
		this.label = label;
	}

	/**
	 * @return true if this provider can carry an image alongside the message
	 */
	public boolean supportsScreenshots()
	{
		return this == NTFY || this == DISCORD;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
