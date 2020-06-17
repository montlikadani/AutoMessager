package hu.montlikadani.AutoMessager.bukkit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.Global;
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
		if ((!loaded || AutoMessager.getInstance().getConf().getConfig().getBoolean("logconsole")) && error != null
				&& !error.trim().isEmpty()) {
			Bukkit.getLogger().log(level == null ? Level.INFO : level, "[AutoMessager] " + error);
		}
	}

	public static String colorMsg(String msg) {
		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	public static String getMsg(String key, Object... placeholders) {
		String msg = "";

		if (AutoMessager.getInstance().getConf().getMessages().getString(key, "").isEmpty()) {
			return msg;
		}

		msg = colorMsg(AutoMessager.getInstance().getConf().getMessages().getString(key));

		for (int i = 0; i < placeholders.length; i++) {
			if (placeholders.length >= i + 2) {
				msg = msg.replace(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
			}

			i++;
		}

		return msg;
	}

	public static void sendMsg(String s, boolean bc) {
		sendMsg(null, s, bc);
	}

	public static void sendMsg(CommandSender sender, String s) {
		sendMsg(sender, s, false);
	}

	public static void sendMsg(CommandSender sender, String s, boolean broadcast) {
		if (s != null && !s.isEmpty()) {
			if (s.contains("\n")) {
				for (String msg : s.split("\n")) {
					if (broadcast) {
						Bukkit.broadcastMessage(msg);
					} else if (sender != null) {
						sender.sendMessage(msg);
					}
				}
			} else {
				if (broadcast) {
					Bukkit.broadcastMessage(s);
				} else if (sender != null) {
					sender.sendMessage(s);
				}
			}
		}
	}

	public static String replaceVariables(Player pl, String str) {
		FileConfiguration config = AutoMessager.getInstance().getConf().getConfig();
		String path = "placeholder-format.";

		if (config.contains("custom-variables")) {
			for (String custom : config.getConfigurationSection("custom-variables").getKeys(true)) {
				if (custom != null && str.contains(custom)) {
					str = str.replace(custom, config.getString("custom-variables." + custom));
				}
			}
		}

		String t = null, dt = null;
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
		if (t != null)
			str = str.replace("%server-time%", t);

		if (dt != null)
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
		if (AutoMessager.getInstance().getConf().papi && AutoMessager.getInstance().isPluginEnabled("PlaceholderAPI")
				&& PlaceholderAPI.containsPlaceholders(s)) {
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
}
