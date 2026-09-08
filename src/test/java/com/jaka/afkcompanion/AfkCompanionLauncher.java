package com.jaka.afkcompanion;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Starts a RuneLite client with this plugin already loaded. Used by the {@code run} Gradle task
 * and as the main class of the standalone shadow jar.
 */
public class AfkCompanionLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AfkCompanionPlugin.class);
		RuneLite.main(args);
	}
}
