package hu.montlikadani.automessager.bukkit.commands.list;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import hu.montlikadani.automessager.bukkit.AutoMessager;
import hu.montlikadani.automessager.bukkit.Perm;
import hu.montlikadani.automessager.bukkit.commands.CommandProcessor;
import hu.montlikadani.automessager.bukkit.commands.ICommand;
import hu.montlikadani.automessager.bukkit.utils.Util;

@CommandProcessor(name = "reload", permission = Perm.RELOAD)
public class reload implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		plugin.reload();
		Util.sendMsg(sender, Util.getMsgProperty("reload-config"));
		return true;
	}
}
