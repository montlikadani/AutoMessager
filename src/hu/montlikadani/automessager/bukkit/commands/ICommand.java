package hu.montlikadani.automessager.bukkit.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import hu.montlikadani.automessager.bukkit.AutoMessager;

public interface ICommand {

	boolean run(final AutoMessager plugin, final CommandSender sender, final Command cmd, final String label, final String[] args);

}
