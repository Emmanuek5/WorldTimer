package org.blueobsidian.worldTimer.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.blueobsidian.worldTimer.config.WorldConfig;
import org.blueobsidian.worldTimer.util.TimeUtil;
import org.bukkit.entity.Player;

import java.util.Collection;

public class PermissionResolver {

    /**
     * Resolves the effective time limit for a player in a configured world.
     * Returns -1 if unlimited, or the limit in seconds.
     */
    public long resolveLimit(Player player, WorldConfig worldConfig) {
        if (player.hasPermission(worldConfig.getBypassPermission())) {
            return -1; // bypass = unlimited
        }

        String prefix = worldConfig.getLimitPermissionPrefix();
        long highest = -2; // -2 means no matching permission found

        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                Collection<Node> nodes = user.resolveInheritedNodes(lp.getContextManager().getQueryOptions(user).orElse(
                        lp.getContextManager().getStaticQueryOptions()
                ));
                for (Node node : nodes) {
                    if (!node.getValue()) continue;
                    String key = node.getKey();
                    if (key.startsWith(prefix)) {
                        String suffix = key.substring(prefix.length());
                        long parsed = TimeUtil.parseTimeToSeconds(suffix);
                        if (parsed == -1) {
                            return -1; // unlimited
                        }
                        if (parsed > highest) {
                            highest = parsed;
                        }
                    }
                }
            }
        } catch (IllegalStateException ignored) {
            // LuckPerms not loaded
        }

        if (highest == -2) {
            return worldConfig.getDefaultLimitSeconds();
        }
        return highest;
    }

    /**
     * Resolves the effective cooldown for a player in a configured world.
     * Returns cooldown in seconds.
     */
    public long resolveCooldown(Player player, WorldConfig worldConfig) {
        if (player.hasPermission(worldConfig.getBypassPermission())) {
            return 0;
        }

        String prefix = worldConfig.getCooldownPermissionPrefix();
        long highest = -2;

        try {
            LuckPerms lp = LuckPermsProvider.get();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                Collection<Node> nodes = user.resolveInheritedNodes(lp.getContextManager().getQueryOptions(user).orElse(
                        lp.getContextManager().getStaticQueryOptions()
                ));
                for (Node node : nodes) {
                    if (!node.getValue()) continue;
                    String key = node.getKey();
                    if (key.startsWith(prefix)) {
                        String suffix = key.substring(prefix.length());
                        long parsed = TimeUtil.parseTimeToSeconds(suffix);
                        if (parsed >= 0 && parsed > highest) {
                            highest = parsed;
                        }
                    }
                }
            }
        } catch (IllegalStateException ignored) {
            // LuckPerms not loaded
        }

        if (highest == -2) {
            return worldConfig.getDefaultCooldownSeconds();
        }
        return highest;
    }
}
