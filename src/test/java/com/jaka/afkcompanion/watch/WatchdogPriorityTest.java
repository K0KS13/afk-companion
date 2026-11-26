package com.jaka.afkcompanion.watch;

import com.jaka.afkcompanion.AfkCompanionConfig;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WatchdogPriorityTest
{
	private final List<Integer> priorities = new ArrayList<>();
	private final NotificationSink sink = (category, title, message, priority) -> priorities.add(priority);

	private AfkCompanionConfig config;
	private AfkWatchdog watchdog;

	@Before
	public void setUp()
	{
		config = mock(AfkCompanionConfig.class);
		when(config.superiorNotify()).thenReturn(true);
		when(config.taskCompleteNotify()).thenReturn(true);
		when(config.cannonEmptyNotify()).thenReturn(true);
		when(config.cannonDecayNotify()).thenReturn(true);
		when(config.slaughterBrokeNotify()).thenReturn(true);
		when(config.expeditiousBrokeNotify()).thenReturn(true);

		watchdog = new AfkWatchdog(mock(Client.class), config, mock(ItemManager.class));
	}

	private void fireEveryUrgentCapableAlert()
	{
		watchdog.onChatAlert(ChatAlert.SUPERIOR, sink);
		watchdog.onChatAlert(ChatAlert.TASK_COMPLETE, sink);
		watchdog.onChatAlert(ChatAlert.CANNON_EMPTY, sink);
		// The cannon has to be seen standing before its decay counts as news.
		watchdog.onCannonDecayedChanged(0, sink);
		watchdog.onCannonDecayedChanged(1, sink);
	}

	@Test
	public void slayerAlertsAreNotUrgentUnlessAsked()
	{
		fireEveryUrgentCapableAlert();

		assertEquals(4, priorities.size());
		for (int priority : priorities)
		{
			assertEquals(4, priority);
		}
	}

	@Test
	public void tickingTheBoxMakesThemUrgent()
	{
		when(config.slayerBreakQuietHours()).thenReturn(true);

		fireEveryUrgentCapableAlert();

		assertEquals(4, priorities.size());
		for (int priority : priorities)
		{
			assertEquals(5, priority);
		}
	}

	@Test
	public void braceletsStayOrdinaryEvenWithTheBoxTicked()
	{
		when(config.slayerBreakQuietHours()).thenReturn(true);

		watchdog.onChatAlert(ChatAlert.SLAUGHTER_BROKE, sink);
		watchdog.onChatAlert(ChatAlert.EXPEDITIOUS_BROKE, sink);

		assertEquals(2, priorities.size());
		assertEquals(4, (int) priorities.get(0));
		assertEquals(4, (int) priorities.get(1));
	}

	@Test
	public void bothBoxesAreOffUntilTheUserTicksThem()
	{
		// The real defaults rather than stubs: this is what keeps these off at night for everyone
		// who never opens the settings.
		final AfkCompanionConfig defaults = mock(AfkCompanionConfig.class, CALLS_REAL_METHODS);

		assertFalse(defaults.slayerBreakQuietHours());
		assertFalse(defaults.clueNestBreakQuietHours());
	}
}
