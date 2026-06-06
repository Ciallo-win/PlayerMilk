# PlayerMilk

A fun Minecraft Bukkit/Spigot plugin that allows players to use an empty bucket to "milk" other players, obtaining a custom-named milk bucket. Drinking this special milk applies custom potion effects.

## Features

- **Milking Mechanic**: Right-click players with an empty bucket to milk them
- **Custom Milk Buckets**: Obtained milk buckets can have custom names with placeholders for player names, time, etc.
- **Damage & Death**: Configurable damage when milking, with custom death messages
- **Cooldown System**: Configurable per-player cooldown with persistent storage
- **Potion Effects**: Drinking "Player Milk" applies custom potion effects
- **Hot Reload**: Modify configuration via commands with auto-save, no server restart needed
- **Full Command Support**: Complete command system to modify all settings dynamically
- **Multi-Language**: Built-in English/Turkish support

## Commands

| Command | Description |
|---------|-------------|
| `/playermilk help` | Show help information |
| `/playermilk reload` | Reload configuration |
| `/playermilk toggle` | Enable/disable the plugin |
| `/playermilk about` | Show plugin information |
| `/playermilk set` | List all configuration keys |
| `/playermilk set <key> <value>` | Modify a config key (auto-saves) |
| `/playermilk effects list` | List all potion effects |
| `/playermilk effects add <name> <id> <dur> <amp>` | Add a potion effect |
| `/playermilk effects remove <name>` | Remove a potion effect |
| `/playermilk effects set <name> <field> <value>` | Edit an effect field |

**Permission**: All commands require `playermilk.admin` permission. OPs have it by default.

## Placeholders

| Placeholder | Description |
|-------------|-------------|
| `{player}` | Active player (milker / message sender) |
| `{player2}` | Passive player (milked player / message receiver) |
| `{time}` | Current time (format set by `Date_Format`) |

## Configuration Example

### Base Configuration (`config.yml`)

```yaml
Milk:
  Enabled: true          # Enable/disable milking
  Damage: 1.0            # Damage dealt when milking
  Cooldown: 5            # Cooldown in seconds
  Death_Message: '{player2} was milked to death by {player}'
  Bucket_Name: '&f{player2}`s Milk'
  
  Effects:
    effect1:
      id: 'speed'        # Effect ID (name or numeric)
      duration: 60       # Duration in seconds
      amplifier: 0       # Effect level (0 = level I)
      particles: true    # Show particle effects
```

## build
  run mvn clean package
  put target/PlayerMilk.jar in plugins folder.

### Command Alias
  The main command /playermilk can also be used as /pmilk

## Requirements
  API Version: 1.21

  Dependency: Spigot/Bukkit API

## Author
  Author: P1ay2r / ciallo