package hu.montlikadani.AutoMessager.bukkit;

import static hu.montlikadani.AutoMessager.bukkit.Util.logConsole;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;

import hu.montlikadani.AutoMessager.Global;
import hu.montlikadani.AutoMessager.bukkit.commands.Commands;
import net.md_5.bungee.chat.ComponentSerializer;

public class Announce {

	private final AutoMessager plugin;

	private boolean random = false;
	private int task = -1, messageCounter, lastMessage, lastRandom;

	private List<ScheduledExecutorService> schedulers = new ArrayList<>();

	public Announce(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public boolean isRandom() {
		return random;
	}

	public int getTask() {
		return task;
	}

	public List<ScheduledExecutorService> getSchedulers() {
		return new ArrayList<>(schedulers);
	}

	public void load() {
		// We need to start from -1, due to first line reading
		messageCounter = -1;
		random = false;

		int cm = plugin.getFileHandler().getTexts().size();
		if (plugin.getConf().getConfig().getBoolean("random") && cm > 2) {
			random = true;
		}

		lastMessage = cm;
	}

	public void schedule() {
		if (!plugin.getConf().getConfig().getBoolean("enable-broadcast")) {
			return;
		}

		for (final String o : plugin.getFileHandler().getTexts()) {
			if (!o.startsWith("[time:"))
				continue;

			String customTime = o.split("]")[0].replace("[time:", "");
			if (!customTime.contains(":")) {
				continue;
			}

			String[] times = customTime.split(":");
			if (times.length != 3) {
				continue;
			}

			int h = Integer.parseInt(times[0]),
					min = Integer.parseInt(times[1]),
					sec = Integer.parseInt(times[2]);

			final String finalMessage = o.replace("[time:" + times[0] + ":" + times[1] + ":" + times[2] + "]", "");

			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(() -> {
				if (plugin.checkOnlinePlayers()) {
					send(finalMessage);
				}
			}, Util.calcNextDelay(h, min, sec), TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);

			schedulers.add(scheduler);
		}

		final Time time = plugin.getTimeC();
		final long t = time.countTimer();

		if (time.isGiven()) {
			if (!time.getTime().contains(":")) {
				return;
			}

			String[] times = time.getTime().split(":");
			if (times.length != 3) {
				return;
			}

			int h = Integer.parseInt(times[0]),
					min = Integer.parseInt(times[1]),
					sec = Integer.parseInt(times[2]);

			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(this::prepare, Util.calcNextDelay(h, min, sec), TimeUnit.DAYS.toSeconds(1),
					TimeUnit.SECONDS);
			schedulers.add(scheduler);
			return;
		}

		if (task == -1) {
			task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::prepare, t, t);
		}
	}

	public void cancelTask() {
		schedulers.forEach(ScheduledExecutorService::shutdown);
		schedulers.clear();

		Bukkit.getScheduler().cancelTask(task);
		task = -1;
	}

	private void prepare() {
		if (!plugin.checkOnlinePlayers()) {
			return;
		}

		int size = plugin.getFileHandler().getTexts().size();
		if (lastMessage != size) {
			lastMessage = size;
		}

		int nm = getNextMessage();
		String message = plugin.getFileHandler().getTexts().get(nm);

		// skip
		if (message.startsWith("[time:")) {
			prepare();
			return;
		}

		if (random) {
			lastRandom = nm;
		}

		send(message);
	}

	int getNextMessage() {
		if (random) {
			int r = Global.getRandomInt(0, lastMessage - 1);
			while (r == lastRandom) {
				r = Global.getRandomInt(0, lastMessage - 1);
			}

			return r;
		}

		int nm = (messageCounter + 1);
		if (nm >= lastMessage) {
			messageCounter = 0;
			return 0;
		}

		++messageCounter;
		return nm;
	}

