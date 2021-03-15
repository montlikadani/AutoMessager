package hu.montlikadani.automessager.bukkit.announce;

import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.automessager.bukkit.AutoMessager;
import hu.montlikadani.automessager.bukkit.announce.message.Message;

public interface BaseAnnounce {

	default List<Message> getMessageList() {
		return JavaPlugin.getPlugin(AutoMessager.class).getFileHandler().getTexts();
	}
}
