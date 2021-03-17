package hu.montlikadani.automessager.bukkit.utils.stuff;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.md_5.bungee.chat.ComponentSerializer;

@SuppressWarnings("deprecation")
public final class Complement1 implements Complement {

	@Override
	public String getMotd() {
		return Bukkit.getServer().getMotd();
	}

	@Override
	public void sendMessage(Player player, String json) {
		player.sendMessage(ComponentSerializer.parse(json));
	}

	@Override
	public String getDisplayName(Player player) {
		return player.getDisplayName();
	}
}
