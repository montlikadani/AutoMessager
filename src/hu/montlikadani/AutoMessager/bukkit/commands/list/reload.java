package hu.montlikadani.AutoMessager.bukkit.commands.list;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.Util;
import hu.montlikadani.AutoMessager.bukkit.commands.ICommand;

public class reload implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.RELOAD.getPerm())) {
			Util.sendMsg(sender, Util.getMsg("no-permission", "%perm%", Perm.RELOAD.getPerm()));
			return false;
		}

		plugin.reload();
		Util.sendMsg(sender, Util.getMsg("reload-config"));
		return true;
	}
}
