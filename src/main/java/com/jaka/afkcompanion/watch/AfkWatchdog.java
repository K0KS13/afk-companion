package com.jaka.afkcompanion.watch;

import com.jaka.afkcompanion.AfkCompanionConfig;
import com.jaka.afkcompanion.NotificationCategory;
import com.jaka.afkcompanion.util.Format;
import com.jaka.afkcompanion.util.NameList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;

/**
 * Every general AFK warning that is not tied to the Gemstone Crab.
 * <p>
 * Nothing here touches the game - it only reads state and reports. Each warning fires once
 * and re-arms when the condition clears, so a lasting condition cannot loop your phone.
 */
@Slf4j
@Singleton
public class AfkWatchdog
{
	/** The game logs you out after five minutes without mouse or keyboard input. */
	private static final int LOGOUT_IDLE_SECONDS = 5 * 60;

	/** NPCs stop being aggressive after roughly ten minutes in one area. */
	private static final int AGGRO_TICKS = 1000;

	/** How far you have to travel for the aggression timer to reset. */
	private static final int AGGRO_RESET_DISTANCE = 12;

	/** The game re-sends every offer on login, so ignore the first few ticks. */
	private static final int GE_LOGIN_GRACE_TICKS = 10;

	private static final int INVENTORY_SIZE = 28;
	private static final int SPEC_FULL = 1000;
	private static final int VENOM_THRESHOLD = 1_000_000;

	private final Client client;
	private final AfkCompanionConfig config;
	private final ItemManager itemManager;

	private int tick;
	private int loggedInAtTick = -1;

	private int lastAnimationTick = -1;
	private boolean warnedAnimationIdle;

	private int lastCombatTick = -1;
	private boolean warnedCombatIdle;

	private boolean warnedIdleLogout;
	private boolean warnedLowHp;
	private boolean warnedLowPrayer;
	private boolean warnedInventoryFull;
	private boolean warnedAmmo;
	private boolean warnedSpec;
	private boolean warnedPoison;
	private boolean warnedAggro;

	private WorldPoint aggroAnchor;
	private int aggroTicksLeft = AGGRO_TICKS;

	private final int[] knownLevels = new int[Skill.values().length];
	private Set<String> watchedItemsPresent = new HashSet<>();

	@Inject
	private AfkWatchdog(Client client, AfkCompanionConfig config, ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
		this.itemManager = itemManager;
	}

	public void reset()
	{
		tick = 0;
		loggedInAtTick = -1;
		lastAnimationTick = -1;
		lastCombatTick = -1;
		warnedAnimationIdle = false;
		warnedCombatIdle = false;
		warnedIdleLogout = false;
		warnedLowHp = false;
		warnedLowPrayer = false;
		warnedInventoryFull = false;
		warnedAmmo = false;
		warnedSpec = false;
		warnedPoison = false;
		warnedAggro = false;
		aggroAnchor = null;
		aggroTicksLeft = AGGRO_TICKS;
		Arrays.fill(knownLevels, 0);
		watchedItemsPresent = new HashSet<>();
	}

	public void onLogin()
	{
		loggedInAtTick = tick;
	}

	/** Called on every hit you land, so we know when you were last fighting. */
	public void noteCombat()
	{
		lastCombatTick = tick;
		warnedCombatIdle = false;
	}

	/**
	 * @return seconds until aggression is estimated to expire, or -1 when not tracking
	 */
	public int getAggroSecondsLeft()
	{
		return config.aggroNotify() && aggroAnchor != null ? (int) Math.round(aggroTicksLeft * 0.6d) : -1;
	}

	/**
	 * @param suppressCombatIdle true when the Gemstone Crab side already covers combat
	 */
	public void onGameTick(NotificationSink sink, boolean suppressCombatIdle)
	{
		tick++;

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		checkAnimationIdle(sink, player);
		checkCombatIdle(sink, player, suppressCombatIdle);
		checkIdleLogout(sink);
		checkLowHitpoints(sink);
		checkLowPrayer(sink);
		checkPoison(sink);
		checkSpecialAttack(sink);
		checkInventory(sink);
		checkAmmo(sink);
		checkAggro(sink, player);
	}

