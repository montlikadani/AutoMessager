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

		private String typeName = super.toString().toLowerCase();

		@Override
		public String toString() {
			return typeName;
		}
	}
}
