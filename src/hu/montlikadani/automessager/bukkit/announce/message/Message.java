package hu.montlikadani.automessager.bukkit.announce.message;

import static hu.montlikadani.automessager.bukkit.utils.Util.logConsole;

import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.automessager.Global;
import hu.montlikadani.automessager.bukkit.AutoMessager;
import hu.montlikadani.automessager.bukkit.Perm;
import hu.montlikadani.automessager.bukkit.announce.message.actionNameType.ActionName;
import hu.montlikadani.automessager.bukkit.announce.message.actionNameType.ActionNameCleaner;
import hu.montlikadani.automessager.bukkit.announce.message.actionNameType.ActionNameCleaner.CleanedName;
import hu.montlikadani.automessager.bukkit.commands.Commands;
import hu.montlikadani.automessager.bukkit.config.ConfigConstants;
import hu.montlikadani.automessager.bukkit.config.ConfigConstants.ExecutableCommands;
import hu.montlikadani.automessager.bukkit.config.ConfigConstants.SoundProperties;
import hu.montlikadani.automessager.bukkit.config.ConfigConstants.ExecutableCommands.SenderType;
import hu.montlikadani.automessager.bukkit.utils.PluginUtils;
import hu.montlikadani.automessager.bukkit.utils.ServerVersion;
import hu.montlikadani.automessager.bukkit.utils.Util;

public final class Message implements ActionName {

	private final String text;
	private ActionNameType type;

	private final ActionNameCleaner actionNameCleaner = new ActionNameCleaner();
	private final AutoMessager plugin = JavaPlugin.getPlugin(AutoMessager.class);

	public Message(String text) {
		this(text, ActionNameType.WITHOUT);
	}

	public Message(String text, ActionNameType type) {
		this.text = text == null ? "" : text;
		setType(type);
	}

	public final void setTypeFromText() {
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
		} else if (text.startsWith("[time:")) {
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

	public final void logToConsole() {
		if (ConfigConstants.isBcToConsole() && (type == ActionNameType.WITHOUT || type == ActionNameType.TIME)) {
			Bukkit.getConsoleSender().sendMessage(text);
		}
	}

	@Override
	public void sendTo(Player player, boolean ignoreConditions) {
		if (text.isEmpty()) {
			return;
		}

		if (!ignoreConditions && (!Commands.ENABLED.getOrDefault(player.getUniqueId(), true)
				|| (ConfigConstants.isDisableMsgsInAfk() && PluginUtils.isAfk(player))
				|| ConfigConstants.getDisabledWorlds().contains(player.getWorld().getName())
				|| plugin.getConf().getRestrictConfig().getStringList("restricted-players").contains(player.getName())
				|| (ConfigConstants.isUsePermission() && !PluginUtils.hasPermission(player, Perm.SEEMSG.getPerm())))) {
			return;
		}

		String msg = text;
		msg = Util.replaceVariables(player, msg);

		if (type != ActionNameType.JSON) {
			if (msg.startsWith("center:")) {
				msg = StringUtils.replace(msg, "center:", "");

				int amount = 15; // Default value
				if (msg.contains("_")) {
					try {
						amount = Integer.parseInt(msg.split("_", 2)[0]);
					} catch (NumberFormatException ex) {
					}

					msg = StringUtils.replace(msg, amount + "_", "");
				}

				if (amount > 0) {
					msg = Global.centerText(msg, amount);
				}
			}

			msg = StringUtils.replace(msg, "\\n", "\n");
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

			java.util.List<Player> list = player.getWorld().getPlayers();
			if (msg.contains("json:")) {
				msg = StringUtils.replace(msg, "json:", "");

				if (list.size() == 1) {
					player.sendMessage(msg);
				} else {
					for (Player wp : list) {
						if (wp != player) {
							sendJSON(wp, msg);
						}
					}
				}
			} else if (list.size() == 1) { // Send the world message to the first player
				player.sendMessage(msg);
			} else {
				for (Player wp : list) {
					if (wp != player) { // If this condition not exist, it will send two messages at once
						wp.sendMessage(msg);
					}
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
			if (!plugin.isPluginEnabled("Vault")) {
				return;
			}

			try {
				for (String group : plugin.getVaultPerm().getPlayerGroups(player)) {
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

		if (ignoreConditions) {
			return;
		}

		if (!ConfigConstants.getExecutableCommands().isEmpty()) {
			Bukkit.getScheduler().callSyncMethod(plugin, () -> {
				for (ExecutableCommands cmd : ConfigConstants.getExecutableCommands()) {
					if (cmd.getType() == SenderType.CONSOLE) {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
								Util.setPlaceholders(player, cmd.getCommand()));
					} else if (cmd.getType() == SenderType.PLAYER) {
						player.performCommand(Util.setPlaceholders(player, cmd.getCommand()));
					}
				}

				return true;
			});
		}

		if (plugin.getConfig().getBoolean("sound.enable", true)) { // TODO remove this check
			SoundProperties sound = ConfigConstants.getSoundProperties();
			if (sound.getSound() != null) {
				player.playSound(player.getLocation(), sound.getSound(), sound.getVolume(), sound.getPitch());
			}
		}
	}

	private void sendJSON(Player p, String msg) {
		try {
			if (plugin.isSpigot() || plugin.isPaper()) {
				plugin.getComplement().sendMessage(p, msg);
			} else { // CraftBukkit
				String version = ServerVersion.getArrayVersion()[3], nms = "net.minecraft.server." + version + ".";

				Object parsedMessage = Class.forName(nms + "IChatBaseComponent$ChatSerializer")
						.getMethod("a", String.class)
						.invoke(null, org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));

				Object packetPlayOutChat;
				if (ServerVersion.isCurrentEqualOrHigher(ServerVersion.v1_16_R1)) {
					packetPlayOutChat = Class.forName(nms + "PacketPlayOutChat")
							.getConstructor(Class.forName(nms + "IChatBaseComponent"), UUID.class)
							.newInstance(parsedMessage, p.getUniqueId());
				} else {
					packetPlayOutChat = Class.forName(nms + "PacketPlayOutChat")
							.getConstructor(Class.forName(nms + "IChatBaseComponent")).newInstance(parsedMessage);
				}

				Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
				Object craftHandle = craftPlayer.getMethod("getHandle").invoke(craftPlayer.cast(p));
				Object playerConnection = Class.forName(nms + "EntityPlayer").getField("playerConnection")
						.get(craftHandle);

				Class.forName(nms + "PlayerConnection").getMethod("sendPacket", Class.forName(nms + "Packet"))
						.invoke(playerConnection, packetPlayOutChat);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logConsole(Level.WARNING, "Invalid JSON format: " + msg);
		}
	}
}
