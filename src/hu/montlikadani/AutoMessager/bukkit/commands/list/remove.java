package hu.montlikadani.AutoMessager.bukkit.commands.list;

import static hu.montlikadani.AutoMessager.bukkit.Util.getMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.sendMsg;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.commands.ICommand;

public class remove implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.REMOVE.getPerm())) {
			sendMsg(sender, getMsg("no-permission", "%perm%", Perm.REMOVE.getPerm()));
			return false;
		}

		if (args.length < 2) {
			if (sender instanceof Player) {
				((Player) sender).performCommand("am help 2");
			} else {
				Bukkit.dispatchCommand(sender, "am help");
			}

			return false;
		}

		int index = 0;
		try {
			index = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			sendMsg(sender, getMsg("bad-number"));
			return false;
		}

		if (index < 0) {
			sendMsg(sender, getMsg("bad-number"));
			return false;
		}

		if (index > plugin.getFileHandler().getTexts().size() - 1) {
			sendMsg(sender, getMsg("index-start"));
			return false;
		}

		plugin.getFileHandler().removeText(index);
		sendMsg(sender, getMsg("text-removed", "%index%", index));
		return true;
	}
}
