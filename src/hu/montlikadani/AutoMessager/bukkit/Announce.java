package hu.montlikadani.AutoMessager.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;

import hu.montlikadani.AutoMessager.bukkit.Permissions.Perm;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class Announce {

	private final AutoMessager plugin;

	private List<UUID> msgEnabled = new ArrayList<>();
	private int task = -1;

	private boolean isRandom;

	private int messageCounter;
	private int lastMessage;
	private int lastRandom;
	private int warningCounter;

	public Announce(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public void load() {
		// We need to start from -1, due to first line reading
		messageCounter = -1;
		warningCounter = 0;
		isRandom = false;

		int cm = plugin.getMessages().size();
		if (plugin.getConfig().getBoolean("random") && cm > 2) {
			isRandom = true;
		}

		lastMessage = cm;
	}

	public void schedule(final Player p) {
		if (task == -1) {
			task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
				if (warningCounter <= 4) {
					if (Commands.enabled != null
							&& (Commands.enabled.containsKey(p.getUniqueId()) && !Commands.enabled.get(p.getUniqueId()))) {
						return;
					}

					if (plugin.getMessages().size() < 1) {
						plugin.logConsole(Level.WARNING,
								"There is no message in '" + plugin.getConfig().getString("message-file") + "' file!");

						warningCounter++;

						if (warningCounter == 5) {
							plugin.logConsole(Level.WARNING,
									"Will stop outputing warnings now. Please write a message to the '"
											+ plugin.getConfig().getString("message-file") + "' file.");
						}
					} else {
						if (isRandom) {
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

	public int getTask() {
		return task;
	}

	private void onRandom(Player p) {
		int nm = getNextMessage();
		String message = plugin.getMessages().get(nm);
		lastRandom = nm;
		send(p, message);
	}

	private void onInOrder(Player p) {
		int nm = getNextMessage();
		String message = plugin.getMessages().get(nm);
		send(p, message);
	}

	int getNextMessage() {
		if (isRandom) {
			int r = plugin.getRandomInt(lastMessage - 1);
			while (r == lastRandom) {
				r = plugin.getRandomInt(lastMessage - 1);
			}

			return r;
		}

		int nm = (messageCounter + 1);
		if (nm == lastMessage) {
			messageCounter = 0;
			return 0;
		}

		messageCounter++;
		return nm;
	}

	private void send(Player p, String message) {
		String msg = message;

		if (msg.isEmpty()) {
			return;
		}

		if (plugin.getConfig().getBoolean("disable-messages-when-player-afk")) {
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
				plugin.logConsole(Level.WARNING, "The Essentials plugin is not enabled or loaded, please enable.");
			}
		}

		if (plugin.getConfig().getStringList("disabled-worlds").contains(p.getWorld().getName())) {
			return;
		}

		if (plugin.bpls_file != null && plugin.bpls_file.exists()
				&& plugin.bpls.getStringList("banned-players").contains(p.getName()))
			return;

		msg = plugin.replaceVariables(p, msg);

		if ((Bukkit.getPluginManager().isPluginEnabled("PermissionsEx")
				&& PermissionsEx.getPermissionManager().has(p, Perm.SEEMSG.getPerm()))
				|| p.hasPermission(Perm.SEEMSG.getPerm())) {
			if (plugin.getConfig().getBoolean("use-json-message") && message.startsWith("json:")) {
				msg = msg.replace("json:", "");

				try {
					try {
						Class.forName("org.spigotmc.SpigotConfig");

						BaseComponent[] bc = ComponentSerializer.parse(msg);
						p.spigot().sendMessage(bc);
					} catch (ClassNotFoundException e) {
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
								.getMethod("getHandle", new Class[0]).invoke(craftPlayer, new Object[0]);
						Object playerConnection = Class.forName("net.minecraft.server." + ver + ".EntityPlayer")
								.getField("playerConnection").get(craftHandle);

						Class.forName("net.minecraft.server." + ver + ".PlayerConnection")
								.getMethod("sendPacket",
										new Class[] { Class.forName("net.minecraft.server." + ver + ".Packet") })
								.invoke(playerConnection, new Object[] { packetPlayOutChat });
					}
				} catch (Throwable e) {
					plugin.logConsole(Level.WARNING, "Invalid JSON format: " + msg);
					return;
				}
			}

			if (message.startsWith("world:")) {
				Worlds w = new Worlds(p.getWorld());

				String wName = w.getWorld().getName();
				String world = msg.split("_")[0].replace("world:", "").replace("_", "");

				if (wName.equals(world)) {
					msg = msg.replace("world:" + wName + "_", "");

					for (int x = 0; x < w.getWorld().getPlayers().size(); x++) {
						Bukkit.getWorld(wName).getPlayers().get(x).sendMessage(msg);
					}
				} else
					return;
			} else if (message.startsWith("player:")) {
				String player = msg.split("_")[0].replace("player:", "").replace("_", "");

				if (p.getName().equals(player)) {
					msg = msg.replace("player:" + Bukkit.getPlayer(player).getName() + "_", "");

					Bukkit.getPlayer(player).sendMessage(msg);
				} else
					return;
			} else if (message.startsWith("group:")) {
				if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
					String gr = msg.split("_")[0].replace("group:", "").replace("_", "");

					msg = msg.replace("group:" + gr + "_", "");

					String group = plugin.getVaultPerm().getPrimaryGroup(p);
					if (group.equals(gr)) {
						p.sendMessage(msg);
					}
				} else {
					plugin.logConsole(Level.WARNING,
							"The Vault plugin not found. Without the per-group messages not work.");
					return;
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

			if (plugin.getConfig().getStringList("run-commands.commands") != null
					&& !plugin.getConfig().getStringList("run-commands.commands").isEmpty()) {
				for (String cmd : plugin.getConfig().getStringList("run-commands.commands")) {
					String[] arg = cmd.split(": ");
					if (arg.length < 2) {
						plugin.logConsole(Level.WARNING,
								"The command " + cmd + " invalid. Please follow the instructions which found in comments.");
						continue;
					}

					String t = arg[1];
					t = plugin.setPlaceholders(p, t);

					if (arg[0].equals("console")) {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), t);
					} else if (arg[0].equals("player")) {
						p.performCommand(t);
					}
				}
			}

			if (plugin.getConfig().getBoolean("sound.enable")) {
				try {
					String[] split = plugin.getConfig().getString("sound.type").split(", ");
					p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(split[0]),
							(split.length == 2 ? Float.valueOf(split[1]) : 1f),
							(split.length == 3 ? Float.valueOf(split[2]) : 1f));
				} catch (Exception e) {
					plugin.logConsole(Level.WARNING,
							"Sound type is invalid: " + plugin.getConfig().getString("sound.type"));
				}
			}
		}

		if (plugin.getConfig().getBoolean("broadcast-to-console")) {
			if (!(message.startsWith("json:") && message.startsWith("world:") && message.startsWith("player:")
					&& message.startsWith("group:") && message.startsWith("permission:"))) {
				Bukkit.getConsoleSender().sendMessage(msg);
			}
		}
	}
}