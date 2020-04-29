package hu.montlikadani.AutoMessager.bukkit;

public class Time {

	private AutoMessager plugin;
	private String time;

	public Time(AutoMessager plugin, String time) {
		this.plugin = plugin;
		this.time = time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getTime() {
		return time;
	}

	public int countTimer() {
		int t = 0;

		if (!time.contains(":")) {
			t = Integer.parseInt(time);
			if (t < 1) {
				return t;
			}
		}

		String s = plugin.getConf().getConfig().getString("time-setup", "");
		if (s.trim().isEmpty()) {
			return t *= 20;
		}

		switch (s.toLowerCase()) {
		case "custom":
			if (!time.contains(":")) {
				return t *= 20;
			}

			String[] split = time.split(":");
			if (split.length < 1) {
				return t *= 20;
			}

			int hour = Integer.parseInt(split[0]);
			int minute = split.length > 1 ? Integer.parseInt(split[1]) : 0;
			int second = split.length > 2 ? Integer.parseInt(split[2]) : 0;

			// hehe
			if ((hour == 0) && (minute == 0)) {
				t = second * 20; // including default
			} else if ((hour == 0) && (minute != 0) && (second != 0)) {
				t = minute * 1200 + second * 20;
			} else if ((hour != 0) && (minute == 0) && (second != 0)) {
				t = hour * 72000 + second * 20;
			} else if ((hour != 0) && (minute == 0) && (second == 0)) {
				t = hour * 72000;
			} else if ((hour == 0) && (minute != 0) && (second == 0)) {
				t = minute * 1200;
			} else if ((hour != 0) && (minute != 0) && (second == 0)) {
				t = hour * 72000 + minute * 1200;
			} else if ((hour != 0) && (minute != 0) && (second != 0)) {
				t = hour * 72000 + minute * 1200 + second * 20;
			}

			return t;
		case "tick":
			return t *= 1;
		case "sec":
		case "second":
			return t *= 20;
		case "min":
		case "minute":
			return t *= 1200;
		case "h":
		case "hour":
			return t *= 72000;
		default:
			return t *= 20;
		}
	}
}