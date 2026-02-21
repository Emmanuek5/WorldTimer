# WorldTimer

A Paper 1.20.1 plugin that enforces configurable time limits for specific worlds (e.g., resource worlds) with rank-based overrides via LuckPerms.

## Features

- **Per-world time limits** — Configure how long players can stay in each world
- **Rank-based overrides** — Different ranks get different time limits and cooldowns via LuckPerms permissions
- **Cooldown system** — Players must wait before re-entering a world after their time expires
- **Warning messages** — Configurable warnings at specific time thresholds
- **Expiry actions** — Run commands when a player's time expires (e.g., titles, effects)
- **Return teleport** — Players are teleported back to their exact pre-entry location
- **Persistent storage** — SQLite database survives restarts and crashes
- **Bypass permission** — Staff can bypass all restrictions per-world

## Requirements

- **Paper 1.20.1+** (or compatible fork)
- **Java 17+**

### Optional Dependencies

- **LuckPerms** — For permission-based time limits and cooldowns. Without it, all players use the default values from config.
- **Multiverse-Core** — Works seamlessly with Multiverse-managed worlds

## Installation

1. Download the latest `world-timer-x.x.jar` from releases
2. Place it in your server's `plugins/` folder
3. Restart the server
4. Edit `plugins/WorldTimer/config.yml` to configure your worlds
5. Reload with `/worldtimer reload`

## Configuration

```yaml
# How often the plugin checks timers (in seconds)
tick-seconds: 1

storage:
  type: sqlite

messages:
  prefix: "&8[&bWorldTimer&8] &r"
  warn: "&eYou have &6%time_remaining% &eremaining in &6%world%&e."
  expired: "&cYour time in &6%world% &chas expired."
  cooldown: "&cYou must wait &6%cooldown_remaining% &cbefore re-entering &6%world%&c."

worlds:
  resource:                              # World name (case-insensitive)
    enabled: true
    main-world: "survival"               # Fallback teleport world
    teleport-location: "spawn"           # Currently uses return point or spawn
    default-limit-seconds: 1800          # 30 minutes (used if no permission)
    default-cooldown-seconds: 600        # 10 minutes (used if no permission)
    limit-permission-prefix: "worldtimer.limit.resource."
    cooldown-permission-prefix: "worldtimer.cooldown.resource."
    bypass-permission: "worldtimer.bypass.resource"
    warn-at-seconds: [600, 300, 60, 10]  # Warn at 10m, 5m, 1m, 10s remaining
    expire-commands:
      - "title %player% title {\"text\":\"Time is up!\",\"color\":\"red\"}"
```

## Permissions

### Time Limit Permissions

Format: `worldtimer.limit.<world>.<duration>`

| Permission | Effect |
|------------|--------|
| `worldtimer.limit.resource.30m` | 30 minute limit |
| `worldtimer.limit.resource.1h` | 1 hour limit |
| `worldtimer.limit.resource.2h` | 2 hour limit |
| `worldtimer.limit.resource.unlimited` | No time limit |

**Duration formats:** `30s`, `30m`, `2h`, `1d`, `unlimited`

If a player has multiple limit permissions, the **highest** value is used.

### Cooldown Permissions

Format: `worldtimer.cooldown.<world>.<duration>`

| Permission | Effect |
|------------|--------|
| `worldtimer.cooldown.resource.5m` | 5 minute cooldown |
| `worldtimer.cooldown.resource.10m` | 10 minute cooldown |
| `worldtimer.cooldown.resource.0s` | No cooldown |

If a player has multiple cooldown permissions, the **highest** value is used.

### Other Permissions

| Permission | Description |
|------------|-------------|
| `worldtimer.bypass.<world>` | Bypass all restrictions for a world |
| `worldtimer.admin` | Full access to all commands |
| `worldtimer.reload` | Access to `/worldtimer reload` |
| `worldtimer.manage` | Access to reset/addtime/setcooldown commands |
| `worldtimer.status` | Access to `/worldtimer status` |

## Commands

| Command | Description |
|---------|-------------|
| `/worldtimer reload` | Reload configuration |
| `/worldtimer status [player]` | View timer status for yourself or another player |
| `/worldtimer reset <player> <world>` | Reset a player's timer and cooldown |
| `/worldtimer addtime <player> <world> <time>` | Add time to a player's allowance |
| `/worldtimer setcooldown <player> <world> <time>` | Set or clear a player's cooldown |

**Alias:** `/wt`

## How It Works

### Entry Flow
1. Player teleports to a configured world
2. Plugin checks for bypass permission → if present, allow freely
3. Plugin checks for active cooldown → if active, block entry and show message
4. Plugin saves the player's current location as their **return point**
5. Plugin resolves time limit from permissions (or uses default)
6. Timer tracking begins

### While In World
- Timer increments every tick interval
- Warnings are sent at configured thresholds
- Time is tracked per-player per-world

### On Expiry
1. Expiry message is sent
2. Configured expire-commands are executed
3. Cooldown begins (based on permissions or default)
4. Player is teleported back to their **saved return point**
5. If no return point exists, player goes to main world spawn

### Disconnect Safety
- If a player disconnects while in a limited world, their timer is saved
- On rejoin, if their timer is expired or cooldown is active, they're teleported to safety
- This prevents bypassing limits by disconnecting or switching servers

## LuckPerms Setup Example

```bash
# Default rank: 30 minute limit, 10 minute cooldown
/lp group default permission set worldtimer.limit.resource.30m true
/lp group default permission set worldtimer.cooldown.resource.10m true

# VIP rank: 1 hour limit, 5 minute cooldown
/lp group vip permission set worldtimer.limit.resource.1h true
/lp group vip permission set worldtimer.cooldown.resource.5m true

# MVP rank: 2 hour limit, no cooldown
/lp group mvp permission set worldtimer.limit.resource.2h true
/lp group mvp permission set worldtimer.cooldown.resource.0s true

# Staff: bypass all restrictions
/lp group staff permission set worldtimer.bypass.resource true
```

## Without LuckPerms

If LuckPerms is not installed, the plugin still works:
- All players use `default-limit-seconds` from config
- All players use `default-cooldown-seconds` from config
- Bypass permissions still work via Bukkit's permission system
- A warning is logged on startup

## Storage

Data is stored in `plugins/WorldTimer/data.db` (SQLite):
- **timer_data** — Per-player per-world: seconds used, cooldown timestamp
- **player_worlds** — Last known world for each player
- **return_locations** — Saved return points (world, x, y, z, yaw, pitch)

## Placeholders

Available in messages:
- `%player%` — Player name
- `%world%` — World name
- `%time_remaining%` — Formatted time remaining (e.g., "5m 30s")
- `%cooldown_remaining%` — Formatted cooldown remaining

## Building

```bash
mvn clean package
```

Output: `target/world-timer-1.0-SNAPSHOT.jar`

## License

MIT
