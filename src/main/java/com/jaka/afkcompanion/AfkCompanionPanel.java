package com.jaka.afkcompanion;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: live status, session and lifetime totals, a delivery test button and a log of
 * the notifications this session has produced.
 */
@Singleton
public class AfkCompanionPanel extends PluginPanel
{
	private static final int HISTORY_SIZE = 12;
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final JLabel serviceValue = value();
	private final JLabel controlValue = value();
	private final JLabel crabValue = value();
	private final JLabel shellValue = value();
	private final JLabel sessionValue = value();
	private final JLabel xpValue = value();
	private final JLabel sentValue = value();
	private final JLabel lifetimeCrabsValue = value();
	private final JLabel lifetimeSentValue = value();

	private final JPanel historyPanel = new JPanel();
	private final Deque<String> history = new ArrayDeque<>();

	private Runnable onTest = () ->
	{
	};
	private Runnable onResetSession = () ->
	{
	};

	@Inject
	private AfkCompanionPanel()
	{
		super(false);

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		content.add(header("Status"));
		content.add(rows(
			"Service", serviceValue,
			"Control", controlValue,
			"Burrows in", crabValue,
			"Shell", shellValue));

		content.add(spacer());
		content.add(header("Session"));
		content.add(rows(
			"Time", sessionValue,
			"XP", xpValue,
			"Sent", sentValue));

		content.add(spacer());
		content.add(header("All time"));
		content.add(rows(
			"Crabs", lifetimeCrabsValue,
			"Notifications", lifetimeSentValue));

		content.add(spacer());
		content.add(button("Send test notification", () -> onTest.run()));
		content.add(button("Reset session", () -> onResetSession.run()));

		content.add(spacer());
		content.add(header("Recent notifications"));
		historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
		historyPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		historyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(historyPanel);

		add(content, BorderLayout.NORTH);
		renderHistory();
	}

	public void setActions(Runnable onTest, Runnable onResetSession)
	{
		this.onTest = onTest;
		this.onResetSession = onResetSession;
	}

	/**
	 * Called from the client thread; all Swing work is pushed to the EDT.
	 */
	public void update(String service, String control, String burrow, String shell,
		String session, String xp, String sent, String lifetimeCrabs, String lifetimeSent)
	{
		SwingUtilities.invokeLater(() ->
		{
			serviceValue.setText(service);
			controlValue.setText(control);
			crabValue.setText(burrow);
			shellValue.setText(shell);
			sessionValue.setText(session);
			xpValue.setText(xp);
			sentValue.setText(sent);
			lifetimeCrabsValue.setText(lifetimeCrabs);
			lifetimeSentValue.setText(lifetimeSent);
		});
	}

	public void addHistory(String title, String message)
	{
		final String line = LocalTime.now().format(CLOCK) + "  " + title + " - " + message;

		SwingUtilities.invokeLater(() ->
		{
			history.addFirst(line);
			while (history.size() > HISTORY_SIZE)
			{
				history.removeLast();
			}
			renderHistory();
		});
	}

	public void clearHistory()
	{
		SwingUtilities.invokeLater(() ->
		{
			history.clear();
			renderHistory();
		});
	}

	private void renderHistory()
	{
		historyPanel.removeAll();

		if (history.isEmpty())
		{
			historyPanel.add(note("Nothing yet this session."));
		}
		else
		{
			for (String line : history)
			{
				historyPanel.add(note(line));
			}
		}

		historyPanel.revalidate();
		historyPanel.repaint();
	}

	private static JLabel header(String text)
	{
		final JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		return label;
	}

	private static JLabel value()
	{
		final JLabel label = new JLabel("-");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}

	private static JLabel note(String text)
	{
		// HTML wrapping keeps long notification text inside the narrow side panel.
		final JLabel label = new JLabel("<html><body style='width:150px'>" + escape(text) + "</body></html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
		return label;
	}

	private JPanel rows(Object... labelsAndValues)
	{
		final JPanel panel = new JPanel(new GridLayout(labelsAndValues.length / 2, 2, 4, 2));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

		for (int i = 0; i < labelsAndValues.length; i += 2)
		{
			final JLabel key = new JLabel((String) labelsAndValues[i]);
			key.setFont(FontManager.getRunescapeSmallFont());
			key.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			panel.add(key);
			panel.add((JLabel) labelsAndValues[i + 1]);
		}

		return panel;
	}

	private static JButton button(String text, Runnable action)
	{
		final JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setFocusPainted(false);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		button.addActionListener(e -> action.run());
		return button;
	}

	private static Component spacer()
	{
		final JPanel panel = new JPanel();
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
