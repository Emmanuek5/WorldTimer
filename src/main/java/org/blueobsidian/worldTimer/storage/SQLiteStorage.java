package org.blueobsidian.worldTimer.storage;

import org.blueobsidian.worldTimer.WorldTimer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteStorage implements StorageManager {

    private final WorldTimer plugin;
    private Connection connection;

    public SQLiteStorage(WorldTimer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String url = "jdbc:sqlite:" + new File(dataFolder, "data.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS timer_data (" +
                                "uuid TEXT NOT NULL, " +
                                "world TEXT NOT NULL, " +
                                "seconds_used BIGINT NOT NULL DEFAULT 0, " +
                                "cooldown_until BIGINT NOT NULL DEFAULT 0, " +
                                "PRIMARY KEY (uuid, world)" +
                                ")"
                );
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS player_worlds (" +
                                "uuid TEXT NOT NULL PRIMARY KEY, " +
                                "last_world TEXT NOT NULL" +
                                ")"
                );
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS return_locations (" +
                                "uuid TEXT NOT NULL PRIMARY KEY, " +
                                "world TEXT NOT NULL, " +
                                "x REAL NOT NULL, " +
                                "y REAL NOT NULL, " +
                                "z REAL NOT NULL, " +
                                "yaw REAL NOT NULL DEFAULT 0, " +
                                "pitch REAL NOT NULL DEFAULT 0" +
                                ")"
                );
            }
            plugin.getLogger().info("SQLite storage initialized.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite storage", e);
        }
    }

    @Override
    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to close SQLite connection", e);
            }
        }
    }

    @Override
    public PlayerTimerData loadData(UUID playerUuid, String worldName) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT seconds_used, cooldown_until FROM timer_data WHERE uuid = ? AND world = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, worldName.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new PlayerTimerData(
                        playerUuid,
                        worldName.toLowerCase(),
                        rs.getLong("seconds_used"),
                        rs.getLong("cooldown_until")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load timer data for " + playerUuid, e);
        }
        return new PlayerTimerData(playerUuid, worldName.toLowerCase(), 0, 0);
    }

    @Override
    public void saveData(PlayerTimerData data) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO timer_data (uuid, world, seconds_used, cooldown_until) VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT(uuid, world) DO UPDATE SET seconds_used = ?, cooldown_until = ?")) {
            ps.setString(1, data.getPlayerUuid().toString());
            ps.setString(2, data.getWorldName().toLowerCase());
            ps.setLong(3, data.getSecondsUsed());
            ps.setLong(4, data.getCooldownUntil());
            ps.setLong(5, data.getSecondsUsed());
            ps.setLong(6, data.getCooldownUntil());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save timer data for " + data.getPlayerUuid(), e);
        }
    }

    @Override
    public void resetData(UUID playerUuid, String worldName) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM timer_data WHERE uuid = ? AND world = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, worldName.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to reset timer data for " + playerUuid, e);
        }
    }

    @Override
    public void saveLastWorld(UUID playerUuid, String worldName) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_worlds (uuid, last_world) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET last_world = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, worldName);
            ps.setString(3, worldName);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save last world for " + playerUuid, e);
        }
    }

    @Override
    public String loadLastWorld(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT last_world FROM player_worlds WHERE uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("last_world");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load last world for " + playerUuid, e);
        }
        return null;
    }

    @Override
    public void saveReturnLocation(UUID playerUuid, Location location) {
        if (location == null || location.getWorld() == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO return_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, location.getWorld().getName());
            ps.setDouble(3, location.getX());
            ps.setDouble(4, location.getY());
            ps.setDouble(5, location.getZ());
            ps.setFloat(6, location.getYaw());
            ps.setFloat(7, location.getPitch());
            ps.setString(8, location.getWorld().getName());
            ps.setDouble(9, location.getX());
            ps.setDouble(10, location.getY());
            ps.setDouble(11, location.getZ());
            ps.setFloat(12, location.getYaw());
            ps.setFloat(13, location.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save return location for " + playerUuid, e);
        }
    }

    @Override
    public Location loadReturnLocation(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT world, x, y, z, yaw, pitch FROM return_locations WHERE uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String worldName = rs.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) return null;
                return new Location(
                        world,
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load return location for " + playerUuid, e);
        }
        return null;
    }

    @Override
    public void clearReturnLocation(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM return_locations WHERE uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to clear return location for " + playerUuid, e);
        }
    }
}
