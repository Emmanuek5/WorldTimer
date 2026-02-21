package org.blueobsidian.worldTimer.storage;

import java.util.UUID;

public class PlayerTimerData {

    private final UUID playerUuid;
    private final String worldName;
    private long secondsUsed;
    private long cooldownUntil; // epoch seconds

    public PlayerTimerData(UUID playerUuid, String worldName, long secondsUsed, long cooldownUntil) {
        this.playerUuid = playerUuid;
        this.worldName = worldName;
        this.secondsUsed = secondsUsed;
        this.cooldownUntil = cooldownUntil;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getWorldName() {
        return worldName;
    }

    public long getSecondsUsed() {
        return secondsUsed;
    }

    public void setSecondsUsed(long secondsUsed) {
        this.secondsUsed = secondsUsed;
    }

    public void addSeconds(long seconds) {
        this.secondsUsed += seconds;
    }

    public long getCooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(long cooldownUntil) {
        this.cooldownUntil = cooldownUntil;
    }

    public boolean isCooldownActive() {
        return cooldownUntil > System.currentTimeMillis() / 1000;
    }

    public long getCooldownRemainingSeconds() {
        long remaining = cooldownUntil - (System.currentTimeMillis() / 1000);
        return Math.max(0, remaining);
    }
}
