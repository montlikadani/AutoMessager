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

public class add implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.ADD.getPerm())) {
			sendMsg(sender, getMsg("no-permission", "%perm%", Perm.ADD.getPerm()));
			return false;
		}

		if (args.length < 2) {
			if (sender instanceof Player) {
				((Player) sender).performCommand("am add 2");
			} else {
				Bukkit.dispatchCommand(sender, "am add");
			}

			sendMsg(sender, getMsg("add-cmd-usage", "%command%", label));
			return false;
		}

		StringBuilder builder = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			builder.append(args[i] + " ");
		}

		String msg = builder.toString();
		plugin.getFileHandler().addText(msg);

		sendMsg(sender, getMsg("success-add-msg", "%message%", msg));
		return true;
	}
}
