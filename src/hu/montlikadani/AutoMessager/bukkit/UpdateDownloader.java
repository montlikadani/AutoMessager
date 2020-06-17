package hu.montlikadani.AutoMessager.bukkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author montlikadani
 *
 */
public class UpdateDownloader {

	public static void checkFromGithub(org.bukkit.command.CommandSender sender) {
		FileConfiguration conf = AutoMessager.getInstance().getConf().getConfig();
		if (!conf.getBoolean("check-update", false)) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				String versionString = "",
						lineWithVersion = "";
				int newVersion = 0,
						currentVersion = 0;

				try {
					URL githubUrl = new URL(
							"https://raw.githubusercontent.com/montlikadani/AutoMessager/master/plugin.yml");
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

					if (newVersion <= currentVersion || currentVersion >= newVersion) {
						return;
					}

					String msg = "";
					if (sender instanceof Player) {
						msg = Util.colorMsg("&8&m&l--------------------------------------------------\n"
								+ "&aA new update for AutoMessager is available!&4 Version:&7 " + versionString
								+ (conf.getBoolean("download-updates") ? ""
										: "\n&6Download:&c &nhttps://www.spigotmc.org/resources/43875/")
								+ "\n&8&m&l--------------------------------------------------");
					} else {
						msg = "New version (" + versionString
								+ ") is available at https://www.spigotmc.org/resources/43875/";
					}

					sender.sendMessage(msg);

					if (!conf.getBoolean("download-updates", false)) {
						return;
					}

					final String name = "AutoMessager-v" + newVersion;

					String updatesFolder = AutoMessager.getInstance().getFolder() + File.separator + "releases";
					File temp = new File(updatesFolder);
					if (!temp.exists()) {
						temp.mkdir();
					}

					// Do not attempt to download the file again, when it is already downloaded
					final File jar = new File(updatesFolder + File.separator + name + ".jar");
					if (jar.exists()) {
						return;
					}

					Util.logConsole("Downloading new version of AutoMessager...");

					final URL download = new URL(
							"https://github.com/montlikadani/AutoMessager/releases/latest/download/AutoMessager.jar");

					InputStream in = download.openStream();
					Files.copy(in, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);

					in.close();

					Util.logConsole("The new AutoMessager has been downloaded to releases folder.");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.runTaskLaterAsynchronously(AutoMessager.getInstance(), 1);
	}
}
