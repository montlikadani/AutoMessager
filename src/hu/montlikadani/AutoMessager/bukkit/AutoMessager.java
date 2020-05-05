package hu.montlikadani.AutoMessager.bukkit;

import static hu.montlikadani.AutoMessager.bukkit.Util.colorMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.logConsole;
import static hu.montlikadani.AutoMessager.bukkit.Util.sendMsg;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

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
		long load = System.currentTimeMillis();

		instance = this;

		try {
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
				getServer().getPluginManager().registerEvents(this, this);
			}

			time = new Time(this, conf.timer);
			announce = new Announce(this);
			announce.load();

			Commands cmds = new Commands(this);
			getCommand("automessager").setExecutor(cmds);
			getCommand("automessager").setTabCompleter(cmds);

			getServer().getPluginManager().registerEvents(new Listeners(), this);

			loadToggledMessages();
			announce.schedule();

			UpdateDownloader.checkFromGithub(getServer().getConsoleSender());

			FileConfiguration config = conf.getConfig();
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
				metrics.addCustomChart(new Metrics.SingleLineChart("amount_of_texts", fileHandler.getTexts()::size));
				logConsole("Metrics enabled.");
			}

			if (config.getBoolean("logconsole")) {
				String msg = "&6[&4Auto&9Messager&6]&7 >&a The plugin successfully enabled&6 v"
						+ getDescription().getVersion() + "&a! (" + (System.currentTimeMillis() - load) + "ms)";
				sendMsg(getServer().getConsoleSender(), colorMsg(msg));
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

		if (conf.getConfig().getBoolean("logconsole")) {
			String msg = "&6[&4Auto&9Messager&6]&7 >&c The plugin successfully disabled!";
			Util.sendMsg(getServer().getConsoleSender(), colorMsg(msg));
		}

		instance = null;
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
		perm = rsp == null ? null : rsp.getProvider();
		return perm != null;
	}

	private void loadToggledMessages() {
		if (!conf.getConfig().getBoolean("remember-toggle-to-file", true)) {
			return;
		}

		Commands.ENABLED.clear();

		File f = new File(getFolder(), "toggledmessages.yml");
		if (!f.exists()) {
			return;
		}

		FileConfiguration config = YamlConfiguration.loadConfiguration(f);

		if (config.contains("players")) {
			for (String uuid : config.getConfigurationSection("players").getKeys(false)) {
				Commands.ENABLED.put(UUID.fromString(uuid), config.getConfigurationSection("players").getBoolean(uuid));
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

		if (Commands.ENABLED.isEmpty()) {
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

		for (Entry<UUID, Boolean> list : Commands.ENABLED.entrySet()) {
			if (!list.getValue()) {
				config.set("players." + list.getKey(), list.getValue());
			}
		}

		try {
			config.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Commands.ENABLED.clear();
	}

	boolean checkOnlinePlayers() {
		int min = conf.getConfig().getInt("min-players", 1);
		if (min < 1) {
			return false;
		}

		int online = getServer().getOnlinePlayers().size();
		return online >= min;
	}

	File getFolder() {
		File folder = getDataFolder();
		if (!folder.exists()) {
			folder.mkdir();
		}

		return folder;
	}

	boolean isPluginEnabled(String name) {
		return getServer().getPluginManager().getPlugin(name) != null
				&& getServer().getPluginManager().isPluginEnabled(name);
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
			if (p.isOp()) {
				UpdateDownloader.checkFromGithub(p);
			}
		}
	}
}