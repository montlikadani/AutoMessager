package hu.montlikadani.AutoMessager.bukkit.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;

public abstract class UpdateDownloader {

	private static final File RELEASESFOLDER = new File(AutoMessager.getInstance().getFolder(), "releases");

	public static void checkFromGithub(org.bukkit.command.CommandSender sender) {
		FileConfiguration conf = AutoMessager.getInstance().getConfig();
		if (!conf.getBoolean("check-update", false)) {
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

				String versionString = lineWithVersion.split(": ")[1],
						nVersion = versionString.replaceAll("[^0-9]", ""),
						cVersion = AutoMessager.getInstance().getDescription().getVersion().replaceAll("[^0-9]", "");

				int newVersion = Integer.parseInt(nVersion);
				int currentVersion = Integer.parseInt(cVersion);

				if (newVersion <= currentVersion || currentVersion >= newVersion) {
					deleteDirectory();
					return false;
				}

				String msg = "";
				if (sender instanceof Player) {
					msg = Util.colorMsg("&aA new update for AutoMessager is available!&4 Version:&7 " + versionString
							+ (conf.getBoolean("download-updates") ? ""
									: "\n&6Download:&c &nhttps://www.spigotmc.org/resources/43875/"));
				} else {
					msg = "New version (" + versionString
							+ ") is available at https://www.spigotmc.org/resources/43875/";
				}

				sender.sendMessage(msg);

				if (!conf.getBoolean("download-updates", false)) {
					deleteDirectory();
					return false;
				}

				final String name = "AutoMessager-v" + newVersion;

				if (!RELEASESFOLDER.exists()) {
					RELEASESFOLDER.mkdir();
				}

				// Do not attempt to download the file again, when it is already downloaded
				final File jar = new File(RELEASESFOLDER, name + ".jar");
				if (jar.exists()) {
					return false;
				}

				Util.logConsole("Downloading new version of AutoMessager...");

				final URL download = new URL(
						"https://github.com/montlikadani/AutoMessager/releases/latest/download/AutoMessager.jar");

				InputStream in = download.openStream();
				try {
					Files.copy(in, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} finally {
					in.close();
				}

				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}).thenAccept(success -> {
			if (success) {
				Util.logConsole("The new AutoMessager has been downloaded to releases folder.");
			}
		});
	}

	private static void deleteDirectory() {
		if (!RELEASESFOLDER.exists()) {
			return;
		}

		for (File file : RELEASESFOLDER.listFiles()) {
			try {
				file.delete();
			} catch (SecurityException e) {
			}
		}

		try {
			RELEASESFOLDER.delete();
		} catch (SecurityException e) {
		}
	}
}