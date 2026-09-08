package com.jaka.afkcompanion.push;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PushSenderTest
{
	@Test
	public void wrapsBareIdsIntoMentions()
	{
		assertEquals("<@113847130>", PushSender.discordMention("113847130"));
		assertEquals("<@113847130>", PushSender.discordMention("  113847130  "));
		assertEquals("<@&987654321>", PushSender.discordMention("&987654321"));
	}

	@Test
	public void leavesReadyMadeMentionsAlone()
	{
		assertEquals("<@113847130>", PushSender.discordMention("<@113847130>"));
		assertEquals("<@&987654321>", PushSender.discordMention("<@&987654321>"));
		assertEquals("@everyone", PushSender.discordMention("@everyone"));
		assertEquals("@here", PushSender.discordMention("@here"));
		assertEquals("@everyone", PushSender.discordMention("@Everyone"));
	}

	@Test
	public void stripsTheAtPeopleCopyFromDiscord()
	{
		// Discord shows mentions as "@name", so ids get pasted with the @ still attached.
		// Passed through as typed they render as plain text and ping nobody.
		assertEquals("<@193349754950778880>", PushSender.discordMention("@193349754950778880"));
		assertEquals("<@&987654321>", PushSender.discordMention("@&987654321"));
	}

	@Test
	public void passesThroughAnythingItCannotParse()
	{
		// A username does not resolve through a webhook, but mangling it would be worse.
		assertEquals("@koks", PushSender.discordMention("@koks"));
	}

	@Test
	public void emptyMeansNoPing()
	{
		assertEquals("", PushSender.discordMention(""));
		assertEquals("", PushSender.discordMention("   "));
		assertEquals("", PushSender.discordMention(null));
	}

	@Test
	public void stripsNonAsciiFromHeaders()
	{
		// ntfy carries the text in HTTP headers when a screenshot is attached, and OkHttp
		// rejects anything outside printable ASCII.
		assertEquals("Crab burrows in 60s", PushSender.header("Crab burrows in 60s"));
		assertEquals("Skoljka  90 s", PushSender.header("Školjka \n90 s"));
	}
}
