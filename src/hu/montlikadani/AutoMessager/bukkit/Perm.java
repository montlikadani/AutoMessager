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
	BLADD("automessager.blacklistedplayers.add"),
	BLREMOVE("automessager.blacklistedplayers.remove"),
	BLLIST("automessager.blacklistedplayers.list");

	private String perm;

	Perm(String perm) {
		this.perm = perm;
	}

	public String getPerm() {
		return perm;
	}
}