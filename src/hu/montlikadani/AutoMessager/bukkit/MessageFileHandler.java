package hu.montlikadani.AutoMessager.bukkit;

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

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import static hu.montlikadani.AutoMessager.bukkit.Util.logConsole;

public class MessageFileHandler {

	private AutoMessager plugin;

	private boolean isYaml = false;
	private File file = null;

	private List<String> texts = new ArrayList<>();

	public MessageFileHandler(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public File getFile() {
		return file;
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

	public void loadFile() {
		FileConfiguration config = plugin.getConf().getConfig();
		String fName = config.getString("message-file", "");
		if (fName.isEmpty()) {
			logConsole(Level.WARNING, "The message-file string is empty or not found. Defaulting to messages.txt");
			fName = "messages.txt";
		}

		if (fName.equals("messages.yml")) {
			logConsole(Level.WARNING,
					"The message file cannot be an existing message file! Defaulting to messages.txt");
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
		texts.clear();

		if (file == null) {
			loadFile();
		}

		String fileName = file.getName();
		if (fileName.endsWith(".yml")) {
			FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);

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
					if (line.startsWith("#")) {
						continue;
					}

					texts.add(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Util.sendInfo();
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
				FileConfiguration msgC = YamlConfiguration.loadConfiguration(file);
				msgC.set("messages", null);

				msgC.set("messages", texts);
				msgC.save(file);
			} else {
				FileWriter fw = new FileWriter(file, true);
				PrintWriter pw = new PrintWriter(fw);
				pw.println(msg);
				pw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Util.sendInfo();
		}
	}

	public void removeText(int lines) {
		if (!isFileExists()) {
			loadFile();
			return;
		}

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			StringBuffer sb = new StringBuffer();
			int linenumber = 1;
			String line;
			int numlines = 1;
			while ((line = br.readLine()) != null) {
				if (linenumber < lines || linenumber >= lines + numlines) {
					sb.append(line + "\n");
				}

				linenumber++;
			}

			br.close();

			if (lines + numlines > linenumber) {
				logConsole("End of file reached.");
				return;
			}

			String msg = sb.toString();
			texts.remove(msg);

			FileWriter fw = new FileWriter(file);
			fw.write(msg);
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
			Util.sendInfo();
		}
	}
}
