package hu.montlikadani.automessager.bukkit.announce;

import java.util.List;
import java.util.concurrent.Executor;

public abstract class IAnnounce implements BaseAnnounce {

	public abstract void reset();

	public abstract void cancelSchedulers();

	public abstract boolean isRandom();

	public abstract boolean haveEnoughOnlinePlayers();

	public abstract AnnounceTime getAnnounceTime();

	public abstract <T extends Executor> List<T> getSchedulers();

	public abstract void beginScheduling();

}
