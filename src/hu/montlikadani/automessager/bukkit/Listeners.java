package hu.montlikadani.automessager.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import hu.montlikadani.automessager.bukkit.utils.UpdateDownloader;

public class Listeners implements Listener {

	private final AutoMessager plugin;

	public Listeners(AutoMessager plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlJoin(PlayerJoinEvent event) {
		plugin.getAnnounce().beginScheduling();

		if (event.getPlayer().isOp()) {
			UpdateDownloader.checkFromGithub(event.getPlayer());
		}
	}
}
