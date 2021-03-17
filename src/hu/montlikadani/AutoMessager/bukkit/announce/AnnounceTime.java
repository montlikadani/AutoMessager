package hu.montlikadani.automessager.bukkit.announce;

import java.util.concurrent.TimeUnit;

import hu.montlikadani.automessager.bukkit.config.ConfigConstants;

public class AnnounceTime {

	private String time;

	private TimeType timeType = TimeType.SECOND;

	public AnnounceTime(String time) {
		setTime(time);
	}

	public void setTime(String time) {
		this.time = time == null ? "" : time;
	}

	public String getTime() {
		return time;
	}

	public TimeType getTimeType() {
		return timeType;
	}

	public long countTimer() {
		if (time.isEmpty()) {
			return 0L;
		}

		if ((timeType = TimeType.getType(ConfigConstants.getTimeSetup())) == TimeType.CUSTOM) {
			if (!time.contains(":")) {
				return 0L;
			}

			String[] split = time.split(":", 3);
			if (split.length == 0) {
				return 0L;
			}

			int hour = Integer.parseInt(split[0]), minute = split.length > 1 ? Integer.parseInt(split[1]) : 0,
					second = split.length > 2 ? Integer.parseInt(split[2]) : 0;

			long result = 0L;
			if (second > 0) {
				result = second;
			}

			if (minute > 0) {
				result += minute * 60L;
			}

			if (hour > 0) {
				result += hour * 60L;
			}

			return result;
		}

		return !time.contains(":") ? Long.parseLong(time) : 0L;
	}

	public enum TimeType {
		GIVEN("specified"), CUSTOM, TICKS, MINUTE("min"), HOUR("h"), SECOND("sec"), NOTHING;

		private String aliaseName = toString().toLowerCase();

		TimeType() {
		}

		TimeType(String aliaseName) {
			this.aliaseName = aliaseName;
		}

		public String getAliaseName() {
			return aliaseName;
		}

		public TimeUnit asTimeUnit() {
			switch (this) {
			case TICKS:
				return TimeUnit.MILLISECONDS;
			case MINUTE:
				return TimeUnit.MINUTES;
			case HOUR:
				return TimeUnit.HOURS;
			default:
				return TimeUnit.SECONDS;
			}
		}

		public static TimeType getType(String name) {
			if (name.trim().isEmpty()) {
				return TimeType.SECOND;
			}

			for (TimeType type : values()) {
				if (type.aliaseName.equalsIgnoreCase(name) || type.toString().equalsIgnoreCase(name)) {
					return type;
				}
			}

			return TimeType.SECOND;
		}
	}
}