package org.blueobsidian.worldTimer.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.blueobsidian.worldTimer.WorldTimer;

import java.util.*;
import java.util.stream.Collectors;

public class PluginConfig {

    private final WorldTimer plugin;
    private int tickSeconds;
    private String storageType;
    private MessageConfig messageConfig;
    private final Map<String, WorldConfig> worldConfigs = new HashMap<>();

    // MySQL settings
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;

    public PluginConfig(WorldTimer plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.tickSeconds = config.getInt("tick-seconds", 1);

        // Storage
        this.storageType = config.getString("storage.type", "sqlite");
        this.mysqlHost = config.getString("storage.mysql.host", "localhost");
        this.mysqlPort = config.getInt("storage.mysql.port", 3306);
        this.mysqlDatabase = config.getString("storage.mysql.database", "worldtimer");
        this.mysqlUsername = config.getString("storage.mysql.username", "root");
        this.mysqlPassword = config.getString("storage.mysql.password", "");

        // Messages
        this.messageConfig = new MessageConfig(
                config.getString("messages.prefix", "&8[&bWorldTimer&8] &r"),
                config.getString("messages.warn", "&eYou have &6%time_remaining% &eremaining in &6%world%&e."),
                config.getString("messages.expired", "&cYour time in &6%world% &chas expired."),
                config.getString("messages.cooldown", "&cYou must wait &6%cooldown_remaining% &cbefore re-entering &6%world%&c."),
                config.getString("messages.cooldown-bypass", "&aYour cooldown for &6%world% &ahas been cleared.")
        );

        // Worlds
        worldConfigs.clear();
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldKey : worldsSection.getKeys(false)) {
                ConfigurationSection ws = worldsSection.getConfigurationSection(worldKey);
                if (ws == null) continue;

                List<Long> warnAt = ws.getLongList("warn-at-seconds");
                if (warnAt.isEmpty()) {
                    warnAt = ws.getIntegerList("warn-at-seconds").stream()
                            .map(Integer::longValue)
                            .collect(Collectors.toList());
                }

                WorldConfig wc = new WorldConfig(
                        worldKey,
                        ws.getBoolean("enabled", true),
                        ws.getString("main-world", "world"),
                        ws.getString("teleport-location", "spawn"),
                        ws.getLong("default-limit-seconds", 1800),
                        ws.getLong("default-cooldown-seconds", 600),
                        ws.getString("limit-permission-prefix", "worldtimer.limit." + worldKey + "."),
                        ws.getString("cooldown-permission-prefix", "worldtimer.cooldown." + worldKey + "."),
                        ws.getString("bypass-permission", "worldtimer.bypass." + worldKey),
                        warnAt,
                        ws.getStringList("expire-commands")
                );
                worldConfigs.put(worldKey.toLowerCase(), wc);
            }
        }
    }

    public int getTickSeconds() {
        return tickSeconds;
    }

    public String getStorageType() {
        return storageType;
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    public Map<String, WorldConfig> getWorldConfigs() {
        return Collections.unmodifiableMap(worldConfigs);
    }

    public WorldConfig getWorldConfig(String worldName) {
        return worldConfigs.get(worldName.toLowerCase());
    }

    public boolean isWorldConfigured(String worldName) {
        WorldConfig wc = worldConfigs.get(worldName.toLowerCase());
        return wc != null && wc.isEnabled();
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }
}
