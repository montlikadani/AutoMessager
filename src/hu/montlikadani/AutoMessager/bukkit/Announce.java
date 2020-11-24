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
import org.bukkit.scheduler.BukkitTask;

import hu.montlikadani.AutoMessager.Global;
import hu.montlikadani.AutoMessager.bukkit.commands.Commands;
import net.md_5.bungee.chat.ComponentSerializer;

public class Announce {

	private final AutoMessager plugin;

	private boolean random = false;
	private int messageCounter, lastMessage, lastRandom;
	private BukkitTask task;

	private final List<ScheduledExecutorService> schedulers = new ArrayList<>();

	public Announce(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public boolean isRandom() {
		return random;
	}

	public BukkitTask getTask() {
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

			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);
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

			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);
			scheduler.scheduleAtFixedRate(this::prepare, Util.calcNextDelay(h, min, sec), TimeUnit.DAYS.toSeconds(1),
					TimeUnit.SECONDS);
			schedulers.add(scheduler);
			return;
		}

		if (task == null) {
			task = Bukkit.getScheduler().runTaskTimer(plugin, this::prepare, t, t);
		}
	}

	public void cancelTask() {
		schedulers.forEach(ScheduledExecutorService::shutdown);
		schedulers.clear();

		if (task != null) {
			task.cancel();
			task = null;
		}
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
			if (!Commands.ENABLED.getOrDefault(p.getUniqueId(), true)
					|| (config.getBoolean("disable-messages-when-player-afk") && PluginUtils.isAfk(p))
					|| config.getStringList("disabled-worlds").contains(p.getWorld().getName())
					|| (plugin.getConf().isRestrictFileExists() && plugin.getConf().getRestrictConfig()
							.getStringList("restricted-players").contains(p.getName()))
					|| !PluginUtils.hasPermission(p, Perm.SEEMSG.getPerm())) {
				continue;
			}

			String msg = message;
			msg = Util.replaceVariables(p, msg);

			if (!message.startsWith("json:")) {
				msg = msg.replace("\\n", "\n");
			}

			if (message.startsWith("json:")) {
				msg = msg.replace("json:", "");

				if (!sendJSON(p, msg)) {
					return;
				}
			} else if (message.startsWith("world:")) {
				String wName = p.getWorld().getName();
				String world = msg.split("_")[0].replaceAll("world:|_", "");

				if (!wName.equalsIgnoreCase(world)) {
					continue;
				}

				msg = msg.replace("world:" + wName + "_", "");

				if (msg.contains("json:")) {
					msg = msg.replace("json:", "");

					for (Player wp : Bukkit.getWorld(wName).getPlayers()) {
						if (wp != p) {
							sendJSON(wp, msg);
						}
					}
				} else {
					for (Player wp : Bukkit.getWorld(wName).getPlayers()) {
						if (wp != p) {
							wp.sendMessage(msg);
						}
					}
				}
			} else if (message.startsWith("player:")) {
				String player = msg.split("_")[0].replaceAll("player:|_", "");

				if (!p.getName().equalsIgnoreCase(player)) {
					continue;
				}

				msg = msg.replace("player:" + player + "_", "");

				Bukkit.getPlayer(player).sendMessage(msg);
			} else if (message.startsWith("group:")) {
				if (!plugin.isPluginEnabled("Vault")) {
					logConsole(Level.WARNING, "The Vault plugin not found. Without the per-group messages not work.");
					return;
				}

				String gr = msg.split("_")[0].replaceAll("group:|_", "");
				try {
					for (String group : plugin.getVaultPerm().getPlayerGroups(p)) {
						if (gr.equalsIgnoreCase(group)) {
							msg = msg.replace("group:" + gr + "_", "");
							p.sendMessage(msg);
							break;
						}
					}
				} catch (UnsupportedOperationException e) {
				}
			} else if (message.startsWith("permission:")) {
				String perm = msg.split("_")[0].replaceAll("permission:|_", "");

				msg = msg.replace("permission:" + perm + "_", "");

				if (PluginUtils.hasPermission(p, perm)) {
					p.sendMessage(msg);
				}
			} else {
				p.sendMessage(msg);
			}

			for (String cmd : config.getStringList("run-commands.commands")) {
				if (!cmd.contains(":")) {
					continue;
				}

				String[] arg = cmd.split(": ");
				if (arg.length < 2) {
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

			if (config.getBoolean("sound.enable")) {
				String type = config.getString("sound.type", "");
				if (type.isEmpty()) {
					return;
				}

				if (!type.contains(",")) {
					Sound sound = null;
					try {
						sound = Sound.valueOf(type.toUpperCase());
					} catch (IllegalArgumentException e) {
						logConsole(Level.WARNING, "Sound by this name not found: " + type);
						return;
					}

					p.playSound(p.getLocation(), sound, 1f, 1f);
					return;
				}

				String[] split = type.split(", ");

				Sound sound = null;
				try {
					sound = Sound.valueOf(split[0].toUpperCase());
				} catch (IllegalArgumentException e) {
					logConsole(Level.WARNING, "Sound by this name not found: " + split[0]);
					return;
				}

				float volume = split.length > 1 ? Float.parseFloat(split[1]) : 1f;
				float pitch = split.length > 2 ? Float.parseFloat(split[2]) : 1f;

				p.playSound(p.getLocation(), sound, volume, pitch);
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
				String ver = Bukkit.getServer().getClass().getPackage().getName().replace('.', ',').split(",")[3],
						nms = "net.minecraft.server." + ver + ".", obc = "org.bukkit.craftbukkit." + ver + ".";

				Object parsedMessage = Class.forName(nms + "IChatBaseComponent$ChatSerializer")
						.getMethod("a", String.class)
						.invoke(null, org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));

				Object packetPlayOutChat;
				if (ver.contains("16")) {
					packetPlayOutChat = Class.forName(nms + "PacketPlayOutChat")
							.getConstructor(Class.forName(nms + "IChatBaseComponent"), UUID.class)
							.newInstance(parsedMessage, p.getUniqueId());
				} else {
					packetPlayOutChat = Class.forName(nms + "PacketPlayOutChat")
							.getConstructor(Class.forName(nms + "IChatBaseComponent")).newInstance(parsedMessage);
				}

				Object craftPlayer = Class.forName(obc + "entity.CraftPlayer").cast(p);
				Object craftHandle = Class.forName(obc + "entity.CraftPlayer").getMethod("getHandle")
						.invoke(craftPlayer);
				Object playerConnection = Class.forName(nms + "EntityPlayer").getField("playerConnection")
						.get(craftHandle);

				Class.forName(nms + "PlayerConnection").getMethod("sendPacket", Class.forName(nms + "Packet"))
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