	// ------------------------------------------------------------------ activity

	private void checkAnimationIdle(NotificationSink sink, Player player)
	{
		if (!config.idleAnimationNotify())
		{
			return;
		}

		if (player.getAnimation() != -1)
		{
			lastAnimationTick = tick;
			warnedAnimationIdle = false;
			return;
		}

		// If we never saw you working there is nothing to report - otherwise standing
		// in a bank would warn you every time.
		if (lastAnimationTick < 0 || warnedAnimationIdle)
		{
			return;
		}

		if (tick - lastAnimationTick >= toTicks(config.idleAnimationSeconds()))
		{
			warnedAnimationIdle = true;
			sink.notify(NotificationCategory.SKILLING, "AFK Companion", "You stopped working " + config.idleAnimationSeconds() + "s ago.", 4);
		}
	}

	private void checkCombatIdle(NotificationSink sink, Player player, boolean suppress)
	{
		if (!config.combatIdleNotify() || suppress)
		{
			return;
		}

		if (player.getInteracting() != null)
		{
			lastCombatTick = tick;
			warnedCombatIdle = false;
			return;
		}

		if (lastCombatTick < 0 || warnedCombatIdle)
		{
			return;
		}

		if (tick - lastCombatTick >= toTicks(config.combatIdleSeconds()))
		{
			warnedCombatIdle = true;
			sink.notify(NotificationCategory.SKILLING, "AFK Companion", "Out of combat for " + config.combatIdleSeconds() + "s.", 4);
		}
	}

	private void checkIdleLogout(NotificationSink sink)
	{
		if (!config.idleLogoutWarning())
		{
			return;
		}

		final int idleClientTicks = Math.min(client.getKeyboardIdleTicks(), client.getMouseIdleTicks());
		final int idleSeconds = idleClientTicks * Constants.CLIENT_TICK_LENGTH / 1000;

		if (idleSeconds < 10)
		{
			warnedIdleLogout = false;
			return;
		}

		if (!warnedIdleLogout && idleSeconds >= LOGOUT_IDLE_SECONDS - config.idleLogoutWarnSeconds())
		{
			warnedIdleLogout = true;
			sink.notify(NotificationCategory.AFK, "AFK Companion", "Logging out for inactivity in "
				+ Math.max(0, LOGOUT_IDLE_SECONDS - idleSeconds) + "s.", 4);
		}
	}

	private void checkAggro(NotificationSink sink, Player player)
	{
		if (!config.aggroNotify())
		{
			aggroAnchor = null;
			return;
		}

		final WorldPoint here = player.getWorldLocation();
		if (here == null)
		{
			return;
		}

		if (aggroAnchor == null || here.distanceTo(aggroAnchor) > AGGRO_RESET_DISTANCE)
		{
			// Far enough from the anchor that aggression starts counting again.
			aggroAnchor = here;
			aggroTicksLeft = AGGRO_TICKS;
			warnedAggro = false;
			return;
		}

		if (aggroTicksLeft > 0)
		{
			aggroTicksLeft--;
		}

		if (!warnedAggro && aggroTicksLeft <= toTicks(config.aggroWarnSeconds()))
		{
			warnedAggro = true;
			sink.notify(NotificationCategory.SKILLING, "Aggression", "NPCs stop attacking in about " + config.aggroWarnSeconds()
				+ "s - step away and back to reset it.", 4);
		}
	}

	// ------------------------------------------------------------------ character state

	private void checkLowHitpoints(NotificationSink sink)
	{
		if (!config.lowHitpointsWarning())
		{
			return;
		}

		final int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
		if (hp > config.lowHitpointsThreshold())
		{
			warnedLowHp = false;
			return;
		}

		if (!warnedLowHp && hp > 0)
		{
			warnedLowHp = true;
			sink.notify(NotificationCategory.AFK, "AFK Companion", "Low hitpoints: " + hp + ".", 5);
		}
	}

