package hu.montlikadani.AutoMessager.bukkit.commands.list;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.commands.ICommand;
import hu.montlikadani.AutoMessager.bukkit.utils.Util;

public class reload implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (!hasPerm(sender, Perm.RELOAD.getPerm())) {
			Util.sendMsg(sender, Util.getMsgProperty("no-permission", "%perm%", Perm.RELOAD.getPerm()));
			return false;
		}

		plugin.reload();
		Util.sendMsg(sender, Util.getMsgProperty("reload-config"));
		return true;
	}
}
