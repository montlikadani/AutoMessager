package hu.montlikadani.AutoMessager.bungee;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import hu.montlikadani.AutoMessager.Global;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class Announce {

	private final AutoMessager plugin;

	private ScheduledTask task = null;

	private boolean random;

	private int messageCounter;
	private int lastMessage;
	private int lastRandom;
	private int warningCounter;

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
		warningCounter = 0;
		random = false;

		int cm = plugin.getTexts().size();
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

		task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
			if (warningCounter <= 4) {
				if (plugin.getTexts().size() < 1) {
					plugin.getLogger().log(Level.WARNING,
							"There is no message in '" + plugin.getConfig().getString("message-file") + "' file!");

					warningCounter++;

					if (warningCounter == 5) {
						plugin.getLogger().log(Level.WARNING,
								"Will stop outputing warnings now. Please write a message to the '"
										+ plugin.getConfig().getString("message-file") + "' file.");
					}

					return;
				}

				if (!plugin.checkOnlinePlayers()) {
					return;
				}

				for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
					if (plugin.getEnabledMessages().contains(p.getUniqueId())) {
						continue;
					}

					if (random) {
						onRandom(p);
					} else {
						onInOrder(p);
					}
				}
			}
		}, 0L, plugin.getConfig().getInt("time", 5),
				TimeUnit.valueOf(plugin.getConfig().getString("time-setup", "minutes").toUpperCase()));
	}

	public void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	private void onRandom(ProxiedPlayer p) {
		int nm = getNextMessage();
		String message = plugin.getTexts().get(nm);
		lastRandom = nm;
		send(p, message);
	}

	private void onInOrder(ProxiedPlayer p) {
		int nm = getNextMessage();
		String message = plugin.getTexts().get(nm);
		send(p, message);
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

	private void send(ProxiedPlayer p, String message) {
		if (message.isEmpty()) {
			return;
		}

		String msg = message;

		String server = "";
		if (message.startsWith("server:")) {
			msg = msg.replace("server:", "");

			String[] split = msg.split("_");

			server = split[0];
			msg = split[1];
		}

		String path = "placeholder-format.time.";
		if (!plugin.getConfig().getString(path + "title", "").isEmpty()) {
			msg = msg.replace("%title%", plugin.getConfig().getString(path + "title").replace("%newline%", "\n"));
		}

		if (!plugin.getConfig().getString(path + "suffix", "").isEmpty()) {
			msg = msg.replace("%suffix%", plugin.getConfig().getString(path + "suffix"));
		}

		msg = plugin.replaceVariables(msg, p);

		String plServer = p.getServer().getInfo().getName();

		if (server.isEmpty() && !plugin.getConfig().getStringList("disabled-servers").contains(plServer)) {
			plugin.sendMessage(p, msg);
		} else if ((server.equalsIgnoreCase(plServer)
				&& !plugin.getConfig().getStringList("disabled-servers").contains(server))) {
			plugin.sendMessage(p, msg);
		}

		if (plugin.getConfig().getBoolean("broadcast-to-console")) {
			plugin.sendMessage(plugin.getProxy().getConsole(), msg);
		}
	}
}