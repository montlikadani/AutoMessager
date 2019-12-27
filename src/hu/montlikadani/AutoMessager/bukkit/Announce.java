package hu.montlikadani.AutoMessager.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;

import hu.montlikadani.AutoMessager.Global;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import static hu.montlikadani.AutoMessager.bukkit.Util.logConsole;

public class Announce {

	private final AutoMessager plugin;

	private List<UUID> msgEnabled = new ArrayList<>();

	private boolean random = false;
	private int task = -1;
	private int messageCounter;
	private int lastMessage;
	private int lastRandom;
	private int warningCounter;

	public Announce(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public boolean isRandom() {
		return random;
	}

	public int getTask() {
		return task;
	}

	public void load() {
		// We need to start from -1, due to first line reading
		messageCounter = -1;
		warningCounter = 0;
		random = false;

		int cm = plugin.getFileHandler().getTexts().size();
		if (plugin.getConf().getConfig().getBoolean("random") && cm > 2) {
			random = true;
		}

		lastMessage = cm;
	}

	public void schedule() {
		final FileConfiguration config = plugin.getConf().getConfig();
		if (!config.getBoolean("enable-broadcast")) {
			return;
		}

		if (task == -1) {
			task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
				if (warningCounter <= 4) {
					if (plugin.getFileHandler().getTexts().size() < 1) {
						logConsole(Level.WARNING,
								"There is no message in '" + config.getString("message-file") + "' file!");

						warningCounter++;

						if (warningCounter == 5) {
							logConsole(Level.WARNING,
									"Will stop outputing warnings now. Please write a message to the '"
											+ config.getString("message-file") + "' file.");
						}

						return;
					}

					if (!plugin.checkOnlinePlayers()) {
						return;
					}

					for (Player p : Bukkit.getOnlinePlayers()) {
						if (Commands.enabled.containsKey(p.getUniqueId()) && !Commands.enabled.get(p.getUniqueId())) {
							continue;
						}

						if (random) {
							onRandom(p);
						} else {
							onInOrder(p);
						}
					}
				}
			}, plugin.getTimeC().getTime(), plugin.getTimeC().getTime());
		}
	}

	public void cancelTask() {
		Bukkit.getScheduler().cancelTask(task);
		task = -1;
	}

	private void onRandom(Player p) {
		int nm = getNextMessage();
		String message = plugin.getFileHandler().getTexts().get(nm);
		lastRandom = nm;
		send(p, message);
	}

	private void onInOrder(Player p) {
		int nm = getNextMessage();
		String message = plugin.getFileHandler().getTexts().get(nm);
		send(p, message);
	}

	int getNextMessage() {
		if (random) {
			int r = Global.getRandomInt(lastMessage - 1);
			while (r == lastRandom) {
				r = Global.getRandomInt(lastMessage - 1);
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

	private void send(Player p, final String message) {
		if (message.isEmpty()) {
			return;
		}

		String msg = message;

		FileConfiguration config = plugin.getConf().getConfig();

		if (config.getBoolean("disable-messages-when-player-afk", false)) {
			if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
				if (org.bukkit.plugin.java.JavaPlugin.getPlugin(Essentials.class).getUser(p).isAfk()) {
					if (!msgEnabled.contains(p.getUniqueId())) {
						msgEnabled.add(p.getUniqueId());
						return;
					}
				} else {
					if (msgEnabled.contains(p.getUniqueId())) {
						msgEnabled.remove(p.getUniqueId());
					}
				}
			} else {
				logConsole(Level.WARNING, "The Essentials plugin is not enabled or loaded, please enable.");
			}
		}

		if (config.getStringList("disabled-worlds").contains(p.getWorld().getName())) {
			return;
		}

		if (plugin.getConf().isBannedFileExists()
				&& plugin.getConf().getBpls().getStringList("banned-players").contains(p.getName()))
			return;

		msg = Util.replaceVariables(p, msg);
		msg = msg.replace("\\n", "\n");

		if ((Bukkit.getPluginManager().isPluginEnabled("PermissionsEx")
				&& PermissionsEx.getPermissionManager().has(p, Perm.SEEMSG.getPerm()))
				|| p.hasPermission(Perm.SEEMSG.getPerm())) {
			if (config.getBoolean("use-json-message") && message.startsWith("json:")) {
				msg = msg.replace("json:", "");

				try {
					if (plugin.isSpigot()) {
						BaseComponent[] bc = ComponentSerializer.parse(msg);
						p.spigot().sendMessage(bc);
					} else {
						String ver = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
								.split(",")[3];
						Object parsedMessage = Class
								.forName("net.minecraft.server." + ver + ".IChatBaseComponent$ChatSerializer")
								.getMethod("a", new Class[] { String.class }).invoke(null, new Object[] {
										org.bukkit.ChatColor.translateAlternateColorCodes("&".charAt(0), msg) });
						Object packetPlayOutChat = Class.forName("net.minecraft.server." + ver + ".PacketPlayOutChat")
								.getConstructor(new Class[] {
										Class.forName("net.minecraft.server." + ver + ".IChatBaseComponent") })
								.newInstance(new Object[] { parsedMessage });

						Object craftPlayer = Class.forName("org.bukkit.craftbukkit." + ver + ".entity.CraftPlayer")
								.cast(p);
						Object craftHandle = Class.forName("org.bukkit.craftbukkit." + ver + ".entity.CraftPlayer")
								.getMethod("getHandle").invoke(craftPlayer);
						Object playerConnection = Class.forName("net.minecraft.server." + ver + ".EntityPlayer")
								.getField("playerConnection").get(craftHandle);

						Class.forName("net.minecraft.server." + ver + ".PlayerConnection")
								.getMethod("sendPacket",
										new Class[] { Class.forName("net.minecraft.server." + ver + ".Packet") })
								.invoke(playerConnection, new Object[] { packetPlayOutChat });
					}
				} catch (Throwable e) {
					logConsole(Level.WARNING, "Invalid JSON format: " + msg);
					return;
				}
			}

			if (message.startsWith("world:")) {
				Worlds w = new Worlds(p.getWorld());

				String wName = w.getWorld().getName();
				String world = msg.split("_")[0].replace("world:", "").replace("_", "");

				if (!wName.equals(world)) {
					return;
				}

				msg = msg.replace("world:" + wName + "_", "");

				for (int x = 0; x < w.getWorld().getPlayers().size(); x++) {
					Bukkit.getWorld(wName).getPlayers().get(x).sendMessage(msg);
				}
			} else if (message.startsWith("player:")) {
				String player = msg.split("_")[0].replace("player:", "").replace("_", "");

				if (!p.getName().equals(player)) {
					return;
				}

				msg = msg.replace("player:" + Bukkit.getPlayer(player).getName() + "_", "");

				Bukkit.getPlayer(player).sendMessage(msg);
			} else if (message.startsWith("group:")) {
				if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
					logConsole(Level.WARNING, "The Vault plugin not found. Without the per-group messages not work.");
					return;
				}

				String gr = msg.split("_")[0].replace("group:", "").replace("_", "");

				msg = msg.replace("group:" + gr + "_", "");

				String group = plugin.getVaultPerm().getPrimaryGroup(p);
				if (group != null && group.equals(gr)) {
					p.sendMessage(msg);
				}
			} else if (message.startsWith("permission:")) {
				String perm = msg.split("_")[0].replace("permission:", "").replace("_", "");

				msg = msg.replace("permission:" + perm + "_", "");

				if (Bukkit.getPluginManager().isPluginEnabled("PermissionsEx")) {
					if (PermissionsEx.getPermissionManager().has(p, perm)) {
						PermissionsEx.getUser(p).getPlayer().sendMessage(msg);
					}
				} else if (p.hasPermission(perm)) {
					p.sendMessage(msg);
				}
			}

			if (!(message.startsWith("json:") || message.startsWith("world:") || message.startsWith("player:")
					|| message.startsWith("group:") || message.startsWith("permission:"))) {
				p.sendMessage(msg);
			}

			if (config.getStringList("run-commands.commands") != null
					&& !config.getStringList("run-commands.commands").isEmpty()) {
				for (String cmd : config.getStringList("run-commands.commands")) {
					String[] arg = cmd.split(": ");
					if (arg.length < 2) {
						logConsole(Level.WARNING, "The command " + cmd
								+ " invalid. Please follow the instructions which found in comments.");
						continue;
					}

					String t = arg[1];
					t = Util.setPlaceholders(p, t);

					if (arg[0].equals("console")) {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), t);
					} else if (arg[0].equals("player")) {
						p.performCommand(t);
					}
				}
			}

			if (config.getBoolean("sound.enable")) {
				try {
					String[] split = config.getString("sound.type").split(", ");
					p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(split[0]),
							(split.length == 2 ? Float.valueOf(split[1]) : 1f),
							(split.length == 3 ? Float.valueOf(split[2]) : 1f));
				} catch (Exception e) {
					logConsole(Level.WARNING, "Sound type is invalid: " + config.getString("sound.type"));
				}
			}
		}

		if (config.getBoolean("broadcast-to-console")) {
			if (!(message.startsWith("json:") && message.startsWith("world:") && message.startsWith("player:")
					&& message.startsWith("group:") && message.startsWith("permission:"))) {
				Bukkit.getConsoleSender().sendMessage(msg);
			}
		}
	}
}