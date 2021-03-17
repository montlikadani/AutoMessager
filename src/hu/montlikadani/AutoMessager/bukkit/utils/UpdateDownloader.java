package hu.montlikadani.automessager.bukkit.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.automessager.bukkit.AutoMessager;
import hu.montlikadani.automessager.bukkit.config.ConfigConstants;

public abstract class UpdateDownloader {

	private static final AutoMessager PLUGIN = JavaPlugin.getPlugin(AutoMessager.class);

	private static File releasesFolder;

	public static void checkFromGithub(org.bukkit.command.CommandSender sender) {
		releasesFolder = new File(PLUGIN.getFolder(), "releases");

		if (!ConfigConstants.isCheckUpdate()) {
			deleteDirectory();
			return;
		}

		CompletableFuture.supplyAsync(() -> {
			try {
				URL githubUrl = new URL(
						"https://raw.githubusercontent.com/montlikadani/AutoMessager/master/plugin.yml");
				BufferedReader br = new BufferedReader(new InputStreamReader(githubUrl.openStream()));

				String s, lineWithVersion = "";

				while ((s = br.readLine()) != null) {
					String line = s;
					if (line.toLowerCase().contains("version")) {
						lineWithVersion = line;
						break;
					}
				}

				String versionString = lineWithVersion.split(": ", 2)[1],
						nVersion = versionString.replaceAll("[^0-9]", ""),
						cVersion = PLUGIN.getDescription().getVersion().replaceAll("[^0-9]", "");

				int newVersion = Integer.parseInt(nVersion);
				int currentVersion = Integer.parseInt(cVersion);

				if (newVersion <= currentVersion || currentVersion >= newVersion) {
					deleteDirectory();
					return false;
				}

				String msg = "";
				if (sender instanceof Player) {
					msg = Util.colorMsg("&aA new update for AutoMessager is available!&4 ServerVersion:&7 "
							+ versionString + (ConfigConstants.isDownloadUpdates() ? ""
									: "\n&6Download:&c &nhttps://www.spigotmc.org/resources/43875/"));
				} else {
					msg = "New version (" + versionString
							+ ") is available at https://www.spigotmc.org/resources/43875/";
				}

				sender.sendMessage(msg);

				if (!ConfigConstants.isDownloadUpdates()) {
					deleteDirectory();
					return false;
				}

				if (!releasesFolder.exists()) {
					releasesFolder.mkdir();
				}

				// Do not attempt to download the file again, when it is already downloaded
				final File jar = new File(releasesFolder, "AutoMessager-v" + newVersion + ".jar");
				if (jar.exists()) {
					return false;
				}

				Util.logConsole("Downloading new version of AutoMessager...");

				InputStream in = new URL(
						"https://github.com/montlikadani/AutoMessager/releases/latest/download/AutoMessager.jar")
								.openStream();
				try {
					Files.copy(in, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} finally {
					in.close();
				}

				return true;
			} catch (FileNotFoundException f) {
			} catch (Exception e) {
				e.printStackTrace();
			}

			return false;
		}).thenAccept(success -> {
			if (success) {
				Util.logConsole("The new AutoMessager has been downloaded to releases folder.");
			}
		});
	}

	private static void deleteDirectory() {
		if (!releasesFolder.exists()) {
			return;
		}

		for (File file : releasesFolder.listFiles()) {
			try {
				file.delete();
			} catch (SecurityException e) {
			}
		}

		try {
			releasesFolder.delete();
		} catch (SecurityException e) {
		}
	}
}
