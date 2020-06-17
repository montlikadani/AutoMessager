package hu.montlikadani.AutoMessager.bukkit;

import static hu.montlikadani.AutoMessager.bukkit.Util.logConsole;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Configuration {

	private AutoMessager plugin;

	private FileConfiguration config, messages, restricted;
	private File config_file, messages_file, restricted_file;

	public boolean papi = false;
	public String timer = "-1";

	private int cver = 6;

	public Configuration(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public void loadFiles() {
		File folder = plugin.getFolder();

		if (config_file == null) {
			config_file = new File(folder, "config.yml");
		}

		if (messages_file == null) {
			messages_file = new File(folder, "plugin-messages.yml");
		}

		if (restricted_file == null) {
			restricted_file = new File(folder, "restricted-players.yml");
		}
	}

	public void loadConfigs() {
		try {
			if (!config_file.exists()) {
				createFile(config_file, "config.yml", false);
			}

			config = YamlConfiguration.loadConfiguration(config_file);
			config.load(config_file);

			if (!config.isSet("config-version") || !config.get("config-version").equals(cver)) {
				logConsole(Level.WARNING, "Found outdated configuration (config.yml)! (Your version: "
						+ config.getInt("config-version") + " | Newest version: " + cver + ")");
			}

			if (!messages_file.exists()) {
				createFile(messages_file, "plugin-messages.yml", false);
			}

			messages = YamlConfiguration.loadConfiguration(messages_file);
			messages.load(messages_file);
			messages.save(messages_file);

			if (isRestrictFileExists()) {
				restricted = YamlConfiguration.loadConfiguration(restricted_file);
				restricted.load(restricted_file);

				List<String> old = restricted.getStringList("blacklisted-players");
				restricted.set("restricted-players", old);
				restricted.set("blacklisted-players", null);
				restricted.save(restricted_file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadValues() {
		this.papi = this.config.getBoolean("placeholderapi", false);
		this.timer = this.config.getString("time", "3");
	}

	void createFile(File file, String name, boolean newFile) {
		if (newFile) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			plugin.saveResource(name, false);
		}

		logConsole("The '" + name + "' file successfully created!", false);
	}

	public void createRestrictedFile() {
		if (restricted != null && isRestrictFileExists()) {
			return;
		}

		try {
			createFile(restricted_file, "restricted-players.yml", true);

			restricted = YamlConfiguration.loadConfiguration(restricted_file);
			restricted.load(restricted_file);
			restricted.save(restricted_file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isRestrictFileExists() {
		return restricted_file != null && restricted_file.exists();
	}

	public FileConfiguration getMessages() {
		return messages;
	}

	public File getMessagesFile() {
		return messages_file;
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public FileConfiguration getRestrictConfig() {
		return restricted;
	}

	public File getConfigFile() {
		return config_file;
	}

	public File getRestrictFile() {
		return restricted_file;
	}
}
