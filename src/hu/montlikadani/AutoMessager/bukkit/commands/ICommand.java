package hu.montlikadani.AutoMessager.bukkit.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;

public interface ICommand {

	boolean run(final AutoMessager plugin, final CommandSender sender, final Command cmd, final String label, final String[] args);

}
