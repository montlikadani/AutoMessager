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

public class restricted implements ICommand {

	private enum Actions {
		ADD, REMOVE, LIST;
	}

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.RESTRICTEDPLAYERS.getPerm())) {
			sendMsg(sender, getMsg("no-permission", "%perm%", Perm.RESTRICTEDPLAYERS.getPerm()));
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

		plugin.getConf().createRestrictedFile();

		final FileConfiguration file = plugin.getConf().getRestrictConfig();
		final List<String> restricted = file.getStringList("restricted-players");

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

		boolean fileChanged = false;

		switch (action) {
		case ADD:
			if (sender instanceof Player && !sender.hasPermission(Perm.RESTRICTEDADD.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.RESTRICTEDADD.getPerm()));
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
				sendMsg(sender, getMsg("restricted.player-not-found", "%player%", args[2]));
				return false;
			}

			String name = target.getName();
			if (restricted.contains(name)) {
				sendMsg(sender, getMsg("restricted.player-already-added", "%player%", name));
				return false;
			}

			fileChanged = restricted.add(name);
			sendMsg(sender, getMsg("restricted.success-add", "%player%", name));
			break;
		case REMOVE:
			if (sender instanceof Player && !sender.hasPermission(Perm.RESTRICTEDREMOVE.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.RESTRICTEDREMOVE.getPerm()));
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
			if (!restricted.contains(pName)) {
				sendMsg(sender, getMsg("restricted.player-already-removed", "%player%", pName));
				return false;
			}

			fileChanged = restricted.remove(pName);
			sendMsg(sender, getMsg("restricted.success-remove", "%player%", pName));
			break;
		case LIST:
			if (sender instanceof Player && !sender.hasPermission(Perm.RESTRICTEDLIST.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.RESTRICTEDLIST.getPerm()));
				return false;
			}

			if (restricted.isEmpty()) {
				sendMsg(sender, getMsg("restricted.no-player-added"));
				return false;
			}

			Collections.sort(restricted);

			String msg = "";
			for (String fpl : restricted) {
				if (!msg.isEmpty()) {
					msg += "&r, ";
				}

				msg += fpl;
			}

			for (String bp : plugin.getConf().getMessages().getStringList("restricted.list")) {
				sendMsg(sender, colorMsg(bp.replace("%players%", msg)));
			}

			break;
		default:
			break;
		}

		if (fileChanged) {
			file.set("restricted-players", restricted);
			try {
				file.save(plugin.getConf().getRestrictFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;
	}
}
