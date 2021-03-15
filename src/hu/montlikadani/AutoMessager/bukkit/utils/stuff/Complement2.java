package hu.montlikadani.AutoMessager.bukkit.utils.stuff;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

public class Complement2 implements Complement {

	private String serialize(Component component) {
		return PlainComponentSerializer.plain().serialize(component);
	}

	@Override
	public String getMotd() {
		return serialize(Bukkit.getServer().motd());
	}

	@Override
	public void sendMessage(Player player, String json) {
		player.sendMessage(GsonComponentSerializer.gson().deserialize(json));
	}

	@Override
	public String getDisplayName(Player player) {
		return serialize(player.displayName());
	}
}
