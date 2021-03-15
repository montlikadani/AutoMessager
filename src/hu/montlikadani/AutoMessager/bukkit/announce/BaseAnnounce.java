package hu.montlikadani.AutoMessager.bukkit.announce;

import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.announce.message.Message;

public interface BaseAnnounce {

	default List<Message> getMessageList() {
		return JavaPlugin.getPlugin(AutoMessager.class).getFileHandler().getTexts();
	}
}
