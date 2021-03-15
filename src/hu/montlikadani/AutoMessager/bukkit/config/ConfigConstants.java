package hu.montlikadani.automessager.bukkit.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigConstants {

	private static boolean logConsole = false, checkUpdate, downloadUpdates, broadcastEnabled, placeholderapi,
			rememberToggleToFile, disableMsgsInAfk, useSystemZone, random, bcToConsole;

	private static int listMaxRow, minPlayers;

	private static List<String> disabledWorlds;

	private static String messageFile, timeZone, timeFormat, dateFormat, time, timeSetup;

	public static final Map<String, String> CUSTOM_VARIABLES = new HashMap<>();
	private static final Set<ExecutableCommands> EXECUTABLE_COMMANDS = new HashSet<>();

	private static SoundProperties soundProperties;

	static void load(FileConfiguration conf) {
		CUSTOM_VARIABLES.clear();
		EXECUTABLE_COMMANDS.clear();

		checkUpdate = conf.getBoolean("check-update");
		downloadUpdates = conf.getBoolean("download-updates");
		logConsole = conf.getBoolean("logconsole");
		broadcastEnabled = conf.getBoolean("enable-broadcast", true);
		placeholderapi = conf.getBoolean("placeholderapi", true);
		rememberToggleToFile = conf.getBoolean("remember-toggle-to-file", true);
		disableMsgsInAfk = conf.getBoolean("disable-messages-when-player-afk");
		useSystemZone = conf.getBoolean("placeholder-format.time.use-system-zone");
		random = conf.getBoolean("random");
		bcToConsole = conf.getBoolean("broadcast-to-console");

		listMaxRow = conf.getInt("show-max-row-in-one-page", 4);
		minPlayers = conf.getInt("min-players", 1);

		disabledWorlds = conf.getStringList("disabled-worlds");

		for (String cmd : conf.getStringList("run-commands.commands")) {
			EXECUTABLE_COMMANDS.add(new ExecutableCommands(cmd));
		}

		messageFile = conf.getString("message-file", "announces.yml");
		timeZone = conf.getString("placeholder-format.time.time-zone", "GMT0");
		timeFormat = conf.getString("placeholder-format.time.time-format.format", "mm:HH");
		dateFormat = conf.getString("placeholder-format.time.date-format.format", "dd/MM/yyyy");
		timeSetup = conf.getString("time-setup", "min");
		time = conf.getString("time", "3");

		soundProperties = new SoundProperties(conf.getString("sound.type", ""));

		if (conf.isConfigurationSection("custom-variables")) {
			for (String name : conf.getConfigurationSection("custom-variables").getKeys(true)) {
				CUSTOM_VARIABLES.put(name, conf.getString("custom-variables." + name, ""));
			}
		}
	}

	public static final class ExecutableCommands {

		private String command = "";
		private SenderType type = SenderType.CONSOLE;

		ExecutableCommands(String command) {
			String[] arg = command.contains(":") ? command.split(": ") : new String[] { "console", command };
			if (arg.length < 2) {
				return;
			}

			String line = arg[1];

			if (line.charAt(0) == '/') {
				line = line.substring(1, line.length());
			}

			if (arg[0].equalsIgnoreCase("console")) {
				type = SenderType.CONSOLE;
			} else if (arg[0].equalsIgnoreCase("player")) {
				type = SenderType.PLAYER;
			}

			this.command = command;
		}

		public String getCommand() {
			return command;
		}

		public SenderType getType() {
			return type;
		}

		public enum SenderType {
			CONSOLE, PLAYER
		}
	}

	public static final class SoundProperties {

		private float volume = 1f, pitch = 1f;

		private Sound sound;

		SoundProperties(String soundType) {
			if (soundType.isEmpty()) {
				return;
			}

			if (!soundType.contains(",")) {
				try {
					sound = Sound.valueOf(soundType.toUpperCase());
				} catch (IllegalArgumentException e) {
				}

				return;
			}

			String[] split = soundType.split(", ");

			try {
				sound = Sound.valueOf(split[0].toUpperCase());
			} catch (IllegalArgumentException e) {
				return;
			}

			if (split.length > 1) {
				volume = Float.parseFloat(split[1]);
			}

			if (split.length > 2) {
				pitch = Float.parseFloat(split[2]);
			}
		}

		public float getVolume() {
			return volume;
		}

		public float getPitch() {
			return pitch;
		}

		public Sound getSound() {
			return sound;
		}
	}

	public static boolean isBroadcastEnabled() {
		return broadcastEnabled;
	}

	public static boolean isPlaceholderapi() {
		return placeholderapi;
	}

	public static boolean isRememberToggleToFile() {
		return rememberToggleToFile;
	}

	public static boolean isDisableMsgsInAfk() {
		return disableMsgsInAfk;
	}

	public static boolean isUseSystemZone() {
		return useSystemZone;
	}

	public static boolean isRandom() {
		return random;
	}

	public static boolean isBcToConsole() {
		return bcToConsole;
	}

	public static int getListMaxRow() {
		return listMaxRow;
	}

	public static int getMinPlayers() {
		return minPlayers;
	}

	public static List<String> getDisabledWorlds() {
		return disabledWorlds;
	}

	public static String getMessageFile() {
		return messageFile;
	}

	public static String getTimeZone() {
		return timeZone;
	}

	public static String getTimeFormat() {
		return timeFormat;
	}

	public static String getDateFormat() {
		return dateFormat;
	}

	public static String getTime() {
		return time;
	}

	public static String getTimeSetup() {
		return timeSetup;
	}

	public static boolean isLogConsole() {
		return logConsole;
	}

	public static SoundProperties getSoundProperties() {
		return soundProperties;
	}

	public static Set<ExecutableCommands> getExecutableCommands() {
		return EXECUTABLE_COMMANDS;
	}

	public static boolean isCheckUpdate() {
		return checkUpdate;
	}

	public static boolean isDownloadUpdates() {
		return downloadUpdates;
	}
}
