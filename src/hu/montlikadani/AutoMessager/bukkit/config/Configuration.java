package hu.montlikadani.AutoMessager.bukkit.config;

import static hu.montlikadani.AutoMessager.bukkit.utils.Util.logConsole;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;

public class Configuration {

	private AutoMessager plugin;

	private FileConfiguration config, messages, restricted;
	private File configFile, messagesFile, restrictedFile;

	private int cver = 6;

	public Configuration(AutoMessager plugin) {
		this.plugin = plugin;

		File folder = plugin.getFolder();

		configFile = new File(folder, "config.yml");
		messagesFile = new File(folder, "plugin-messages.yml");
		restrictedFile = new File(folder, "restricted-players.yml");
	}

	public void loadConfigs() {
		try {
			if (!configFile.exists()) {
				createFile(configFile, "config.yml", false);
			}

			config = YamlConfiguration.loadConfiguration(configFile);
			config.load(configFile);
			ConfigConstants.load(config);

			if (!config.isSet("config-version") || !config.get("config-version").equals(cver)) {
				logConsole(Level.WARNING, "Found outdated configuration (config.yml)! (Your version: "
						+ config.getInt("config-version") + " | Newest version: " + cver + ")");
			}

			if (!messagesFile.exists()) {
				createFile(messagesFile, "plugin-messages.yml", false);
			}

			messages = YamlConfiguration.loadConfiguration(messagesFile);
			messages.load(messagesFile);
			messages.save(messagesFile);

			if (restrictedFile.exists()) {
				restricted = YamlConfiguration.loadConfiguration(restrictedFile);
				restricted.load(restrictedFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	}

	public void removeUnnededFiles() {
		try {
			if (restrictedFile.exists()) {
				restrictedFile.delete();
			}
		} catch (SecurityException e) {
		}
	}

	public FileConfiguration getMessages() {
		return messages;
	}

	public File getMessagesFile() {
		return messagesFile;
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public FileConfiguration getRestrictConfig() {
		if (restricted == null || !restrictedFile.exists()) {
			createFile(restrictedFile, "restricted-players.yml", true);
			restricted = YamlConfiguration.loadConfiguration(restrictedFile);
		}

		return restricted;
	}

	public File getConfigFile() {
		return configFile;
	}

	public File getRestrictFile() {
		return restrictedFile;
	}
}
