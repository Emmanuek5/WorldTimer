package org.blueobsidian.worldTimer;

import org.blueobsidian.worldTimer.command.WorldTimerCommand;
import org.blueobsidian.worldTimer.config.PluginConfig;
import org.blueobsidian.worldTimer.listener.PlayerListener;
import org.blueobsidian.worldTimer.permission.PermissionResolver;
import org.blueobsidian.worldTimer.storage.SQLiteStorage;
import org.blueobsidian.worldTimer.storage.StorageManager;
import org.blueobsidian.worldTimer.timer.TimerManager;
import org.blueobsidian.worldTimer.timer.TimerTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldTimer extends JavaPlugin {

    private PluginConfig pluginConfig;
    private StorageManager storageManager;
    private PermissionResolver permissionResolver;
    private TimerManager timerManager;
    private TimerTask timerTask;

    @Override
    public void onEnable() {
        // Load configuration
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        // Initialize storage
        storageManager = new SQLiteStorage(this);
        storageManager.init();

        // Initialize permission resolver
        permissionResolver = new PermissionResolver();

        // Initialize timer manager
        timerManager = new TimerManager(this, storageManager, permissionResolver);

        // Register events
        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, timerManager, storageManager), this
        );

        // Register commands
        WorldTimerCommand cmdExecutor = new WorldTimerCommand(this);
        PluginCommand cmd = getCommand("worldtimer");
        if (cmd != null) {
            cmd.setExecutor(cmdExecutor);
            cmd.setTabCompleter(cmdExecutor);
        }

        // Start timer task
        startTimerTask();

        getLogger().info("WorldTimer enabled. Tracking " + pluginConfig.getWorldConfigs().size() + " world(s).");
    }

    @Override
    public void onDisable() {
        // Stop timer task
        if (timerTask != null) {
            timerTask.cancel();
        }

        // Save all active timers
        if (timerManager != null) {
            timerManager.saveAll();
        }

        // Shutdown storage
        if (storageManager != null) {
            storageManager.shutdown();
        }

        getLogger().info("WorldTimer disabled.");
    }

    /**
     * Reloads the plugin configuration and restarts the timer task.
     */
    public void reload() {
        // Save active timers before reload
        if (timerManager != null) {
            timerManager.saveAll();
        }

        // Stop current task
        if (timerTask != null) {
            timerTask.cancel();
        }

        // Reload config
        pluginConfig.load();

        // Restart timer task
        startTimerTask();

        getLogger().info("WorldTimer reloaded. Tracking " + pluginConfig.getWorldConfigs().size() + " world(s).");
    }

    private void startTimerTask() {
        int tickSeconds = pluginConfig.getTickSeconds();
        long intervalTicks = tickSeconds * 20L; // Convert seconds to ticks
        timerTask = new TimerTask(timerManager, tickSeconds);
        timerTask.runTaskTimer(this, intervalTicks, intervalTicks);
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public PermissionResolver getPermissionResolver() {
        return permissionResolver;
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }
}
