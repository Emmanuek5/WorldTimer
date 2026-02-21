package org.blueobsidian.worldTimer.timer;

import org.blueobsidian.worldTimer.WorldTimer;
import org.blueobsidian.worldTimer.config.MessageConfig;
import org.blueobsidian.worldTimer.config.WorldConfig;
import org.blueobsidian.worldTimer.permission.PermissionResolver;
import org.blueobsidian.worldTimer.storage.PlayerTimerData;
import org.blueobsidian.worldTimer.storage.StorageManager;
import org.blueobsidian.worldTimer.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TimerManager {

    private final WorldTimer plugin;
    private final StorageManager storage;
    private final PermissionResolver permissionResolver;

    // Active session cache: key = "uuid:world"
    private final Map<String, PlayerTimerData> activeTimers = new ConcurrentHashMap<>();
    // Cached resolved limits: key = "uuid:world", value = limit in seconds (-1 = unlimited)
    private final Map<String, Long> resolvedLimits = new ConcurrentHashMap<>();

    public TimerManager(WorldTimer plugin, StorageManager storage, PermissionResolver permissionResolver) {
        this.plugin = plugin;
        this.storage = storage;
        this.permissionResolver = permissionResolver;
    }

    /**
     * Called when a player enters a configured world.
     * Returns false if entry should be blocked (cooldown active).
     */
    public boolean handleWorldEntry(Player player, WorldConfig worldConfig) {
        UUID uuid = player.getUniqueId();
        String world = worldConfig.getWorldName().toLowerCase();
        String key = uuid + ":" + world;
        MessageConfig msg = plugin.getPluginConfig().getMessageConfig();

        // Check bypass
        if (player.hasPermission(worldConfig.getBypassPermission())) {
            return true;
        }

        // Load data from storage
        PlayerTimerData data = storage.loadData(uuid, world);

        // Check cooldown
        if (data.isCooldownActive()) {
            String cooldownStr = TimeUtil.formatTime(data.getCooldownRemainingSeconds());
            player.sendMessage(msg.format(msg.getCooldown(), world, null, cooldownStr, player.getName()));
            teleportToMainWorld(player, worldConfig);
            return false;
        }

        // Resolve time limit
        long limit = permissionResolver.resolveLimit(player, worldConfig);
        resolvedLimits.put(key, limit);

        // If unlimited, no tracking needed but cache for reference
        activeTimers.put(key, data);

        // Send entry notification
        if (limit == -1) {
            player.sendMessage(msg.format(msg.getEnterUnlimited(), world, null, null, player.getName()));
        } else {
            long remaining = Math.max(0, limit - data.getSecondsUsed());
            String timeStr = TimeUtil.formatTime(remaining);
            player.sendMessage(msg.format(msg.getEnter(), world, timeStr, null, player.getName()));
        }

        return true;
    }

    /**
     * Called when a player leaves a configured world (world change or disconnect).
     */
    public void handleWorldExit(Player player, String worldName) {
        UUID uuid = player.getUniqueId();
        String key = uuid + ":" + worldName.toLowerCase();

        PlayerTimerData data = activeTimers.remove(key);
        resolvedLimits.remove(key);

        if (data != null) {
            storage.saveData(data);
        }
    }

    /**
     * Called every tick interval. Increments time for all active players.
     */
    public void tick(int tickSeconds) {
        MessageConfig msg = plugin.getPluginConfig().getMessageConfig();

        for (Map.Entry<String, PlayerTimerData> entry : activeTimers.entrySet()) {
            String key = entry.getKey();
            PlayerTimerData data = entry.getValue();

            Long limit = resolvedLimits.get(key);
            if (limit == null || limit == -1) {
                continue; // unlimited or no data
            }

            UUID uuid = data.getPlayerUuid();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }

            String worldName = data.getWorldName();
            WorldConfig worldConfig = plugin.getPluginConfig().getWorldConfig(worldName);
            if (worldConfig == null) continue;

            // Verify player is still in this world
            if (!player.getWorld().getName().equalsIgnoreCase(worldName)) {
                continue;
            }

            // Increment time
            data.addSeconds(tickSeconds);

            long remaining = limit - data.getSecondsUsed();

            // Check warnings
            for (long warnAt : worldConfig.getWarnAtSeconds()) {
                // Fire warning if we just crossed this threshold
                long previousRemaining = remaining + tickSeconds;
                if (previousRemaining > warnAt && remaining <= warnAt && remaining > 0) {
                    String timeStr = TimeUtil.formatTime(remaining);
                    player.sendMessage(msg.format(msg.getWarn(), worldName, timeStr, null, player.getName()));
                }
            }

            // Check expiry
            if (remaining <= 0) {
                // Time expired
                player.sendMessage(msg.format(msg.getExpired(), worldName, null, null, player.getName()));

                // Execute expire commands
                for (String cmd : worldConfig.getExpireCommands()) {
                    String parsed = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                }

                // Start cooldown
                long cooldownSeconds = permissionResolver.resolveCooldown(player, worldConfig);
                if (cooldownSeconds > 0) {
                    data.setCooldownUntil((System.currentTimeMillis() / 1000) + cooldownSeconds);
                }

                // Save and remove from active
                storage.saveData(data);
                activeTimers.remove(key);
                resolvedLimits.remove(key);

                // Teleport to main world
                teleportToMainWorld(player, worldConfig);
            }
        }
    }

    /**
     * Saves all active timer data to storage. Called on shutdown.
     */
    public void saveAll() {
        for (PlayerTimerData data : activeTimers.values()) {
            storage.saveData(data);
        }
        activeTimers.clear();
        resolvedLimits.clear();
    }

    /**
     * Gets the active timer data for a player in a world, or loads from storage.
     */
    public PlayerTimerData getData(UUID uuid, String worldName) {
        String key = uuid + ":" + worldName.toLowerCase();
        PlayerTimerData data = activeTimers.get(key);
        if (data != null) {
            return data;
        }
        return storage.loadData(uuid, worldName);
    }

    /**
     * Gets the resolved limit for a player in a world.
     * Returns null if not cached (player not in world).
     */
    public Long getResolvedLimit(UUID uuid, String worldName) {
        return resolvedLimits.get(uuid + ":" + worldName.toLowerCase());
    }

    /**
     * Adds time to a player's allowance (reduces seconds used).
     */
    public void addTime(UUID uuid, String worldName, long seconds) {
        String key = uuid + ":" + worldName.toLowerCase();
        PlayerTimerData data = activeTimers.get(key);
        if (data == null) {
            data = storage.loadData(uuid, worldName);
        }
        data.setSecondsUsed(Math.max(0, data.getSecondsUsed() - seconds));
        storage.saveData(data);
        if (activeTimers.containsKey(key)) {
            activeTimers.put(key, data);
        }
    }

    /**
     * Resets a player's timer and cooldown for a world.
     */
    public void resetPlayer(UUID uuid, String worldName) {
        String key = uuid + ":" + worldName.toLowerCase();
        activeTimers.remove(key);
        resolvedLimits.remove(key);
        storage.resetData(uuid, worldName);
    }

    /**
     * Sets the cooldown for a player in a world.
     */
    public void setCooldown(UUID uuid, String worldName, long seconds) {
        String key = uuid + ":" + worldName.toLowerCase();
        PlayerTimerData data = activeTimers.get(key);
        if (data == null) {
            data = storage.loadData(uuid, worldName);
        }
        if (seconds <= 0) {
            data.setCooldownUntil(0);
        } else {
            data.setCooldownUntil((System.currentTimeMillis() / 1000) + seconds);
        }
        storage.saveData(data);
        if (activeTimers.containsKey(key)) {
            activeTimers.put(key, data);
        }
    }

    /**
     * Teleports a player back to their saved pre-entry location.
     * Falls back to the main world spawn if no return location is stored.
     */
    public void teleportToMainWorld(Player player, WorldConfig worldConfig) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location returnLoc = storage.loadReturnLocation(player.getUniqueId());
            if (returnLoc != null && returnLoc.getWorld() != null) {
                player.teleport(returnLoc);
                storage.clearReturnLocation(player.getUniqueId());
            } else {
                World mainWorld = Bukkit.getWorld(worldConfig.getMainWorld());
                if (mainWorld != null) {
                    player.teleport(mainWorld.getSpawnLocation());
                } else {
                    plugin.getLogger().warning("Main world '" + worldConfig.getMainWorld() + "' not found for teleport!");
                }
            }
        });
    }

    public Map<String, PlayerTimerData> getActiveTimers() {
        return activeTimers;
    }
}
