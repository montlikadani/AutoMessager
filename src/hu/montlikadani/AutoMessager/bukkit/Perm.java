package hu.montlikadani.AutoMessager.bukkit;

public enum Perm {
	HELP,
	RELOAD,
	TOGGLE,
	BC("broadcast"),
	LIST,
	SEEMSG,
	ADD,
	REMOVE,
	CLEARALL,
	RESTRICTEDPLAYERS,
	RESTRICTEDADD("restrictedplayers.add"),
	RESTRICTEDREMOVE("restrictedplayers.remove"),
	RESTRICTEDLIST("restrictedplayers.list");

	private String perm;

	Perm() {
		this("");
	}

	Perm(String perm) {
		this.perm = "automessager." + (perm.trim().isEmpty() ? toString().toLowerCase() : perm);
	}

	public String getPerm() {
		return perm;
	}
}