	private void send(final String message) {
		if (message.isEmpty()) {
			return;
		}

		final FileConfiguration config = plugin.getConf().getConfig();

		for (Player p : Bukkit.getOnlinePlayers()) {
			if (Commands.ENABLED.containsKey(p.getUniqueId()) && !Commands.ENABLED.get(p.getUniqueId())) {
				continue;
			}

			if (config.getBoolean("disable-messages-when-player-afk", false)) {
				if (plugin.isPluginEnabled("Essentials")) {
					if (org.bukkit.plugin.java.JavaPlugin.getPlugin(Essentials.class).getUser(p).isAfk()) {
						continue;
					}
				} else {
					logConsole(Level.WARNING, "The Essentials plugin is not enabled or loaded, please enable.");
				}
			}

			if (config.getStringList("disabled-worlds").contains(p.getWorld().getName())
					|| (plugin.getConf().isRestrictFileExists() && plugin.getConf().getRestrictConfig()
							.getStringList("restricted-players").contains(p.getName()))) {
				continue;
			}

			String msg = message;
			msg = Util.replaceVariables(p, msg);
			if (!message.startsWith("json:")) {
				msg = msg.replace("\\n", "\n");
			}

			if (PluginUtils.hasPermission(p, Perm.SEEMSG.getPerm())) {
				if (message.startsWith("json:")) {
					msg = msg.replace("json:", "");

					if (!sendJSON(p, msg)) {
						return;
					}
				} else if (message.startsWith("world:")) {
					String wName = p.getWorld().getName();
					String world = msg.split("_")[0].replace("world:", "").replace("_", "");

					if (!wName.equals(world)) {
						continue;
					}

					msg = msg.replace("world:" + wName + "_", "");

					if (config.getBoolean("use-json-message") && msg.contains("json:")) {
						msg = msg.replace("json:", "");

						for (Player wp : Bukkit.getWorld(wName).getPlayers()) {
							if (!sendJSON(wp, msg)) {
								continue;
							}
						}
					} else {
						for (Player wp : Bukkit.getWorld(wName).getPlayers()) {
							wp.sendMessage(msg);
						}
					}
				} else if (message.startsWith("player:")) {
					String player = msg.split("_")[0].replace("player:", "").replace("_", "");

					if (!p.getName().equals(player)) {
						continue;
					}

					msg = msg.replace("player:" + Bukkit.getPlayer(player).getName() + "_", "");

					Bukkit.getPlayer(player).sendMessage(msg);
				} else if (message.startsWith("group:")) {
					if (!plugin.isPluginEnabled("Vault")) {
						logConsole(Level.WARNING,
								"The Vault plugin not found. Without the per-group messages not work.");
						return;
					}

					String gr = msg.split("_")[0].replace("group:", "").replace("_", "");
					for (String group : plugin.getVaultPerm().getPlayerGroups(p)) {
						if (!gr.equals(group)) {
							continue;
						}

						msg = msg.replace("group:" + gr + "_", "");
						p.sendMessage(msg);
						break;
					}
				} else if (message.startsWith("permission:")) {
					String perm = msg.split("_")[0].replace("permission:", "").replace("_", "");

					msg = msg.replace("permission:" + perm + "_", "");

					if (PluginUtils.hasPermission(p, perm)) {
						p.sendMessage(msg);
					}
				} else {
					p.sendMessage(msg);
				}

				if (!config.getStringList("run-commands.commands").isEmpty()) {
					for (String cmd : config.getStringList("run-commands.commands")) {
						if (!cmd.contains(":")) {
							continue;
						}

						String[] arg = cmd.split(": ");
						if (arg.length < 2) {
							logConsole(Level.WARNING, "The command " + cmd
									+ " invalid. Please follow the instructions which found in comments.");
							continue;
						}

						String t = arg[1];
						t = Util.setPlaceholders(p, t);

						if (arg[0].equalsIgnoreCase("console")) {
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), t);
						} else if (arg[0].equalsIgnoreCase("player")) {
							p.performCommand(t);
						}
					}
				}

				s: if (config.getBoolean("sound.enable")) {
					String type = config.getString("sound.type", "");
					if (type.isEmpty()) {
						break s;
					}

					if (!type.contains(",")) {
						Sound sound = null;
						try {
							sound = Sound.valueOf(type.toUpperCase());
						} catch (IllegalArgumentException e) {
							logConsole(Level.WARNING, "Sound by this name not found: " + type);
							break s;
						}

						p.playSound(p.getLocation(), sound, 1f, 1f);
						break s;
					}

					String[] split = type.split(", ");

					Sound sound = null;
					try {
						sound = Sound.valueOf(split[0].toUpperCase());
					} catch (IllegalArgumentException e) {
						logConsole(Level.WARNING, "Sound by this name not found: " + split[0]);
						break s;
					}

					float volume = split.length > 1 ? Float.parseFloat(split[1]) : 1f;
					float pitch = split.length > 2 ? Float.parseFloat(split[2]) : 1f;

					p.playSound(p.getLocation(), sound, volume, pitch);
				}
			}
		}

		if (config.getBoolean("broadcast-to-console")
				&& !(message.startsWith("json:") && message.startsWith("world:") && message.startsWith("player:")
						&& message.startsWith("group:") && message.startsWith("permission:"))) {
			Bukkit.getConsoleSender().sendMessage(message);
		}
	}

	private boolean sendJSON(Player p, String msg) {
		try {
			if (plugin.isSpigot()) {
				p.spigot().sendMessage(ComponentSerializer.parse(msg));
			} else {
				String ver = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
				Object parsedMessage = Class
						.forName("net.minecraft.server." + ver + ".IChatBaseComponent$ChatSerializer")
						.getMethod("a", String.class)
						.invoke(null, org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));

				Object packetPlayOutChat;
				if (ver.contains("16")) {
					packetPlayOutChat = Class.forName("net.minecraft.server." + ver + ".PacketPlayOutChat")
							.getConstructor(Class.forName("net.minecraft.server." + ver + ".IChatBaseComponent"),
									UUID.class)
							.newInstance(parsedMessage, p.getUniqueId());
				} else {
					packetPlayOutChat = Class.forName("net.minecraft.server." + ver + ".PacketPlayOutChat")
							.getConstructor(Class.forName("net.minecraft.server." + ver + ".IChatBaseComponent"))
							.newInstance(parsedMessage);
				}

				Object craftPlayer = Class.forName("org.bukkit.craftbukkit." + ver + ".entity.CraftPlayer").cast(p);
				Object craftHandle = Class.forName("org.bukkit.craftbukkit." + ver + ".entity.CraftPlayer")
						.getMethod("getHandle").invoke(craftPlayer);
				Object playerConnection = Class.forName("net.minecraft.server." + ver + ".EntityPlayer")
						.getField("playerConnection").get(craftHandle);

				Class.forName("net.minecraft.server." + ver + ".PlayerConnection")
						.getMethod("sendPacket", Class.forName("net.minecraft.server." + ver + ".Packet"))
						.invoke(playerConnection, packetPlayOutChat);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			logConsole(Level.WARNING, "Invalid JSON format: " + msg);
			return false;
		}

		return true;
	}
}