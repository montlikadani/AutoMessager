package hu.montlikadani.AutoMessager.bukkit;

public enum Perm {
	HELP("automessager.help"),
	RELOAD("automessager.reload"),
	TOGGLE("automessager.toggle"),
	BC("automessager.broadcast"),
	LIST("automessager.list"),
	SEEMSG("automessager.seemsg"),
	ADD("automessager.add"),
	REMOVE("automessager.remove"),
	CLEARALL("automessager.clearall"),
	BLACKLISTEDPLAYERS("automessager.blacklistedplayers"),
	BPADD("automessager.bannedplayers.add"),
	BPREMOVE("automessager.bannedplayers.remove"),
	BPLIST("automessager.bannedplayers.list");

	private String perm;

	Perm(String perm) {
		this.perm = perm;
	}

	public String getPerm() {
		return perm;
	}
}