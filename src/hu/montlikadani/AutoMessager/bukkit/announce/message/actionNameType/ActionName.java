package hu.montlikadani.automessager.bukkit.announce.message.actionNameType;

import org.bukkit.entity.Player;

public interface ActionName {

	String getText();

	ActionNameType getType();

	void setType(ActionNameType type);

	void sendTo(Player player, boolean ignoreConditions);

	ActionNameCleaner getCleaner();

	public enum ActionNameType {
		TIME, WORLD, JSON, PLAYER, GROUP, PERMISSION, WITHOUT;

		private String typeName = toString().toLowerCase();

		ActionNameType() {
		}

		ActionNameType(String typeName) {
			this.typeName = typeName;
		}

		public static ActionNameType getType(String name) {
			for (ActionNameType t : ActionNameType.values()) {
				if (t.name().equalsIgnoreCase(name) || t.typeName.equalsIgnoreCase(name)) {
					return t;
				}
			}

			return ActionNameType.WITHOUT;
		}

		public String getTypeName() {
			return typeName;
		}
	}
}
