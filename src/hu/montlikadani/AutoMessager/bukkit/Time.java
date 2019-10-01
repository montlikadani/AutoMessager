package hu.montlikadani.AutoMessager.bukkit;

public class Time {

	private AutoMessager plugin;
	private int time;

	public Time(AutoMessager plugin, int time) {
		this.plugin = plugin;
		this.time = time;

		if (time > 0) {
			countTimer();
		}
	}

	private final void countTimer() {
		String s = plugin.getConfig().getString("time-setup", "");
		if (!s.equals("")) {
			switch (s.toLowerCase()) {
			case "tick":
				time *= 1;
				break;
			case "sec":
			case "second":
				time *= 20;
				break;
			case "min":
			case "minute":
				time *= 1200;
				break;
			case "h":
			case "hour":
				time *= 72000;
				break;
			default:
				time *= 20;
				break;
			}
		} else {
			time *= 20;
		}
	}

	public int getTime() {
		return time;
	}
}