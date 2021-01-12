package hu.montlikadani.AutoMessager.bukkit.announce.message.actionNameType;

import hu.montlikadani.AutoMessager.bukkit.announce.message.actionNameType.ActionName.ActionNameType;

public class ActionNameCleaner {

	private final CleanedName empty = new CleanedName("", "");

	public CleanedName clean(String name, ActionNameType type) {
		if (name == null || type == ActionNameType.WITHOUT) {
			return empty;
		}

		if (type == ActionNameType.TIME && name.startsWith("[time:")) {
			String customTime = name.split("]")[0].replace("[time:", "");
			if (!customTime.contains(":")) {
				return empty;
			}

			String[] times = customTime.split(":");
			if (times.length != 3) {
				return empty;
			}

			return new CleanedName(name, times,
					name.replace("[time:" + times[0] + ":" + times[1] + ":" + times[2] + "]", ""));
		} else if (type == ActionNameType.JSON) {
			return new CleanedName(name, name.replace("json:", ""));
		}

		String action = name.split("_")[0].replaceAll(type.getTypeName().toLowerCase() + ":|_", "");
		return new CleanedName(name, name.replace(type.getTypeName().toLowerCase() + ":" + action + "_", ""), action);
	}

	public static class CleanedName {

		private String originalState, secondaryResult = "";
		private Object result;

		public CleanedName(String originalState, Object result) {
			this.originalState = originalState;
			this.result = result;
		}

		public CleanedName(String originalState, Object result, String secondaryResult) {
			this.originalState = originalState;
			this.result = result;
			this.secondaryResult = secondaryResult;
		}

		public String getOriginalState() {
			return originalState;
		}

		public String getSecondaryResult() {
			return secondaryResult;
		}

		@SuppressWarnings("unchecked")
		public <T> T getResult() {
			return (T) result;
		}
	}
}
