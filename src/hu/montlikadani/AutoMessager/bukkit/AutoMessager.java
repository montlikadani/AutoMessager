package hu.montlikadani.automessager.bukkit;

import static hu.montlikadani.automessager.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.automessager.bukkit.utils.Util.logConsole;
import static hu.montlikadani.automessager.bukkit.utils.Util.sendMsg;

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

import hu.montlikadani.automessager.bukkit.announce.Announce;
import hu.montlikadani.automessager.bukkit.commands.Commands;
import hu.montlikadani.automessager.bukkit.config.ConfigConstants;
import hu.montlikadani.automessager.bukkit.config.Configuration;
import hu.montlikadani.automessager.bukkit.config.MessageFileHandler;
import hu.montlikadani.automessager.bukkit.utils.ServerVersion;
import hu.montlikadani.automessager.bukkit.utils.UpdateDownloader;
import hu.montlikadani.automessager.bukkit.utils.stuff.Complement;
import hu.montlikadani.automessager.bukkit.utils.stuff.Complement1;
import hu.montlikadani.automessager.bukkit.utils.stuff.Complement2;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import net.milkbowl.vault.permission.Permission;

public final class AutoMessager extends JavaPlugin implements Listener {

	private Configuration conf;
	private Announce announce;
	private MessageFileHandler fileHandler;
	private Permission perm;
	private Complement complement;

	private boolean isPaper = false, isSpigot = false;

	@Override
	public void onEnable() {
		long load = System.currentTimeMillis();

		try {
			if (ServerVersion.isCurrentLower(ServerVersion.v1_8_R1)) {
				logConsole(Level.SEVERE,
						"Your server version does not supported by this plugin! Please use 1.8+ or higher versions!");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}

			verifyServerSoftware();
			startUp();

			if (ConfigConstants.isPlaceholderapi() && isPluginEnabled("PlaceholderAPI")) {
				logConsole("Hooked PlaceholderAPI version: "
						+ PlaceholderAPIPlugin.getInstance().getDescription().getVersion());
			}

			setupVaultPerm();

			Optional.ofNullable(getCommand("automessager")).ifPresent(cmd -> {
				Commands cmds = new Commands(this);
				cmd.setExecutor(cmds);
				cmd.setTabCompleter(cmds);
			});

			getServer().getPluginManager().registerEvents(new Listeners(), this);

			loadToggledMessages();
			announce.beginScheduling();

			UpdateDownloader.checkFromGithub(getServer().getConsoleSender());

			if (ConfigConstants.isLogConsole()) {
				sendMsg(getServer().getConsoleSender(), colorMsg("&6[&4Auto&9Messager&6]&7 >&a Enabled&6 v"
						+ getDescription().getVersion() + "&a! (" + (System.currentTimeMillis() - load) + "ms)"));
			}
		} catch (Throwable e) {
			e.printStackTrace();
			logConsole(Level.WARNING,
					"There was an error. Please report it here:\nhttps://github.com/montlikadani/AutoMessager/issues");
		}
	}

	@Override
	public void onDisable() {
		announce.cancelSchedulers();
		saveToggledMessages();
		conf.removeUnnededFiles();

		getServer().getScheduler().cancelTasks(this);
	}

	@Override
	public FileConfiguration getConfig() {
		return conf.getConfig();
	}

	@Override
	public void saveConfig() {
	}

	private void verifyServerSoftware() {
		try {
			Class.forName("org.spigotmc.SpigotConfig");
			isSpigot = true;
		} catch (ClassNotFoundException e) {
			isSpigot = false;
		}

		try {
			Class.forName("com.destroystokyo.paper.PaperConfig");
			isPaper = true;
		} catch (ClassNotFoundException e) {
			isPaper = false;
		}

		boolean kyoriSupported = false;
		try {
			Class.forName("net.kyori.adventure.text.Component");
			kyoriSupported = true;
		} catch (ClassNotFoundException e) {
		}

		complement = (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R3) && kyoriSupported)
				? new Complement2()
				: new Complement1();
	}

	private void startUp() {
		conf = new Configuration(this);
		conf.loadConfigs();

		fileHandler = new MessageFileHandler(this);
		fileHandler.loadMessages();

		announce = new Announce();
	}

	public void reload() {
		conf.loadConfigs();

		if (fileHandler == null) {
			fileHandler = new MessageFileHandler(this);
		}

		fileHandler.loadFile();
		fileHandler.loadMessages();

		announce.cancelSchedulers();
		(announce = new Announce()).beginScheduling();
	}

	private boolean setupVaultPerm() {
		if (!isPluginEnabled("Vault")) {
			return false;
		}

		org.bukkit.plugin.RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager()
				.getRegistration(Permission.class);
		return rsp != null && (perm = rsp.getProvider()) != null;
	}

	private void loadToggledMessages() {
		if (!ConfigConstants.isRememberToggleToFile()) {
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

		if (!ConfigConstants.isRememberToggleToFile() || Commands.ENABLED.isEmpty()) {
			if (f.exists()) {
				f.delete();
			}

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

	public File getFolder() {
		File folder = getDataFolder();
		folder.mkdirs();
		return folder;
	}

	public boolean isPluginEnabled(String name) {
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

	public boolean isSpigot() {
		return isSpigot;
	}

	public boolean isPaper() {
		return isPaper;
	}

	public Complement getComplement() {
		return complement;
	}

	// To fix issue when Vault not found
	private class Listeners implements Listener {

		@EventHandler
		public void onPlJoin(PlayerJoinEvent event) {
			Player p = event.getPlayer();

			announce.beginScheduling();

			if (p.isOp()) {
				UpdateDownloader.checkFromGithub(p);
			}
		}
	}
}