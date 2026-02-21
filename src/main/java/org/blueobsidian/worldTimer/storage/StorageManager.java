package org.blueobsidian.worldTimer.storage;

import org.bukkit.Location;
import java.util.UUID;

public interface StorageManager {

    /**
     * Initialize the storage backend (create tables, etc.).
     */
    void init();

    /**
     * Shut down the storage backend (close connections, etc.).
     */
    void shutdown();

    /**
     * Load timer data for a player in a specific world.
     * Returns a new default entry if none exists.
     */
    PlayerTimerData loadData(UUID playerUuid, String worldName);

    /**
     * Save timer data for a player in a specific world.
     */
    void saveData(PlayerTimerData data);

    /**
     * Reset timer data (seconds used and cooldown) for a player in a specific world.
     */
    void resetData(UUID playerUuid, String worldName);

    /**
     * Save the last known world for a player.
     */
    void saveLastWorld(UUID playerUuid, String worldName);

    /**
     * Load the last known world for a player.
     * Returns null if not stored.
     */
    String loadLastWorld(UUID playerUuid);

    /**
     * Save the return location for a player entering a limited world.
     * This is the location they were at before entering, used to teleport them back.
     */
    void saveReturnLocation(UUID playerUuid, Location location);

    /**
     * Load the saved return location for a player.
     * Returns null if none is stored.
     */
    Location loadReturnLocation(UUID playerUuid);

    /**
     * Clear the saved return location for a player.
     */
    void clearReturnLocation(UUID playerUuid);
}
