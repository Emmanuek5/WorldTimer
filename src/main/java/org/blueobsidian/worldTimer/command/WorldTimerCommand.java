package org.blueobsidian.worldTimer.command;

import org.blueobsidian.worldTimer.WorldTimer;
import org.blueobsidian.worldTimer.config.MessageConfig;
import org.blueobsidian.worldTimer.config.WorldConfig;
import org.blueobsidian.worldTimer.permission.PermissionResolver;
import org.blueobsidian.worldTimer.storage.PlayerTimerData;
import org.blueobsidian.worldTimer.timer.TimerManager;
import org.blueobsidian.worldTimer.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class WorldTimerCommand implements CommandExecutor, TabCompleter {

    private final WorldTimer plugin;

    public WorldTimerCommand(WorldTimer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender, args);
            case "reset" -> handleReset(sender, args);
            case "addtime" -> handleAddTime(sender, args);
            case "setcooldown" -> handleSetCooldown(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("worldtimer.reload") && !sender.hasPermission("worldtimer.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return;
        }

        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "WorldTimer configuration reloaded.");
    }

    private void handleStatus(CommandSender sender, String[] args) {
        if (!sender.hasPermission("worldtimer.status") && !sender.hasPermission("worldtimer.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /worldtimer status <player>");
            return;
        }

        TimerManager timerManager = plugin.getTimerManager();
        PermissionResolver resolver = plugin.getPermissionResolver();
        MessageConfig msg = plugin.getPluginConfig().getMessageConfig();

        sender.sendMessage(ChatColor.GOLD + "=== WorldTimer Status: " + target.getName() + " ===");
        sender.sendMessage(ChatColor.GRAY + "Current world: " + target.getWorld().getName());

        for (Map.Entry<String, WorldConfig> entry : plugin.getPluginConfig().getWorldConfigs().entrySet()) {
            WorldConfig wc = entry.getValue();
            if (!wc.isEnabled()) continue;

            String worldName = wc.getWorldName();
            PlayerTimerData data = timerManager.getData(target.getUniqueId(), worldName);

            boolean bypass = target.hasPermission(wc.getBypassPermission());
            long limit = resolver.resolveLimit(target, wc);

            sender.sendMessage(ChatColor.AQUA + "  " + worldName + ":");

            if (bypass) {
                sender.sendMessage(ChatColor.GREEN + "    Bypass: " + ChatColor.WHITE + "Yes");
                continue;
            }

            if (limit == -1) {
                sender.sendMessage(ChatColor.GREEN + "    Limit: " + ChatColor.WHITE + "Unlimited");
            } else {
                String used = TimeUtil.formatTime(data.getSecondsUsed());
                String total = TimeUtil.formatTime(limit);
                long remaining = Math.max(0, limit - data.getSecondsUsed());
                String remainStr = TimeUtil.formatTime(remaining);

                sender.sendMessage(ChatColor.YELLOW + "    Used: " + ChatColor.WHITE + used + " / " + total);
                sender.sendMessage(ChatColor.YELLOW + "    Remaining: " + ChatColor.WHITE + remainStr);
            }

            if (data.isCooldownActive()) {
                String cdStr = TimeUtil.formatTime(data.getCooldownRemainingSeconds());
                sender.sendMessage(ChatColor.RED + "    Cooldown: " + ChatColor.WHITE + cdStr);
            } else {
                sender.sendMessage(ChatColor.GREEN + "    Cooldown: " + ChatColor.WHITE + "None");
            }
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("worldtimer.manage") && !sender.hasPermission("worldtimer.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /worldtimer reset <player> <world>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }

        String worldName = args[2].toLowerCase();
        if (!plugin.getPluginConfig().isWorldConfigured(worldName)) {
            sender.sendMessage(ChatColor.RED + "World not configured: " + worldName);
            return;
        }

        plugin.getTimerManager().resetPlayer(target.getUniqueId(), worldName);
        sender.sendMessage(ChatColor.GREEN + "Reset timer data for " + target.getName() + " in " + worldName + ".");
    }

    private void handleAddTime(CommandSender sender, String[] args) {
        if (!sender.hasPermission("worldtimer.manage") && !sender.hasPermission("worldtimer.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /worldtimer addtime <player> <world> <time>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }

        String worldName = args[2].toLowerCase();
        if (!plugin.getPluginConfig().isWorldConfigured(worldName)) {
            sender.sendMessage(ChatColor.RED + "World not configured: " + worldName);
            return;
        }

        long seconds = TimeUtil.parseTimeToSeconds(args[3]);
        if (seconds < 0) {
            // Try parsing as raw seconds
            try {
                seconds = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid time format: " + args[3] + " (use e.g. 30m, 2h, 3600s, or raw seconds)");
                return;
            }
        }

        plugin.getTimerManager().addTime(target.getUniqueId(), worldName, seconds);
        sender.sendMessage(ChatColor.GREEN + "Added " + TimeUtil.formatTime(seconds) + " to " + target.getName() + " in " + worldName + ".");
    }

    private void handleSetCooldown(CommandSender sender, String[] args) {
        if (!sender.hasPermission("worldtimer.manage") && !sender.hasPermission("worldtimer.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /worldtimer setcooldown <player> <world> <time>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }

        String worldName = args[2].toLowerCase();
        if (!plugin.getPluginConfig().isWorldConfigured(worldName)) {
            sender.sendMessage(ChatColor.RED + "World not configured: " + worldName);
            return;
        }

        long seconds = TimeUtil.parseTimeToSeconds(args[3]);
        if (seconds < 0) {
            try {
                seconds = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid time format: " + args[3]);
                return;
            }
        }

        plugin.getTimerManager().setCooldown(target.getUniqueId(), worldName, seconds);
        if (seconds <= 0) {
            sender.sendMessage(ChatColor.GREEN + "Cleared cooldown for " + target.getName() + " in " + worldName + ".");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Set cooldown for " + target.getName() + " in " + worldName + " to " + TimeUtil.formatTime(seconds) + ".");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== WorldTimer Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/worldtimer reload" + ChatColor.GRAY + " - Reload config");
        sender.sendMessage(ChatColor.YELLOW + "/worldtimer status [player]" + ChatColor.GRAY + " - View timer status");
        sender.sendMessage(ChatColor.YELLOW + "/worldtimer reset <player> <world>" + ChatColor.GRAY + " - Reset player timer");
        sender.sendMessage(ChatColor.YELLOW + "/worldtimer addtime <player> <world> <time>" + ChatColor.GRAY + " - Add time for player");
        sender.sendMessage(ChatColor.YELLOW + "/worldtimer setcooldown <player> <world> <time>" + ChatColor.GRAY + " - Set player cooldown");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(args[0], List.of("reload", "status", "reset", "addtime", "setcooldown"));
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "status", "reset", "addtime", "setcooldown" -> {
                    return filterStartsWith(args[1], getOnlinePlayerNames());
                }
            }
        }

        if (args.length == 3) {
            switch (sub) {
                case "reset", "addtime", "setcooldown" -> {
                    return filterStartsWith(args[2], getConfiguredWorlds());
                }
            }
        }

        if (args.length == 4) {
            switch (sub) {
                case "addtime", "setcooldown" -> {
                    return filterStartsWith(args[3], List.of("30s", "1m", "5m", "10m", "30m", "1h", "2h"));
                }
            }
        }

        return Collections.emptyList();
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> getConfiguredWorlds() {
        return new ArrayList<>(plugin.getPluginConfig().getWorldConfigs().keySet());
    }

    private List<String> filterStartsWith(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
