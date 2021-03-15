package hu.montlikadani.AutoMessager.bukkit.config;

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
import java.util.stream.Collectors;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.announce.message.Message;
import hu.montlikadani.AutoMessager.bukkit.utils.Util;

public class MessageFileHandler {

	private AutoMessager plugin;

	private boolean isYaml = false;
	private File file;
	private FileConfiguration yaml;

	private final List<Message> texts = new ArrayList<>();

	public MessageFileHandler(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public File getFile() {
		return file;
	}

	public FileConfiguration getFileConfig() {
		return yaml;
	}

	public List<Message> getTexts() {
		return texts;
	}

	public boolean isYaml() {
		return isYaml;
	}

	public boolean isFileExists() {
		return file != null && file.exists();
	}

	public String getFileName() {
		return isFileExists() ? file.getName() : ConfigConstants.getMessageFile();
	}

	public void loadFile() {
		String msg = "";

		String fName = ConfigConstants.getMessageFile();
		if (fName.trim().isEmpty()) {
			msg = "The message-file string is empty or not found.";
		} else if (fName.equals(plugin.getConf().getMessages().getName())) {
			msg = "The message file cannot be an existing message file!";
		}

		if (!fName.contains(".")) {
			msg = "The message file does not have any file type to create.";
		}

		if (!msg.isEmpty()) {
			Util.logConsole(Level.WARNING, msg + " Defaulting to " + (fName = "messages.txt"));
		}

		if (!(file = new File(plugin.getFolder(), fName)).exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void loadMessages() {
		texts.clear();

		if (!isFileExists()) {
			loadFile();
		}

		if (getFileName().endsWith(".yml")) {
			yaml = new YamlConfiguration();

			try {
				yaml.load(file);
			} catch (InvalidConfigurationException e) {
				Util.logConsole(e.getLocalizedMessage());
			} catch (IOException e1) {
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

			yaml.getStringList("messages").forEach(t -> texts.add(new Message(t)));
		} else {
			isYaml = false;

			try (BufferedReader read = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = read.readLine()) != null) {
					texts.add(new Message(line));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void addText(String msg) {
		if (!isFileExists()) {
			loadFile();
			loadMessages();
		}

		texts.add(new Message(msg));
		saveText();
	}

	public void removeText(int index) {
		if (!isFileExists()) {
			loadFile();
			return;
		}

		texts.remove(index);
		saveText();
	}

	private void saveText() {
		try {
			if (isYaml) {
				yaml.set("messages", texts.stream().map(Message::getText).collect(Collectors.toList()));
				yaml.save(file);
			} else {
				FileWriter fw = new FileWriter(file, true);
				PrintWriter writer = new PrintWriter(fw);
				writer.print("");

				texts.stream().map(Message::getText).forEach(writer::println);
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
