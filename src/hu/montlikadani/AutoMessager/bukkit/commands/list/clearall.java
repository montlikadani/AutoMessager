package hu.montlikadani.AutoMessager.bukkit.commands.list;

import static hu.montlikadani.AutoMessager.bukkit.Util.getMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.sendMsg;

import java.io.File;
import java.io.PrintWriter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.MessageFileHandler;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.commands.ICommand;

public class clearall implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.CLEARALL.getPerm())) {
			sendMsg(sender, getMsg("no-permission", "%perm%", Perm.CLEARALL.getPerm()));
			return false;
		}

		MessageFileHandler handler = plugin.getFileHandler();
		if (handler.isFileExists() || handler.getTexts().size() < 1) {
			sendMsg(sender, getMsg("no-messages-in-file"));
			return false;
		}

		handler.clearTexts();

		try {
			File file = handler.getFile();
			if (handler.isYaml()) {
				handler.getFileConfig().set("messages", null);
				handler.getFileConfig().save(file);
			} else {
				PrintWriter writer = new PrintWriter(file);
				writer.print("");
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		sendMsg(sender, getMsg("all-messages-cleared"));
		return true;
	}

}
