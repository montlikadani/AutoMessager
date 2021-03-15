package hu.montlikadani.AutoMessager.bukkit.commands.list;

import static hu.montlikadani.AutoMessager.bukkit.utils.Util.getMsgProperty;
import static hu.montlikadani.AutoMessager.bukkit.utils.Util.sendMsg;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.announce.message.Message;
import hu.montlikadani.AutoMessager.bukkit.commands.CommandProcessor;
import hu.montlikadani.AutoMessager.bukkit.commands.ICommand;
import hu.montlikadani.AutoMessager.bukkit.config.ConfigConstants;

@CommandProcessor(name = "list", permission = Perm.LIST)
public class list implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		List<Message> texts = plugin.getFileHandler().getTexts();

		if (texts.isEmpty()) {
			sendMsg(sender, getMsgProperty("no-message-to-list"));
			return false;
		}

		if (!(sender instanceof Player)) {
			texts.forEach(t -> sendMsg(sender, t.getText()));
			return true;
		}

		Player p = (Player) sender;

		int maxRow = ConfigConstants.getListMaxRow();
		int size = texts.size();

		if (args.length == 1) {
			List<String> page = makePage(texts, 1, maxRow);
			if (page.isEmpty()) {
				sendMsg(p, getMsgProperty("list.no-page"));
				return false;
			}

			int maxPage = (int) ((Math.ceil(size / (double) maxRow)));

			sendMsg(p, getMsgProperty("list.header", "%page%", 1, "%max-page%", maxPage));
			page.forEach(t -> sendMsg(p, '\n' + getMsgProperty("list.list-texts", "%texts%", t)));
			sendMsg(p, getMsgProperty("list.footer", "%page%", 1, "%max-page%", maxPage));
			return true;
		}

		String page = args[1];
		if (!page.matches("[0-9]+")) {
			sendMsg(p, getMsgProperty("list.page-must-be-number"));
			return false;
		}

		List<String> pages = makePage(texts, Integer.parseInt(page), maxRow);
		if (pages.isEmpty()) {
			sendMsg(p, getMsgProperty("list.no-page"));
			return true;
		}

		int maxPage = (int) ((Math.ceil(size / (double) maxRow)));

		sendMsg(p, getMsgProperty("list.header", "%page%", page, "%max-page%", maxPage));
		pages.forEach(t -> sendMsg(p, '\n' + getMsgProperty("list.list-texts", "%texts%", t)));
		sendMsg(p, getMsgProperty("list.footer", "%page%", page, "%max-page%", maxPage));
		return true;
	}

	private List<String> makePage(List<Message> texts, int page, int size) {
		List<String> contents = new ArrayList<>();

		if (page <= 0 || page * size - (size - 1) > texts.size()) {
			return contents;
		}

		for (int i = (page - 1) * size; i < page * size; i++) {
			String p = texts.get(i).getText();
			if (!p.isEmpty()) {
				contents.add(p);
			}

			if (texts.size() == (i + 1)) {
				break;
			}
		}

		return contents;
	}
}
