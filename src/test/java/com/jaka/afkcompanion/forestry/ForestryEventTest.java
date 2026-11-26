package com.jaka.afkcompanion.forestry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

public class ForestryEventTest
{
	@Test
	public void mapsEveryEventNpc()
	{
		assertEquals(ForestryEvent.LEPRECHAUN, ForestryEvent.forNpc(NpcID.GATHERING_EVENT_WOODCUTTING_LEPRECHAUN));
		assertEquals(ForestryEvent.POACHERS, ForestryEvent.forNpc(NpcID.GATHERING_EVENT_POACHERS_FOX_OUTDOORS));
		assertEquals(ForestryEvent.POACHERS, ForestryEvent.forNpc(NpcID.GATHERING_EVENT_POACHERS_FOX_INDOORS));
		assertEquals(ForestryEvent.PHEASANT_CONTROL, ForestryEvent.forNpc(NpcID.GATHERING_EVENT_PHEASANT_FORESTER));
		assertEquals(ForestryEvent.BEE_HIVE, ForestryEvent.forNpc(NpcID.GATHERING_EVENT_BEES_BEEBOX_4));
		assertEquals(ForestryEvent.ENCHANTMENT_RITUAL, ForestryEvent.forNpc(NpcID.GATHERING_EVENT_ENCHANTED_RITUAL_DRYAD));
		assertEquals(ForestryEvent.FRIENDLY_ENT, ForestryEvent.forNpc(NpcID.GATHERING_EVENT_ENTLINGS_NPC_01));
		assertEquals(ForestryEvent.FLOWERING_TREE, ForestryEvent.forNpc(NpcID.GATHERING_EVENT_FLOWERING_TREE_BUSH_COL01));
		assertEquals(ForestryEvent.FLOWERING_TREE, ForestryEvent.forNpc(NpcID.GATHERING_EVENT_FLOWERING_TREE_BUSH_COL08));
	}

	@Test
	public void mapsEveryEventObject()
	{
		assertEquals(ForestryEvent.RISING_ROOTS, ForestryEvent.forObject(ObjectID.GATHERING_EVENT_RISING_ROOTS));
		assertEquals(ForestryEvent.RISING_ROOTS, ForestryEvent.forObject(ObjectID.GATHERING_EVENT_RISING_ROOTS_SPECIAL));
		assertEquals(ForestryEvent.STRUGGLING_SAPLING, ForestryEvent.forObject(ObjectID.GATHERING_EVENT_SAPLING_1X1));
		assertEquals(ForestryEvent.STRUGGLING_SAPLING, ForestryEvent.forObject(ObjectID.GATHERING_EVENT_SAPLING_WITHERING_3X3));
	}

	@Test
	public void ignoresUnrelatedIds()
	{
		// The unfinished hives and the bee box are part of the event but do not start it.
		assertNull(ForestryEvent.forNpc(NpcID.GATHERING_EVENT_BEES_BEEBOX_1));
		assertNull(ForestryEvent.forNpc(NpcID.GATHERING_EVENT_POACHERS_TRAP));
		assertNull(ForestryEvent.forNpc(-1));
		assertNull(ForestryEvent.forObject(ObjectID.GATHERING_EVENT_SAPLING_INGREDIENT_1));
		assertNull(ForestryEvent.forObject(-1));
	}
}
