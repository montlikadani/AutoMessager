package hu.montlikadani.AutoMessager.bukkit.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;

public interface ICommand {

	boolean run(final AutoMessager plugin, final CommandSender sender, final Command cmd, final String label, final String[] args);

	default boolean hasPerm(CommandSender sender, String permission) {
		return !(sender instanceof Player) || sender.hasPermission(permission);
	}
}
