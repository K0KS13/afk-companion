package com.jaka.afkcompanion.push;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jaka.afkcompanion.AfkCompanionConfig;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Delivers a notification to the configured phone service.
 * <p>
 * Everything past the cooldown check - PNG encoding included - happens off the client
 * thread, so a slow or unreachable service can never stutter the game.
 */
@Slf4j
@Singleton
public class PushSender
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final MediaType PNG = MediaType.parse("image/png");
	private static final String SCREENSHOT_NAME = "osrs.png";

	/** Discord embed accent, by notification priority. */
	private static final int COLOR_URGENT = 0xE03D3D;
	private static final int COLOR_NORMAL = 0xF2B038;
	private static final int COLOR_LOW = 0x5865F2;

	private final OkHttpClient httpClient;
	private final AfkCompanionConfig config;
	private final ScheduledExecutorService executor;

	private long lastSentAt;
	private int sentCount;
	private int failedCount;

	@Inject
	private PushSender(OkHttpClient httpClient, AfkCompanionConfig config, ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.config = config;
		this.executor = executor;
	}

	public void resetCooldown()
	{
		lastSentAt = 0;
	}

	public int getSentCount()
	{
		return sentCount;
	}

	public int getFailedCount()
	{
		return failedCount;
	}

	public void resetCounters()
	{
		sentCount = 0;
		failedCount = 0;
	}

	public boolean send(String title, String message, int priority)
	{
		return send(title, message, priority, null);
	}

	/**
	 * @param screenshot image to attach (ntfy and Discord only), or null
	 * @return true if the request was accepted for delivery
	 */
	public boolean send(String title, String message, int priority, BufferedImage screenshot)
	{
		return send(title, message, priority, screenshot, null);
	}

	/**
	 * @param onResult optional callback receiving a human-readable delivery result
	 */
	public boolean send(String title, String message, int priority, BufferedImage screenshot, Consumer<String> onResult)
	{
		final PushProvider provider = config.pushProvider();
		if (provider == PushProvider.OFF)
		{
			return false;
		}

		final long now = System.currentTimeMillis();
		final long cooldownMs = config.pushCooldownSeconds() * 1000L;
		if (lastSentAt != 0 && now - lastSentAt < cooldownMs)
		{
			log.debug("Push suppressed by cooldown: {}", title);
			return false;
		}

		try
		{
			checkConfigured(provider);
		}
		catch (IllegalArgumentException e)
		{
			log.warn("Push service is not configured: {}", e.getMessage());
			report(onResult, e.getMessage());
			return false;
		}

		lastSentAt = now;
		executor.execute(() -> dispatch(provider, title, message, priority, screenshot, onResult));
		return true;
	}

	/**
	 * Sends without touching the shared cooldown. Used for replies to a command from the phone,
	 * which the user explicitly asked for and should never be silently dropped.
	 */
	public void sendImmediate(String title, String message, int priority)
	{
		final PushProvider provider = config.pushProvider();
		if (provider == PushProvider.OFF)
		{
			return;
		}

		try
		{
			checkConfigured(provider);
		}
		catch (IllegalArgumentException e)
		{
			log.warn("Push service is not configured: {}", e.getMessage());
			return;
		}

		executor.execute(() -> dispatch(provider, title, message, priority, null, null));
	}

	private void dispatch(PushProvider provider, String title, String message, int priority,
		BufferedImage screenshot, Consumer<String> onResult)
	{
		final byte[] png = encode(screenshot);

		final Request request;
		try
		{
			request = buildRequest(provider, title, message, priority, png);
		}
		catch (IllegalArgumentException e)
		{
			log.warn("Push service is not configured: {}", e.getMessage());
			report(onResult, e.getMessage());
			return;
		}

		if (request == null)
		{
			return;
		}

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				failedCount++;
				log.warn("Push notification failed", e);
				report(onResult, "failed: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (response.isSuccessful())
				{
					sentCount++;
					report(onResult, "delivered");
				}
				else
				{
					failedCount++;
					log.warn("Push notification rejected: HTTP {}", response.code());
					report(onResult, "rejected with HTTP " + response.code());
				}
				response.close();
			}
		});
	}

	private static void report(Consumer<String> onResult, String result)
	{
		if (onResult != null)
		{
			onResult.accept(result);
		}
	}

	private byte[] encode(BufferedImage image)
	{
		if (image == null)
		{
			return null;
		}

		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		}
		catch (IOException e)
		{
			log.warn("Could not encode the screenshot", e);
			return null;
		}
	}

	private void checkConfigured(PushProvider provider)
	{
		switch (provider)
		{
			case NTFY:
				require(trimmed(config.ntfyTopic()), "ntfy topic is empty");
				break;
			case DISCORD:
				require(trimmed(config.discordWebhookUrl()), "Discord webhook URL is empty");
				break;
			case PUSHOVER:
				require(trimmed(config.pushoverAppToken()), "Pushover app token is empty");
				require(trimmed(config.pushoverUserKey()), "Pushover user key is empty");
				break;
			case TELEGRAM:
				require(trimmed(config.telegramBotToken()), "Telegram bot token is empty");
				require(trimmed(config.telegramChatId()), "Telegram chat id is empty");
				break;
			case WEBHOOK:
				require(trimmed(config.webhookUrl()), "Webhook URL is empty");
				break;
			default:
				break;
		}
	}

	private Request buildRequest(PushProvider provider, String title, String message, int priority, byte[] png)
	{
		switch (provider)
		{
			case NTFY:
				return ntfyRequest(title, message, priority, png);
			case DISCORD:
				return discordRequest(title, message, priority, png);
			case PUSHOVER:
				return pushoverRequest(title, message, priority);
			case TELEGRAM:
				return telegramRequest(title, message);
			case WEBHOOK:
				return webhookRequest(title, message, priority);
			default:
				return null;
		}
	}

	private Request ntfyRequest(String title, String message, int priority, byte[] png)
	{
		final String topic = trimmed(config.ntfyTopic());
		final HttpUrl base = ntfyBaseUrl();
		final int prio = clamp(priority, 1, 5);

		if (png != null)
		{
			// With an attachment the body is the image itself, so the text moves into
			// headers - and those must be plain ASCII.
			return new Request.Builder()
				.url(base.newBuilder().addPathSegment(topic).build())
				.addHeader("Title", header(title))
				.addHeader("Message", header(message))
				.addHeader("Priority", String.valueOf(prio))
				.addHeader("Tags", "crab")
				.addHeader("Filename", SCREENSHOT_NAME)
				.post(RequestBody.create(PNG, png))
				.build();
		}

		final JsonObject body = new JsonObject();
		body.addProperty("topic", topic);
		body.addProperty("title", title);
		body.addProperty("message", message);
		// ntfy priorities run 1 (min) to 5 (max).
		body.addProperty("priority", prio);
		final JsonArray tags = new JsonArray();
		tags.add("crab");
		body.add("tags", tags);

		return new Request.Builder()
			.url(base)
			.post(RequestBody.create(JSON, body.toString()))
			.build();
	}

	HttpUrl ntfyBaseUrl()
	{
		final String server = defaultIfBlank(trimmed(config.ntfyServer()), "https://ntfy.sh");
		final HttpUrl base = HttpUrl.parse(server);
		require(base, "ntfy server is not a valid URL");
		return base;
	}

	private Request discordRequest(String title, String message, int priority, byte[] png)
	{
		final HttpUrl url = HttpUrl.parse(trimmed(config.discordWebhookUrl()));
		require(url, "Discord webhook URL is not valid");

		final JsonObject embed = new JsonObject();
		embed.addProperty("title", title);
		embed.addProperty("description", message);
		embed.addProperty("color", priority >= 5 ? COLOR_URGENT : priority >= 4 ? COLOR_NORMAL : COLOR_LOW);

		if (png != null)
		{
			final JsonObject image = new JsonObject();
			image.addProperty("url", "attachment://" + SCREENSHOT_NAME);
			embed.add("image", image);
		}

		final JsonArray embeds = new JsonArray();
		embeds.add(embed);

		final JsonObject payload = new JsonObject();
		payload.addProperty("username", "AFK Companion");
		payload.add("embeds", embeds);

		if (png != null)
		{
			final MultipartBody body = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("payload_json", payload.toString())
				.addFormDataPart("files[0]", SCREENSHOT_NAME, RequestBody.create(PNG, png))
				.build();

			return new Request.Builder().url(url).post(body).build();
		}

		return new Request.Builder()
			.url(url)
			.post(RequestBody.create(JSON, payload.toString()))
			.build();
	}

	private Request pushoverRequest(String title, String message, int priority)
	{
		final FormBody body = new FormBody.Builder()
			.add("token", trimmed(config.pushoverAppToken()))
			.add("user", trimmed(config.pushoverUserKey()))
			.add("title", title)
			.add("message", message)
			// Pushover priorities run -2 to 2; 2 needs a retry/expiry pair, so we stop at 1.
			.add("priority", String.valueOf(clamp(priority - 3, -2, 1)))
			.build();

		return new Request.Builder()
			.url("https://api.pushover.net/1/messages.json")
			.post(body)
			.build();
	}

	private Request telegramRequest(String title, String message)
	{
		final FormBody body = new FormBody.Builder()
			.add("chat_id", trimmed(config.telegramChatId()))
			.add("text", title + "\n" + message)
			.build();

		return new Request.Builder()
			.url("https://api.telegram.org/bot" + trimmed(config.telegramBotToken()) + "/sendMessage")
			.post(body)
			.build();
	}

	private Request webhookRequest(String title, String message, int priority)
	{
		final HttpUrl url = HttpUrl.parse(trimmed(config.webhookUrl()));
		require(url, "Webhook URL is not valid");

		final JsonObject body = new JsonObject();
		body.addProperty("source", "afk-companion");
		body.addProperty("title", title);
		body.addProperty("message", message);
		body.addProperty("priority", priority);

		return new Request.Builder()
			.url(url)
			.post(RequestBody.create(JSON, body.toString()))
			.build();
	}

	/**
	 * HTTP headers may only carry printable ASCII, and never a line break.
	 */
	static String header(String value)
	{
		final StringBuilder sb = new StringBuilder(value.length());
		for (char c : value.toCharArray())
		{
			sb.append(c >= 32 && c < 127 ? c : ' ');
		}
		return sb.toString().trim();
	}

	private static int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}

	private static void require(Object value, String error)
	{
		if (value == null || (value instanceof String && ((String) value).isEmpty()))
		{
			throw new IllegalArgumentException(error);
		}
	}

	private static String trimmed(String value)
	{
		return value == null ? "" : value.trim();
	}

	private static String defaultIfBlank(String value, String fallback)
	{
		return value.isEmpty() ? fallback : value;
	}
}
