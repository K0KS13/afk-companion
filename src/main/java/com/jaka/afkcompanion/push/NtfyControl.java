package com.jaka.afkcompanion.push;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.jaka.afkcompanion.AfkCompanionConfig;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Listens on an ntfy topic so you can ask the client a question from your phone.
 * <p>
 * This is deliberately read-only: commands can request information, never perform an
 * action in the game. Anyone who knows the control topic can query it, which is why it
 * is opt-in and documented as such.
 */
@Slf4j
@Singleton
public class NtfyControl
{
	private static final int RETRY_MIN_SECONDS = 5;
	private static final int RETRY_MAX_SECONDS = 300;

	private final OkHttpClient httpClient;
	private final AfkCompanionConfig config;
	private final ScheduledExecutorService executor;
	private final Gson gson;

	private WebSocket socket;
	private Consumer<String> commandHandler;
	private volatile boolean running;
	private volatile boolean connected;
	private int retrySeconds = RETRY_MIN_SECONDS;

	@Inject
	private NtfyControl(OkHttpClient httpClient, AfkCompanionConfig config,
		ScheduledExecutorService executor, Gson gson)
	{
		this.httpClient = httpClient;
		this.config = config;
		this.executor = executor;
		this.gson = gson;
	}

	public boolean isConnected()
	{
		return connected;
	}

	public boolean isEnabled()
	{
		return config.sendNtfy() && !controlTopic().isEmpty();
	}

	public synchronized void start(Consumer<String> handler)
	{
		this.commandHandler = handler;

		if (running)
		{
			return;
		}

		if (!isEnabled())
		{
			return;
		}

		running = true;
		retrySeconds = RETRY_MIN_SECONDS;
		connect();
	}

	public synchronized void stop()
	{
		running = false;
		connected = false;

		if (socket != null)
		{
			socket.close(1000, "plugin stopped");
			socket = null;
		}
	}

	/**
	 * Re-reads the configuration and reconnects if the topic changed.
	 */
	public synchronized void restart()
	{
		final Consumer<String> handler = commandHandler;
		stop();

		if (handler != null && isEnabled())
		{
			start(handler);
		}
	}

	private synchronized void connect()
	{
		if (!running)
		{
			return;
		}

		final HttpUrl base = HttpUrl.parse(server());
		if (base == null)
		{
			log.warn("ntfy server is not a valid URL, control channel disabled");
			running = false;
			return;
		}

		// OkHttp performs the WebSocket upgrade itself, so an http(s) URL is what it wants here.
		final HttpUrl url = base.newBuilder()
			.addPathSegment(controlTopic())
			.addPathSegment("ws")
			.build();

		log.debug("Opening ntfy control channel on {}", url);
		socket = httpClient.newWebSocket(new Request.Builder().url(url).build(), new Listener());
	}

	private void scheduleReconnect()
	{
		if (!running)
		{
			return;
		}

		final int delay = retrySeconds;
		retrySeconds = Math.min(RETRY_MAX_SECONDS, retrySeconds * 2);
		log.debug("Reconnecting to ntfy control channel in {}s", delay);
		executor.schedule(this::connect, delay, TimeUnit.SECONDS);
	}

	private String server()
	{
		final String server = config.ntfyServer() == null ? "" : config.ntfyServer().trim();
		return server.isEmpty() ? "https://ntfy.sh" : server;
	}

	private String controlTopic()
	{
		return config.ntfyControlTopic() == null ? "" : config.ntfyControlTopic().trim();
	}

	private class Listener extends WebSocketListener
	{
		@Override
		public void onOpen(WebSocket webSocket, Response response)
		{
			connected = true;
			retrySeconds = RETRY_MIN_SECONDS;
			log.debug("ntfy control channel open");
			response.close();
		}

		@Override
		public void onMessage(WebSocket webSocket, String text)
		{
			final String command = parseCommand(text);
			if (command == null || command.isEmpty())
			{
				return;
			}

			final Consumer<String> handler = commandHandler;
			if (handler != null)
			{
				handler.accept(command);
			}
		}

		@Override
		public void onFailure(WebSocket webSocket, Throwable t, Response response)
		{
			connected = false;
			if (response != null)
			{
				response.close();
			}

			if (running)
			{
				log.debug("ntfy control channel dropped: {}", t.getMessage());
				scheduleReconnect();
			}
		}

		@Override
		public void onClosed(WebSocket webSocket, int code, String reason)
		{
			connected = false;
			if (running)
			{
				scheduleReconnect();
			}
		}
	}

	/**
	 * ntfy streams one JSON object per frame; only "message" events carry user text.
	 */
	private String parseCommand(String frame)
	{
		try
		{
			final JsonObject json = gson.fromJson(frame, JsonObject.class);
			if (json == null || !json.has("event"))
			{
				return null;
			}

			if (!"message".equals(json.get("event").getAsString()) || !json.has("message"))
			{
				return null;
			}

			return json.get("message").getAsString().trim().toLowerCase(Locale.ROOT);
		}
		catch (JsonParseException | IllegalStateException | UnsupportedOperationException e)
		{
			log.debug("Unparseable ntfy frame: {}", e.getMessage());
			return null;
		}
	}
}
