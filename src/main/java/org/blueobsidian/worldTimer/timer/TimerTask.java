package org.blueobsidian.worldTimer.timer;

import org.bukkit.scheduler.BukkitRunnable;

public class TimerTask extends BukkitRunnable {

    private final TimerManager timerManager;
    private final int tickSeconds;

    public TimerTask(TimerManager timerManager, int tickSeconds) {
        this.timerManager = timerManager;
        this.tickSeconds = tickSeconds;
    }

    @Override
    public void run() {
        timerManager.tick(tickSeconds);
    }
}
