package dev.melncat.identitytheft;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class IdentityTheftListener implements Listener {
	private final IdentityTheft plugin;
	
	public IdentityTheftListener(IdentityTheft plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void on(AsyncPlayerPreLoginEvent event) {
		if (IdentityManager.getInstance().hasChangedIdentity(event.getUniqueId())) {
			PlayerProfile newProfile = Bukkit.createProfile(IdentityManager.getInstance().getChangedIdentity(event.getUniqueId()));
			newProfile.complete(true);
			newProfile.setProperty(new ProfileProperty("it_real", event.getUniqueId().toString()));
			event.setPlayerProfile(newProfile);
		}
	}

	/**
	 * If the target identity is banned, allow the login anyway since the
	 * player is using identity theft and their real account is not banned.
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onPreLoginBanBypass(AsyncPlayerPreLoginEvent event) {
		if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.KICK_BANNED
				&& event.getPlayerProfile().hasProperty("it_real")) {
			event.allow();
		}
	}

	/**
	 * Second ban-bypass check at the synchronous login stage, in case
	 * the server re-checks the ban based on the changed profile name.
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onLoginBanBypass(PlayerLoginEvent event) {
		if (event.getResult() == PlayerLoginEvent.Result.KICK_BANNED
				&& event.getPlayer().getPlayerProfile().hasProperty("it_real")) {
			event.allow();
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	private void on(PlayerKickEvent event) {
		if (event.getCause() == PlayerKickEvent.Cause.INVALID_PUBLIC_KEY_SIGNATURE
				&& event.getPlayer().getPlayerProfile().hasProperty("it_real")) {
			event.setCancelled(true);
		}
	}
}
