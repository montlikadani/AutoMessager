package hu.montlikadani.AutoMessager.bukkit.utils.stuff;

import org.bukkit.entity.Player;

public interface Complement {

	String getMotd();

	String getDisplayName(Player player);

	void sendMessage(Player player, String json);

}
