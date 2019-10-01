package hu.montlikadani.AutoMessager.bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import hu.montlikadani.AutoMessager.bukkit.Permissions.Perm;

public class Commands implements CommandExecutor, TabCompleter {

	private AutoMessager plugin;

	Commands(AutoMessager plugin) {
		this.plugin = plugin;
	}

	static Map<UUID, Boolean> enabled = new HashMap<>();

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		try {
			if (cmd.getName().equalsIgnoreCase("automessager")) {
				if (args.length == 0) {
					sender.sendMessage(replColor("&e&l[&3&lAuto&a&lMessager&b&l Info&e&l]"));
					sender.sendMessage(replColor("&5Version:&a " + plugin.getDescription().getVersion()));
					sender.sendMessage(replColor("&5Author, created by:&a montlikadani"));
					sender.sendMessage(replColor("&5Commands:&8 /&7" + commandLabel + "&a help"));
					sender.sendMessage(replColor("&4If you find a bug, send issue here:&e &nhttps://github.com/montlikadani/AutoMessager/issues"));
				} else if (args[0].equalsIgnoreCase("help")) {
					if (sender instanceof Player && !sender.hasPermission(Perm.HELP.getPerm())) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.HELP.getPerm())));
						return true;
					}

					if (args.length > 2) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("unknown-command", "%command%", commandLabel)));
						return true;
					}

					if (sender instanceof Player) {
						if (args.length == 1) {
							plugin.messages.getStringList("chat-messages.1").forEach(
									msg -> sender.sendMessage(plugin.colorMsg(msg.replace("%command%", commandLabel))));
						} else if (args.length == 2) {
							if (args[1].equalsIgnoreCase("2")) {
								plugin.messages.getStringList("chat-messages.2").forEach(
										msg -> sender.sendMessage(plugin.colorMsg(msg.replace("%command%", commandLabel))));
							} else if (args[1].equalsIgnoreCase("3")) {
								plugin.messages.getStringList("chat-messages.3").forEach(
										msg -> sender.sendMessage(plugin.colorMsg(msg.replace("%command%", commandLabel))));
							}
						}
					} else {
						plugin.messages.getStringList("chat-messages.1")
								.forEach(msg -> sender.sendMessage(plugin.colorMsg(msg.replace("%command%", commandLabel))));

						plugin.messages.getStringList("chat-messages.2")
								.forEach(msg -> sender.sendMessage(plugin.colorMsg(msg.replace("%command%", commandLabel))));

						plugin.messages.getStringList("chat-messages.3")
								.forEach(msg -> sender.sendMessage(plugin.colorMsg(msg.replace("%command%", commandLabel))));
					}
				} else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
					if (sender instanceof Player && !sender.hasPermission(Perm.RELOAD.getPerm())) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.RELOAD.getPerm())));
						return true;
					}

					if (args.length > 1) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("unknown-command", "%command%", commandLabel)));
						return true;
					}

					plugin.loadFiles();
					plugin.getAnnounce().cancelTask();
					plugin.getAnnounce().load();

					for (Player pl : Bukkit.getOnlinePlayers()) {
						plugin.getAnnounce().schedule(pl);
					}

					plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("reload-config")));
				} else if (args[0].equalsIgnoreCase("toggle")) {
					if (sender instanceof Player && !sender.hasPermission(Perm.TOGGLE.getPerm())) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.TOGGLE.getPerm())));
						return true;
					}

					if (args.length > 2) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("unknown-command", "%command%", commandLabel)));
						return true;
					}

					if (!(sender instanceof Player)) {
						if (args.length < 2) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("toggle.console-usage", "%command%", commandLabel)));
							return true;
						}
					}

					if (args.length == 2) {
						if (args[1].equalsIgnoreCase("all")) {
							for (Player pl : Bukkit.getOnlinePlayers()) {
								if (!enabled.containsKey(pl.getUniqueId())) {
									enabled.put(pl.getUniqueId(), false);
									plugin.getAnnounce().cancelTask();
									plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("toggle.disabled")));
								} else {
									enabled.remove(pl.getUniqueId());
									plugin.getAnnounce().schedule(pl);
									plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("toggle.enabled")));
								}
							}
							return true;
						}

						Player target = Bukkit.getPlayer(args[1]);
						if (target == null) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("toggle.player-not-found", "%target%", args[1])));
							return true;
						}

						if (!enabled.containsKey(target.getUniqueId())) {
							enabled.put(target.getUniqueId(), false);
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("toggle.disabled")));
						} else {
							enabled.remove(target.getUniqueId());
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("toggle.enabled")));
						}
						return true;
					}

					Player p = (Player) sender;
					if (!enabled.containsKey(p.getUniqueId())) {
						enabled.put(p.getUniqueId(), false);
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("toggle.disabled")));
					} else {
						enabled.remove(p.getUniqueId());
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("toggle.enabled")));
					}
				} else if (args[0].equalsIgnoreCase("broadcast") || args[0].equalsIgnoreCase("bc")) {
					if (sender instanceof Player && !sender.hasPermission(Perm.BC.getPerm())) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.BC.getPerm())));
						return true;
					}

					if (args.length < 2) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("broadcast-usage", "%command%", commandLabel, "%args%", args[0])));
						return true;
					}

					StringBuilder builder = new StringBuilder();
					for (int i = 1; i < args.length; i++) {
						builder.append(args[i] + " ");
					}

					String msg = builder.toString();
					msg = plugin.colorMsg(msg);
					msg = plugin.setSymbols(msg);

					for (Player pla : Bukkit.getOnlinePlayers()) {
						msg = plugin.setPlaceholders(pla, msg);
					}

					if (plugin.getMsg("broadcast-message") != null && !plugin.getMsg("broadcast-message").equals("")) {
						Bukkit.getServer().broadcastMessage(plugin.defaults(plugin.getMsg("broadcast-message", "%message%", msg)));
					}
				} else if (args[0].equalsIgnoreCase("list")) {
					if (sender instanceof Player && !sender.hasPermission(Perm.LIST.getPerm())) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.LIST.getPerm())));
						return true;
					}

					if (args.length > 2) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("unknown-command", "%command%", commandLabel)));
						return true;
					}

					int size = plugin.getMessages().size();
					if (size < 1) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-message-to-list")));
						return true;
					}

					if (!(sender instanceof Player)) {
						for (int i = 1; i < size; i++) {
							String msgto = plugin.getMessages().get(i);
							msgto = plugin.defaults(msgto);
							msgto = plugin.setSymbols(msgto);

							if (!plugin.getConfig().getString("title", "").equals("")) {
								msgto = msgto.replace("%title%", plugin.getConfig().getString("title").replace("%newline%", "\n"));
							}
							if (!plugin.getConfig().getString("suffix", "").equals("")) {
								msgto = msgto.replace("%suffix%", plugin.getConfig().getString("suffix"));
							}
							for (Player pls : Bukkit.getOnlinePlayers()) {
								msgto = plugin.setPlaceholders(pls, msgto);
							}

							sender.sendMessage(msgto);
						}
					} else {
						Player p = (Player) sender;
						int maxRow = plugin.getConfig().getInt("show-max-row-in-one-page");

						if (args.length == 1) {
							List<String> page = makePage(plugin.getMessages(), 1, maxRow);
							if (page == null) {
								plugin.sendMsg(p, plugin.defaults(plugin.getMsg("list.no-page")));
								return true;
							}

							plugin.sendMsg(p, plugin.defaults(plugin.getMsg("list.header", "%page%", 1,
									"%max-page%", Integer.toString((int) ((Math.ceil(size / (double) maxRow)))))));

							page.forEach(t -> plugin.sendMsg(p,
									plugin.defaults(plugin.getMsg("list.list-texts", "%texts%", t))));

							plugin.sendMsg(p, plugin.defaults(plugin.getMsg("list.footer", "%page%", 1,
									"%max-page%", Integer.toString((int) ((Math.ceil(size / (double) maxRow)))))));
							return true;
						}

						if (!args[1].matches("[0-9]+")) {
							plugin.sendMsg(p, plugin.defaults(plugin.getMsg("list.page-must-be-number")));
							return true;
						}

						List<String> page = makePage(plugin.getMessages(), Integer.parseInt(args[1]), maxRow);
						if (page == null) {
							plugin.sendMsg(p, plugin.defaults(plugin.getMsg("list.no-page")));
							return true;
						}

						// Not bug - less texts show in first page (reason for the "" blank line)
						plugin.sendMsg(p, plugin.defaults(plugin.getMsg("list.header", "%page%", args[1],
								"%max-page%", Integer.toString((int) ((Math.ceil(size / (double) maxRow)))))));

						page.forEach(t -> plugin.sendMsg(p,
								plugin.defaults(plugin.getMsg("list.list-texts", "%texts%", t))));

						plugin.sendMsg(p, plugin.defaults(plugin.getMsg("list.footer", "%page%", args[1],
								"%max-page%", Integer.toString((int) ((Math.ceil(size / (double) maxRow)))))));
					}
				} else if (args[0].equalsIgnoreCase("add")) {
					if (sender instanceof Player && !sender.hasPermission(Perm.ADD.getPerm())) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.ADD.getPerm())));
						return true;
					}

					if (args.length < 2) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("add-cmd-usage", "%command%", commandLabel)));
						return true;
					}

					StringBuilder builder = new StringBuilder();
					for (int i = 1; i < args.length; i++) {
						builder.append(args[i] + " ");
					}

					String msg = builder.toString();

					File file = plugin.getMsgFile();
					if (!file.exists()) {
						plugin.loadMessages();
					}

					plugin.getMessages().add(msg);

					if (plugin.getConfig().getString("message-file").endsWith(".yml")) {
						FileConfiguration msgC = YamlConfiguration.loadConfiguration(file);
						msgC.set("messages", null);

						msgC.set("messages", plugin.getMessages());
						msgC.save(file);
					} else {
						FileWriter fw = new FileWriter(file, true);
						PrintWriter pw = new PrintWriter(fw);
						pw.println(msg);
						pw.close();
					}

					plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("success-add-msg", "%message%", msg)));
				} else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("rem")) {
					if (sender instanceof Player && !sender.hasPermission(Perm.REMOVE.getPerm())) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.REMOVE.getPerm())));
						return true;
					}

					if (args.length < 2) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("remove-cmd-usage", "%command%", commandLabel, "%args%", args[0])));
						return true;
					}

					if (args.length > 2) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("unknown-command", "%command%", commandLabel)));
						return true;
					}

					int line = 0;
					try {
						line = Integer.parseInt(args[1]);
					} catch (NumberFormatException e) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("bad-number")));
						return true;
					}

					if (line < 1) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("bad-number")));
						return true;
					}

					plugin.deleteMessage(plugin.getMsgFile(), line);
					plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("text-removed", "%word%", args[1])));
				} else if (args[0].equalsIgnoreCase("clearall")) {
					if (sender instanceof Player && !sender.hasPermission(Perm.CLEARALL.getPerm())) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.CLEARALL.getPerm())));
						return true;
					}

					if (args.length > 1) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("unknown-command", "%command%", commandLabel)));
						return true;
					}

					File file = plugin.getMsgFile();

					if (!file.exists() || plugin.getMessages().size() < 1) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-messages-in-file")));
						return true;
					}

					plugin.getMessages().clear();

					if (plugin.getConfig().getString("message-file").endsWith(".yml")) {
						FileConfiguration msgC = YamlConfiguration.loadConfiguration(file);
						msgC.set("messages", null);
						msgC.save(file);
					} else {
						PrintWriter writer = new PrintWriter(file);
						writer.print("");
						writer.close();
					}

					plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("all-messages-cleared")));
				} else if (args[0].equalsIgnoreCase("bannedplayers") || args[0].equalsIgnoreCase("bp")) {
					if (sender instanceof Player && !sender.hasPermission(Perm.BANNEDPLAYERS.getPerm())) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.BANNEDPLAYERS.getPerm())));
						return true;
					}

					if (args.length < 2) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("banned-players.usage", "%command%", commandLabel, "%args%", args[0])));
						return true;
					}

					if (args.length > 3) {
						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("unknown-command", "%command%", commandLabel)));
						return true;
					}

					if (!plugin.bpls_file.exists()) {
						plugin.bpls_file.createNewFile();
						plugin.bpls = YamlConfiguration.loadConfiguration(plugin.bpls_file);
						plugin.logConsole("The 'banned-players.yml' file successfully created!");
					}

					if (args[1].equalsIgnoreCase("add")) {
						if (sender instanceof Player && !sender.hasPermission(Perm.BPADD.getPerm())) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.BPADD.getPerm())));
							return true;
						}

						if (args.length < 3) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("banned-players.add-usage", "%command%", commandLabel, "%args%", args[0])));
							return true;
						}

						Player target = Bukkit.getPlayer(args[2]);
						if (target == null) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("banned-players.player-not-found", "%player%", args[2])));
							return true;
						}

						List<String> banpls = plugin.bpls.getStringList("banned-players");
						if (banpls.contains(target.getName())) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("banned-players.player-already-added", "%player%", target.getName())));
							return true;
						}

						banpls.add(target.getName());
						plugin.bpls.set("banned-players", banpls);
						plugin.bpls.save(plugin.bpls_file);

						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("banned-players.success-add", "%player%", target.getName())));
					} else if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("rem")) {
						if (sender instanceof Player && !sender.hasPermission(Perm.BPREMOVE.getPerm())) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.BPREMOVE.getPerm())));
							return true;
						}

						if (args.length < 3) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("banned-players.remove-usage", "%command%", commandLabel, "%args%", args[0], "%args2%", args[1])));
							return true;
						}

						Player target = Bukkit.getPlayer(args[2]);
						if (target == null) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("banned-players.player-not-found", "%player%", args[2])));
							return true;
						}

						List<String> banpls = plugin.bpls.getStringList("banned-players");
						if (!banpls.contains(target.getName())) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("banned-players.player-already-removed", "%player%", target.getName())));
							return true;
						}

						banpls.remove(target.getName());
						plugin.bpls.set("banned-players", banpls);
						plugin.bpls.save(plugin.bpls_file);

						plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("banned-players.success-remove", "%player%", target.getName())));
					} else if (args[1].equalsIgnoreCase("list")) {
						if (sender instanceof Player && !sender.hasPermission(Perm.BPLIST.getPerm())) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("no-permission", "%perm%", Perm.BPLIST.getPerm())));
							return true;
						}

						List<String> listPlayers = plugin.bpls.getStringList("banned-players");
						if (listPlayers == null || listPlayers.isEmpty()) {
							plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("banned-players.no-banned-players")));
							return true;
						}

						for (String bp : plugin.messages.getStringList("banned-players.list")) {
							Collections.sort(listPlayers);

							String msg = "";
							for (String fpl : listPlayers) {
								if (!msg.isEmpty()) {
									msg += ", ";
								}

								msg += fpl;
							}

							sender.sendMessage(plugin.defaults(bp.replace("%players%", msg)));
						}
					}
				} else {
					plugin.sendMsg(sender, plugin.defaults(plugin.getMsg("unknown-sub-command", "%subcmd%", args[0])));
					return true;
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			plugin.throwMsg();
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
			if (args[0].equalsIgnoreCase("bannedplayers") || args[0].equalsIgnoreCase("bp")) {
				for (String bp : new String[] { "add", "remove", "rem", "list" }) {
					cmds.add(bp);
				}

				partOfCommand = args[1];
			}

			StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
			Collections.sort(completionList);
			return completionList;
		}

		if (args.length == 3) {
			if (args[0].equalsIgnoreCase("bannedplayers") || args[0].equalsIgnoreCase("bp")) {
				if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("rem")) {
					plugin.bpls.getStringList("banned-players").forEach(cmds::add);
					partOfCommand = args[2];
				}

				StringUtil.copyPartialMatches(partOfCommand, cmds, completionList);
				Collections.sort(completionList);
				return completionList;
			}
		}

		return null;
	}

	private List<String> makePage(List<String> list, int page, int size) {
		if (page <= 0 || page * size - (size - 1) > list.size()) return null;

		List<String> contents = new ArrayList<>();
		for (int i = (page - 1) * size; i < page * size; i++) {
			contents.add(list.get(i));

			if (list.size() == (i + 1)) {
				break;
			}
		}
		return contents;
	}

	private List<String> getCmds(CommandSender sender) {
		List<String> c = new ArrayList<>();
		for (String cmds : new String[] { "help", "reload", "toggle", "broadcast", "list", "add", "remove", "clearall",
				"bannedplayers" }) {
			if (!sender.hasPermission("automessager." + cmds)) {
				continue;
			}

			c.add(cmds);
		}
		return c;
	}

	private String replColor(String s) {
		return s.replace("&", "\u00a7");
	}
}