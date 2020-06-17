package hu.montlikadani.AutoMessager.bukkit.commands;

import static hu.montlikadani.AutoMessager.bukkit.Util.colorMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.getMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.sendMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;

public class Commands implements CommandExecutor, TabCompleter {

	private AutoMessager plugin;

	public Commands(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public static final Map<UUID, Boolean> ENABLED = new HashMap<>();

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (args.length == 0) {
			sendMsg(sender, colorMsg("&e&l[&3&lAuto&a&lMessager&b&l Info&e&l]"));
			sendMsg(sender, colorMsg("&5Version:&a " + plugin.getDescription().getVersion()));
			sendMsg(sender, colorMsg("&5Author, created by:&a montlikadani"));
			sendMsg(sender, colorMsg("&5Commands:&8 /&7" + commandLabel + "&a help"));
			sendMsg(sender, colorMsg(
					"&4If you find a bug, send issue here:&e &nhttps://github.com/montlikadani/AutoMessager/issues"));
			return true;
		}

		if (args[0].equalsIgnoreCase("help")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.HELP.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.HELP.getPerm()));
				return false;
			}

			FileConfiguration messages = plugin.getConf().getMessages();
			if (sender instanceof Player) {
				if (args.length == 1) {
					messages.getStringList("chat-messages.1")
							.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", commandLabel))));
				} else if (args.length == 2) {
					if (args[1].equals("2")) {
						messages.getStringList("chat-messages.2")
								.forEach(msg -> sender.sendMessage(colorMsg(msg.replace("%command%", commandLabel))));
					} else if (args[1].equals("3")) {
						messages.getStringList("chat-messages.3")
								.forEach(msg -> sender.sendMessage(colorMsg(msg.replace("%command%", commandLabel))));
					}
				}
			} else {
				messages.getStringList("chat-messages.1")
						.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", commandLabel))));

				messages.getStringList("chat-messages.2")
						.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", commandLabel))));

				messages.getStringList("chat-messages.3")
						.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", commandLabel))));
			}

			return true;
		}

		String path = "hu.montlikadani.AutoMessager.bukkit.commands.list";
		ICommand command = null;
		try {
			command = (ICommand) AutoMessager.class.getClassLoader().loadClass(path + "." + args[0].toLowerCase())
					.newInstance();
		} catch (ClassNotFoundException e) {
			sendMsg(sender, getMsg("unknown-sub-command", "%subcmd%", args[0]));
		} catch (IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}

		if (command != null) {
			command.run(plugin, sender, cmd, commandLabel, args);
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		List<String> completionList = new ArrayList<>();
		List<String> cmds = new ArrayList<>();
		String partOfCommand = "";

		if (args.length == 1) {
			getCmds(sender).forEach(cmds::add);
			partOfCommand = args[0];

			StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
			Collections.sort(completionList);
			return completionList;
		}

		if (args.length == 2) {
			if (args[0].equalsIgnoreCase("restricted")) {
				Arrays.asList("add", "remove", "list").forEach(cmds::add);
				partOfCommand = args[1];
			}

			StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
			Collections.sort(completionList);
			return completionList;
		}

		if (args.length == 3) {
			if (args[0].equalsIgnoreCase("restricted")) {
				if (args[1].equalsIgnoreCase("remove")) {
					plugin.getConf().getRestrictConfig().getStringList("restricted-players").forEach(cmds::add);
					partOfCommand = args[2];
				}

				if (partOfCommand.isEmpty() || cmds.isEmpty()) {
					return null;
				}

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);
				return completionList;
			}
		}

		return null;
	}

	private List<String> getCmds(CommandSender sender) {
		List<String> c = new ArrayList<>();
		for (String cmds : Arrays.asList("help", "reload", "toggle", "broadcast", "list", "add", "remove", "clearall",
				"restricted")) {
			if (!sender.hasPermission("automessager." + cmds)) {
				continue;
			}

			c.add(cmds);
		}

		return c;
	}
}