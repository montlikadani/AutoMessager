package hu.montlikadani.AutoMessager.bukkit.utils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.reflect.TypeToken;

import hu.montlikadani.AutoMessager.Global;
import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.config.ConfigConstants;
import hu.montlikadani.AutoMessager.bukkit.config.Configuration;
import me.clip.placeholderapi.PlaceholderAPI;

public class Util {

	private static final AutoMessager PLUGIN = JavaPlugin.getPlugin(AutoMessager.class);

	public static void logConsole(String error) {
		logConsole(Level.INFO, error);
	}

	public static void logConsole(Level level, String msg) {
		if (ConfigConstants.isLogConsole() && msg != null && !msg.trim().isEmpty()) {
			Bukkit.getLogger().log(level == null ? Level.INFO : level, "[AutoMessager] " + msg);
		}
	}

	public static String colorMsg(String msg) {
		if (msg == null) {
			return "";
		}

		if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R1) && !msg.contains("json:") && msg.contains("#")) {
			msg = Global.matchColorRegex(msg);
		}

		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	public static String getMsgProperty(String key, Object... placeholders) {
		return getMsgProperty(TypeToken.of(String.class), key, placeholders);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getMsgProperty(TypeToken<T> type, String key, Object... placeholders) {
		if (key == null || key.isEmpty()) {
			return (T) "null";
		}

		final Configuration conf = PLUGIN.getConf();

		if (type.getRawType().isAssignableFrom(String.class)) {
			String msg = "";

			if (!conf.getMessages().contains(key)) {
				conf.getMessages().set(key, "");
				try {
					conf.getMessages().save(conf.getMessagesFile());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (conf.getMessages().getString(key).isEmpty()) {
				return (T) msg;
			}

			msg = colorMsg(conf.getMessages().getString(key));

			for (int i = 0; i < placeholders.length; i++) {
				if (placeholders.length >= i + 2) {
					msg = StringUtils.replace(msg, String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
				}

				i++;
			}

			return (T) msg;
		}

		if (type.getRawType().isAssignableFrom(List.class)) {
			List<String> list = new ArrayList<>();

			for (String one : conf.getMessages().getStringList(key)) {
				one = colorMsg(one);

				for (int i = 0; i < placeholders.length; i++) {
					if (placeholders.length >= i + 2) {
						one = StringUtils.replace(one, String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
					}

					i++;
				}

				list.add(one);
			}

			return (T) list;
		}

		return (T) "no msg";
	}

	public static void sendMsg(CommandSender sender, String s) {
		if (s != null && !s.isEmpty() && sender != null) {
			if (s.contains("\n")) {
				for (String msg : s.split("\n")) {
					sender.sendMessage(msg);
				}
			} else {
				sender.sendMessage(s);
			}
		}
	}

	public static String replaceVariables(Player pl, String str) {
		for (java.util.Map.Entry<String, String> map : ConfigConstants.CUSTOM_VARIABLES.entrySet()) {
			if (str.indexOf(map.getKey()) >= 0) {
				str = StringUtils.replace(str, map.getKey(), map.getValue());
			}
		}

		String time = str.indexOf("%server-time%") >= 0 ? getTimeAsString(ConfigConstants.getTimeFormat()) : "";
		String date = str.indexOf("%date%") >= 0 ? getTimeAsString(ConfigConstants.getDateFormat()) : "";

		String path = "placeholder-format.";
		FileConfiguration config = PLUGIN.getConfig();
		// Old
		if (!config.getString(path + "title", "").isEmpty()) {
			str = StringUtils.replace(str, "%title%", config.getString(path + "title").replace("\\n", "\n"));
		}
		if (!config.getString(path + "suffix", "").isEmpty()) {
			str = StringUtils.replace(str, "%suffix%", config.getString(path + "suffix").replace("\\n", "\n"));
		}

		str = setPlaceholders(pl, str);
		str = Global.setSymbols(str);

		if (!time.isEmpty())
			str = StringUtils.replace(str, "%server-time%", time);

		if (!date.isEmpty())
			str = StringUtils.replace(str, "%date%", date);

		if (str.contains("%online-players%"))
			str = StringUtils.replace(str, "%online-players%", Integer.toString(Bukkit.getOnlinePlayers().size()));

		if (str.contains("%max-players%"))
			str = StringUtils.replace(str, "%max-players%", Integer.toString(Bukkit.getMaxPlayers()));

		if (str.contains("%servertype%"))
			str = StringUtils.replace(str, "%servertype%", Bukkit.getServer().getName());

		if (str.contains("%mc-version%"))
			str = StringUtils.replace(str, "%mc-version%", Bukkit.getBukkitVersion());

		if (str.contains("%motd%"))
			str = StringUtils.replace(str, "%motd%", PLUGIN.getComplement().getMotd());

		return colorMsg(str);
	}

	@SuppressWarnings("deprecation")
	public static String setPlaceholders(Player p, String s) {
		if (ConfigConstants.isPlaceholderapi() && PLUGIN.isPluginEnabled("PlaceholderAPI")) {
			s = PlaceholderAPI.setPlaceholders(p, s);
		}

		if (s.contains("%player%"))
			s = StringUtils.replace(s, "%player%", p.getName());

		if (s.contains("%player-displayname%"))
			s = StringUtils.replace(s, "%player-displayname%", PLUGIN.getComplement().getDisplayName(p));

		if (s.contains("%player-uuid%"))
			s = StringUtils.replace(s, "%player-uuid%", p.getUniqueId().toString());

		if (s.contains("%world%"))
			s = StringUtils.replace(s, "%world%", p.getWorld().getName());

		if (s.contains("%player-gamemode%"))
			s = StringUtils.replace(s, "%player-gamemode%", p.getGameMode().name());

		if (s.contains("%max-players%"))
			s = StringUtils.replace(s, "%max-players%", Integer.toString(Bukkit.getServer().getMaxPlayers()));

		if (s.contains("%online-players%"))
			s = StringUtils.replace(s, "%online-players%",
					Integer.toString(Bukkit.getServer().getOnlinePlayers().size()));

		if (s.contains("%player-health%"))
			s = StringUtils.replace(s, "%player-health%", String.valueOf(p.getHealth()));

		if (s.contains("%player-max-health%"))
			s = StringUtils.replace(s, "%player-max-health%",
					String.valueOf(ServerVersion.isCurrentLower(ServerVersion.v1_9_R1) ? p.getMaxHealth()
							: p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue()));

		return s;
	}

	private static String getTimeAsString(String pattern) {
		if (pattern.isEmpty()) {
			return pattern;
		}

		TimeZone zone = ConfigConstants.isUseSystemZone() ? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
				: TimeZone.getTimeZone(ConfigConstants.getTimeZone());
		LocalDateTime now = zone == null ? LocalDateTime.now() : LocalDateTime.now(zone.toZoneId());

		return now.format(DateTimeFormatter.ofPattern(pattern));
	}

	public static long calcNextDelay(int hour, int minute, int second) {
		ZonedDateTime zonedNow = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
		ZonedDateTime zonedNextTarget = zonedNow.withHour(hour).withMinute(minute).withSecond(second);

		if (zonedNow.compareTo(zonedNextTarget) > 0) {
			zonedNextTarget = zonedNextTarget.plusDays(1);
		}

		return Duration.between(zonedNow, zonedNextTarget).getSeconds();
	}

	private static int jVersion = -1;

	public static int getCurrentVersion() {
		if (jVersion != -1) {
			return jVersion;
		}

		String currentVersion = System.getProperty("java.version");
		if (currentVersion.contains("_")) {
			currentVersion = currentVersion.split("_")[0];
		}

		currentVersion = currentVersion.replaceAll("[^\\d]|_", "");

		for (int i = 8; i <= 18; i++) {
			if (currentVersion.contains(Integer.toString(i))) {
				return jVersion = i;
			}
		}

		return 0;
	}
}
