package me.NoChance.PvPManager.Listeners;

import me.NoChance.PvPManager.PvPManager;
import me.NoChance.PvPManager.PvPlayer;
import me.NoChance.PvPManager.Config.Messages;
import me.NoChance.PvPManager.Config.Variables;
import me.NoChance.PvPManager.Managers.PlayerHandler;
import me.NoChance.PvPManager.Utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class PlayerListener implements Listener {

	private PvPManager plugin;
	private PlayerHandler ph;

	public PlayerListener(PvPManager plugin) {
		this.plugin = plugin;
		this.ph = plugin.getPlayerHandler();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogout(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PvPlayer pvPlayer = ph.get(player);
		if (pvPlayer.isInCombat()) {
			if (Variables.broadcastPvpLog)
				plugin.getServer().broadcastMessage(Messages.PvPLog_Broadcast.replace("%p", player.getName()));
			if (Variables.punishmentsEnabled)
				ph.applyPunishments(player);

			pvPlayer.setTagged(false);
		}
		ph.remove(pvPlayer);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		PvPlayer pvPlayer = ph.get(player);
		if (pvPlayer.hasPvPLogged() && !Variables.dropExp) {
			event.setKeepLevel(true);
			event.setDroppedExp(0);
		}
		if (pvPlayer.isInCombat())
			pvPlayer.setTagged(false);
		if (Variables.killAbuseEnabled && player.getKiller() != null && !player.getKiller().hasMetadata("NPC")) {
			PvPlayer killer = ph.get(player.getKiller());
			killer.addVictim(player.getName());
		}
		if (Variables.toggleOffOnDeath && player.hasPermission("pvpmanager.pvpstatus.change") && pvPlayer.hasPvPEnabled())
			pvPlayer.setPvP(false);

	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		if (Utils.PMAllowed(player.getWorld().getName()) && Variables.autoSoupEnabled) {
			if (player.getHealth() == player.getMaxHealth())
				return;
			if (player.getItemInHand().getType() == Material.MUSHROOM_SOUP) {
				player.setHealth(player.getHealth() + Variables.soupHealth > player.getMaxHealth() ? player.getMaxHealth() : player
						.getHealth() + Variables.soupHealth);
				player.getItemInHand().setType(Material.BOWL);
				return;
			}
		}
	}

	@EventHandler
	public void onPlayerLogin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		ph.get(player);
		if (player.isOp() || player.hasPermission("pvpmanager.admin")) {
			if (Variables.update)
				Messages.updateMessage(player);
			if (Variables.configUpdated)
				Messages.configUpdated(player);
		}
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		PvPlayer pvPlayer = ph.get(event.getPlayer());
		if (pvPlayer.isInCombat())
			pvPlayer.setTagged(false);
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.getCause().equals(TeleportCause.ENDER_PEARL)) {
			PvPlayer player = ph.get(event.getPlayer());
			if (Variables.inCombatEnabled && Variables.blockEnderPearl && player.isInCombat()) {
				player.message(Messages.EnderPearl_Blocked_InCombat);
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		if (Variables.stopCommands && Variables.inCombatEnabled) {
			if (plugin.getPlayerHandler().get(event.getPlayer()).isInCombat()) {
				if (!Variables.commandsAllowed.contains(event.getMessage().substring(1).split(" ")[0])) {
					event.setCancelled(true);
					event.getPlayer().sendMessage(Messages.Command_Denied_InCombat);
				}
			}
		}
	}
}
