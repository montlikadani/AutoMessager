package hu.montlikadani.AutoMessager.bukkit.announce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

import hu.montlikadani.AutoMessager.bukkit.announce.AnnounceTime.TimeType;
import hu.montlikadani.AutoMessager.bukkit.announce.message.Message;
import hu.montlikadani.AutoMessager.bukkit.announce.message.actionNameType.ActionName.ActionNameType;
import hu.montlikadani.AutoMessager.bukkit.announce.message.actionNameType.ActionNameCleaner.CleanedName;
import hu.montlikadani.AutoMessager.bukkit.config.ConfigConstants;
import hu.montlikadani.AutoMessager.bukkit.utils.Util;

public final class Announce extends IAnnounce {

	private boolean random = false;
	private long time = 0;

	protected int messageCounter, lastMessage, lastRandom;

	private final AnnounceTime announceTime;
	private final List<Executor> schedulers = new ArrayList<>();

	public Announce() {
		announceTime = new AnnounceTime(ConfigConstants.getTime());
		time = announceTime.countTimer();
	}

	@Override
	public void beginScheduling() {
		if (!schedulers.isEmpty() && !this.<ScheduledExecutorService>getSchedulers().get(0).isShutdown()) {
			return;
		}

		cancelSchedulers();

		if (!ConfigConstants.isBroadcastEnabled() || Bukkit.getOnlinePlayers().isEmpty()) {
			return;
		}

		reset();

		for (Message msg : getMessageList()) {
			msg.setTypeFromText();

			if (msg.getType() != ActionNameType.TIME) {
				continue;
			}

			CleanedName cleaned = msg.getCleaner().clean(msg.getText(), msg.getType());
			if (cleaned.getOriginalState().isEmpty()) {
				continue;
			}

			String[] result = cleaned.<String[]>getResult();

			int hour = Integer.parseInt(result[0]), minutes = Integer.parseInt(result[1]),
					seconds = Integer.parseInt(result[2]);

			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);
			scheduler.scheduleAtFixedRate(() -> {
				if (Bukkit.getOnlinePlayers().isEmpty()) {
					cancelSchedulers();
					return;
				}

				if (haveEnoughOnlinePlayers()) {
					Bukkit.getOnlinePlayers().forEach(msg::sendTo);
					msg.logToConsole();
				}
			}, Util.calcNextDelay(hour, minutes, seconds), TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);

			schedulers.add(scheduler);
		}

		if (announceTime.getTimeType() == TimeType.GIVEN || announceTime.getTimeType() == TimeType.CUSTOM) {
			if (!announceTime.getTime().contains(":")) {
				return;
			}

			final String[] times = announceTime.getTime().split(":");
			if (times.length != 3) {
				return;
			}

			final int hour = Integer.parseInt(times[0]), minutes = Integer.parseInt(times[1]),
					seconds = Integer.parseInt(times[2]);

			final AnnounceScheduler announceScheduler = new AnnounceScheduler(this);
			final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);

			if (announceTime.getTimeType() == TimeType.CUSTOM) {
				if (time <= 0L) {
					return;
				}

				scheduler.scheduleAtFixedRate(() -> {
					if (Bukkit.getOnlinePlayers().isEmpty()) {
						cancelSchedulers();
						return;
					}

					if (haveEnoughOnlinePlayers()) {
						announceScheduler.prepare();
					}
				}, time, time, TimeUnit.SECONDS);
			} else {
				scheduler.scheduleAtFixedRate(() -> {
					if (Bukkit.getOnlinePlayers().isEmpty()) {
						cancelSchedulers();
						return;
					}

					if (haveEnoughOnlinePlayers()) {
						announceScheduler.prepare();
					}
				}, Util.calcNextDelay(hour, minutes, seconds), TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
			}

			schedulers.add(scheduler);
			return;
		}

		if (time <= 0L) {
			return;
		}

		final AnnounceScheduler announceScheduler = new AnnounceScheduler(this);
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0);
		scheduler.scheduleAtFixedRate(announceScheduler, time, time, announceTime.getTimeType().asTimeUnit());
		schedulers.add(scheduler);
	}

	@Override
	public AnnounceTime getAnnounceTime() {
		return announceTime;
	}

	@Override
	public boolean haveEnoughOnlinePlayers() {
		int min = ConfigConstants.getMinPlayers();
		return min > 0 && Bukkit.getServer().getOnlinePlayers().size() >= min;
	}

	@Override
	public void cancelSchedulers() {
		this.<ScheduledExecutorService>getSchedulers().forEach(ScheduledExecutorService::shutdown);
		schedulers.clear();
	}

	@Override
	public void reset() {
		messageCounter = -1;
		random = ConfigConstants.isRandom() && (lastMessage = getMessageList().size()) > 2;
	}

	@Override
	public boolean isRandom() {
		return random;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Executor> List<T> getSchedulers() {
		return (List<T>) schedulers;
	}
}
