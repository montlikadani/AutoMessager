package hu.montlikadani.AutoMessager.bukkit.utils;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;

import hu.montlikadani.AutoMessager.bukkit.AutoMessager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PluginUtils {

	public static boolean hasPermission(Player player, String perm) {
		if (perm.isEmpty()) {
			return false;
		}

		if (AutoMessager.getInstance().isPluginEnabled("PermissionsEx")) {
			try {
				return PermissionsEx.getPermissionManager().has(player, perm);
			} catch (Exception e) {
			}
		}

		return player.isPermissionSet(perm) && player.hasPermission(perm);
	}

	public static boolean isAfk(Player p) {
		if (AutoMessager.getInstance().isPluginEnabled("Essentials")) {
			return JavaPlugin.getPlugin(Essentials.class).getUser(p).isAfk();
		}

		if (AutoMessager.getInstance().isPluginEnabled("CMI")) {
			CMIUser user = CMI.getInstance().getPlayerManager().getUser(p);
			return user != null && user.isVanished();
		}

		return false;
	}
}
