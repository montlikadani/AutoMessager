package hu.montlikadani.automessager.bukkit.commands.list;

import static hu.montlikadani.automessager.bukkit.utils.Util.getMsgProperty;
import static hu.montlikadani.automessager.bukkit.utils.Util.sendMsg;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.automessager.bukkit.AutoMessager;
import hu.montlikadani.automessager.bukkit.Perm;
import hu.montlikadani.automessager.bukkit.commands.CommandProcessor;
import hu.montlikadani.automessager.bukkit.commands.ICommand;

@CommandProcessor(name = "add", permission = Perm.ADD)
public class add implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length < 2) {
			if (sender instanceof Player) {
				((Player) sender).performCommand("am help");
			} else {
				Bukkit.dispatchCommand(sender, "am help");
			}

			sendMsg(sender, getMsgProperty("add-cmd-usage", "%command%", label));
			return false;
		}

		StringBuilder builder = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			builder.append(args[i] + (i + 1 < args.length ? " " : ""));
		}

		String msg = builder.toString();
		plugin.getFileHandler().addText(msg);

		sendMsg(sender, getMsgProperty("success-add-msg", "%message%", msg));
		return true;
	}
}
