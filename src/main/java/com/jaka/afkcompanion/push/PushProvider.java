package com.jaka.afkcompanion.push;

public enum PushProvider
{
	OFF("Off"),
	NTFY("ntfy.sh"),
	DISCORD("Discord webhook"),
	PUSHOVER("Pushover"),
	TELEGRAM("Telegram bot"),
	WEBHOOK("Custom webhook");

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
