package hu.montlikadani.automessager.bukkit.commands.list;

import static hu.montlikadani.automessager.bukkit.utils.Util.getMsgProperty;
import static hu.montlikadani.automessager.bukkit.utils.Util.sendMsg;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.automessager.bukkit.AutoMessager;
import hu.montlikadani.automessager.bukkit.Perm;
import hu.montlikadani.automessager.bukkit.commands.CommandProcessor;
import hu.montlikadani.automessager.bukkit.commands.ICommand;

@CommandProcessor(name = "forcesend", permission = Perm.FORCESEND, playerOnly = true)
public class forcesend implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length < 2) {
			((Player) sender).performCommand("am help");
			return false;
		}

		int index = 0;
		try {
			index = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
		}

		if (index < 0) {
			sendMsg(sender, getMsgProperty("bad-number"));
			return false;
		}

		if (index > plugin.getFileHandler().getTexts().size() - 1) {
			sendMsg(sender, getMsgProperty("index-start"));
			return false;
		}

		plugin.getFileHandler().getTexts().get(index).sendTo(((Player) sender), true);
		return true;
	}
}
