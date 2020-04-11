package hu.montlikadani.AutoMessager.bukkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author montlikadani
 *
 */
public class UpdateDownloader {

	public static String checkFromGithub(String sender) {
		FileConfiguration conf = AutoMessager.getInstance().getConf().getConfig();
		if (!conf.getBoolean("check-update")) {
			return "";
		}

		String msg = "";
		String versionString = "";
		String lineWithVersion = "";

		int newVersion = 0;
		int currentVersion = 0;

		try {
			URL githubUrl = new URL("https://raw.githubusercontent.com/montlikadani/AutoMessager/master/plugin.yml");
			BufferedReader br = new BufferedReader(new InputStreamReader(githubUrl.openStream()));
			String s;
			while ((s = br.readLine()) != null) {
				String line = s;
				if (line.toLowerCase().contains("version")) {
					lineWithVersion = line;
					break;
				}
			}

			versionString = lineWithVersion.split(": ")[1];
			String nVersion = versionString.replaceAll("[^0-9]", "");
			newVersion = Integer.parseInt(nVersion);

			String cVersion = AutoMessager.getInstance().getDescription().getVersion().replaceAll("[^0-9]", "");
			currentVersion = Integer.parseInt(cVersion);

			if (newVersion > currentVersion) {
				if ("player".equals(sender)) {
					msg = "&8&m&l--------------------------------------------------\n"
							+ "&aA new update for AutoMessager is available!&4 Version:&7 " + versionString
							+ (conf.getBoolean("download-updates") ? ""
									: "\n&6Download:&c &nhttps://www.spigotmc.org/resources/43875/")
							+ "\n&8&m&l--------------------------------------------------";
				} else if ("console".equals(sender)) {
					msg = "New version (" + versionString
							+ ") is available at https://www.spigotmc.org/resources/43875/";
				}

				msg = Util.colorMsg(msg);
			} else if ("console".equals(sender)) {
				return "You're running the latest version.";
			}

			if (newVersion <= currentVersion) {
				return msg;
			}

			if (!conf.getBoolean("download-updates")) {
				return msg;
			}

			Util.logConsole("Downloading new version of AutoMessager...");

			final String name = "AutoMessager-" + newVersion;
			final URL download = new URL(
					"https://github.com/montlikadani/AutoMessager/releases/latest/download/AutoMessager.jar");

			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						InputStream in = download.openStream();
						String per = File.separator;
						String updatesFolder = AutoMessager.getInstance().getFolder() + per + "releases";
						File temp = new File(updatesFolder);
						if (!temp.exists()) {
							temp.mkdir();
						}

						File jar = new File(updatesFolder + per + name + ".jar");
						if (jar.exists()) {
							in.close();
							cancel();
							return;
						}

						Files.copy(in, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);

						in.close();

						Util.logConsole("The new AutoMessager has been downloaded to releases folder.");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.runTaskLaterAsynchronously(AutoMessager.getInstance(), 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return msg;
	}
}
