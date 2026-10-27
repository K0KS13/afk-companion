package com.jaka.afkcompanion.forestry;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Turns the stream of Forestry NPCs and objects into one piece of news per event.
 * <p>
 * Most events are made of several parts - up to six roots, eight bushes, a handful of
 * entlings - and parts come and go while the event runs: a sapling swaps object as it grows,
 * a lone root moves, a scene reload drops and re-adds every object. So an event is reported
 * when its first part shows up, and only re-arms once it has had no parts at all for a few
 * ticks.
 */
public class ForestryWatch
{
	/** How long an event must be gone before it counts as over. Events never recur this fast. */
	static final int REARM_TICKS = 10;

	private final Map<Object, ForestryEvent> parts = new IdentityHashMap<>();
	private final Map<ForestryEvent, Integer> partCounts = new EnumMap<>(ForestryEvent.class);
	private final Map<ForestryEvent, Integer> lastSeenTick = new EnumMap<>(ForestryEvent.class);

	private int tick;

	public void onTick()
	{
		tick++;
	}

	/**
	 * @param part the NPC or object that spawned, compared by identity
	 * @return true when this is the first part of an event that was not already running
	 */
	public boolean onSpawn(Object part, ForestryEvent event)
	{
		if (parts.put(part, event) != null)
		{
			return false;
		}

		if (partCounts.merge(event, 1, Integer::sum) > 1)
		{
			return false;
		}

		final Integer lastSeen = lastSeenTick.remove(event);
		return lastSeen == null || tick - lastSeen >= REARM_TICKS;
	}

	public void onDespawn(Object part)
	{
		release(parts.remove(part));
	}

	/** Hopping or logging out ends everything in sight. */
	public void clear()
	{
		parts.clear();
		partCounts.clear();
		lastSeenTick.clear();
	}

	/**
	 * Forgets only some parts. A scene reload drops every object without a despawn event, while
	 * NPCs carry over, so the plugin clears the objects alone.
	 */
	public void clear(Predicate<Object> which)
	{
		final Iterator<Map.Entry<Object, ForestryEvent>> it = parts.entrySet().iterator();
		while (it.hasNext())
		{
			final Map.Entry<Object, ForestryEvent> entry = it.next();
			if (which.test(entry.getKey()))
			{
				release(entry.getValue());
				it.remove();
			}
		}
	}

	private void release(ForestryEvent event)
	{
		if (event == null)
		{
			return;
		}

		if (partCounts.computeIfPresent(event, (e, count) -> count > 1 ? count - 1 : null) == null)
		{
			lastSeenTick.put(event, tick);
		}
	}
}
