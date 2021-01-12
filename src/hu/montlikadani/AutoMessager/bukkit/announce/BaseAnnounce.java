package hu.montlikadani.AutoMessager.bukkit.announce;

import java.util.List;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import hu.montlikadani.AutoMessager.bukkit.announce.message.Message;

public interface BaseAnnounce {

	default List<Message> getMessageList() {
		return getPlugin().getFileHandler().getTexts();
	}

	default AutoMessager getPlugin() {
		return AutoMessager.getInstance();
	}

}
