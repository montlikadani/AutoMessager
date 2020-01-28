package hu.montlikadani.AutoMessager.bukkit;

import static hu.montlikadani.AutoMessager.bukkit.Util.colorMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.logConsole;
import static hu.montlikadani.AutoMessager.bukkit.Util.sendMsg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.StandardSystemProperty;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import net.milkbowl.vault.permission.Permission;

public class AutoMessager extends JavaPlugin implements Listener {

	private static AutoMessager instance;

	private Configuration conf = null;
	private Announce announce = null;
	private Time time = null;
	private Permission perm = null;
	private MessageFileHandler fileHandler = null;

	private boolean isSpigot = false;

	@Override
	public void onEnable() {
		instance = this;

		try {
			if (!checkJavaVersion()) {
				getServer().getPluginManager().disablePlugin(this);
				return;
			}

			try {
				Class.forName("org.spigotmc.SpigotConfig");
				isSpigot = true;
			} catch (ClassNotFoundException e) {
				isSpigot = false;
			}

			startUp();

			if (conf.papi) {
				if (isPluginEnabled("PlaceholderAPI")) {
					logConsole("Hooked PlaceholderAPI version: "
							+ PlaceholderAPIPlugin.getInstance().getDescription().getVersion());
				} else {
					logConsole(Level.WARNING, "Could not find PlaceholderAPI!");
					logConsole("PlaceholderAPI Download: https://www.spigotmc.org/resources/6245/");
				}
			}

			// This should need to register to prevent issues when using perm
			// I don't know why Vault need this
			if (setupVaultPerm()) {
				Bukkit.getPluginManager().registerEvents(this, this);
			}

			time = new Time(this, conf.timer);
			announce = new Announce(this);
			announce.load();

			Commands cmds = new Commands(this);
			getCommand("automessager").setExecutor(cmds);
			getCommand("automessager").setTabCompleter(cmds);

			Bukkit.getPluginManager().registerEvents(new Listeners(), this);

			loadToggledMessages();
			announce.schedule();

			FileConfiguration config = conf.getConfig();
			if (config.getBoolean("check-update")) {
				logConsole(checkVersion("console"));
			}

			Metrics metrics = new Metrics(this, 1594);
			if (metrics.isEnabled()) {
				metrics.addCustomChart(
						new Metrics.SimplePie("using_placeholderapi", () -> config.getString("placeholderapi")));
				metrics.addCustomChart(
						new Metrics.SimplePie("using_random_messages", () -> config.getString("random")));
				metrics.addCustomChart(new Metrics.SimplePie("message_delay", () -> config.getString("time")));
				metrics.addCustomChart(new Metrics.SimplePie("time_type", () -> config.getString("time-setup")));
				metrics.addCustomChart(
						new Metrics.SimplePie("use_json_message", () -> config.getString("use-json-message")));
				metrics.addCustomChart(
						new Metrics.SingleLineChart("amount_of_texts", () -> fileHandler.getTexts().size()));
				logConsole("Metrics enabled.");
			}

			if (!config.getString("plugin-enable", "").isEmpty()) {
				sendMsg(getServer().getConsoleSender(), colorMsg(config.getString("plugin-enable")));
			}
		} catch (Throwable e) {
			e.printStackTrace();
			logConsole(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues",
					false);
		}
	}

	@Override
	public void onDisable() {
		if (instance == null) return;

		saveToggledMessages();
		getServer().getScheduler().cancelTasks(this);
		instance = null;

		if (!conf.getConfig().getString("plugin-disable", "").isEmpty()) {
			sendMsg(getServer().getConsoleSender(), colorMsg(conf.getConfig().getString("plugin-disable")));
		}
	}

	private void startUp() {
		conf = new Configuration(this);
		conf.loadFiles();
		conf.loadConfigs();
		conf.loadValues();

		fileHandler = new MessageFileHandler(this);
		fileHandler.loadMessages();
	}

