# AFK Companion

A RuneLite plugin that tells your **phone** when the AFK is over — and shows you, on screen,
exactly how long is left.

Built around the Gemstone Crab, then extended to cover everything else that quietly ends an
AFK session: you stopped chopping, your inventory filled up, aggression ran out, you got
poisoned, something valuable dropped.

Nothing here plays the game for you. The plugin only reads state and reports it.

---

## Gemstone Crab

The crab's health bar does not drop from damage — it drops with time. When it empties the crab
burrows, sheds its shell and moves to the next of three mines, on a roughly ten minute cycle.
The health bar is therefore a countdown.

The plugin reads it and gives you:

- **an on-screen countdown** to the burrow, your damage to the current crab, the 90 second
  shell timer, and session totals;
- **a phone notification** a configurable number of seconds before the burrow (default 60);
- **a notification when it burrows**, so you can mine the shell in time;
- **a notification when you stop hitting it**, which usually means you dropped out of the fight.

The bar has only a few dozen steps, so rather than trusting a fixed cycle length the plugin
learns how many ticks one step actually lasts and interpolates between steps. A value that
would otherwise jump every ~20 seconds becomes accurate to the second. Until it has seen its
first step change, the countdown is marked with a `?`.

The crab and its shell are matched by NPC id (14779 and 14780), with a name check as a fallback.

## Everything else it watches

**AFK safeguards** — logout warning before the five minute idle kick, low hitpoints, low prayer,
poison and venom, valuable drops (by Grand Exchange value), random event NPCs, and a free-form
chat trigger you write as a regular expression.

**Skilling and combat** — your animation stopped, you are out of combat, inventory full, an item
from your watch list ran out, low ammo, special attack back to 100%, aggression about to expire.

**Account events** — level ups, death, Grand Exchange offers completing.

Two of these deserve a caveat:

- **Aggression** is an estimate. The game does not expose that timer, so the plugin measures how
  long you have stood near one spot and resets when you move more than 12 tiles. It is off by
  default, because otherwise it fires while you stand in a bank.
- **Stopped working** only fires if the plugin saw you working first, so idling in a bank stays
  quiet.

## Notifications

Each service is its own switch, so you can enable several at once — ntfy on your phone and
Discord for the record, say. Every notification then goes to all of them, and a screenshot is
encoded once and shared. A service that is switched on but not filled in is skipped with a
warning in the log instead of holding up the others.

| Service | What you need | Notes |
|---|---|---|
| **ntfy.sh** | just a topic name | Easiest. Install the ntfy app, subscribe to a topic, put the same name in the plugin. No account. |
| Discord webhook | webhook URL | Arrives as a colour-coded embed; screenshots attach inline, and it can ping you. |
| Pushover | user key + app token | |
| Telegram bot | bot token + chat id | Token comes from @BotFather. |
| Custom webhook | any URL | Receives `POST {source,title,message,priority}`. |

Useful settings:

- **Only when unfocused** (on by default) — stay silent while you are actually at the keyboard.
- **Screenshots** — chosen per kind of event, not all at once: separate switches for Gemstone
  Crab, AFK safeguards, skilling and combat, and account events. A picture is worth it for a
  crab burrow and pure noise for a level up. ntfy and Discord only; if the window is not
  drawing, the notification goes out after two seconds without it.
- **Quiet hours** — a window (may wrap past midnight) where notifications are held back, with an
  option to let urgent ones through anyway.
- **Cooldown** — minimum gap between notifications.
- **Discord mention** — who to ping. Paste a user id, a role id prefixed with `&`, or
  `@everyone` / `@here`; get an id by enabling Developer Mode in Discord and using Copy ID.
  *Only ping when urgent* limits it to death, low hitpoints, poison, running out of something and
  valuable drops. The ping goes in the message content rather than the embed, because a mention
  inside an embed is displayed but never actually notifies anyone.

> An ntfy topic name is the only thing protecting it. Pick something nobody would guess, like
> `jaka-osrs-8f3k9x2m`. This matters more if you enable screenshots: the image shows your
> username, chat and inventory.

## Asking the client a question from your phone

Set an **ntfy control topic** and the plugin subscribes to it. Publish a word to that topic from
the ntfy app and the client answers on your normal notification topic:

| Command | Answer |
|---|---|
| `status` | hitpoints, prayer, crab countdown, aggression, session time and XP rate |
| `crab` | burrow countdown, your damage, crabs this session |
| `stats` | session and lifetime totals |
| `help` | the list above |

This is deliberately read-only — commands report, they never act in the game. Anyone who knows
the control topic can query it, so use a separate unguessable name, or leave it empty to disable.

## Side panel

The toolbar icon opens a panel with live status, session and lifetime totals, a *Send test
notification* button and a log of the notifications this session produced. Lifetime counters
(crabs, damage, notifications, hours) persist across restarts.

## In-game commands

- `::afktest` — send a test notification
- `::afkstatus` — print the status line in chat
- `::afkreset` — reset the session counters

## Running it

### From Gradle (development)

```bash
./gradlew run
```

On Windows PowerShell, note that `&&` is not a statement separator — use `;`:

```powershell
cd path\to\afk-companion; .\gradlew run
```

Gradle 8.10 supports up to JDK 22. If your default JDK is newer, copy `gradle.properties.sample`
to `gradle.properties` and point `org.gradle.java.home` at a JDK 11–17. The plugin itself is
always compiled to Java 11 bytecode.

### As a standalone client

```bash
./gradlew shadowJar
java --add-opens=java.base/java.lang.reflect=ALL-UNNAMED -jar build/libs/afk-companion-1.0.0-all.jar
```

This produces a RuneLite client with the plugin built in — no Gradle, no developer mode. The
RuneLite version is pinned at build time, so rebuild when a new one ships.

### Logging in with a Jagex account

Developer mode cannot be launched from the Jagex Launcher. The
[official workaround](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) is to write
your credentials out once:

1. RuneLite launcher 2.6.3 or newer.
2. Run **RuneLite (configure)** from the start menu.
3. Add `--insecure-write-credentials` to **Client arguments** and save.
4. Launch RuneLite through the Jagex Launcher once — it writes `.runelite/credentials.properties`.
5. Remove the argument again.

That file logs into your account without a password. Never share it or commit it. Revoke it with
**End sessions** in your account settings on runescape.com, or delete the file.

## Project layout

```
src/main/java/com/jaka/afkcompanion/
  AfkCompanionPlugin.java          wiring, crab warnings, notification delivery
  AfkCompanionConfig.java          settings
  AfkCompanionPanel.java           side panel
  push/PushSender.java             ntfy, Discord, Pushover, Telegram, webhook
  push/NtfyControl.java            read-only control channel from your phone
  gemstonecrab/                    tracker, overlay, infobox timers
  watch/AfkWatchdog.java           every general AFK warning
  stats/SessionStats.java          session and persisted lifetime totals
  util/                            formatting and quiet-hours logic
```

## Licence

BSD 2-Clause. See [LICENSE](LICENSE).
