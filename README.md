# SlimeAnnihilator

A powerful Minecraft plugin for managing slime spawning and removal, specifically designed to handle flat worlds and provide fine-grained control over slime behavior across your server.

## Features

### 🎯 Core Functionality
- **Automatic Slime Removal**: Removes slimes from flat worlds on server startup
- **Spawn Prevention**: Configurable slime spawning prevention with multiple spawn method controls
- **Flat World Detection**: Automatic detection of flat worlds with manual override support
- **World Exemptions**: Exempt specific worlds from all slime management
- **Fine-grained Control**: Individual control over natural, egg, command, and custom spawning

### 🛠️ Advanced Features
- **Multi-module Architecture**: Organized with common functionality and main plugin modules
- **Confirmation System**: Safety confirmations for non-flat world operations
- **Debug Logging**: Comprehensive debug output for troubleshooting
- **Tab Completion**: Full command tab completion support
- **Configuration Management**: Hot-reloadable configuration

## Installation

1. Download the latest JAR from the releases page
2. Place `SlimeAnnihilator-<version>-all.jar` in your server's `plugins/` directory
3. Restart your server
4. Configure the plugin by editing `plugins/SlimeAnnihilator/config.yml`

## Requirements

- **Minecraft Version**: 1.21+
- **Server Software**: Paper or Spigot
- **Java Version**: 21+

## Commands

All commands use the base `/slimes` command with various subcommands:

| Command | Description | Permission |
|---------|-------------|------------|
| `/slimes nuke [world]` | Remove all slimes from specified world | `slimeannihilator.use` |
| `/slimes disable [world]` | Disable slime spawning in world | `slimeannihilator.use` |
| `/slimes enable [world]` | Enable slime spawning in world | `slimeannihilator.use` |
| `/slimes info [world]` | Show detailed slime information for world | `slimeannihilator.use` |
| `/slimes exempt [world]` | Exempt world from slime management | `slimeannihilator.exempt` |
| `/slimes unexempt [world]` | Remove world exemption | `slimeannihilator.exempt` |
| `/slimes confirm` | Confirm pending dangerous operations | `slimeannihilator.use` |
| `/slimes reload` | Reload plugin configuration | `slimeannihilator.admin` |
| `/slimes config` | Display current configuration | `slimeannihilator.admin` |
| `/slimes setflat [world]` | Manually mark world as flat | `slimeannihilator.admin` |
| `/slimes unsetflat [world]` | Remove flat world marking | `slimeannihilator.admin` |
| `/slimes listflat` | List all flat worlds (manual and auto-detected) | `slimeannihilator.use` |

> **Note**: If no world is specified, the command will use your current world. Console users must always specify a world name.

## Configuration

The plugin creates a `config.yml` file with the following options:

```yaml
# General Settings
auto-remove-on-startup: true
prevent-spawning-in-flat-worlds: true
require-confirmation-for-non-flat-worlds: true
confirmation-timeout-seconds: 30
debug-messages: false

# Spawn Prevention Settings
prevent-egg-spawning: false
prevent-command-spawning: false
prevent-custom-spawning: false

# World Lists
flat-worlds: []
exempt-worlds: []
worlds-with-spawning-disabled: []
```

### Configuration Options

- **auto-remove-on-startup**: Automatically remove slimes from flat worlds when the server starts
- **prevent-spawning-in-flat-worlds**: Block natural slime spawning in detected flat worlds
- **require-confirmation-for-non-flat-worlds**: Safety feature requiring confirmation for operations on non-flat worlds
- **prevent-egg-spawning**: Block slimes from spawn eggs (overrides flat world settings)
- **prevent-command-spawning**: Block slimes from `/summon` commands
- **prevent-custom-spawning**: Block slimes from plugin/custom spawning
- **flat-worlds**: List of worlds manually configured as flat
- **exempt-worlds**: Worlds completely exempt from slime management
- **worlds-with-spawning-disabled**: Worlds with manually disabled slime spawning

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `slimeannihilator.use` | Basic slime management commands | `op` |
| `slimeannihilator.admin` | Administrative commands (config, reload) | `op` |
| `slimeannihilator.exempt` | Manage world exemptions | `op` |

## How It Works

### Flat World Detection
The plugin uses multiple methods to detect flat worlds:
1. **Manual Configuration**: Worlds listed in `flat-worlds` config
2. **Generator Detection**: Checks for flat/void world generators
3. **Name Pattern Detection**: Recognizes common flat world naming patterns

### Spawn Prevention Hierarchy
1. **World Exemptions**: Exempt worlds bypass all restrictions
2. **Manual Enable/Disable**: Per-world manual settings take precedence
3. **Specific Spawn Methods**: Egg/command/custom spawn settings override flat world restrictions
4. **Flat World Restrictions**: Natural spawning blocked in flat worlds (if enabled)

## Building from Source

This project uses Gradle with a multi-module structure:

```bash
# Clone the repository
git clone <repository-url>
cd SlimeAnnihilator

# Build the plugin
./gradlew build

# The JAR will be created in ./out/SlimeAnnihilator-<version>-all.jar
```

## Project Structure

```
SlimeAnnihilator/
├── slime-common/          # Common functionality module
│   └── src/main/java/com/mrerenk/slimeannihilator/common/
│       ├── SlimeManager.java
│       ├── SlimeSpawnListener.java
│       ├── commands/
│       └── config/
└── src/main/              # Main plugin module
    ├── java/com/mrerenk/slimeAnnihilator/
    │   └── SlimeAnnihilator.java
    └── resources/
        ├── config.yml
        └── plugin.yml
```

## License

This project is licensed under the [MIT License](LICENSE).

## Support

For issues, feature requests, or questions:
- Create an issue on the GitHub repository

---
