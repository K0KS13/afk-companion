package com.jaka.afkcompanion.watch;

import com.jaka.afkcompanion.NotificationCategory;

/**
 * Where {@link AfkWatchdog} hands its notifications. Keeping this an interface means the
 * watchdog knows nothing about quiet hours, window focus, screenshots or delivery - the
 * plugin owns all of that.
 */
@FunctionalInterface
public interface NotificationSink
{
	void notify(NotificationCategory category, String title, String message, int priority);
}
