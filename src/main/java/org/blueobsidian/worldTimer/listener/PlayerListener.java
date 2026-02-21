package org.blueobsidian.worldTimer.listener;

import org.blueobsidian.worldTimer.WorldTimer;
import org.blueobsidian.worldTimer.config.PluginConfig;
import org.blueobsidian.worldTimer.config.WorldConfig;
import org.blueobsidian.worldTimer.storage.PlayerTimerData;
import org.blueobsidian.worldTimer.storage.StorageManager;
import org.blueobsidian.worldTimer.timer.TimerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {

    private final WorldTimer plugin;
    private final TimerManager timerManager;
    private final StorageManager storage;

    public PlayerListener(WorldTimer plugin, TimerManager timerManager, StorageManager storage) {
        this.plugin = plugin;
        this.timerManager = timerManager;
        this.storage = storage;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PluginConfig config = plugin.getPluginConfig();

        // Delay by 1 tick to ensure the player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            String currentWorld = player.getWorld().getName();

            // Check if last known world was a limited world
            String lastWorld = storage.loadLastWorld(player.getUniqueId());
            if (lastWorld != null && config.isWorldConfigured(lastWorld)) {
                WorldConfig wc = config.getWorldConfig(lastWorld);
                if (wc != null && !player.hasPermission(wc.getBypassPermission())) {
                    PlayerTimerData data = storage.loadData(player.getUniqueId(), lastWorld);

                    // Check if timer expired or cooldown active
                    long limit = plugin.getPermissionResolver().resolveLimit(player, wc);
                    boolean expired = limit != -1 && data.getSecondsUsed() >= limit;

                    if (expired || data.isCooldownActive()) {
                        timerManager.teleportToMainWorld(player, wc);
                        return;
                    }
                }
            }

            // If current world is configured, start tracking
            if (config.isWorldConfigured(currentWorld)) {
                WorldConfig wc = config.getWorldConfig(currentWorld);
                if (wc != null) {
                    timerManager.handleWorldEntry(player, wc);
                }
            }

            // Save current world
            storage.saveLastWorld(player.getUniqueId(), currentWorld);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        PluginConfig config = plugin.getPluginConfig();

        // Save last world
        storage.saveLastWorld(player.getUniqueId(), worldName);

        // If in a configured world, save and stop tracking
        if (config.isWorldConfigured(worldName)) {
            timerManager.handleWorldExit(player, worldName);

            // Best effort: teleport to main world before they fully disconnect
            WorldConfig wc = config.getWorldConfig(worldName);
            if (wc != null && !player.hasPermission(wc.getBypassPermission())) {
                timerManager.teleportToMainWorld(player, wc);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PluginConfig config = plugin.getPluginConfig();

        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();

        // Stop tracking old world if it was configured
        if (config.isWorldConfigured(fromWorld)) {
            timerManager.handleWorldExit(player, fromWorld);
        }

        // Start tracking new world if configured
        if (config.isWorldConfigured(toWorld)) {
            WorldConfig wc = config.getWorldConfig(toWorld);
            if (wc != null) {
                timerManager.handleWorldEntry(player, wc);
                // If entry was blocked, player was teleported away already
            }
        }

        // Update last known world
        storage.saveLastWorld(player.getUniqueId(), toWorld);
    }

    /**
     * Handles cross-world teleports into a limited world:
     * 1. Blocks the teleport if the player is on cooldown.
     * 2. Saves the player's current location as their return point before entry.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() == null || event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }

        String fromWorld = event.getFrom().getWorld().getName();
        String toWorld = event.getTo().getWorld().getName();

        if (fromWorld.equalsIgnoreCase(toWorld)) {
            return;
        }

        Player player = event.getPlayer();
        PluginConfig config = plugin.getPluginConfig();

        if (!config.isWorldConfigured(toWorld)) {
            return;
        }

        WorldConfig wc = config.getWorldConfig(toWorld);
        if (wc == null || player.hasPermission(wc.getBypassPermission())) {
            return;
        }

        // Block entry if cooldown is active
        PlayerTimerData data = storage.loadData(player.getUniqueId(), toWorld);
        if (data.isCooldownActive()) {
            event.setCancelled(true);
            String cooldownStr = org.blueobsidian.worldTimer.util.TimeUtil.formatTime(data.getCooldownRemainingSeconds());
            player.sendMessage(config.getMessageConfig().format(
                    config.getMessageConfig().getCooldown(),
                    toWorld, null, cooldownStr, player.getName()
            ));
            return;
        }

        // Save the player's current location as their return point
        storage.saveReturnLocation(player.getUniqueId(), event.getFrom());
    }
}
