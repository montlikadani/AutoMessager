package hu.montlikadani.AutoMessager.bukkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.StandardSystemProperty;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import net.milkbowl.vault.permission.Permission;

public class AutoMessager extends JavaPlugin implements Listener {

	private static AutoMessager instance;

	private FileConfiguration config;
	FileConfiguration messages, bpls;
	private File config_file, messages_file;
	File bpls_file;

	private Announce announce = null;
	private Time time = null;
	private Permission perm = null;
	private File file;

	private List<String> msgs = new ArrayList<>();

	private boolean papi;
	private int timer = -1;
	private int cver = 5;
	private int msver = 4;

	@Override
	public void onEnable() {
		instance = this;

		try {
			if (!checkJavaVersion()) {
				getServer().getPluginManager().disablePlugin(this);
				return;
			}

			loadFiles();

			if (config.getBoolean("placeholderapi")) {
				if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
					papi = true;
					logConsole("Hooked PlaceholderAPI version: "
							+ PlaceholderAPIPlugin.getInstance().getDescription().getVersion());
				} else {
					papi = false;
					logConsole(Level.WARNING, "Could not find PlaceholderAPI!");
					logConsole("PlaceholderAPI Download: https://www.spigotmc.org/resources/6245/");
				}
			} else {
				papi = false;
			}

			setupVaultPerm();

			time = new Time(this, timer);
			announce = new Announce(this);
			announce.load();

			Commands cmds = new Commands(this);
			getCommand("automessager").setExecutor(cmds);
			getCommand("automessager").setTabCompleter(cmds);

			Bukkit.getPluginManager().registerEvents(this, this);

			loadToggledMessages();
			announce.schedule();

			if (config.getBoolean("check-update")) {
				logConsole(checkVersion("console"));
			}

			Metrics metrics = new Metrics(this);
			if (metrics.isEnabled()) {
				metrics.addCustomChart(
						new Metrics.SimplePie("using_placeholderapi", () -> config.getString("placeholderapi")));
				metrics.addCustomChart(
						new Metrics.SimplePie("using_random_messages", () -> config.getString("random")));
				metrics.addCustomChart(new Metrics.SimplePie("message_delay", () -> config.getString("time")));
				metrics.addCustomChart(new Metrics.SimplePie("time_type", () -> config.getString("time-setup")));
				metrics.addCustomChart(
						new Metrics.SimplePie("use_json_message", () -> config.getString("use-json-message")));
				logConsole("Metrics enabled.");
			}

			if (!config.getString("plugin-enable", "").equals("")) {
				sendMsg(getServer().getConsoleSender(), colorMsg(config.getString("plugin-enable")));
			}
		} catch (Throwable e) {
			e.printStackTrace();
			getLogger().log(Level.WARNING, "There was an error. Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues");
		}
	}

	@Override
	public void onDisable() {
		if (instance == null) return;

		try {
			saveToggledMessages();
			getServer().getScheduler().cancelTasks(this);
			instance = null;

			if (!config.getString("plugin-disable", "").equals("")) {
				sendMsg(getServer().getConsoleSender(), colorMsg(config.getString("plugin-disable")));
			}
		} catch (Throwable e) {
			e.printStackTrace();
			getLogger().log(Level.WARNING, "There was an error. Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues");
		}
	}

	void loadFiles() {
		try {
			if (config_file == null) {
				config_file = new File(getFolder(), "config.yml");
			}

			if (messages_file == null) {
				messages_file = new File(getFolder(), "messages.yml");
			}

			if (bpls_file == null) {
				bpls_file = new File(getFolder(), "banned-players.yml");
			}

			if (config_file.exists()) {
				config = YamlConfiguration.loadConfiguration(config_file);
				config.load(config_file);
				reloadConfig();

				this.papi = this.config.getBoolean("placeholderapi");
				this.timer = this.config.getInt("time", 3);

				if (!config.isSet("config-version") || !config.get("config-version").equals(cver)) {
					logConsole(Level.WARNING, "Found outdated configuration (config.yml)! (Your version: " + config.getInt("config-version") + " | Newest version: " + cver + ")");
				}
			} else {
				saveResource("config.yml", false);
				config = YamlConfiguration.loadConfiguration(config_file);
				logConsole("The 'config.yml' file successfully created!");
			}

			if (messages_file.exists()) {
				messages = YamlConfiguration.loadConfiguration(messages_file);
				messages.load(messages_file);

				if (!messages.isSet("config-version") || !messages.get("config-version").equals(msver)) {
					logConsole(Level.WARNING, "Found outdated configuration (messages.yml)! (Your version: " + messages.getInt("config-version") + " | Newest version: " + msver + ")");
				}
			} else {
				saveResource("messages.yml", false);
				messages = YamlConfiguration.loadConfiguration(messages_file);
				logConsole("The 'messages.yml' file successfully created!");
			}

			if (bpls_file.exists()) {
				bpls = YamlConfiguration.loadConfiguration(bpls_file);
				bpls.load(bpls_file);
				bpls.save(bpls_file);
			}

			loadMessages();
		} catch (Throwable e) {
			e.printStackTrace();
			throwMsg();
		}
	}

	private boolean setupVaultPerm() {
		if (!getServer().getPluginManager().isPluginEnabled("Vault")) {
			return false;
		}

		org.bukkit.plugin.RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager()
				.getRegistration(Permission.class);
		if (rsp == null) {
			return false;
		}

		perm = rsp.getProvider();
		return perm != null;
	}

	void loadMessages() {
		String fName = config.getString("message-file", "");
		if (fName.equals("")) {
			logConsole(Level.WARNING, "The message-file string is empty or not found. Defaulting to messages.txt");
			fName = "messages.txt";
		}

		if (fName.equals("messages.yml")) {
			logConsole(Level.WARNING, "The message file cannot be an existing message file! Defaulting to messages.txt");
			fName = "messages.txt";
		}

		msgs.clear();

		if (file == null) {
			file = new File(getFolder(), config.getString("message-file", "messages.txt"));
		}

		if (fName.endsWith(".yml")) {
			FileConfiguration msgC = YamlConfiguration.loadConfiguration(file);
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				msgC.load(file);
			} catch (InvalidConfigurationException | IOException e1) {
				e1.printStackTrace();
			}

			if (!msgC.contains("messages")) {
				List<String> m = Arrays.asList("&aYes, this is an&b Auto&6Message&a.",
						"world:myWorld_&aThis message appeared in myWorld.");

				msgC.set("messages", m);
			}

			try {
				msgC.save(file);
			} catch (IOException e) {
				e.printStackTrace();
			}

			for (String list : msgC.getStringList("messages")) {
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
				throwMsg();
			}
		}
	}

	@EventHandler
	public void onPlJoin(PlayerJoinEvent event) {
		if (announce == null) {
			announce = new Announce(this);
			announce.load();
			announce.schedule();
		}

		Player p = event.getPlayer();
		if (config.getBoolean("check-update") && p.isOp()) {
			p.sendMessage(checkVersion("player"));
		}
	}

	private String checkVersion(String sender) {
		String msg = "";
		String[] nVersion;
		String[] cVersion;
		String lineWithVersion;
		try {
			URL githubUrl = new URL("https://raw.githubusercontent.com/montlikadani/AutoMessager/master/plugin.yml");
			lineWithVersion = "";
			BufferedReader br = new BufferedReader(new InputStreamReader(githubUrl.openStream()));
			String s;
			while ((s = br.readLine()) != null) {
				String line = s;
				if (line.toLowerCase().contains("version")) {
					lineWithVersion = line;
					break;
				}
			}
			String versionString = lineWithVersion.split(": ")[1];
			nVersion = versionString.split("\\.");
			double newestVersionNumber = Double.parseDouble(nVersion[0] + "." + nVersion[1]);
			cVersion = getDescription().getVersion().split("\\.");
			double currentVersionNumber = Double.parseDouble(cVersion[0] + "." + cVersion[1]);
			if (newestVersionNumber > currentVersionNumber) {
				if (sender.equals("console")) {
					msg = "New version (" + versionString + ") is available at https://www.spigotmc.org/resources/43875/";
				} else if (sender.equals("player")) {
					msg = colorMsg("&8&m&l--------------------------------------------------\n"
							+ "&aA new update for AutoMessager is available!&4 Version:&7 " + versionString
							+ "\n&6Download:&c &nhttps://www.spigotmc.org/resources/43875/"
							+ "\n&8&m&l--------------------------------------------------");
				}
			} else if (sender.equals("console")) {
				msg = "You're running the latest version.";
			}
		} catch (Throwable e) {
			e.printStackTrace();
			logConsole(Level.WARNING, "Failed to compare versions. " + e
					+ " Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues");
		}

		return msg;
	}

	private void loadToggledMessages() {
		Commands.enabled.clear();

		File f = new File(getFolder(), "toggledmessages.yml");
		if (f.exists()) {
			FileConfiguration config = YamlConfiguration.loadConfiguration(f);

			if (config.contains("players")) {
				for (String uuid : config.getConfigurationSection("players").getKeys(false)) {
					Commands.enabled.put(UUID.fromString(uuid),
							config.getConfigurationSection("players").getBoolean(uuid));
				}
			}
		}
	}

	private void saveToggledMessages() {
		if (!Commands.enabled.isEmpty()) {
			File f = new File(getFolder(), "toggledmessages.yml");
			if (!f.exists()) {
				try {
					f.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			FileConfiguration config = YamlConfiguration.loadConfiguration(f);
			config.set("players", null);

			for (Entry<UUID, Boolean> list : Commands.enabled.entrySet()) {
				if (list.getValue()) {
					continue;
				}

				config.set("players." + list.getKey(), list.getValue());
			}

			try {
				config.save(f);
			} catch (IOException e) {
				e.printStackTrace();
			}

			Commands.enabled.clear();
		}
	}

	void logConsole(String error) {
		logConsole(Level.INFO, error);
	}

	void logConsole(Level level, String error) {
		if (config.getBoolean("logconsole")) {
			Bukkit.getLogger().log(level, "[AutoMessager] " + error);
		}

		if (config.getBoolean("log-to-file")) {
			try {
				File saveTo = new File(getFolder(), "log.txt");
				if (!saveTo.exists()) {
					saveTo.createNewFile();
				}
				FileWriter fw = new FileWriter(saveTo, true);
				PrintWriter pw = new PrintWriter(fw);
				Date dt = new Date();
				SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
				String time = df.format(dt);
				pw.println(time + " - [" + level + "] " + error);
				pw.flush();
				pw.close();
			} catch (Throwable e) {
				e.printStackTrace();
				throwMsg();
			}
		}
	}

	int getRandomInt(int maxAmount) {
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
				getRandomInt(maxAmount);
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
				logConsole("End of file reached.");
				return;
			}

			String msg = sb.toString();
			msgs.remove(msg);

			FileWriter fw = new FileWriter(new File(file.getPath()));
			fw.write(msg);
			fw.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throwMsg();
		}
	}

	boolean checkOnlinePlayers() {
		if (config.getInt("min-players") == 0) {
			return true;
		}

		int conf = config.getInt("min-players", 1);
		int online = Bukkit.getOnlinePlayers().size();
		return online >= conf;
	}

	File getFolder() {
		File folder = getDataFolder();
		if (!folder.exists()) {
			folder.mkdir();
		}
		return folder;
	}

	File getMsgFile() {
		return file;
	}

	private boolean checkJavaVersion() {
		try {
			if (Float.parseFloat(StandardSystemProperty.JAVA_CLASS_VERSION.value()) < 52.0) {
				Bukkit.getLogger().log(Level.WARNING, "You are using an older Java that is not supported. Please use 1.8 or higher versions!");
				return false;
			}
		} catch (NumberFormatException e) {
			Bukkit.getLogger().log(Level.WARNING, "Failed to detect Java version.");
			return false;
		}
		return true;
	}

	public List<String> getMessages() {
		return msgs;
	}

	public Announce getAnnounce() {
		return announce;
	}

	public Permission getVaultPerm() {
		return perm;
	}

	public Time getTimeC() {
		return time;
	}

	public static AutoMessager getInstance() {
		return instance;
	}

	void throwMsg() {
		logConsole(Level.WARNING, "There was an error. Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues");
	}

	public String colorMsg(String msg) {
		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	String getMsg(String key, Object... placeholders) {
		String msg = "";

		if (messages.getString(key, "").equals(""))
			return msg;

		msg = colorMsg(messages.getString(key));

		if (placeholders.length > 0) {
			for (int i = 0; i < placeholders.length; i++) {
				if (placeholders.length >= i + 2) {
					msg = msg.replace(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
				}
				i++;
			}
		}
		return msg;
	}

	void sendMsg(org.bukkit.command.CommandSender sender, String s) {
		if (s != null && !s.equals("")) {
			if (s.contains("\n")) {
				for (String msg : s.split("\n")) {
					sender.sendMessage(msg);
				}
			} else {
				sender.sendMessage(s);
			}
		}
	}

	String replaceVariables(Player pl, String str) {
		String online = Integer.toString(Bukkit.getOnlinePlayers().size());
		String max = Integer.toString(Bukkit.getMaxPlayers());

		String path = "placeholder-format.";

		String t = null;
		String dt = null;
		if (str.contains("%server-time%") || str.contains("%date%")) {
			String tPath = path + "time.";
			TimeZone zone = TimeZone.getTimeZone(config.getString(tPath + "time-zone"));
			Calendar now = null;
			if (zone == null) {
				now = Calendar.getInstance();
			} else {
				now = Calendar.getInstance(zone);
			}

			java.text.DateFormat tf = null;
			if (!config.getString(tPath + "time-format.format", "").equals(""))
				tf = new SimpleDateFormat(config.getString(tPath + "time-format.format"));

			java.text.DateFormat df = null;
			if (!config.getString(tPath + "date-format.format", "").equals(""))
				df = new SimpleDateFormat(config.getString(tPath + "date-format.format"));

			t = tf != null ? tf.format(now.getTime()) : now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE);
			dt = df != null ? df.format(now.getTime()) : now.get(Calendar.YEAR) + "/" + now.get(Calendar.DATE);
		}

		if (!config.getString(path + "title", "").equals("")) {
			str = str.replace("%title%", config.getString(path + "title").replace("\\n", "\n"));
		}
		if (!config.getString(path + "suffix", "").equals("")) {
			str = str.replace("%suffix%", config.getString(path + "suffix").replace("\\n", "\n"));
		}

		str = setPlaceholders(pl, str);
		str = setSymbols(str);
		if (t != null)
			str = str.replace("%server-time%", t);

		if (dt != null)
			str = str.replace("%date%", dt);

		if (str.contains("%online-players%"))
			str = str.replace("%online-players%", online);

		if (str.contains("%max-players%"))
			str = str.replace("%max-players%", max);

		if (str.contains("%servertype%"))
			str = str.replace("%servertype%", Bukkit.getServer().getName());

		if (str.contains("%mc-version%"))
			str = str.replace("%mc-version%", Bukkit.getBukkitVersion());

		if (str.contains("%motd%"))
			str = str.replace("%motd%", Bukkit.getServer().getMotd());

		return colorMsg(str);
	}

	@SuppressWarnings("deprecation")
	String setPlaceholders(Player p, String s) {
		if (papi && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			if (PlaceholderAPI.containsPlaceholders(s)) {
				s = PlaceholderAPI.setPlaceholders(p, s);
			}
		} else {
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
				s = s.replace("%player-health%", p.getHealth() + "");
			if (s.contains("%player-max-health%"))
				s = s.replace("%player-max-health%", Bukkit.getVersion().contains("1.8") ? String.valueOf(p.getMaxHealth())
						: String.valueOf(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue()));
		}
		return s;
	}

	String setSymbols(String s) {
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