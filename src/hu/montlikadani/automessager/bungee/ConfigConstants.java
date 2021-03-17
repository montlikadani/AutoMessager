package hu.montlikadani.automessager.bungee;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.md_5.bungee.config.Configuration;

public final class ConfigConstants {

	private static boolean enableBroadcast, random, useSystemZone, broadcastToConsole;

	private static int time, minPlayers;

	private static String timeSetup, timeFormat, dateFormat, timeZone;

	private static List<String> disabledServers;

	public static final Map<String, String> CUSTOM_VARIABLES = new HashMap<>();

	static void load(Configuration c) {
		CUSTOM_VARIABLES.clear();

		enableBroadcast = c.getBoolean("enable-broadcast");
		random = c.getBoolean("random");
		time = c.getInt("time", 5);
		minPlayers = c.getInt("min-players", 1);
		timeSetup = c.getString("time-setup", "minutes").toUpperCase();
		disabledServers = c.getStringList("disabled-servers");
		timeFormat = c.getString("placeholder-format.time.time-format.format", "");
		dateFormat = c.getString("placeholder-format.time.date-format.format", "");
		useSystemZone = c.getBoolean("placeholder-format.time.use-system-zone");
		timeZone = c.getString("placeholder-format.time.time-zone");
		broadcastToConsole = c.getBoolean("broadcast-to-console");

		if (c.contains("custom-variables")) {
			for (String name : c.getSection("custom-variables").getKeys()) {
				CUSTOM_VARIABLES.put(name, c.getString("custom-variables." + name, ""));
			}
		}
	}

	public static boolean isEnableBroadcast() {
		return enableBroadcast;
	}

	public static boolean isRandom() {
		return random;
	}

	public static int getTime() {
		return time;
	}

	public static String getTimeSetup() {
		return timeSetup;
	}

	public static List<String> getDisabledServers() {
		return disabledServers;
	}

	public static String getTimeFormat() {
		return timeFormat;
	}

	public static String getDateFormat() {
		return dateFormat;
	}

	public static boolean isUseSystemZone() {
		return useSystemZone;
	}

	public static String getTimeZone() {
		return timeZone;
	}

	public static int getMinPlayers() {
		return minPlayers;
	}

	public static boolean isBroadcastToConsole() {
		return broadcastToConsole;
	}
}
