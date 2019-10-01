package hu.montlikadani.AutoMessager.bukkit;

import org.bukkit.World;

public class Worlds {

	private World world;

	public Worlds(World world) {
		this.world = world;
	}

	public World getWorld() {
		return world;
	}
}