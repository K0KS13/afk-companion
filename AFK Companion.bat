@echo off
rem Launches RuneLite with AFK Companion built in - no Gradle, no developer mode.
rem Logging in uses .runelite\credentials.properties, written once by the Jagex Launcher.

setlocal

set JAR="%~dp0build\libs\afk-companion-1.0.0-all.jar"

rem Prefer a JDK 11-17 if one is installed, otherwise fall back to java on PATH.
set JAVA=
if exist "%USERPROFILE%\.jdks\temurin-17.0.20\bin\javaw.exe" set JAVA="%USERPROFILE%\.jdks\temurin-17.0.20\bin\javaw.exe"
if not defined JAVA set JAVA=javaw

if not exist %JAR% (
	echo Could not find %JAR%
	echo Build it first with: gradlew shadowJar
	pause
	exit /b 1
)

start "" %JAVA% --add-opens=java.base/java.lang.reflect=ALL-UNNAMED -jar %JAR%
