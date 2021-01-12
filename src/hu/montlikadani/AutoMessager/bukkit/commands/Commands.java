package hu.montlikadani.AutoMessager.bukkit.commands;

import static hu.montlikadani.AutoMessager.bukkit.utils.Util.colorMsg;
import static hu.montlikadani.AutoMessager.bukkit.utils.Util.sendMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.utils.Util;

public class Commands implements CommandExecutor, TabCompleter {

	private AutoMessager plugin;

	public static final Map<UUID, Boolean> ENABLED = new HashMap<>();

	private final ImmutableList<String> subCmds = ImmutableList.<String>builder()
			.add("help", "reload", "toggle", "list", "add", "remove", "clearall", "restricted").build();

	private final Set<ICommand> cmds = new HashSet<>();

	@SuppressWarnings("deprecation")
	public Commands(AutoMessager plugin) {
		this.plugin = plugin;

		for (String s : subCmds) {
			try {
				Class<?> c = null;
				try {
					c = AutoMessager.class.getClassLoader()
							.loadClass("hu.montlikadani.AutoMessager.bukkit.commands.list." + s);
				} catch (ClassNotFoundException e) {
				}

				if (c == null) {
					continue;
				}

				if (Util.getCurrentVersion() >= 9) {
					cmds.add((ICommand) c.getDeclaredConstructor().newInstance());
				} else {
					cmds.add((ICommand) c.newInstance());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("serial")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (args.length == 0) {
			sendMsg(sender, colorMsg("&3&lAuto&a&lMessager"));
			sendMsg(sender, colorMsg("&5Version:&a " + plugin.getDescription().getVersion()));
			sendMsg(sender, colorMsg("&5Author, created by:&a montlikadani"));
			sendMsg(sender, colorMsg("&5Commands:&8 /&7" + commandLabel + "&a help"));
			sendMsg(sender, colorMsg(
					"&4If you find a bug, send issue here:&e &nhttps://github.com/montlikadani/AutoMessager/issues"));
			return true;
		}

		if (args[0].equalsIgnoreCase("help")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.HELP.getPerm())) {
				sendMsg(sender, Util.getMsgProperty("no-permission", "%perm%", Perm.HELP.getPerm()));
				return false;
			}

			Util.getMsgProperty(new TypeToken<List<String>>() {}.getSubtype(List.class),
					"chat-messages", "%command%", commandLabel).forEach(s -> sendMsg(sender, s));
			return true;
		}

		boolean found = false;
		for (ICommand command : cmds) {
			if (command.getClass().getSimpleName().equalsIgnoreCase(args[0])) {
				command.run(plugin, sender, cmd, commandLabel, args);
				found = true;
				break;
			}
		}

		if (!found) {
			sendMsg(sender, Util.getMsgProperty("unknown-sub-command", "%subcmd%", args[0]));
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		List<String> completionList = new ArrayList<>(), cmds = new ArrayList<>();
		String partOfCommand = null;

		switch (args.length) {
		case 1:
			getCmds(sender).forEach(cmds::add);
			partOfCommand = args[0];
			break;
		case 2:
			if (args[0].equalsIgnoreCase("restricted")) {
				Arrays.asList("add", "remove", "list").forEach(cmds::add);
				partOfCommand = args[1];
			}

			break;
		case 3:
			if (args[1].equalsIgnoreCase("remove")) {
				plugin.getConf().getRestrictConfig().getStringList("restricted-players").forEach(cmds::add);
				partOfCommand = args[2];
			}

			break;
		default:
			break;
		}

		if (partOfCommand == null || cmds.isEmpty()) {
			return null;
		}

		StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
		Collections.sort(completionList);
		return completionList;
	}

	private Set<String> getCmds(CommandSender sender) {
		if (!(sender instanceof Player)) {
			return subCmds.stream().collect(Collectors.toSet());
		}

		Set<String> c = new HashSet<>();
		for (String cmds : subCmds) {
			if (sender.hasPermission("automessager." + cmds)) {
				c.add(cmds);
			}
		}

		return c;
	}
}