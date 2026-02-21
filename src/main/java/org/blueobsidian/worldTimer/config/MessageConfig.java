package org.blueobsidian.worldTimer.config;

import org.bukkit.ChatColor;

public class MessageConfig {

    private final String prefix;
    private final String warn;
    private final String expired;
    private final String cooldown;
    private final String cooldownBypass;
    private final String enter;
    private final String enterUnlimited;

    public MessageConfig(String prefix, String warn, String expired, String cooldown, String cooldownBypass,
                         String enter, String enterUnlimited) {
        this.prefix = colorize(prefix);
        this.warn = colorize(warn);
        this.expired = colorize(expired);
        this.cooldown = colorize(cooldown);
        this.cooldownBypass = colorize(cooldownBypass);
        this.enter = colorize(enter);
        this.enterUnlimited = colorize(enterUnlimited);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getWarn() {
        return warn;
    }

    public String getExpired() {
        return expired;
    }

    public String getCooldown() {
        return cooldown;
    }

    public String getCooldownBypass() {
        return cooldownBypass;
    }

    public String getEnter() {
        return enter;
    }

    public String getEnterUnlimited() {
        return enterUnlimited;
    }

    public String format(String template, String world, String timeRemaining, String cooldownRemaining, String playerName) {
        String msg = template;
        if (world != null) msg = msg.replace("%world%", world);
        if (timeRemaining != null) msg = msg.replace("%time_remaining%", timeRemaining);
        if (cooldownRemaining != null) msg = msg.replace("%cooldown_remaining%", cooldownRemaining);
        if (playerName != null) msg = msg.replace("%player%", playerName);
        return prefix + msg;
    }

    private static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
