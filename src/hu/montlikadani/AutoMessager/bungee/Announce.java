package hu.montlikadani.AutoMessager.bungee;

import java.util.concurrent.TimeUnit;

import hu.montlikadani.AutoMessager.Global;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class Announce {

	private final AutoMessager plugin;

	private ScheduledTask task = null;

	private boolean random;

	private int messageCounter, lastMessage, lastRandom;

	public Announce(AutoMessager plugin) {
		this.plugin = plugin;
	}

	public boolean isRandom() {
		return random;
	}

	public ScheduledTask getTask() {
		return task;
	}

	public void load() {
		// We need to start from -1, due to first line reading
		messageCounter = -1;
		random = false;

		int cm = plugin.getMessageFileHandler().getTexts().size();
		if (plugin.getConfig().getBoolean("random") && cm > 2) {
			random = true;
		}

		lastMessage = cm;
	}

	public void schedule() {
		if (!plugin.getConfig().getBoolean("enable-broadcast")) {
			return;
		}

		if (task != null) {
			return;
		}

		final int time = plugin.getConfig().getInt("time", 5);
		final String timeSetup = plugin.getConfig().getString("time-setup", "minutes").toUpperCase();

		task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
			if (!plugin.checkOnlinePlayers() || plugin.getMessageFileHandler().getTexts().isEmpty()) {
				return;
			}

			int size = plugin.getMessageFileHandler().getTexts().size();
			if (lastMessage != size) {
				lastMessage = size;
			}

			prepare();
		}, time, time, TimeUnit.valueOf(timeSetup));

	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	private void prepare() {
		int nm = getNextMessage();
		String message = plugin.getMessageFileHandler().getTexts().get(nm);

		if (random) {
			lastRandom = nm;
		}

		send(message);
	}

	int getNextMessage() {
		if (random) {
			int r = Global.getRandomInt(0, lastMessage - 1);
			while (r == lastRandom) {
				r = Global.getRandomInt(0, lastMessage - 1);
			}

			return r;
		}

		int nm = (messageCounter + 1);
		if (nm >= lastMessage) {
			messageCounter = 0;
			return 0;
		}

		++messageCounter;
		return nm;
	}

	private void send(final String message) {
		if (message.isEmpty()) {
			return;
		}

		for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
			if (plugin.getEnabledMessages().contains(p.getUniqueId())) {
				continue;
			}

			String msg = message,
					server = "",
					plServer = p.getServer() != null ? p.getServer().getInfo().getName() : "";

			if (message.startsWith("server:")) {
				msg = msg.replace("server:", "");

				String[] split = msg.split("_");

				server = split[0];
				msg = split[1];
			}

			msg = plugin.replaceVariables(msg, p);

			if (server.isEmpty() && !plugin.getConfig().getStringList("disabled-servers").contains(plServer)) {
				plugin.sendMessage(p, msg);
			} else if ((server.equalsIgnoreCase(plServer)
					&& !plugin.getConfig().getStringList("disabled-servers").contains(server))) {
				plugin.sendMessage(p, msg);
			}
		}

		if (plugin.getConfig().getBoolean("broadcast-to-console")) {
			plugin.sendMessage(plugin.getProxy().getConsole(), message);
		}
	}
}