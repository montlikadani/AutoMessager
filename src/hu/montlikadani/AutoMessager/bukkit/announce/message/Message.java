package hu.montlikadani.AutoMessager.bukkit.announce.message;

import hu.montlikadani.AutoMessager.bukkit.announce.message.actionNameType.ActionNameCleaner;
import hu.montlikadani.AutoMessager.bukkit.announce.message.actionNameType.ActionNameCleaner.CleanedName;
import hu.montlikadani.AutoMessager.bukkit.commands.Commands;
import hu.montlikadani.AutoMessager.bukkit.utils.PluginUtils;
import hu.montlikadani.AutoMessager.bukkit.utils.Util;
import net.md_5.bungee.chat.ComponentSerializer;

import static hu.montlikadani.AutoMessager.bukkit.utils.Util.logConsole;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.announce.message.actionNameType.ActionName;

public final class Message implements ActionName {

	private final String text;
	private ActionNameType type;

	private final ActionNameCleaner actionNameCleaner = new ActionNameCleaner();

	public Message(String text) {
		this(text, ActionNameType.WITHOUT);
	}

	public Message(String text, ActionNameType type) {
		this.text = text == null ? "" : text;
		setType(type);
	}

	public AutoMessager getPlugin() {
		return AutoMessager.getInstance();
	}

	public void setTypeFromText() {
		if (text.startsWith("json:")) {
			setType(ActionNameType.JSON);
		} else if (text.startsWith("world:")) {
			setType(ActionNameType.WORLD);
		} else if (text.startsWith("player:")) {
			setType(ActionNameType.PLAYER);
		} else if (text.startsWith("group:")) {
			setType(ActionNameType.GROUP);
		} else if (text.startsWith("permission:")) {
			setType(ActionNameType.PERMISSION);
		} else if (text.contains("[time:")) {
			setType(ActionNameType.TIME);
		}
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public void setType(ActionNameType type) {
		if (type != null) {
			this.type = type;
		}
	}

	@Override
	public ActionNameType getType() {
		return type;
	}

	@Override
	public ActionNameCleaner getCleaner() {
		return actionNameCleaner;
	}

	public void logToConsole() {
		if (getPlugin().getConfig().getBoolean("broadcast-to-console")
				&& (type == ActionNameType.WITHOUT || type == ActionNameType.TIME)) {
			Bukkit.getConsoleSender().sendMessage(text);
		}
	}

	@Override
	public void sendTo(Player player) {
		if (text.isEmpty()) {
			return;
		}

		final FileConfiguration config = getPlugin().getConfig();

		if (!Commands.ENABLED.getOrDefault(player.getUniqueId(), true)
				|| (config.getBoolean("disable-messages-when-player-afk") && PluginUtils.isAfk(player))
				|| config.getStringList("disabled-worlds").contains(player.getWorld().getName())
				|| (getPlugin().getConf().isRestrictFileExists() && getPlugin().getConf().getRestrictConfig()
						.getStringList("restricted-players").contains(player.getName()))
				|| !PluginUtils.hasPermission(player, Perm.SEEMSG.getPerm())) {
			return;
		}

		String msg = text;

		msg = Util.replaceVariables(player, msg);

		if (type != ActionNameType.JSON) {
			msg = msg.replace("\\n", "\n");
		}

		CleanedName cleaned = actionNameCleaner.clean(msg, type);

		switch (type) {
		case TIME:
			player.sendMessage(msg = cleaned.getSecondaryResult());
			break;
		case JSON:
			sendJSON(player, msg = cleaned.<String>getResult());
			break;
		case WORLD:
			if (!player.getWorld().getName().equalsIgnoreCase(cleaned.getSecondaryResult())) {
				return;
			}

			msg = cleaned.<String>getResult();

			if (msg.contains("json:")) {
				msg = msg.replace("json:", "");

				for (Player wp : player.getWorld().getPlayers()) {
					sendJSON(wp, msg);
				}
			} else {
				for (Player wp : player.getWorld().getPlayers()) {
					wp.sendMessage(msg);
				}
			}

			break;
		case PLAYER:
			String playerName = cleaned.getSecondaryResult();
			if (!player.getName().equalsIgnoreCase(playerName)) {
				return;
			}

			Bukkit.getPlayer(playerName).sendMessage(msg = cleaned.<String>getResult());
			break;
		case GROUP:
			if (!getPlugin().isPluginEnabled("Vault")) {
				logConsole(Level.WARNING, "Vault plugin not found. Without the per-group messages not work.");
				return;
			}

			try {
				for (String group : getPlugin().getVaultPerm().getPlayerGroups(player)) {
					if (cleaned.getSecondaryResult().equalsIgnoreCase(group)) {
						player.sendMessage(msg = cleaned.<String>getResult());
						break;
					}
				}
			} catch (UnsupportedOperationException e) {
			}

			break;
		case PERMISSION:
			if (PluginUtils.hasPermission(player, cleaned.getSecondaryResult())) {
				player.sendMessage(msg = cleaned.<String>getResult());
			}

			break;
		default:
			player.sendMessage(msg);
			break;
		}

		for (String cmd : config.getStringList("run-commands.commands")) {
			if (!cmd.contains(":")) {
				continue;
			}

			String[] arg = cmd.split(": ");
			if (arg.length < 2) {
				continue;
			}

			String t = Util.setPlaceholders(player, arg[1]);
			if (arg[0].equalsIgnoreCase("console")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), t);
			} else if (arg[0].equalsIgnoreCase("player")) {
				player.performCommand(t);
			}
		}

		if (config.getBoolean("sound.enable")) { // TODO remove this check
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

				player.playSound(player.getLocation(), sound, 1f, 1f);
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

			player.playSound(player.getLocation(), sound, volume, pitch);
		}
	}

	private void sendJSON(Player p, String msg) {
		try {
			if (getPlugin().isSpigot()) {
				p.spigot().sendMessage(ComponentSerializer.parse(msg));
			} else { // CraftBukkit
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
		}
	}
}
