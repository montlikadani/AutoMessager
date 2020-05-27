package hu.montlikadani.AutoMessager.bukkit.commands.list;

import static hu.montlikadani.AutoMessager.bukkit.Util.colorMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.getMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.sendMsg;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.commands.ICommand;

public class blacklist implements ICommand {

	private enum Actions {
		ADD, REMOVE, LIST;
	}

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.BLACKLISTEDPLAYERS.getPerm())) {
			sendMsg(sender, getMsg("no-permission", "%perm%", Perm.BLACKLISTEDPLAYERS.getPerm()));
			return false;
		}

		if (args.length < 2) {
			if (sender instanceof Player) {
				((Player) sender).performCommand("am help 3");
			} else {
				Bukkit.dispatchCommand(sender, "am help");
			}

			return false;
		}

		plugin.getConf().createBlacklistFile();

		final FileConfiguration file = plugin.getConf().getBlConfig();
		final List<String> blacklisted = file.getStringList("blacklisted-players");

		Actions action = Actions.ADD;

		switch (args[1].toLowerCase()) {
		case "add":
			action = Actions.ADD;
			break;
		case "remove":
			action = Actions.REMOVE;
			break;
		case "list":
			action = Actions.LIST;
			break;
		default:
			break;
		}

		switch (action) {
		case ADD:
			if (sender instanceof Player && !sender.hasPermission(Perm.BLADD.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.BLADD.getPerm()));
				return false;
			}

			if (args.length < 3) {
				if (sender instanceof Player) {
					((Player) sender).performCommand("am help 3");
				} else {
					Bukkit.dispatchCommand(sender, "am help");
				}

				return false;
			}

			Player target = Bukkit.getPlayer(args[2]);
			if (target == null) {
				sendMsg(sender, getMsg("blacklist.player-not-found", "%player%", args[2]));
				return false;
			}

			String name = target.getName();
			if (blacklisted.contains(name)) {
				sendMsg(sender, getMsg("blacklist.player-already-added", "%player%", name));
				return false;
			}

			blacklisted.add(name);
			file.set("blacklisted-players", blacklisted);
			try {
				file.save(plugin.getConf().getBlFile());
			} catch (IOException e) {
				e.printStackTrace();
			}

			sendMsg(sender, getMsg("blacklist.success-add", "%player%", name));
			break;
		case REMOVE:
			if (sender instanceof Player && !sender.hasPermission(Perm.BLREMOVE.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.BLREMOVE.getPerm()));
				return false;
			}

			if (args.length < 3) {
				if (sender instanceof Player) {
					((Player) sender).performCommand("am help 3");
				} else {
					Bukkit.dispatchCommand(sender, "am help");
				}

				return false;
			}

			String pName = args[2];
			if (!blacklisted.contains(pName)) {
				sendMsg(sender, getMsg("blacklist.player-already-removed", "%player%", pName));
				return false;
			}

			blacklisted.remove(pName);
			file.set("blacklisted-players", blacklisted);
			try {
				file.save(plugin.getConf().getBlFile());
			} catch (IOException e) {
				e.printStackTrace();
			}

			sendMsg(sender, getMsg("blacklist.success-remove", "%player%", pName));
			break;
		case LIST:
			if (sender instanceof Player && !sender.hasPermission(Perm.BLLIST.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.BLLIST.getPerm()));
				return false;
			}

			if (blacklisted.isEmpty()) {
				sendMsg(sender, getMsg("blacklist.no-player-added"));
				return false;
			}

			Collections.sort(blacklisted);

			String msg = "";
			for (String fpl : blacklisted) {
				if (!msg.isEmpty()) {
					msg += "&r, ";
				}

				msg += fpl;
			}

			for (String bp : plugin.getConf().getMessages().getStringList("blacklist.list")) {
				sendMsg(sender, colorMsg(bp.replace("%players%", msg)));
			}

			break;
		default:
			break;
		}

		return true;
	}
}