	void reload() {
		if (conf == null) {
			conf = new Configuration(this);
		}

		conf.loadFiles();
		conf.loadConfigs();
		conf.loadValues();

		if (fileHandler == null) {
			fileHandler = new MessageFileHandler(this);
		}

		fileHandler.loadFile();
		fileHandler.loadMessages();

		if (time == null) {
			time = new Time(this, conf.timer);
		} else {
			time.setTime(conf.timer);
			time.countTimer();
		}

		if (announce != null) {
			announce.cancelTask();
		} else {
			announce = new Announce(this);
		}

		announce.load();
		announce.schedule();
	}

	private boolean setupVaultPerm() {
		if (!isPluginEnabled("Vault")) {
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

	private String checkVersion(String sender) {
		String msg = "";
		String[] nVersion;
		String[] cVersion;
		String lineWithVersion = "";
		try {
			URL githubUrl = new URL("https://raw.githubusercontent.com/montlikadani/AutoMessager/master/plugin.yml");
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
			nVersion = versionString.replaceAll("[^0-9.]", "").split("\\.");
			double newestVersionNumber = Double.parseDouble(nVersion[0] + "." + nVersion[1]);

			cVersion = getDescription().getVersion().replaceAll("[^0-9.]", "").split("\\.");
			double currentVersionNumber = Double.parseDouble(cVersion[0] + "." + cVersion[1]);

			if (newestVersionNumber > currentVersionNumber) {
				if (sender.equals("console")) {
					msg = "New version (" + versionString
							+ ") is available at https://www.spigotmc.org/resources/43875/";
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
		if (!conf.getConfig().getBoolean("remember-toggle-to-file", true)) {
			return;
		}

		Commands.enabled.clear();

		File f = new File(getFolder(), "toggledmessages.yml");
		if (!f.exists()) {
			return;
		}

		FileConfiguration config = YamlConfiguration.loadConfiguration(f);

		if (config.contains("players")) {
			for (String uuid : config.getConfigurationSection("players").getKeys(false)) {
				Commands.enabled.put(UUID.fromString(uuid), config.getConfigurationSection("players").getBoolean(uuid));
			}
		}
	}

	private void saveToggledMessages() {
		File f = new File(getFolder(), "toggledmessages.yml");
		if (!conf.getConfig().getBoolean("remember-toggle-to-file", true)) {
			if (f.exists()) {
				f.delete();
			}

			return;
		}

		if (Commands.enabled.isEmpty()) {
			return;
		}

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

	boolean checkOnlinePlayers() {
		int min = conf.getConfig().getInt("min-players", 1);
		if (min < 1) {
			return false;
		}

		int online = Bukkit.getOnlinePlayers().size();
		return online >= min;
	}

	File getFolder() {
		File folder = getDataFolder();
		if (!folder.exists()) {
			folder.mkdir();
		}

		return folder;
	}

	private boolean checkJavaVersion() {
		try {
			if (Float.parseFloat(StandardSystemProperty.JAVA_CLASS_VERSION.value()) < 52.0) {
				logConsole(Level.WARNING,
						"You are using an older Java that is not supported. Please use 1.8 or higher versions!", false);
				return false;
			}
		} catch (NumberFormatException e) {
			logConsole(Level.WARNING, "Failed to detect Java version.", false);
			return false;
		}

		return true;
	}

	boolean isPluginEnabled(String name) {
		return Bukkit.getPluginManager().getPlugin(name) != null && Bukkit.getPluginManager().isPluginEnabled(name);
	}

	public Configuration getConf() {
		return conf;
	}

	public Announce getAnnounce() {
		return announce;
	}

	public MessageFileHandler getFileHandler() {
		return fileHandler;
	}

	public Permission getVaultPerm() {
		return perm;
	}

	public Time getTimeC() {
		return time;
	}

	public boolean isSpigot() {
		return isSpigot;
	}

	public static AutoMessager getInstance() {
		return instance;
	}

	// To fix issue when Vault not found
	private class Listeners implements Listener {

		@EventHandler
		public void onPlJoin(PlayerJoinEvent event) {
			Player p = event.getPlayer();
			if (conf.getConfig().getBoolean("check-update") && p.isOp()) {
				p.sendMessage(checkVersion("player"));
			}
		}
	}
}