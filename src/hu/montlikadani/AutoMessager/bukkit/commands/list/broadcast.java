package hu.montlikadani.AutoMessager.bukkit.commands.list;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.Util;
import hu.montlikadani.AutoMessager.bukkit.commands.ICommand;

public class broadcast implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.BC.getPerm())) {
			Util.sendMsg(sender, Util.getMsg("no-permission", "%perm%", Perm.BC.getPerm()));
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

		StringBuilder builder = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			builder.append(args[i] + " ");
		}

		String msg = builder.toString();

		msg = Util.colorMsg(msg);
		msg = hu.montlikadani.AutoMessager.Global.setSymbols(msg);

		for (Player pla : Bukkit.getOnlinePlayers()) {
			Util.sendMsg(pla, Util.getMsg("broadcast-message", "%message%", Util.setPlaceholders(pla, msg)));
		}

		return true;
	}
}
