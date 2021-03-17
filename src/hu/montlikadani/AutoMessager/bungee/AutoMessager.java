package hu.montlikadani.automessager.bungee;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

import hu.montlikadani.automessager.Global;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class AutoMessager extends Plugin {

	private final Set<UUID> players = new java.util.HashSet<>();

	private Configuration config;
	private Announce announce;
	private MessageFileHandler messageFileHandler;

	private int cver = 4;

	@Override
	public void onEnable() {
		loadConfig();

		(messageFileHandler = new MessageFileHandler(this)).loadFile();
		messageFileHandler.loadMessages();

		registerCommand();

		(announce = new Announce(this)).load();
		announce.schedule();
	}

	@Override
	public void onDisable() {
		announce.cancelTask();
		getProxy().getPluginManager().unregisterCommands(this);
	}

	private void loadConfig() {
		try {
			File folder = getDataFolder();
			folder.mkdirs();

			File file = new File(folder, "bungeeconfig.yml");
			if (!file.exists()) {
				try (InputStream in = getResourceAsStream("bungeeconfig.yml")) {
					Files.copy(in, file.toPath());
				}
			}

			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
			ConfigConstants.load(config);

			if (!config.get("config-version").equals(cver)) {
				getLogger().log(Level.WARNING, "Found outdated configuration (bungeeconfig.yml)! (Your version: "
						+ config.getInt("config-version") + " | Newest version: " + cver + ")");
			}
		} catch (Exception e) {
			e.printStackTrace();
			getLogger().log(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues");
		}
	}

	private void registerCommand() {
		getProxy().getPluginManager().registerCommand(this, new Command("automessager", "automesager.help", "am") {
			@Override
			public void execute(CommandSender s, String[] args) {
				if (args.length == 0) {
					if (s instanceof ProxiedPlayer && !hasPermission(s)) {
						sendMessage(s, config.getString("messages.no-permission"));
						return;
					}

					config.getStringList("messages.chat-messages").forEach(msg -> sendMessage(s, msg));
					return;
				}

				if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
					if (s instanceof ProxiedPlayer && !s.hasPermission("automessager.reload")) {
						sendMessage(s, config.getString("messages.no-permission"));
						return;
					}

					if (announce != null) {
						announce.cancelTask();
					} else {
						announce = new Announce((AutoMessager) getProxy().getPluginManager().getPlugin("AutoMessager"));
					}

					announce.load();
					announce.schedule();

					loadConfig();

					messageFileHandler.loadMessages();

					sendMessage(s, config.getString("messages.reload-config"));
				} else if (args[0].equalsIgnoreCase("toggle")) {
					if (s instanceof ProxiedPlayer && !s.hasPermission("automessager.toggle")) {
						sendMessage(s, config.getString("messages.no-permission"));
						return;
					}

					if (!(s instanceof ProxiedPlayer) && args.length < 2) {
						sendMessage(s, config.getString("messages.toggle.console-usage"));
						return;
					}

					if (args.length == 2) {
						if (args[1].equalsIgnoreCase("all")) {
							if (getProxy().getPlayers().isEmpty()) {
								sendMessage(s, config.getString("messages.toggle.no-players-available"));
								return;
							}

							for (ProxiedPlayer pl : getProxy().getPlayers()) {
								UUID uuid = pl.getUniqueId();
								if (!players.contains(uuid)) {
									players.add(uuid);
								} else {
									players.remove(uuid);
								}
							}

							return;
						}

						ProxiedPlayer target = getProxy().getPlayer(args[1]);
						if (target == null) {
							sendMessage(s, config.getString("messages.toggle.no-player"));
							return;
						}

						UUID uuid = target.getUniqueId();
						if (!players.contains(uuid)) {
							players.add(uuid);
							sendMessage(s, config.getString("messages.toggle.disabled"));
						} else {
							players.remove(uuid);
							sendMessage(s, config.getString("messages.toggle.enabled"));
						}

						return;
					}

					UUID uuid = ((ProxiedPlayer) s).getUniqueId();
					if (!players.contains(uuid)) {
						players.add(uuid);
						sendMessage(s, config.getString("messages.toggle.disabled"));
					} else {
						players.remove(uuid);
						sendMessage(s, config.getString("messages.toggle.enabled"));
					}
				} else if (args[0].equalsIgnoreCase("list")) {
					if (s instanceof ProxiedPlayer && !s.hasPermission("automessager.list")) {
						sendMessage(s, config.getString("messages.no-permission"));
						return;
					}

					List<String> texts = messageFileHandler.getTexts();
					if (texts.isEmpty()) {
						sendMessage(s, config.getString("messages.no-message-to-list"));
						return;
					}

					texts.forEach(m -> sendMessage(s, m));
				} else if (args[0].equalsIgnoreCase("add")) {
					if (s instanceof ProxiedPlayer && !s.hasPermission("automessager.add")) {
						sendMessage(s, config.getString("messages.no-permission"));
						return;
					}

					if (args.length < 2) {
						config.getStringList("messages.chat-messages").forEach(msg -> sendMessage(s, msg));
						return;
					}

					StringBuilder builder = new StringBuilder();
					for (int i = 1; i < args.length; i++) {
						builder.append(args[i] + (i + 1 < args.length ? " " : ""));
					}

					String msg = builder.toString();
					messageFileHandler.addText(msg);
					sendMessage(s, config.getString("messages.added-text").replace("%text%", msg));
				} else if (args[0].equalsIgnoreCase("remove")) {
					if (s instanceof ProxiedPlayer && !s.hasPermission("automessager.remove")) {
						sendMessage(s, config.getString("messages.no-permission"));
						return;
					}

					if (args.length < 2) {
						config.getStringList("messages.chat-messages").forEach(msg -> sendMessage(s, msg));
						return;
					}

					int index = 0;
					try {
						index = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						sendMessage(s, config.getString("messages.bad-number"));
						return;
					}

					if (index < 0) {
						sendMessage(s, config.getString("messages.bad-number"));
						return;
					}

					if (index > messageFileHandler.getTexts().size() - 1) {
						sendMessage(s, config.getString("messages.index-start"));
						return;
					}

					messageFileHandler.removeText(index);
					sendMessage(s, config.getString("messages.text-removed").replace("%index%", index + ""));
				}
			}
		});
	}

	@SuppressWarnings("deprecation")
	public String replaceVariables(String str, ProxiedPlayer p) {
		Runtime r = Runtime.getRuntime();
		Long fram = Long.valueOf(r.freeMemory() / 1048576L),
				mram = Long.valueOf(r.maxMemory() / 1048576L),
				uram = Long.valueOf((r.totalMemory() - r.freeMemory()) / 1048576L);

		for (java.util.Map.Entry<String, String> map : ConfigConstants.CUSTOM_VARIABLES.entrySet()) {
			if (str.indexOf(map.getKey()) >= 0) {
				str = StringUtils.replace(str, map.getKey(), map.getValue());
			}
		}

		String time = str.indexOf("%server-time%") >= 0 ? getTimeAsString(ConfigConstants.getTimeFormat()) : "";
		String date = str.indexOf("%date%") >= 0 ? getTimeAsString(ConfigConstants.getDateFormat()) : "";

		// Old
		String path = "placeholder-format.time.";
		if (!config.getString(path + "title", "").isEmpty()) {
			str = str.replace("%title%", config.getString(path + "title").replace("%newline%", "\n"));
		}

		if (!config.getString(path + "suffix", "").isEmpty()) {
			str = str.replace("%suffix%", config.getString(path + "suffix"));
		}

		str = Global.setSymbols(str);

		if (!time.isEmpty())
			str = str.replace("%server-time%", time);

		if (!date.isEmpty())
			str = str.replace("%date%", date);

		if (p.getServer() != null) {
			if (str.contains("%server%"))
				str = str.replace("%server%", p.getServer().getInfo().getName());

			if (str.contains("%server-online%"))
				str = str.replace("%server-online%", Integer.toString(p.getServer().getInfo().getPlayers().size()));

			if (str.contains("%bungee-motd%"))
				str = str.replace("%bungee-motd%", p.getServer().getInfo().getMotd());
		}

		if (str.contains("%max-players%"))
			str = str.replace("%max-players%", Integer
					.toString(getProxy().getConfigurationAdapter().getListeners().iterator().next().getMaxPlayers()));

		if (str.contains("%ip%")) {
			InetSocketAddress address = null;
			SocketAddress sAddress = null;
			try {
				address = p.getAddress();
			} catch (Exception e) {
				sAddress = p.getSocketAddress();
			}

			str = str.replace("%ip%", address != null ? address.getAddress().getHostAddress()
					: sAddress != null ? sAddress.toString() : "");
		}

		if (str.contains("%player-language%"))
			str = str.replace("%player-language%", String.valueOf(p.getLocale()));

		if (str.contains("%player-name%"))
			str = str.replace("%player-name%", p.getName());

		if (str.contains("%display-name%"))
			str = str.replace("%display-name%", p.getDisplayName());

		if (str.contains("%ping%"))
			str = str.replace("%ping%", Integer.toString(p.getPing()));

		if (str.contains("%ram-used%"))
			str = str.replace("%ram-used%", Long.toString(uram.longValue()));

		if (str.contains("%ram-max%"))
			str = str.replace("%ram-max%", Long.toString(mram.longValue()));

		if (str.contains("%ram-free%"))
			str = str.replace("%ram-free%", Long.toString(fram.longValue()));

		if (str.contains("%player-uuid%"))
			str = str.replace("%player-uuid%", p.getUniqueId().toString());

		if (str.contains("%game-version%"))
			str = str.replace("%game-version%", getProxy().getGameVersion());

		if (str.contains("%bungee-online%"))
			str = str.replace("%bungee-online%", Integer.toString(getProxy().getOnlineCount()));

		if (str.contains("%player-country%"))
			str = str.replace("%player-country%", p.getLocale() == null ? "unknown" : p.getLocale().getCountry());

		str = str.replace("\\n", "\n");
		return colorMsg(str);
	}

	private String getTimeAsString(String pattern) {
		if (pattern.isEmpty()) {
			return pattern;
		}

		TimeZone zone = ConfigConstants.isUseSystemZone() ? TimeZone.getTimeZone(java.time.ZoneId.systemDefault())
				: TimeZone.getTimeZone(ConfigConstants.getTimeZone());
		LocalDateTime now = zone == null ? LocalDateTime.now() : LocalDateTime.now(zone.toZoneId());

		return now.format(DateTimeFormatter.ofPattern(pattern));
	}

	public boolean checkOnlinePlayers() {
		int min = ConfigConstants.getMinPlayers();
		return min > 0 && getProxy().getPlayers().size() >= min;
	}

	@SuppressWarnings("deprecation")
	void sendMessage(CommandSender s, String msg) {
		if (msg != null && !msg.trim().isEmpty()) {
			s.sendMessage(colorMsg(msg));
		}
	}

	public String colorMsg(String s) {
		if (s == null) {
			return "";
		}

		if (s.contains("#")) {
			s = Global.matchColorRegex(s);
		}

		return ChatColor.translateAlternateColorCodes('&', s);
	}

	public MessageFileHandler getMessageFileHandler() {
		return messageFileHandler;
	}

	public Configuration getConfig() {
		return config;
	}

	public Set<UUID> getToggledPlayers() {
		return players;
	}
}