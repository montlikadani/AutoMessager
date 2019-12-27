package hu.montlikadani.AutoMessager.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import static hu.montlikadani.AutoMessager.bukkit.Util.logConsole;

public class Configuration {

	private AutoMessager plugin;

	private FileConfiguration config, messages, bpls;
	private File config_file, messages_file, bpls_file;

	public boolean papi = false;
	public int timer = -1;

	private int cver = 5;
	private int msver = 4;

	public Configuration(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public void loadFiles() {
		File folder = plugin.getFolder();
		if (config_file == null) {
			config_file = new File(folder, "config.yml");
		}

		if (messages_file == null) {
			messages_file = new File(folder, "messages.yml");
		}

		if (bpls_file == null) {
			bpls_file = new File(folder, "banned-players.yml");
		}
	}

	public void loadConfigs() {
		try {
			if (!config_file.exists()) {
				createFile(config_file, "config.yml", false);
			}

			config = YamlConfiguration.loadConfiguration(config_file);
			config.load(config_file);
			plugin.reloadConfig();

			if (!config.isSet("config-version") || !config.get("config-version").equals(cver)) {
				logConsole(Level.WARNING, "Found outdated configuration (config.yml)! (Your version: "
						+ config.getInt("config-version") + " | Newest version: " + cver + ")");
			}

			if (!messages_file.exists()) {
				createFile(messages_file, "messages.yml", false);
			}

			messages = YamlConfiguration.loadConfiguration(messages_file);
			messages.load(messages_file);

			if (!messages.isSet("config-version") || !messages.get("config-version").equals(msver)) {
				logConsole(Level.WARNING, "Found outdated configuration (messages.yml)! (Your version: "
						+ messages.getInt("config-version") + " | Newest version: " + msver + ")");
			}

			if (bpls_file.exists()) {
				bpls = YamlConfiguration.loadConfiguration(bpls_file);
				bpls.load(bpls_file);
				bpls.save(bpls_file);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Util.sendInfo();
		}
	}

	public void loadValues() {
		this.papi = this.config.getBoolean("placeholderapi", false);
		this.timer = this.config.getInt("time", 3);
		// Do we need to load the full config?
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

	public void createBannedFile() {
		if (bpls != null && isBannedFileExists()) {
			return;
		}

		try {
			createFile(bpls_file, "banned-players.yml", true);

			bpls = YamlConfiguration.loadConfiguration(bpls_file);
			bpls.load(bpls_file);
			bpls.save(bpls_file);
		} catch (Exception e) {
			e.printStackTrace();
			Util.sendInfo();
		}
	}

	public boolean isBannedFileExists() {
		return bpls_file != null && bpls_file.exists();
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

	public FileConfiguration getBpls() {
		return bpls;
	}

	public File getConfigFile() {
		return config_file;
	}

	public File getBplsFile() {
		return bpls_file;
	}
}
