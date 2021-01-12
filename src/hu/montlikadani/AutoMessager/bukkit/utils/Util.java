package hu.montlikadani.AutoMessager.bukkit.utils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.google.common.reflect.TypeToken;

import hu.montlikadani.AutoMessager.Global;
import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Configuration;
import me.clip.placeholderapi.PlaceholderAPI;

public class Util {

	public static void logConsole(String error) {
		logConsole(Level.INFO, error);
	}

	public static void logConsole(String error, boolean loaded) {
		logConsole(Level.INFO, error, loaded);
	}

	public static void logConsole(Level level, String error) {
		logConsole(level, error, true);
	}

	public static void logConsole(Level level, String error, boolean loaded) {
		if ((!loaded || AutoMessager.getInstance().getConfig().getBoolean("logconsole")) && error != null
				&& !error.trim().isEmpty()) {
			Bukkit.getLogger().log(level == null ? Level.INFO : level, "[AutoMessager] " + error);
		}
	}

	public static String colorMsg(String msg) {
		if (msg == null) {
			return "";
		}

		if (msg.contains("#") && !msg.contains("json:") && Bukkit.getVersion().contains("1.16")) {
			msg = Global.matchColorRegex(msg);
		}

		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	public static String getMsgProperty(String key, Object... placeholders) {
		return getMsgProperty(TypeToken.of(String.class), key, placeholders);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getMsgProperty(TypeToken<T> type, String key, Object... placeholders) {
		if (key == null || key.trim().isEmpty()) {
			return (T) "null";
		}

		final Configuration conf = AutoMessager.getInstance().getConf();

		if (type.getRawType().isAssignableFrom(String.class)) {
			if (!conf.getMessagesFile().exists()) {
				return (T) "FILENF";
			}

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
					msg = msg.replace(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
				}

				i++;
			}

			return (T) msg;
		}

		if (type.getRawType().isAssignableFrom(List.class)) {
			if (!conf.getMessagesFile().exists()) {
				return (T) new ArrayList<>();
			}

			List<String> list = new ArrayList<>();

			for (String one : conf.getMessages().getStringList(key)) {
				one = colorMsg(one);

				for (int i = 0; i < placeholders.length; i++) {
					if (placeholders.length >= i + 2) {
						one = one.replace(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
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
		FileConfiguration config = AutoMessager.getInstance().getConfig();

		if (config.contains("custom-variables")) {
			for (String custom : config.getConfigurationSection("custom-variables").getKeys(true)) {
				if (str.contains(custom)) {
					str = str.replace(custom, config.getString("custom-variables." + custom));
				}
			}
		}

		String t = "", dt = "", path = "placeholder-format.";
		if (str.contains("%server-time%") || str.contains("%date%")) {
			String tPath = path + "time.";
			DateTimeFormatter form = !config.getString(tPath + "time-format.format", "").isEmpty()
					? DateTimeFormatter.ofPattern(config.getString(tPath + "time-format.format"))
					: null;

			DateTimeFormatter form2 = !config.getString(tPath + "date-format.format", "").isEmpty()
					? DateTimeFormatter.ofPattern(config.getString(tPath + "date-format.format"))
					: null;

			TimeZone zone = config.getBoolean(tPath + "use-system-zone", false)
					? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
					: TimeZone.getTimeZone(config.getString(tPath + "time-zone", "GMT0"));
			LocalDateTime now = zone == null ? LocalDateTime.now() : LocalDateTime.now(zone.toZoneId());

			Calendar cal = Calendar.getInstance();

			t = form != null ? now.format(form) : cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE);
			dt = form2 != null ? now.format(form2) : cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.DATE);
		}

		// Old
		if (!config.getString(path + "title", "").isEmpty()) {
			str = str.replace("%title%", config.getString(path + "title").replace("\\n", "\n"));
		}
		if (!config.getString(path + "suffix", "").isEmpty()) {
			str = str.replace("%suffix%", config.getString(path + "suffix").replace("\\n", "\n"));
		}

		str = setPlaceholders(pl, str);
		str = Global.setSymbols(str);
		if (!t.isEmpty())
			str = str.replace("%server-time%", t);

		if (!dt.isEmpty())
			str = str.replace("%date%", dt);

		if (str.contains("%online-players%"))
			str = str.replace("%online-players%", Integer.toString(Bukkit.getOnlinePlayers().size()));

		if (str.contains("%max-players%"))
			str = str.replace("%max-players%", Integer.toString(Bukkit.getMaxPlayers()));

		if (str.contains("%servertype%"))
			str = str.replace("%servertype%", Bukkit.getServer().getName());

		if (str.contains("%mc-version%"))
			str = str.replace("%mc-version%", Bukkit.getBukkitVersion());

		if (str.contains("%motd%"))
			str = str.replace("%motd%", Bukkit.getServer().getMotd());

		return colorMsg(str);
	}

	@SuppressWarnings("deprecation")
	public static String setPlaceholders(Player p, String s) {
		if (AutoMessager.getInstance().getConf().papi && AutoMessager.getInstance().isPluginEnabled("PlaceholderAPI")) {
			s = PlaceholderAPI.setPlaceholders(p, s);
		}

		if (s.contains("%player%"))
			s = s.replace("%player%", p.getName());

		if (s.contains("%player-displayname%"))
			s = s.replace("%player-displayname%", p.getDisplayName());

		if (s.contains("%player-uuid%"))
			s = s.replace("%player-uuid%", p.getUniqueId().toString());

		if (s.contains("%world%"))
			s = s.replace("%world%", p.getWorld().getName());

		if (s.contains("%player-gamemode%"))
			s = s.replace("%player-gamemode%", p.getGameMode().name());

		if (s.contains("%max-players%"))
			s = s.replace("%max-players%", Integer.toString(Bukkit.getServer().getMaxPlayers()));

		if (s.contains("%online-players%"))
			s = s.replace("%online-players%", Integer.toString(Bukkit.getServer().getOnlinePlayers().size()));

		if (s.contains("%player-health%"))
			s = s.replace("%player-health%", String.valueOf(p.getHealth()));

		if (s.contains("%player-max-health%"))
			s = s.replace("%player-max-health%", String.valueOf(Bukkit.getVersion().contains("1.8") ? p.getMaxHealth()
					: p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue()));

		return s;
	}

	public static long calcNextDelay(int hour, int minute, int second) {
		LocalDateTime localNow = LocalDateTime.now();
		ZonedDateTime zonedNow = ZonedDateTime.of(localNow, ZoneId.systemDefault());
		ZonedDateTime zonedNextTarget = zonedNow.withHour(hour).withMinute(minute).withSecond(second);
		if (zonedNow.compareTo(zonedNextTarget) > 0)
			zonedNextTarget = zonedNextTarget.plusDays(1);

		return Duration.between(zonedNow, zonedNextTarget).getSeconds();
	}

	public static int getCurrentVersion() {
		String currentVersion = System.getProperty("java.version");
		if (currentVersion.contains("_")) {
			currentVersion = currentVersion.split("_")[0];
		}

		currentVersion = currentVersion.replaceAll("[^\\d]|_", "");

		for (int i = 8; i <= 18; i++) {
			if (currentVersion.contains(Integer.toString(i))) {
				return i;
			}
		}

		return 0;
	}
}