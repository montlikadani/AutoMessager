package hu.montlikadani.automessager.bukkit;

public enum Perm {
	HELP,
	RELOAD,
	TOGGLE,
	LIST,
	SEEMSG,
	ADD,
	REMOVE,
	CLEARALL,
	RESTRICTEDPLAYERS,
	FORCESEND,
	;

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