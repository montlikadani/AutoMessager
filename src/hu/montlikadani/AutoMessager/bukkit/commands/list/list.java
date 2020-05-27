package hu.montlikadani.AutoMessager.bukkit.commands.list;

import static hu.montlikadani.AutoMessager.bukkit.Util.getMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.sendMsg;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.commands.ICommand;

public class list implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.LIST.getPerm())) {
			sendMsg(sender, getMsg("no-permission", "%perm%", Perm.LIST.getPerm()));
			return false;
		}

		List<String> texts = plugin.getFileHandler().getTexts();
		if (texts.isEmpty()) {
			sendMsg(sender, getMsg("no-message-to-list"));
			return false;
		}

		if (!(sender instanceof Player)) {
			texts.stream().filter(t -> !t.trim().isEmpty()).forEach(t -> sendMsg(sender, t));
			return true;
		}

		Player p = (Player) sender;

		int maxRow = plugin.getConf().getConfig().getInt("show-max-row-in-one-page");
		int size = texts.size();

		if (args.length == 1) {
			List<String> page = makePage(texts, 1, maxRow);
			if (page.isEmpty()) {
				sendMsg(p, getMsg("list.no-page"));
				return false;
			}

			int maxPage = (int) ((Math.ceil(size / (double) maxRow)));

			sendMsg(p, getMsg("list.header", "%page%", 1, "%max-page%", Integer.toString(maxPage)));
			page.forEach(t -> sendMsg(p, getMsg("list.list-texts", "%texts%", t)));
			sendMsg(p, getMsg("list.footer", "%page%", 1, "%max-page%", Integer.toString(maxPage)));
			return true;
		}

		String page = args[1];
		if (!page.matches("[0-9]+")) {
			sendMsg(p, getMsg("list.page-must-be-number"));
			return false;
		}

		List<String> pages = makePage(texts, Integer.parseInt(page), maxRow);
		if (pages.isEmpty()) {
			sendMsg(p, getMsg("list.no-page"));
			return true;
		}

		int maxPage = (int) ((Math.ceil(size / (double) maxRow)));

		sendMsg(p, getMsg("list.header", "%page%", page, "%max-page%", Integer.toString(maxPage)));
		pages.forEach(t -> sendMsg(p, getMsg("list.list-texts", "%texts%", t)));
		sendMsg(p, getMsg("list.footer", "%page%", page, "%max-page%", Integer.toString(maxPage)));
		return true;
	}

	private List<String> makePage(List<String> list, int page, int size) {
		List<String> contents = new ArrayList<>();

		if (page <= 0 || page * size - (size - 1) > list.size()) {
			return contents;
		}

		for (int i = (page - 1) * size; i < page * size; i++) {
			String p = list.get(i);
			if (!p.isEmpty()) {
				contents.add(p);
			}

			if (list.size() == (i + 1)) {
				break;
			}
		}

		return contents;
	}
}
