package com.jaka.afkcompanion.forestry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/**
 * The Forestry events, and the NPC or object that announces each one. The ids are the ones the
 * core Woodcutting plugin notifies on, so both plugins agree on what counts as an event.
 */
@Getter
@RequiredArgsConstructor
public enum ForestryEvent
{
	RISING_ROOTS("Rising Roots"),
	STRUGGLING_SAPLING("Struggling Sapling"),
	FLOWERING_TREE("Flowering Tree"),
	LEPRECHAUN("Leprechaun"),
	POACHERS("Poachers"),
	PHEASANT_CONTROL("Pheasant Control"),
	BEE_HIVE("Bee Hive"),
	ENCHANTMENT_RITUAL("Enchantment Ritual"),
	FRIENDLY_ENT("Friendly Ent");

	private final String displayName;

	/**
	 * @return the event this NPC belongs to, or null when it is not one that starts an event
	 */
	public static ForestryEvent forNpc(int npcId)
	{
		switch (npcId)
		{
			case NpcID.GATHERING_EVENT_FLOWERING_TREE_BUSH_COL01:
			case NpcID.GATHERING_EVENT_FLOWERING_TREE_BUSH_COL02:
			case NpcID.GATHERING_EVENT_FLOWERING_TREE_BUSH_COL03:
			case NpcID.GATHERING_EVENT_FLOWERING_TREE_BUSH_COL04:
			case NpcID.GATHERING_EVENT_FLOWERING_TREE_BUSH_COL05:
			case NpcID.GATHERING_EVENT_FLOWERING_TREE_BUSH_COL06:
			case NpcID.GATHERING_EVENT_FLOWERING_TREE_BUSH_COL07:
			case NpcID.GATHERING_EVENT_FLOWERING_TREE_BUSH_COL08:
				return FLOWERING_TREE;
			case NpcID.GATHERING_EVENT_WOODCUTTING_LEPRECHAUN:
				return LEPRECHAUN;
			case NpcID.GATHERING_EVENT_POACHERS_FOX_OUTDOORS:
			case NpcID.GATHERING_EVENT_POACHERS_FOX_INDOORS:
				return POACHERS;
			case NpcID.GATHERING_EVENT_PHEASANT_FORESTER:
				return PHEASANT_CONTROL;
			case NpcID.GATHERING_EVENT_BEES_BEEBOX_4:
				return BEE_HIVE;
			case NpcID.GATHERING_EVENT_ENCHANTED_RITUAL_DRYAD:
				return ENCHANTMENT_RITUAL;
			case NpcID.GATHERING_EVENT_ENTLINGS_NPC_01:
				return FRIENDLY_ENT;
			default:
				return null;
		}
	}

	/**
	 * @return the event this object belongs to, or null when it is not one that starts an event
	 */
	public static ForestryEvent forObject(int objectId)
	{
		switch (objectId)
		{
			case ObjectID.GATHERING_EVENT_RISING_ROOTS:
			case ObjectID.GATHERING_EVENT_RISING_ROOTS_SPECIAL:
				return RISING_ROOTS;
			case ObjectID.GATHERING_EVENT_SAPLING_1X1:
			case ObjectID.GATHERING_EVENT_SAPLING_WITHERING_1X1:
			case ObjectID.GATHERING_EVENT_SAPLING_2X2:
			case ObjectID.GATHERING_EVENT_SAPLING_WITHERING_2X2:
			case ObjectID.GATHERING_EVENT_SAPLING_3X3:
			case ObjectID.GATHERING_EVENT_SAPLING_WITHERING_3X3:
				return STRUGGLING_SAPLING;
			default:
				return null;
		}
	}
}
