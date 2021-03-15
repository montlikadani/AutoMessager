package hu.montlikadani.AutoMessager.bukkit.announce;

import org.bukkit.Bukkit;

import hu.montlikadani.AutoMessager.Global;
import hu.montlikadani.AutoMessager.bukkit.announce.message.Message;
import hu.montlikadani.AutoMessager.bukkit.announce.message.actionNameType.ActionName.ActionNameType;

public final class AnnounceScheduler implements Runnable {

	private Announce announce;

	public AnnounceScheduler(Announce announce) {
		this.announce = announce;
	}

	public void prepare() {
		int size = announce.getMessageList().size();

		if (announce.lastMessage != size) {
			announce.lastMessage = size;
		}

		int next = getNextMessage();
		Message message = announce.getMessageList().get(next);

		// skip time variable
		if (message.getType() == ActionNameType.TIME) {
			prepare();
			return;
		}

		if (announce.isRandom()) {
			announce.lastRandom = next;
		}

		Bukkit.getOnlinePlayers().forEach(message::sendTo);
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
