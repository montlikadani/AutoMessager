package hu.montlikadani.automessager.bukkit.commands.list;

import static hu.montlikadani.automessager.bukkit.utils.Util.getMsgProperty;
import static hu.montlikadani.automessager.bukkit.utils.Util.sendMsg;

import java.io.IOException;
import java.io.PrintWriter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import hu.montlikadani.automessager.bukkit.AutoMessager;
import hu.montlikadani.automessager.bukkit.Perm;
import hu.montlikadani.automessager.bukkit.commands.CommandProcessor;
import hu.montlikadani.automessager.bukkit.commands.ICommand;
import hu.montlikadani.automessager.bukkit.config.MessageFileHandler;

@CommandProcessor(name = "clearall", permission = Perm.CLEARALL)
public class clearall implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		MessageFileHandler handler = plugin.getFileHandler();

		if (!handler.isFileExists() || handler.getTexts().isEmpty()) {
			sendMsg(sender, getMsgProperty("no-messages-in-file"));
			return false;
		}

		handler.getTexts().clear();

		try {
			if (handler.isYaml()) {
				handler.getFileConfig().set("messages", null);
				handler.getFileConfig().save(handler.getFile());
			} else {
				PrintWriter writer = new PrintWriter(handler.getFile());
				writer.print("");
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		sendMsg(sender, getMsgProperty("all-messages-cleared"));
		return true;
	}

}
