# meowtils

meowtils is a client-side utility mod for Minecraft Forge 1.8.9.
It focuses on teleportation helpers, checkpoint handling, and parkour-oriented quality-of-life tools.

## Features

### Look TP

Meowtils includes hotkeys to teleport to the block you're looking at. One hotkey teleports you on top, and another teleports you through (a wall, ceiling, or floor). TP on Top includes a toggleable safety check with fallback behaviors if the block you attempt to teleport to is not safe to stand on.

### Checkpoints

The checkpoint system lets you store a position and return to it later using hotkeys and hotbar slot selection. You can configure hotkeys for the two actions or use the hotbar system. Hotbar slots can be configured for the set and return, and right-clicking with the configured slot active will trigger the assigned action. This is designed for those familiar with practice modes on parkour servers.

### Utility commands

The mod includes commands to quickly change configs. It also overrides the server's /tp command to provide support for relative coordinates, facing angles, and arithmetic expressions on all servers.

### OneConfig support

This mod uses OneConfig to provide a central config menu with convenient options to edit configs.

## Commands

The mod registers these commands (arguments enclosed in brackets are optional):

- `/tp <x> <y> <z> [yaw] [pitch]` - Overrides the server's /tp, providing support for relative (~) coordinates, facing, and expressions.
- `/tpf <yaw> [pitch]` - Change your facing direction without moving (~ for relative).
- `/meowtils` or `/mt` - The mod's central command.

`/mt` subcommands:

- `/mt help` - Show the available `/mt` commands.
- `/mt config`, `/mt settings`, `/mt gui` - Open the OneConfig settings GUI.
- `/mt prefix <text>` - Set the chat prefix text.
- `/mt crs <slot>` or `/mt checkpointreturnslot <slot>` - Set the hotbar slot used to return from a checkpoint.
- `/mt css <slot>` or `/mt checkpointsetslot <slot>` - Set the hotbar slot used to set a checkpoint.
- `/mt safetp` or `/mt safeteleport` - Toggle safety checks for Teleport on Top.
- `/mt tpdist <distance>` or `/mt teleportdistance <distance>` - Set the teleport distance.
- `/mt color1 <name>` - Set the first chat color for mod messages.
- `/mt color2 <name>` - Set the second chat color for mod messages.
- `/mt list` - Show the available chat colors.

## Installation

1. Install the latest version of Minecraft Forge for 1.8.9.
2. Place the mod JAR in your `mods` folder.
3. Launch the Forge profile in Minecraft.

The release JAR bundles OneConfig, so you do not need to install it separately.

## Building from source

```powershell
git clone <repo-url>
cd meowtils
.\gradlew.bat build
```

The compiled mod JAR will be written to `build/libs`.
