package hu.montlikadani.AutoMessager.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import ca.stellardrift.permissionsex.bukkit.PermissionsExPlugin;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PluginUtils {

	protected static boolean hasPermission(Player player, String perm) {
		if (AutoMessager.getInstance().isPluginEnabled("PermissionsEx")) {
			try {
				return PermissionsEx.getPermissionManager().has(player, perm);
			} catch (Exception e) {
				return JavaPlugin.getPlugin(PermissionsExPlugin.class).getUserSubjects()
						.get(player.getUniqueId().toString()).thenAccept(u -> u.hasPermission(perm))
						.completeExceptionally(e.getCause());
			}
		}

		return player.hasPermission(perm);
	}
}