	private void checkLowPrayer(NotificationSink sink)
	{
		if (!config.lowPrayerWarning())
		{
			return;
		}

		final int prayer = client.getBoostedSkillLevel(Skill.PRAYER);
		if (prayer > config.lowPrayerThreshold())
		{
			warnedLowPrayer = false;
			return;
		}

		// At zero there is nothing left to save, so we stay quiet.
		if (!warnedLowPrayer && prayer > 0)
		{
			warnedLowPrayer = true;
			sink.notify(NotificationCategory.AFK, "AFK Companion", "Low prayer: " + prayer + ".", 4);
		}
	}

	private void checkPoison(NotificationSink sink)
	{
		if (!config.poisonNotify())
		{
			return;
		}

		final int poison = client.getVarpValue(VarPlayer.POISON);
		if (poison <= 0)
		{
			warnedPoison = false;
			return;
		}

		if (!warnedPoison)
		{
			warnedPoison = true;
			sink.notify(NotificationCategory.AFK, "AFK Companion", poison >= VENOM_THRESHOLD ? "You are envenomed." : "You are poisoned.", 5);
		}
	}

	private void checkSpecialAttack(NotificationSink sink)
	{
		if (!config.specRestoredNotify())
		{
			return;
		}

		final int spec = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT);
		if (spec < SPEC_FULL)
		{
			warnedSpec = false;
			return;
		}

