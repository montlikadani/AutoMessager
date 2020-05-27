package hu.montlikadani.AutoMessager.bukkit.commands.list;

import static hu.montlikadani.AutoMessager.bukkit.Util.getMsg;
import static hu.montlikadani.AutoMessager.bukkit.Util.sendMsg;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.Perm;
import hu.montlikadani.AutoMessager.bukkit.commands.Commands;
import hu.montlikadani.AutoMessager.bukkit.commands.ICommand;

public class toggle implements ICommand {

	@Override
	public boolean run(AutoMessager plugin, CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(Perm.TOGGLE.getPerm())) {
			sendMsg(sender, getMsg("no-permission", "%perm%", Perm.TOGGLE.getPerm()));
			return false;
		}

		if (!(sender instanceof Player) && args.length < 2) {
			sendMsg(sender, getMsg("toggle.console-usage", "%command%", label));
			return false;
		}

		if (args.length == 2) {
			if (args[1].equalsIgnoreCase("all")) {
				for (Player pl : Bukkit.getOnlinePlayers()) {
					UUID uuid = pl.getUniqueId();
					if (!Commands.ENABLED.containsKey(uuid)) {
						Commands.ENABLED.put(uuid, false);
					} else {
						Commands.ENABLED.remove(uuid);
					}
				}

				return true;
			}

			Player target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sendMsg(sender, getMsg("toggle.player-not-found", "%target%", args[1]));
				return false;
			}

			UUID uuid = target.getUniqueId();
			if (!Commands.ENABLED.containsKey(uuid)) {
				Commands.ENABLED.put(uuid, false);
				sendMsg(sender, getMsg("toggle.disabled"));
			} else {
				Commands.ENABLED.remove(uuid);
				sendMsg(sender, getMsg("toggle.enabled"));
			}

			return true;
		}

		Player p = (Player) sender;
		UUID uuid = p.getUniqueId();
		if (!Commands.ENABLED.containsKey(uuid)) {
			Commands.ENABLED.put(uuid, false);
			sendMsg(sender, getMsg("toggle.disabled"));
		} else {
			Commands.ENABLED.remove(uuid);
			sendMsg(sender, getMsg("toggle.enabled"));
		}

		return true;
	}
}
