package hu.montlikadani.AutoMessager.bukkit;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class MessageFileHandler {

	private AutoMessager plugin;

	private boolean isYaml = false;
	private File file = null;
	private FileConfiguration yaml = null;

	private final List<String> texts = new ArrayList<>();

	public MessageFileHandler(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public File getFile() {
		return file;
	}

	public FileConfiguration getFileConfig() {
		return yaml;
	}

	public List<String> getTexts() {
		return texts;
	}

	public boolean isYaml() {
		return isYaml;
	}

	public void clearTexts() {
		texts.clear();
	}

	public boolean isFileExists() {
		return file != null && file.exists();
	}

	public String getFileName() {
		return isFileExists() ? file.getName() : plugin.getConf().getConfig().getString("message-file", "");
	}

	public void loadFile() {
		String msg = "";

		String fName = plugin.getConf().getConfig().getString("message-file", "");
		if (fName.trim().isEmpty()) {
			msg = "The message-file string is empty or not found.";
		} else if (fName.equals("messages.yml")) {
			msg = "The message file cannot be an existing message file!";
		}

		if (!fName.contains(".")) {
			msg = "The message file does not have any file type to create.";
		}

		if (!msg.isEmpty()) {
			Util.logConsole(Level.WARNING, msg + " Defaulting to messages.txt");
			fName = "messages.txt";
		}

		file = new File(plugin.getFolder(), fName);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void loadMessages() {
		clearTexts();

		if (file == null || !file.exists()) {
			loadFile();
		}

		String fileName = file.getName();
		if (fileName.endsWith(".yml")) {
			yaml = YamlConfiguration.loadConfiguration(file);

			try {
				yaml.load(file);
			} catch (InvalidConfigurationException | IOException e1) {
				e1.printStackTrace();
			}

			if (!yaml.contains("messages")) {
				yaml.set("messages", Arrays.asList("&aYes, this is an&b Auto&6Message&a.",
						"world:myWorld_&aThis message appeared in myWorld."));
			}

			try {
				yaml.save(file);
			} catch (IOException e) {
				e.printStackTrace();
			}

			isYaml = true;

			yaml.getStringList("messages").forEach(texts::add);
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

	public void addText(String msg) {
		if (!isFileExists()) {
			loadFile();
			loadMessages();
		}

		texts.add(msg);

		try {
			if (isYaml) {
				yaml.set("messages", null);
				yaml.set("messages", texts);
				yaml.save(file);
			} else {
				FileWriter fw = new FileWriter(file, true);
				PrintWriter pw = new PrintWriter(fw);
				pw.println(msg);
				pw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void removeText(int index) {
		if (!isFileExists()) {
			loadFile();
			return;
		}

		texts.remove(index);

		try {
			if (isYaml) {
				yaml.set("messages", null);
				yaml.set("messages", texts);
				yaml.save(file);
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
}
