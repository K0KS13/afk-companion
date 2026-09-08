package com.jaka.afkcompanion.push;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jaka.afkcompanion.AfkCompanionConfig;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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

	/** At or above this priority a notification counts as urgent. */
	private static final int URGENT_PRIORITY = 5;

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

	/**
	 * @return every delivery target the user has switched on
	 */
	public List<PushProvider> enabledProviders()
	{
		final List<PushProvider> providers = new ArrayList<>();

		if (config.sendNtfy())
		{
			providers.add(PushProvider.NTFY);
		}
		if (config.sendDiscord())
		{
			providers.add(PushProvider.DISCORD);
		}
		if (config.sendPushover())
		{
			providers.add(PushProvider.PUSHOVER);
		}
		if (config.sendTelegram())
		{
			providers.add(PushProvider.TELEGRAM);
		}
		if (config.sendWebhook())
		{
			providers.add(PushProvider.WEBHOOK);
		}

		return providers;
	}

	public boolean hasAnyProvider()
	{
		return !enabledProviders().isEmpty();
	}

	/**
	 * @return true if at least one enabled target can carry an image
	 */
	public boolean anySupportsScreenshots()
	{
		return enabledProviders().stream().anyMatch(PushProvider::supportsScreenshots);
	}

	/**
	 * @return the enabled targets as a short label, e.g. "ntfy + Discord"
	 */
	public String enabledLabel()
	{
		final List<PushProvider> providers = enabledProviders();
		return providers.isEmpty()
			? "off"
			: providers.stream().map(PushProvider::toString).collect(Collectors.joining(" + "));
	}

	public boolean send(String title, String message, int priority)
	{
		return send(title, message, priority, null);
	}

	/**
	 * @param screenshot image to attach (ntfy and Discord only), or null
	 * @return true if the notification was accepted for delivery by at least one target
	 */
	public boolean send(String title, String message, int priority, BufferedImage screenshot)
	{
		return send(title, message, priority, screenshot, null);
	}

	/**
	 * Delivers to every enabled target. A target that is switched on but not configured is
	 * skipped with a warning rather than blocking the others.
	 *
	 * @param onResult optional callback receiving one human-readable result per target
	 */
	public boolean send(String title, String message, int priority, BufferedImage screenshot, Consumer<String> onResult)
	{
		final List<PushProvider> providers = usableProviders(onResult);
		if (providers.isEmpty())
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

		lastSentAt = now;
		executor.execute(() -> dispatchAll(providers, title, message, priority, screenshot, onResult));
		return true;
	}

	/**
	 * Sends without touching the shared cooldown. Used for replies to a command from the phone,
	 * which the user explicitly asked for and should never be silently dropped.
	 */
	public void sendImmediate(String title, String message, int priority)
	{
		final List<PushProvider> providers = usableProviders(null);
		if (providers.isEmpty())
		{
			return;
		}

		executor.execute(() -> dispatchAll(providers, title, message, priority, null, null));
	}

	private List<PushProvider> usableProviders(Consumer<String> onResult)
	{
		final List<PushProvider> usable = new ArrayList<>();

		for (PushProvider provider : enabledProviders())
		{
			try
			{
				checkConfigured(provider);
				usable.add(provider);
			}
			catch (IllegalArgumentException e)
			{
				log.warn("{} is enabled but not configured: {}", provider, e.getMessage());
				report(onResult, provider + ": " + e.getMessage());
			}
		}

		return usable;
	}

	private void dispatchAll(List<PushProvider> providers, String title, String message, int priority,
		BufferedImage screenshot, Consumer<String> onResult)
	{
		// Encoded once and shared, however many targets are enabled.
		final byte[] png = encode(screenshot);

		for (PushProvider provider : providers)
		{
			dispatch(provider, title, message, priority, png, onResult);
		}
	}

	private void dispatch(PushProvider provider, String title, String message, int priority,
		byte[] png, Consumer<String> onResult)
	{
		final Request request;
		try
		{
			request = buildRequest(provider, title, message, priority,
				provider.supportsScreenshots() ? png : null);
		}
		catch (IllegalArgumentException e)
		{
			log.warn("{} is not configured: {}", provider, e.getMessage());
			report(onResult, provider + ": " + e.getMessage());
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
				log.warn("Push notification to {} failed", provider, e);
				report(onResult, provider + ": failed, " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				if (response.isSuccessful())
				{
					sentCount++;
					report(onResult, provider + ": delivered");
				}
				else
				{
					failedCount++;
					log.warn("Push notification to {} rejected: HTTP {}", provider, response.code());
					report(onResult, provider + ": rejected with HTTP " + response.code());
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

		// Discord only pings for mentions in the message content - one inside an embed is
		// rendered but never notifies anyone.
		if (!config.discordMentionUrgentOnly() || priority >= URGENT_PRIORITY)
		{
			final String mention = discordMention(config.discordMention());
			if (!mention.isEmpty())
			{
				payload.addProperty("content", mention);

				final JsonArray parse = new JsonArray();
				parse.add("users");
				parse.add("roles");
				parse.add("everyone");
				final JsonObject allowed = new JsonObject();
				allowed.add("parse", parse);
				payload.add("allowed_mentions", allowed);
			}
		}

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
	 * Turns whatever the user typed into something Discord will actually ping.
	 * Accepts a bare user id, a role id prefixed with {@code &}, an already formed
	 * {@code <@...>} mention, or {@code @everyone} / {@code @here}.
	 *
	 * @return the mention to put in the message content, or an empty string for no ping
	 */
	static String discordMention(String raw)
	{
		final String value = raw == null ? "" : raw.trim();

		if (value.isEmpty() || value.startsWith("<@") || value.startsWith("@"))
		{
			return value;
		}

		if (value.startsWith("&") && value.substring(1).matches("\\d+"))
		{
			return "<@&" + value.substring(1) + ">";
		}

		if (value.matches("\\d+"))
		{
			return "<@" + value + ">";
		}

		// Anything else is passed through untouched rather than mangled into a broken mention.
		return value;
	}

	/**
	 * HTTP headers may only carry printable ASCII, and never a line break.
	 */
	static String header(String value)
	{
		if (value == null)
		{
			return "";
		}

		// Decomposing first means an accented letter degrades to its base letter rather than
		// vanishing, so "Skoljka" survives where a blind ASCII filter would leave "koljka".
		final String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
		final StringBuilder sb = new StringBuilder(decomposed.length());

		for (char c : decomposed.toCharArray())
		{
			if (Character.getType(c) == Character.NON_SPACING_MARK)
			{
				continue;
			}

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