		if (!warnedSpec)
		{
			warnedSpec = true;
			sink.notify(NotificationCategory.SKILLING, "AFK Companion", "Special attack is back to 100%.", 3);
		}
	}

	// ------------------------------------------------------------------ inventory

	private void checkInventory(NotificationSink sink)
	{
		final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			return;
		}

		final Item[] items = inventory.getItems();

		if (config.inventoryFullNotify())
		{
			int used = 0;
			for (Item item : items)
			{
				if (item.getId() > 0)
				{
					used++;
				}
			}

			if (used >= INVENTORY_SIZE)
			{
				if (!warnedInventoryFull)
				{
					warnedInventoryFull = true;
					sink.notify(NotificationCategory.SKILLING, "AFK Companion", "Your inventory is full.", 4);
				}
			}
			else
			{
				warnedInventoryFull = false;
			}
		}

		checkWatchedItems(sink, items);
	}

	/**
	 * Reports when something from the watch list runs out - food, potions, teleports.
	 * Only fires if you were carrying it, so items you never bring stay silent.
	 */
	private void checkWatchedItems(NotificationSink sink, Item[] items)
	{
		final String raw = config.watchedItems();
		if (raw == null || raw.trim().isEmpty())
		{
			if (!watchedItemsPresent.isEmpty())
			{
				watchedItemsPresent = new HashSet<>();
			}
			return;
		}

		// Twice a second is plenty, and item names come from a cache.
		if (tick % 5 != 0)
		{
			return;
		}

		final Set<String> watched = NameList.parse(raw);
		final Set<String> present = new HashSet<>();
		for (Item item : items)
		{
			if (item.getId() <= 0)
			{
				continue;
			}

			final String itemName = itemManager.getItemComposition(item.getId()).getName().toLowerCase(Locale.ROOT);
			for (String needle : watched)
			{
				if (itemName.contains(needle))
				{
					present.add(needle);
				}
			}
		}

		for (String needle : watchedItemsPresent)
		{
			if (watched.contains(needle) && !present.contains(needle))
			{
				sink.notify(NotificationCategory.SKILLING, "AFK Companion", "You ran out of: " + needle + ".", 5);
			}
		}

		watchedItemsPresent = present;
	}

	private void checkAmmo(NotificationSink sink)
	{
		final int threshold = config.ammoThreshold();
		if (threshold <= 0)
		{
			return;
		}

		final ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null)
		{
			return;
		}

		final Item ammo = equipment.getItem(EquipmentInventorySlot.AMMO.getSlotIdx());
		if (ammo == null || ammo.getId() <= 0)
		{
			return;
		}

		if (ammo.getQuantity() > threshold)
		{
			warnedAmmo = false;
			return;
		}

		if (!warnedAmmo)
		{
			warnedAmmo = true;
			final String name = itemManager.getItemComposition(ammo.getId()).getName();
			sink.notify(NotificationCategory.SKILLING, "AFK Companion", "Low ammo: " + name + " x" + ammo.getQuantity() + ".", 4);
		}
	}

	// ------------------------------------------------------------------ events

	public void onStatChanged(StatChanged event, NotificationSink sink)
	{
		final int index = event.getSkill().ordinal();
		final int previous = knownLevels[index];
		knownLevels[index] = event.getLevel();

		// Every level arrives at once on login; that first batch is not news.
		if (!config.levelUpNotify() || previous <= 0 || event.getLevel() <= previous)
		{
			return;
		}

		sink.notify(NotificationCategory.ACCOUNT, "Level up", event.getSkill().getName() + " " + event.getLevel() + ".", 4);
	}

	public void onActorDeath(ActorDeath event, NotificationSink sink)
	{
		if (config.deathNotify() && event.getActor() == client.getLocalPlayer())
		{
			sink.notify(NotificationCategory.ACCOUNT, "You died", "Your character has died.", 5);
		}
	}

	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event, NotificationSink sink)
	{
		if (!config.geOfferNotify())
		{
			return;
		}

		if (loggedInAtTick < 0 || tick - loggedInAtTick < GE_LOGIN_GRACE_TICKS)
		{
			return;
		}

		final GrandExchangeOffer offer = event.getOffer();
		if (offer == null)
		{
			return;
		}

		final GrandExchangeOfferState state = offer.getState();
		if (state != GrandExchangeOfferState.BOUGHT && state != GrandExchangeOfferState.SOLD)
		{
			return;
		}

		final String name = itemManager.getItemComposition(offer.getItemId()).getName();
		sink.notify(NotificationCategory.ACCOUNT, "Grand Exchange",
			(state == GrandExchangeOfferState.BOUGHT ? "Bought " : "Sold ")
				+ name + " x" + offer.getTotalQuantity() + ".", 3);
	}

	/**
	 * A drop is worth telling you about either because it is expensive or because you said so
	 * by name. The name list is checked independently of the value threshold, so untradeables
	 * and anything the Grand Exchange prices at zero still get through.
	 */
	public void onNpcLootReceived(NpcLootReceived event, NotificationSink sink)
	{
		final Set<String> watched = NameList.parse(config.watchedDropItems());
		if (!config.valuableDropNotify() && watched.isEmpty())
		{
			return;
		}

		long total = 0;
		long bestValue = -1;
		String bestItem = null;
		final List<String> named = new ArrayList<>();

		for (ItemStack stack : event.getItems())
		{
			final String name = itemManager.getItemComposition(stack.getId()).getName();
			final String label = name + (stack.getQuantity() > 1 ? " x" + stack.getQuantity() : "");
			final long value = (long) itemManager.getItemPrice(stack.getId()) * stack.getQuantity();
			total += value;

			if (value > bestValue)
			{
				bestValue = value;
				bestItem = label;
			}

			if (NameList.firstMatch(name, watched) != null)
			{
				named.add(label);
			}
		}

		final boolean byName = !named.isEmpty();
		final boolean byValue = config.valuableDropNotify()
			&& total >= config.valuableDropThreshold()
			&& bestItem != null;

		if (!byName && !byValue)
		{
			return;
		}

		final String npcName = event.getNpc().getName() == null ? "NPC" : event.getNpc().getName();

		if (byName)
		{
			sink.notify(NotificationCategory.AFK, "Drop",
				String.join(", ", named) + " from " + npcName
					+ (total > 0 ? ", whole drop " + Format.gp(total) : ""), 5);
			return;
		}

		sink.notify(NotificationCategory.AFK, "Valuable drop",
			bestItem + " (" + Format.gp(bestValue) + ") from " + npcName
				+ (total > bestValue ? ", whole drop " + Format.gp(total) : ""), 5);
	}

	private static int toTicks(int seconds)
	{
		return Math.max(1, (int) Math.round(seconds / 0.6d));
	}
}
