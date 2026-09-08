package com.jaka.afkcompanion.watch;

/**
 * Where {@link AfkWatchdog} hands its notifications. Keeping this an interface means the
 * watchdog knows nothing about quiet hours, window focus or delivery - the plugin owns that.
 */
@FunctionalInterface
public interface NotificationSink
{
	void notify(String title, String message, int priority);
}
