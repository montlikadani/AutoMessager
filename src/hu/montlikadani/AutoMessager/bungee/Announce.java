package hu.montlikadani.automessager.bungee;

import java.util.concurrent.TimeUnit;

import hu.montlikadani.automessager.Global;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.chat.ComponentSerializer;

public class Announce {

	private final AutoMessager plugin;

	private ScheduledTask task;

	private boolean random;
	private int messageCounter, lastRandom;

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
		messageCounter = -1; // We need to start from -1, due to first line reading
		random = ConfigConstants.isRandom() && plugin.getMessageFileHandler().getTexts().size() > 2;
	}

	public void schedule() {
		if (!ConfigConstants.isEnableBroadcast() || task != null) {
			return;
		}

		final int time = ConfigConstants.getTime();

		task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
			if (!plugin.checkOnlinePlayers() || plugin.getMessageFileHandler().getTexts().isEmpty()) {
				return;
			}

			prepare();
		}, time, time, TimeUnit.valueOf(ConfigConstants.getTimeSetup()));

	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	private void prepare() {
		int nm = getNextMessage();

		if (random) {
			lastRandom = nm;
		}

		send(plugin.getMessageFileHandler().getTexts().get(nm));
	}

	int getNextMessage() {
		int size = plugin.getMessageFileHandler().getTexts().size();

		if (random) {
			int r = Global.getRandomInt(0, size - 1);
			while (r == lastRandom) {
				r = Global.getRandomInt(0, size - 1);
			}

			return r;
		}

		int nm = messageCounter + 1;
		if (nm >= size) {
			return messageCounter = 0;
		}

		++messageCounter;
		return nm;
	}

	private void send(final String message) {
		if (message.isEmpty()) {
			return;
		}

		for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
			if (plugin.getToggledPlayers().contains(p.getUniqueId())) {
				continue;
			}

			String msg = message, server = "",
					plServer = p.getServer() != null ? p.getServer().getInfo().getName() : "";

			if (msg.startsWith("server:")) {
				msg = msg.replace("server:", "");

				String[] split = msg.split("_");

				server = split[0];
				msg = split[1];
			} else if (msg.startsWith("json:")) {
				msg = msg.replace("json:", "");

				if (!ConfigConstants.getDisabledServers().contains(plServer)) {
					p.sendMessage(ComponentSerializer.parse(msg));
				}

				return;
			}

			msg = plugin.replaceVariables(msg, p);

			if (msg.startsWith("center:")) {
				msg = msg.replace("center:", "");

				int amount = 0;
				if (msg.contains("_")) {
					try {
						amount = Integer.parseInt(msg.split("_")[0]);
					} catch (NumberFormatException ex) {
					}

					msg = msg.replace(amount + "_", "");
				}

				if (amount > 0) {
					msg = Global.centerText(msg, amount);
				}
			}

			if ((server.isEmpty() && !ConfigConstants.getDisabledServers().contains(plServer))
					|| (server.equalsIgnoreCase(plServer) && !ConfigConstants.getDisabledServers().contains(server))) {
				plugin.sendMessage(p, msg);
			}
		}

		if (ConfigConstants.isBroadcastToConsole()) {
			plugin.sendMessage(plugin.getProxy().getConsole(), message);
		}
	}
}