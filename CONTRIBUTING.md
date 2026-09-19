# Contributing

Everything here is for working on the plugin itself. If you just want to use it, install it
from the RuneLite Plugin Hub and see the [README](README.md).

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

## Tests

```bash
./gradlew test
```

The tracker's countdown logic, number formatting, quiet hours, name matching and Discord
mention handling are covered. Anything that can be tested without a running client should be.

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
  util/                            formatting, quiet hours, name matching
```

## Publishing a new version

Push to `main`, then update the commit hash in a fork of
[runelite/plugin-hub](https://github.com/runelite/plugin-hub) and open a pull request. The
manifest template is in [hub/afk-companion](hub/afk-companion).
