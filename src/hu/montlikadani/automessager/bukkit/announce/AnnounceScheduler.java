package hu.montlikadani.automessager.bukkit.announce;

import org.bukkit.Bukkit;

import hu.montlikadani.automessager.Global;
import hu.montlikadani.automessager.bukkit.announce.message.Message;
import hu.montlikadani.automessager.bukkit.announce.message.actionNameType.ActionName.ActionNameType;

public final class AnnounceScheduler implements Runnable {

	private Announce announce;

	public AnnounceScheduler(Announce announce) {
		this.announce = announce;
	}

	public void prepare() {
		java.util.List<Message> list = announce.getMessageList();

		if (announce.lastMessage != list.size()) {
			announce.lastMessage = list.size();
		}

		int next = getNextMessage();
		Message message = list.get(next);

		// skip time variable
		if (message.getType() == ActionNameType.TIME) {
			prepare();
			return;
		}

		if (announce.isRandom()) {
			announce.lastRandom = next;
		}

		Bukkit.getOnlinePlayers().forEach(player -> message.sendTo(player, false));
		message.logToConsole();
	}

	private int getNextMessage() {
		if (announce.isRandom()) {
			int r = Global.getRandomInt(0, announce.lastMessage - 1);
			while (r == announce.lastRandom) {
				r = Global.getRandomInt(0, announce.lastMessage - 1);
			}

			return r;
		}

		int nm = announce.messageCounter + 1;
		if (nm >= announce.lastMessage) {
			return announce.messageCounter = 0;
		}

		++announce.messageCounter;
		return nm;
	}

	@Override
	public void run() {
		if (Bukkit.getOnlinePlayers().isEmpty()) {
			announce.cancelSchedulers();
			return;
		}

		if (announce.haveEnoughOnlinePlayers()) {
			prepare();
		}
	}
}
