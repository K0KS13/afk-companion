package com.jaka.afkcompanion.watch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class ChatAlertTest
{
	@Test
	public void recognisesASuperior()
	{
		assertEquals(ChatAlert.SUPERIOR, ChatAlert.of("A superior foe has appeared..."));
	}

	@Test
	public void recognisesAnEmptyCannon()
	{
		assertEquals(ChatAlert.CANNON_EMPTY, ChatAlert.of("Your cannon is out of ammo!"));
	}

	@Test
	public void recognisesABraceletOfSlaughterBreaking()
	{
		assertEquals(ChatAlert.SLAUGHTER_BROKE, ChatAlert.of(
			"Your bracelet of slaughter prevents your slayer count from decreasing. It then crumbles to dust."));
	}

	@Test
	public void recognisesAnExpeditiousBraceletBreaking()
	{
		assertEquals(ChatAlert.EXPEDITIOUS_BROKE, ChatAlert.of(
			"Your expeditious bracelet helps you progress your slayer task faster. It then crumbles to dust."));
		assertEquals(ChatAlert.EXPEDITIOUS_BROKE, ChatAlert.of(
			"Your expeditious bracelet helps you progress your slayer faster. It then crumbles to dust."));
	}

	@Test
	public void ignoresABraceletThatStillHasCharges()
	{
		assertNull(ChatAlert.of("Your bracelet of slaughter prevents your slayer count from decreasing. It has 12 charges left."));
		assertNull(ChatAlert.of("Your expeditious bracelet helps you progress your slayer task faster. It has 1 charge left."));
	}

	@Test
	public void ignoresABraceletThatRegenerates()
	{
		// The core item charges plugin treats this like a break; it is not one.
		assertNull(ChatAlert.of("Your bracelet of slaughter prevents your slayer count from decreasing. "
			+ "It then regenerates itself to full charge!"));
	}

	@Test
	public void recognisesABirdsNest()
	{
		assertEquals(ChatAlert.BIRD_NEST, ChatAlert.of("A bird's nest falls out of the tree."));
	}

	@Test
	public void recognisesAFinishedTask()
	{
		assertEquals(ChatAlert.TASK_COMPLETE, ChatAlert.of(
			"You've completed 52 tasks and received 15 points, giving you a total of 410; return to a Slayer master."));
		assertEquals(ChatAlert.TASK_COMPLETE, ChatAlert.of("You've completed 3 tasks; return to a Slayer master."));
		assertEquals(ChatAlert.TASK_COMPLETE, ChatAlert.of(
			"You've completed 1 Wilderness task and received 25 points, giving you a total of 25; return to a Slayer master."));
	}

	@Test
	public void ignoresEverythingElse()
	{
		assertNull(ChatAlert.of("You get some logs."));
		assertNull(ChatAlert.of(""));
		assertNull(ChatAlert.of(null));
	}
}
