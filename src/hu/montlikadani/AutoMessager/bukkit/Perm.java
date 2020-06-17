package hu.montlikadani.AutoMessager.bukkit;

public enum Perm {
	HELP("help"),
	RELOAD("reload"),
	TOGGLE("toggle"),
	BC("broadcast"),
	LIST("list"),
	SEEMSG("seemsg"),
	ADD("add"),
	REMOVE("remove"),
	CLEARALL("clearall"),
	RESTRICTEDPLAYERS("restrictedplayers"),
	RESTRICTEDADD("restrictedplayers.add"),
	RESTRICTEDREMOVE("restrictedplayers.remove"),
	RESTRICTEDLIST("restrictedplayers.list");

	private String perm;

	Perm(String perm) {
		this.perm = "automessager." + perm;
	}

	public String getPerm() {
		return perm;
	}
}