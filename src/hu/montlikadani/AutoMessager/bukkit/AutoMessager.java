package hu.montlikadani.AutoMessager.bukkit;

import static hu.montlikadani.AutoMessager.bukkit.Util.colorMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.logConsole;
import static hu.montlikadani.AutoMessager.bukkit.Util.sendMsg;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.AutoMessager.bukkit.commands.Commands;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import net.milkbowl.vault.permission.Permission;

public class AutoMessager extends JavaPlugin implements Listener {

	private static AutoMessager instance;

	private Configuration conf;
	private Announce announce;
	private Time time;
	private MessageFileHandler fileHandler;
	private Permission perm;

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

			if (conf.papi && isPluginEnabled("PlaceholderAPI")) {
				logConsole("Hooked PlaceholderAPI version: "
						+ PlaceholderAPIPlugin.getInstance().getDescription().getVersion());
			}

			setupVaultPerm();

			time = new Time(this, conf.timer);
			announce = new Announce(this);
			announce.load();

			Optional.ofNullable(getCommand("automessager")).ifPresent(cmd -> {
				Commands cmds = new Commands(this);
				cmd.setExecutor(cmds);
				cmd.setTabCompleter(cmds);
			});

			getServer().getPluginManager().registerEvents(new Listeners(), this);

			loadToggledMessages();
			announce.schedule();

			UpdateDownloader.checkFromGithub(getServer().getConsoleSender());

			Metrics metrics = new Metrics(this, 1594);
			if (metrics.isEnabled()) {
				metrics.addCustomChart(new Metrics.SimplePie("using_placeholderapi",
						() -> conf.getConfig().getString("placeholderapi")));
				metrics.addCustomChart(
						new Metrics.SimplePie("using_random_messages", () -> conf.getConfig().getString("random")));
				metrics.addCustomChart(
						new Metrics.SimplePie("message_delay", () -> conf.getConfig().getString("time")));
				metrics.addCustomChart(new Metrics.SimplePie("time_type", () -> {
					switch (conf.getConfig().getString("time-setup", "")) {
					case "sec":
					case "second":
						return "second";
					case "min":
					case "minute":
						return "minute";
					case "h":
					case "hour":
						return "hour";
					case "ticks":
						return "ticks";
					case "custom":
						return "custom";
					case "given":
					case "specified":
						return "given";
					default:
						return "";
					}
				}));
				metrics.addCustomChart(new Metrics.SingleLineChart("amount_of_texts", fileHandler.getTexts()::size));
			}

			if (conf.getConfig().getBoolean("logconsole")) {
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

		announce.cancelTask();
		saveToggledMessages();
		getServer().getScheduler().cancelTasks(this);
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

	public void reload() {
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
		if (!config.isConfigurationSection("players")) {
			return;
		}

		for (String uuid : config.getConfigurationSection("players").getKeys(false)) {
			Commands.ENABLED.put(UUID.fromString(uuid), config.getConfigurationSection("players").getBoolean(uuid));
		}

		config.set("players", null);
		try {
			config.save(f);
		} catch (IOException e) {
			e.printStackTrace();
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