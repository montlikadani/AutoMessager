package hu.montlikadani.AutoMessager.bukkit.commands.list;

import static hu.montlikadani.AutoMessager.bukkit.utils.Util.getMsgProperty;
import static hu.montlikadani.AutoMessager.bukkit.utils.Util.sendMsg;

import java.io.File;
import java.io.PrintWriter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.MessageFileHandler;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.commands.ICommand;

public class clearall implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (!hasPerm(sender, Perm.CLEARALL.getPerm())) {
			sendMsg(sender, getMsgProperty("no-permission", "%perm%", Perm.CLEARALL.getPerm()));
			return false;
		}

		MessageFileHandler handler = plugin.getFileHandler();
		if (!handler.isFileExists() || handler.getTexts().isEmpty()) {
			sendMsg(sender, getMsgProperty("no-messages-in-file"));
			return false;
		}

		handler.getTexts().clear();

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

		sendMsg(sender, getMsgProperty("all-messages-cleared"));
		return true;
	}

}
