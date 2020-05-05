package hu.montlikadani.AutoMessager.bungee;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class MessageFileHandler {

	private AutoMessager plugin;

	private File file;

	private boolean isYaml = false;

	private final List<String> texts = new ArrayList<>();

	public MessageFileHandler(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public File getMessageFile() {
		return file;
	}

	public Configuration getConfig() {
		if (file == null || !file.exists()) {
			return null;
		}

		ConfigurationProvider f = ConfigurationProvider.getProvider(YamlConfiguration.class);

		try {
			return f.load(file);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		return null;
	}

	public List<String> getTexts() {
		return texts;
	}

	public boolean isYaml() {
		return isYaml;
	}

	void loadFile() {
		String msg = "";

		String fName = plugin.getConfig().getString("message-file", "");
		if (fName.trim().isEmpty()) {
			msg = "The message-file string is empty or not found.";
		}

		if (!fName.contains(".")) {
			msg = "The message file does not have any file type to create.";
		}

		if (!msg.isEmpty()) {
			plugin.getLogger().log(Level.WARNING, msg + " Defaulting to messages.txt");
			fName = "messages.txt";
		}

		file = new File(plugin.getDataFolder(), fName);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	void loadMessages() {
		texts.clear();

		if (file == null || !file.exists()) {
			loadFile();
		}

		if (file.getName().endsWith(".yml")) {
			Configuration c = getConfig();
			if (c == null) {
				plugin.getLogger().log(Level.SEVERE, "Error has occured while loading the file for you!");
				return;
			}

			if (!c.contains("messages")) {
				c.set("messages", Arrays.asList("&aYes, this is an&b Auto&6Message&a.",
						"&cThis plugin is now in BungeeCord software."));
			}

			saveMessages(c);

			isYaml = true;

			c.getStringList("messages").forEach(texts::add);
		} else {
			isYaml = false;

			try (BufferedReader read = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = read.readLine()) != null) {
					if (!line.startsWith("#")) {
						texts.add(line);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void addText(String message) {
		texts.add(message);

		if (file == null || !file.exists()) {
			loadFile();
		}

		if (isYaml) {
			Configuration c = getConfig();
			if (c == null) {
				return;
			}

			c.set("messages", null);
			c.set("messages", texts);

			saveMessages(c);
		} else {
			try {
				FileWriter fw = new FileWriter(file, true);
				PrintWriter pw = new PrintWriter(fw);
				pw.println(message);
				pw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void removeText(int index) {
		texts.remove(index);

		if (file == null || !file.exists()) {
			return;
		}

		try {
			if (isYaml) {
				Configuration c = getConfig();
				if (c == null) {
					return;
				}

				c.set("messages", null);
				c.set("messages", texts);

				saveMessages(c);
			} else {
				FileWriter fw = new FileWriter(file, true);
				PrintWriter writer = new PrintWriter(fw);
				writer.print("");

				texts.forEach(writer::println);
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveMessages(Configuration conf) {
		if (file == null || !file.exists()) {
			return;
		}

		ConfigurationProvider f = ConfigurationProvider.getProvider(YamlConfiguration.class);

		try {
			f.save(conf, file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
