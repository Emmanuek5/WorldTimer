package org.blueobsidian.worldTimer.config;

import java.util.List;

public class WorldConfig {

    private final String worldName;
    private final boolean enabled;
    private final String mainWorld;
    private final String teleportLocation;
    private final long defaultLimitSeconds;
    private final long defaultCooldownSeconds;
    private final String limitPermissionPrefix;
    private final String cooldownPermissionPrefix;
    private final String bypassPermission;
    private final List<Long> warnAtSeconds;
    private final List<String> expireCommands;

    public WorldConfig(String worldName, boolean enabled, String mainWorld, String teleportLocation,
                       long defaultLimitSeconds, long defaultCooldownSeconds,
                       String limitPermissionPrefix, String cooldownPermissionPrefix,
                       String bypassPermission, List<Long> warnAtSeconds, List<String> expireCommands) {
        this.worldName = worldName;
        this.enabled = enabled;
        this.mainWorld = mainWorld;
        this.teleportLocation = teleportLocation;
        this.defaultLimitSeconds = defaultLimitSeconds;
        this.defaultCooldownSeconds = defaultCooldownSeconds;
        this.limitPermissionPrefix = limitPermissionPrefix;
        this.cooldownPermissionPrefix = cooldownPermissionPrefix;
        this.bypassPermission = bypassPermission;
        this.warnAtSeconds = warnAtSeconds;
        this.expireCommands = expireCommands;
    }

    public String getWorldName() {
        return worldName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMainWorld() {
        return mainWorld;
    }

    public String getTeleportLocation() {
        return teleportLocation;
    }

    public long getDefaultLimitSeconds() {
        return defaultLimitSeconds;
    }

    public long getDefaultCooldownSeconds() {
        return defaultCooldownSeconds;
    }

    public String getLimitPermissionPrefix() {
        return limitPermissionPrefix;
    }

    public String getCooldownPermissionPrefix() {
        return cooldownPermissionPrefix;
    }

    public String getBypassPermission() {
        return bypassPermission;
    }

    public List<Long> getWarnAtSeconds() {
        return warnAtSeconds;
    }

    public List<String> getExpireCommands() {
        return expireCommands;
    }
}
