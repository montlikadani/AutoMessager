package hu.montlikadani.AutoMessager.bukkit;

import static hu.montlikadani.AutoMessager.bukkit.Util.colorMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.getMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.sendMsg;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class Commands implements CommandExecutor, TabCompleter {

	private AutoMessager plugin;

	Commands(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public static final Map<UUID, Boolean> ENABLED = new HashMap<>();

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (args.length == 0) {
			sendMsg(sender, colorMsg("&e&l[&3&lAuto&a&lMessager&b&l Info&e&l]"));
			sendMsg(sender, colorMsg("&5Version:&a " + plugin.getDescription().getVersion()));
			sendMsg(sender, colorMsg("&5Author, created by:&a montlikadani"));
			sendMsg(sender, colorMsg("&5Commands:&8 /&7" + commandLabel + "&a help"));
			sendMsg(sender, colorMsg(
					"&4If you find a bug, send issue here:&e &nhttps://github.com/montlikadani/AutoMessager/issues"));
		} else if (args[0].equalsIgnoreCase("help")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.HELP.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.HELP.getPerm()));
				return true;
			}

			FileConfiguration messages = plugin.getConf().getMessages();
			if (sender instanceof Player) {
				if (args.length == 1) {
					messages.getStringList("chat-messages.1")
							.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", commandLabel))));
				} else if (args.length == 2) {
					if (args[1].equals("2")) {
						messages.getStringList("chat-messages.2")
								.forEach(msg -> sender.sendMessage(colorMsg(msg.replace("%command%", commandLabel))));
					} else if (args[1].equals("3")) {
						messages.getStringList("chat-messages.3")
								.forEach(msg -> sender.sendMessage(colorMsg(msg.replace("%command%", commandLabel))));
					}
				}
			} else {
				messages.getStringList("chat-messages.1")
						.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", commandLabel))));

				messages.getStringList("chat-messages.2")
						.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", commandLabel))));

				messages.getStringList("chat-messages.3")
						.forEach(msg -> sendMsg(sender, colorMsg(msg.replace("%command%", commandLabel))));
			}
		} else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.RELOAD.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.RELOAD.getPerm()));
				return true;
			}

			plugin.reload();

			sendMsg(sender, getMsg("reload-config"));
		} else if (args[0].equalsIgnoreCase("toggle")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.TOGGLE.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.TOGGLE.getPerm()));
				return true;
			}

			if (!(sender instanceof Player) && args.length < 2) {
				sendMsg(sender, getMsg("toggle.console-usage", "%command%", commandLabel));
				return true;
			}

			if (args.length == 2) {
				if (args[1].equalsIgnoreCase("all")) {
					for (Player pl : Bukkit.getOnlinePlayers()) {
						UUID uuid = pl.getUniqueId();
						if (!ENABLED.containsKey(uuid)) {
							ENABLED.put(uuid, false);
						} else {
							ENABLED.remove(uuid);
						}
					}

					return true;
				}

				Player target = Bukkit.getPlayer(args[1]);
				if (target == null) {
					sendMsg(sender, getMsg("toggle.player-not-found", "%target%", args[1]));
					return true;
				}

				UUID uuid = target.getUniqueId();
				if (!ENABLED.containsKey(uuid)) {
					ENABLED.put(uuid, false);
					sendMsg(sender, getMsg("toggle.disabled"));
				} else {
					ENABLED.remove(uuid);
					sendMsg(sender, getMsg("toggle.enabled"));
				}

				return true;
			}

			Player p = (Player) sender;
			UUID uuid = p.getUniqueId();
			if (!ENABLED.containsKey(uuid)) {
				ENABLED.put(uuid, false);
				sendMsg(sender, getMsg("toggle.disabled"));
			} else {
				ENABLED.remove(uuid);
				sendMsg(sender, getMsg("toggle.enabled"));
			}
		} else if (args[0].equalsIgnoreCase("broadcast") || args[0].equalsIgnoreCase("bc")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.BC.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.BC.getPerm()));
				return true;
			}

			if (args.length < 2) {
				if (sender instanceof Player) {
					((Player) sender).performCommand("am help 2");
				} else {
					Bukkit.dispatchCommand(sender, "am help");
				}

				return true;
			}

			StringBuilder builder = new StringBuilder();
			for (int i = 1; i < args.length; i++) {
				builder.append(args[i] + " ");
			}

			String msg = builder.toString();
			msg = colorMsg(msg);
			msg = hu.montlikadani.AutoMessager.Global.setSymbols(msg);

			for (Player pla : Bukkit.getOnlinePlayers()) {
				msg = Util.setPlaceholders(pla, msg);
			}

			Util.sendMsg(getMsg("broadcast-message", "%message%", msg), true);
		} else if (args[0].equalsIgnoreCase("list")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.LIST.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.LIST.getPerm()));
				return true;
			}

			List<String> texts = plugin.getFileHandler().getTexts();
			if (texts.isEmpty()) {
				sendMsg(sender, getMsg("no-message-to-list"));
				return true;
			}

			if (!(sender instanceof Player)) {
				for (String t : texts) {
					if (t != null && !t.trim().isEmpty()) {
						sendMsg(sender, t);
					}
				}

				return true;
			}

			Player p = (Player) sender;

			int maxRow = plugin.getConf().getConfig().getInt("show-max-row-in-one-page");
			int size = texts.size();

			if (args.length == 1) {
				List<String> page = makePage(texts, 1, maxRow);
				if (page.isEmpty()) {
					sendMsg(p, getMsg("list.no-page"));
					return true;
				}

				sendMsg(p, getMsg("list.header", "%page%", 1, "%max-page%",
						Integer.toString((int) ((Math.ceil(size / (double) maxRow))))));

				page.forEach(t -> sendMsg(p, getMsg("list.list-texts", "%texts%", t)));

				sendMsg(p, getMsg("list.footer", "%page%", 1, "%max-page%",
						Integer.toString((int) ((Math.ceil(size / (double) maxRow))))));
				return true;
			}

			if (!args[1].matches("[0-9]+")) {
				sendMsg(p, getMsg("list.page-must-be-number"));
				return true;
			}

			List<String> page = makePage(texts, Integer.parseInt(args[1]), maxRow);
			if (page.isEmpty()) {
				sendMsg(p, getMsg("list.no-page"));
				return true;
			}

			sendMsg(p, getMsg("list.header", "%page%", args[1], "%max-page%",
					Integer.toString((int) ((Math.ceil(size / (double) maxRow))))));

			page.forEach(t -> sendMsg(p, getMsg("list.list-texts", "%texts%", t)));

			sendMsg(p, getMsg("list.footer", "%page%", args[1], "%max-page%",
					Integer.toString((int) ((Math.ceil(size / (double) maxRow))))));
		} else if (args[0].equalsIgnoreCase("add")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.ADD.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.ADD.getPerm()));
				return true;
			}

			if (args.length < 2) {
				if (sender instanceof Player) {
					((Player) sender).performCommand("am add 2");
				} else {
					Bukkit.dispatchCommand(sender, "am add");
				}

				sendMsg(sender, getMsg("add-cmd-usage", "%command%", commandLabel));
				return true;
			}

			StringBuilder builder = new StringBuilder();
			for (int i = 1; i < args.length; i++) {
				builder.append(args[i] + " ");
			}

			String msg = builder.toString();
			plugin.getFileHandler().addText(msg);

			sendMsg(sender, getMsg("success-add-msg", "%message%", msg));
		} else if (args[0].equalsIgnoreCase("remove")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.REMOVE.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.REMOVE.getPerm()));
				return true;
			}

			if (args.length < 2) {
				if (sender instanceof Player) {
					((Player) sender).performCommand("am help 2");
				} else {
					Bukkit.dispatchCommand(sender, "am help");
				}

				return true;
			}

			int index = 0;
			try {
				index = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				sendMsg(sender, getMsg("bad-number"));
				return true;
			}

			if (index < 0) {
				sendMsg(sender, getMsg("bad-number"));
				return true;
			}

			if (index > plugin.getFileHandler().getTexts().size() - 1) {
				sendMsg(sender, getMsg("index-start"));
				return true;
			}

			plugin.getFileHandler().removeText(index);
			sendMsg(sender, getMsg("text-removed", "%index%", index));
		} else if (args[0].equalsIgnoreCase("clearall")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.CLEARALL.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.CLEARALL.getPerm()));
				return true;
			}

			MessageFileHandler handler = plugin.getFileHandler();
			if (handler.isFileExists() || handler.getTexts().size() < 1) {
				sendMsg(sender, getMsg("no-messages-in-file"));
				return true;
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
		} else if (args[0].equalsIgnoreCase("blacklist") || args[0].equalsIgnoreCase("bl")) {
			if (sender instanceof Player && !sender.hasPermission(Perm.BLACKLISTEDPLAYERS.getPerm())) {
				sendMsg(sender, getMsg("no-permission", "%perm%", Perm.BLACKLISTEDPLAYERS.getPerm()));
				return true;
			}

			if (args.length < 2) {
				if (sender instanceof Player) {
					((Player) sender).performCommand("am help 3");
				} else {
					Bukkit.dispatchCommand(sender, "am help");
				}

				return true;
			}

			plugin.getConf().createBlacklistFile();

			FileConfiguration bpls = plugin.getConf().getBlConfig();

			if (args[1].equalsIgnoreCase("add")) {
				if (sender instanceof Player && !sender.hasPermission(Perm.BLADD.getPerm())) {
					sendMsg(sender, getMsg("no-permission", "%perm%", Perm.BLADD.getPerm()));
					return true;
				}

				if (args.length < 3) {
					if (sender instanceof Player) {
						((Player) sender).performCommand("am help 3");
					} else {
						Bukkit.dispatchCommand(sender, "am help");
					}

					return true;
				}

				Player target = Bukkit.getPlayer(args[2]);
				if (target == null) {
					sendMsg(sender, getMsg("blacklist.player-not-found", "%player%", args[2]));
					return true;
				}

				String name = target.getName();
				List<String> banpls = bpls.getStringList("blacklisted-players");
				if (banpls.contains(name)) {
					sendMsg(sender, getMsg("blacklist.player-already-added", "%player%", name));
					return true;
				}

				banpls.add(name);
				bpls.set("blacklisted-players", banpls);
				try {
					bpls.save(plugin.getConf().getBlFile());
				} catch (IOException e) {
					e.printStackTrace();
				}

				sendMsg(sender, getMsg("blacklist.success-add", "%player%", name));
			} else if (args[1].equalsIgnoreCase("remove")) {
				if (sender instanceof Player && !sender.hasPermission(Perm.BLREMOVE.getPerm())) {
					sendMsg(sender, getMsg("no-permission", "%perm%", Perm.BLREMOVE.getPerm()));
					return true;
				}

				if (args.length < 3) {
					if (sender instanceof Player) {
						((Player) sender).performCommand("am help 3");
					} else {
						Bukkit.dispatchCommand(sender, "am help");
					}

					return true;
				}

				String name = args[2];
				List<String> banpls = bpls.getStringList("blacklisted-players");
				if (!banpls.contains(name)) {
					sendMsg(sender, getMsg("blacklist.player-already-removed", "%player%", name));
					return true;
				}

				banpls.remove(name);
				bpls.set("blacklisted-players", banpls);
				try {
					bpls.save(plugin.getConf().getBlFile());
				} catch (IOException e) {
					e.printStackTrace();
				}

				sendMsg(sender, getMsg("blacklist.success-remove", "%player%", name));
			} else if (args[1].equalsIgnoreCase("list")) {
				if (sender instanceof Player && !sender.hasPermission(Perm.BLLIST.getPerm())) {
					sendMsg(sender, getMsg("no-permission", "%perm%", Perm.BLLIST.getPerm()));
					return true;
				}

				List<String> listPlayers = bpls.getStringList("blacklisted-players");
				if (listPlayers.isEmpty()) {
					sendMsg(sender, getMsg("blacklist.no-player-added"));
					return true;
				}

				Collections.sort(listPlayers);

				String msg = "";
				for (String fpl : listPlayers) {
					if (!msg.isEmpty()) {
						msg += "&r, ";
					}

					msg += fpl;
				}

				for (String bp : plugin.getConf().getMessages().getStringList("blacklist.list")) {
					sendMsg(sender, colorMsg(bp.replace("%players%", msg)));
				}
			}
		} else {
			sendMsg(sender, getMsg("unknown-sub-command", "%subcmd%", args[0]));
			return true;
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		List<String> completionList = new ArrayList<>();
		List<String> cmds = new ArrayList<>();
		String partOfCommand = "";

		if (args.length == 1) {
			getCmds(sender).forEach(cmds::add);
			partOfCommand = args[0];

			StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
			Collections.sort(completionList);
			return completionList;
		}

		if (args.length == 2) {
			if (args[0].equalsIgnoreCase("blacklist") || args[0].equalsIgnoreCase("bl")) {
				Arrays.asList("add", "remove", "list").forEach(cmds::add);
				partOfCommand = args[1];
			}

			StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
			Collections.sort(completionList);
			return completionList;
		}

		if (args.length == 3) {
			if (args[0].equalsIgnoreCase("blacklist") || args[0].equalsIgnoreCase("bl")) {
				if (args[1].equalsIgnoreCase("remove")) {
					plugin.getConf().getBlConfig().getStringList("blacklisted-players").forEach(cmds::add);
					partOfCommand = args[2];
				}

				if (partOfCommand.isEmpty() || cmds.isEmpty()) {
					return null;
				}

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);
				return completionList;
			}
		}

		return null;
	}

	private List<String> makePage(List<String> list, int page, int size) {
		List<String> contents = new ArrayList<>();
		if (page <= 0 || page * size - (size - 1) > list.size()) {
			return contents;
		}

		for (int i = (page - 1) * size; i < page * size; i++) {
			String p = list.get(i);
			if (p != null && !p.isEmpty()) {
				contents.add(p);
			}

			if (list.size() == (i + 1)) {
				break;
			}
		}

		return contents;
	}

	private List<String> getCmds(CommandSender sender) {
		List<String> c = new ArrayList<>();
		for (String cmds : Arrays.asList("help", "reload", "toggle", "broadcast", "list", "add", "remove", "clearall",
				"blacklist")) {
			if (!sender.hasPermission("automessager." + cmds)) {
				continue;
			}

			c.add(cmds);
		}

		return c;
	}
}