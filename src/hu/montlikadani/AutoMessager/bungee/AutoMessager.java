package hu.montlikadani.AutoMessager.bungee;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

public class AutoMessager extends Plugin implements Listener {

	private List<UUID> msgEnable = new ArrayList<>();
	private List<String> msgs = new ArrayList<>();

	private static AutoMessager instance;
	private boolean enabled;

	public Configuration config;
	private Announce announce;
	private File file;

	public int time = 0;
	private int cver = 2;

	@Override
	public void onEnable() {
		try {
			enabled = true;
			instance = this;
			createFile();

			registerCommand();
			getProxy().getPluginManager().registerListener(this, this);

			announce = new Announce(this);
			announce.load();
			announce.schedule();
		} catch (Throwable e) {
			e.printStackTrace();
			getLogger().log(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues");
		}
	}

	@Override
	public void onDisable() {
		try {
			announce.cancelTask();
			getProxy().getPluginManager().unregisterCommands(this);
			getProxy().getPluginManager().unregisterListener(this);
			announce = null;
			enabled = false;
		} catch (Throwable e) {
			e.printStackTrace();
			getLogger().log(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues");
		}
	}

	private void createFile() {
		try {
			if (!getDataFolder().exists())
				getDataFolder().mkdir();

			File file = new File(getDataFolder(), "bungeeconfig.yml");
			if (!file.exists()) {
				try (InputStream in = getResourceAsStream("bungeeconfig.yml")) {
					Files.copy(in, file.toPath());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

			if (!config.get("config-version").equals(cver))
				getLogger().log(Level.WARNING, "Found outdated configuration (bungeeconfig.yml)! (Your version: "
						+ config.getInt("config-version") + " | Newest version: " + cver + ")");

			loadMessages();
		} catch (Throwable e) {
			e.printStackTrace();
			getLogger().log(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues");
		}
	}

	void loadMessages() {
		String fName = config.getString("message-file", "");
		if (fName.equals("")) {
			getLogger().log(Level.WARNING, "The message-file string is empty or not found. Defaulting to messages.txt");
			fName = "messages.txt";
		}

		if (fName.equals("messages.yml")) {
			getLogger().log(Level.WARNING,
					"The message file cannot be an existing message file! Defaulting to messages.txt");
			fName = "messages.txt";
		}

		msgs.clear();

		if (file == null) {
			file = new File(getDataFolder(), config.getString("message-file", "messages.txt"));
		}

		if (fName.endsWith(".yml")) {
			ConfigurationProvider msgC = ConfigurationProvider.getProvider(YamlConfiguration.class);
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			Configuration c = null;
			try {
				c = msgC.load(file);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			if (c == null) {
				getLogger().log(Level.SEVERE, "Error has occured while loading the file for you!");
				return;
			}

			if (!c.contains("messages")) {
				List<String> m = Arrays.asList("&aYes, this is an&b Auto&6Message&a.",
						"&cThis plugin is now in BungeeCord software.");

				c.set("messages", m);
			}

			try {
				msgC.save(c, file);
			} catch (IOException e) {
				e.printStackTrace();
			}

			for (String list : c.getStringList("messages")) {
				msgs.add(list);
			}
		} else {
			try {
				if (!file.exists()) {
					file.createNewFile();
				}

				BufferedReader read = new BufferedReader(new FileReader(file));
				try {
					String line;
					while ((line = read.readLine()) != null) {
						if (line.startsWith("#")) {
							continue;
						}

						msgs.add(line);
					}
				} finally {
					read.close();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@EventHandler
	public void onJoin(LoginEvent event) {
		if (announce == null) {
			announce = new Announce(this);
			announce.load();
			announce.schedule();
		}
	}

	private void registerCommand() {
		getProxy().getPluginManager().registerCommand(this, new Command("automessager") {
			public void execute(CommandSender s, String[] args) {
				if (args.length == 0) {
					if (!s.hasPermission("automessager.help")) {
						sendMessage(s, "messages.no-permission");
						return;
					}

					for (String msg : config.getStringList("messages.chat-messages")) {
						s.sendMessage(new ComponentBuilder(colorMsg(msg)).create());
					}

					return;
				}

				if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
					if (!s.hasPermission("automessager.reload")) {
						sendMessage(s, "messages.no-permission");
						return;
					}

					if (announce != null) {
						announce.cancelTask();
					} else {
						announce = new Announce(instance);
						announce.load();
					}

					announce.schedule();

					createFile();
					sendMessage(s, "messages.reload-config");
				} else if (args[0].equalsIgnoreCase("toggle")) {
					if (!s.hasPermission("automessager.toggle")) {
						sendMessage(s, "messages.no-permission");
						return;
					}

					if (getProxy().getPlayers().isEmpty()) {
						sendMessage(s, "messages.toggle.no-player");
						return;
					}

					for (ProxiedPlayer pl : getProxy().getPlayers()) {
						if (!msgEnable.contains(pl.getUniqueId())) {
							msgEnable.add(pl.getUniqueId());
							sendMessage(s, "messages.toggle.disabled");
						} else {
							msgEnable.remove(pl.getUniqueId());
							sendMessage(s, "messages.toggle.enabled");
						}
					}
				} else if (args[0].equalsIgnoreCase("broadcast") || args[0].equalsIgnoreCase("bc")) {
					if (!s.hasPermission("automessager.broadcast")) {
						sendMessage(s, "messages.no-permission");
						return;
					}

					if (args.length < 2) {
						sendMessage(s, "messages.broadcast-usage");
						return;
					}

					StringBuilder builder = new StringBuilder();
					for (int i = 1; i < args.length; i++) {
						builder.append(args[i] + " ");
					}

					String msg = builder.toString();
					msg = colorMsg(msg);
					msg = setSymbols(msg);
					getProxy().broadcast(new ComponentBuilder(
							colorMsg(config.getString("messages.broadcast-message").replace("%message%", msg)))
									.create());
				} else if (args[0].equalsIgnoreCase("list")) {
					if (!s.hasPermission("automessager.list")) {
						sendMessage(s, "messages.no-permission");
						return;
					}

					if (msgs.size() < 1) {
						sendMessage(s, "messages.no-message-to-list");
						return;
					}

					for (int i = 0; i < msgs.size(); i++) {
						String msgt = msgs.get(i);
						if (msgt.isEmpty()) {
							continue;
						}

						s.sendMessage(new ComponentBuilder(msgt).create());
					}
				}
			}
		});
	}

	@SuppressWarnings("deprecation")
	public String replaceVariables(String str, ProxiedPlayer p) {
		int online = p.getServer().getInfo().getPlayers().size();

		Runtime r = Runtime.getRuntime();
		Long fram = Long.valueOf(r.freeMemory() / 1048576L);
		Long mram = Long.valueOf(r.maxMemory() / 1048576L);
		Long uram = Long.valueOf((r.totalMemory() - r.freeMemory()) / 1048576L);

		String t = null;
		String dt = null;
		if (str.contains("%server-time%") || str.contains("%date%")) {
			String path = "placeholder-format.time.";
			TimeZone zone = TimeZone.getTimeZone(config.getString(path + "time-zone"));
			Calendar now = null;
			if (zone == null) {
				now = Calendar.getInstance();
			} else {
				now = Calendar.getInstance(zone);
			}

			java.text.DateFormat tf = null;
			if (!config.getString(path + "time-format.format", "").equals(""))
				tf = new SimpleDateFormat(config.getString(path + "time-format.format"));

			java.text.DateFormat df = null;
			if (!config.getString(path + "date-format.format", "").equals(""))
				df = new SimpleDateFormat(config.getString(path + "date-format.format"));

			t = tf != null ? tf.format(now.getTime()) : now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE);
			dt = df != null ? df.format(now.getTime()) : now.get(Calendar.YEAR) + "/" + now.get(Calendar.DATE);
		}

		str = setSymbols(str);
		if (t != null)
			str = str.replace("%time%", t);

		if (dt != null)
			str = str.replace("%date%", dt);

		if (str.contains("%server%"))
			str = str.replace("%server%", p.getServer().getInfo().getName());

		if (str.contains("%server-online%"))
			str = str.replace("%server-online%", Integer.toString(online));

		if (str.contains("%max-players%"))
			str = str.replace("%max-players%", Integer
					.toString(getProxy().getConfigurationAdapter().getListeners().iterator().next().getMaxPlayers()));

		if (str.contains("%ip%"))
			str = str.replace("%ip%", String.valueOf(p.getAddress().getAddress().getHostAddress()));

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

		if (str.contains("%bungee-motd%"))
			str = str.replace("%bungee-motd%", p.getServer().getInfo().getMotd());

		if (str.contains("%player-country%"))
			str = str.replace("%player-country%", p.getLocale() == null ? "unknown" : p.getLocale().getCountry());

		str = str.replace("\\n", "\n");
		return colorMsg(str);
	}

	public boolean checkOnlinePlayers() {
		if (config.getInt("min-players") == 0) {
			return true;
		}

		int conf = config.getInt("min-players", 1);
		int online = getProxy().getPlayers().size();
		return online >= conf;
	}

	public int getRandomInt(int maxAmount) {
		int qwe = 0;
		int a;
		if (maxAmount < 3 || qwe == 0) {
			Random r = new Random();
			int num = 0;
			for (int count = 1; count <= 2; count++) {
				num = 1 + r.nextInt(maxAmount);
			}
			a = num;
		} else {
			Random r = new Random();
			int num = 0;
			for (int count = 1; count <= 2; count++) {
				num = 1 + r.nextInt(maxAmount);
			}
			a = num;
			if (qwe == a) {
				this.getRandomInt(maxAmount);
			}
		}
		return a;
	}

	void deleteMessage(File file, int lines) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			StringBuffer sb = new StringBuffer("");
			int linenumber = 1;
			String line;
			int numlines = 1;
			while ((line = br.readLine()) != null) {
				if (linenumber < lines || linenumber >= lines + numlines)
					sb.append(line + "\n");

				linenumber++;
			}

			br.close();

			if ((lines + numlines) > linenumber) {
				getLogger().log(Level.INFO, "End of file reached.");
				return;
			}

			String msg = sb.toString();
			msgs.remove(msg);

			FileWriter fw = new FileWriter(new File(file.getPath()));
			fw.write(msg);
			fw.close();
		} catch (Throwable e) {
			e.printStackTrace();
			getLogger().log(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues");
		}
	}

	private void sendMessage(CommandSender s, String path) {
		if (!config.getString(path, "").equals(""))
			s.sendMessage(new ComponentBuilder(colorMsg(config.getString(path))).create());
	}

	public String colorMsg(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public List<String> getMessages() {
		return msgs;
	}

	public List<UUID> getEnabledMessages() {
		return msgEnable;
	}

	public File getMsgFile() {
		return file;
	}

	public AutoMessager getInstance() {
		return instance;
	}

	public String setSymbols(String s) {
		s = s.replace("<0>", "•");
		s = s.replace("<1>", "➤");
		s = s.replace("<2>", "™");
		s = s.replace("<3>", "↑");
		s = s.replace("<4>", "→");
		s = s.replace("<5>", "↓");
		s = s.replace("<6>", "∞");
		s = s.replace("<7>", "░");
		s = s.replace("<8>", "▲");
		s = s.replace("<9>", "▶");
		s = s.replace("<10>", "◀");
		s = s.replace("<11>", "●");
		s = s.replace("<12>", "★");
		s = s.replace("<13>", "☆");
		s = s.replace("<14>", "☐");
		s = s.replace("<15>", "☑");
		s = s.replace("<16>", "☠");
		s = s.replace("<17>", "☢");
		s = s.replace("<18>", "☣");
		s = s.replace("<19>", "☹");
		s = s.replace("<20>", "☺");
		s = s.replace("<21>", "✓");
		s = s.replace("<22>", "✔");
		s = s.replace("<23>", "✘");
		s = s.replace("<24>", "✚");
		s = s.replace("<25>", "℻");
		s = s.replace("<26>", "✠");
		s = s.replace("<27>", "✡");
		s = s.replace("<28>", "✦");
		s = s.replace("<29>", "✧");
		s = s.replace("<30>", "✩");
		s = s.replace("<31>", "✪");
		s = s.replace("<32>", "✮");
		s = s.replace("<33>", "✯");
		s = s.replace("<34>", "㋡");
		s = s.replace("<35>", "❝");
		s = s.replace("<36>", "❞");
		s = s.replace("<37>", "ツ");
		s = s.replace("<38>", "♩");
		s = s.replace("<39>", "♪");
		s = s.replace("<40>", "♫");
		s = s.replace("<41>", "♬");
		s = s.replace("<42>", "♭");
		s = s.replace("<43>", "♮");
		s = s.replace("<44>", "♯");
		s = s.replace("<45>", "¶");
		s = s.replace("<46>", "\u00A9");
		s = s.replace("<47>", "\u00AE");
		s = s.replace("<48>", "⏎");
		s = s.replace("<49>", "⇧");
		s = s.replace("<50>", "⇪");
		s = s.replace("<51>", "ᴴᴰ");
		s = s.replace("<52>", "☒");
		s = s.replace("<53>", "♠");
		s = s.replace("<54>", "♣");
		s = s.replace("<55>", "☻");
		s = s.replace("<56>", "▓");
		s = s.replace("<57>", "➾");
		s = s.replace("<58>", "➔");
		s = s.replace("<59>", "➳");
		s = s.replace("<60>", "➧");
		s = s.replace("<61>", "《");
		s = s.replace("<62>", "》");
		s = s.replace("<63>", "︾");
		s = s.replace("<64>", "︽");
		s = s.replace("<65>", "☃");
		s = s.replace("<66>", "¹");
		s = s.replace("<67>", "²");
		s = s.replace("<68>", "³");
		s = s.replace("<69>", "≈");
		s = s.replace("<70>", "℠");
		s = s.replace("<71>", "\u2665");
		s = s.replace("<72>", "✬");
		s = s.replace("<73>", "↔");
		s = s.replace("<74>", "«");
		s = s.replace("<75>", "»");
		s = s.replace("<76>", "☀");
		s = s.replace("<77>", "♦");
		s = s.replace("<78>", "₽");
		s = s.replace("<79>", "☎");
		s = s.replace("<80>", "☂");
		s = s.replace("<81>", "←");
		s = s.replace("<82>", "↖");
		s = s.replace("<83>", "↗");
		s = s.replace("<84>", "↘");
		s = s.replace("<85>", "↙");
		s = s.replace("<86>", "➲");
		s = s.replace("<87>", "✐");
		s = s.replace("<88>", "✎");
		s = s.replace("<89>", "✏");
		s = s.replace("<90>", "✆");
		s = s.replace("<91>", "◄");
		s = s.replace("<92>", "☼");
		s = s.replace("<93>", "►");
		s = s.replace("<94>", "↕");
		s = s.replace("<95>", "▼");
		s = s.replace("<96>", "①");
		s = s.replace("<97>", "②");
		s = s.replace("<98>", "③");
		s = s.replace("<99>", "④");
		s = s.replace("<100>", "⑤");
		s = s.replace("<101>", "⑥");
		s = s.replace("<102>", "⑦");
		s = s.replace("<103>", "⑧");
		s = s.replace("<104>", "⑨");
		s = s.replace("<105>", "⑩");
		s = s.replace("<106>", "⑪");
		s = s.replace("<107>", "⑫");
		s = s.replace("<108>", "⑬");
		s = s.replace("<109>", "⑭");
		s = s.replace("<110>", "⑮");
		s = s.replace("<111>", "⑯");
		s = s.replace("<112>", "⑰");
		s = s.replace("<113>", "⑱");
		s = s.replace("<114>", "⑲");
		s = s.replace("<115>", "⑳");
		s = s.replace("<116>", "♨");
		s = s.replace("<117>", "✑");
		s = s.replace("<118>", "✖");
		s = s.replace("<119>", "✰");
		s = s.replace("<120>", "✶");
		s = s.replace("<121>", "╗");
		s = s.replace("<122>", "╣");
		s = s.replace("<123>", "◙");
		s = s.replace("<124>", "○");
		s = s.replace("<125>", "╠");
		s = s.replace("<126>", "┤");
		s = s.replace("<127>", "║");
		s = s.replace("<128>", "╝");
		s = s.replace("<129>", "⌂");
		s = s.replace("<130>", "┐");
		s = s.replace("<131>", "❉");
		s = s.replace("<132>", "⌲");
		s = s.replace("<133>", "½");
		s = s.replace("<134>", "¼");
		s = s.replace("<135>", "¾");
		s = s.replace("<136>", "⅓");
		s = s.replace("<137>", "⅔");
		s = s.replace("<138>", "№");
		s = s.replace("<139>", "†");
		s = s.replace("<140>", "‡");
		s = s.replace("<141>", "µ");
		s = s.replace("<142>", "¢");
		s = s.replace("<143>", "£");
		s = s.replace("<144>", "∅");
		s = s.replace("<145>", "≤");
		s = s.replace("<146>", "≥");
		s = s.replace("<147>", "≠");
		s = s.replace("<148>", "∧");
		s = s.replace("<149>", "∨");
		s = s.replace("<150>", "∩");
		s = s.replace("<151>", "∪");
		s = s.replace("<152>", "∈");
		s = s.replace("<153>", "∀");
		s = s.replace("<154>", "∃");
		s = s.replace("<155>", "∄");
		s = s.replace("<156>", "∑");
		s = s.replace("<157>", "∏");
		s = s.replace("<158>", "↺");
		s = s.replace("<159>", "↻");
		s = s.replace("<160>", "Ω");
		return s;
	}
}