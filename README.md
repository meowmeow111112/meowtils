# Meowtils

Meowtils is a client-side utility mod for Minecraft Forge 1.8.9 that adds teleportation tools, checkpoint handling, and quality-of-life features for movement and parkour.

## Features

### Checkpoints

Save a checkpoint and return to it in one click using Meowtils's checkpoint system. You can use separate hotkeys to set and return to checkpoints, or use the hotbar system, where you can configure hotbar slots to set and return to checkpoints by right-clicking, inspired by practice modes commonly found on parkour servers.

### Look TP

Choose hotkeys to teleport to where you're looking in different ways. Teleport directly onto the block you're looking at, move through walls, ceilings, and floors, or jump a configurable distance forward.
Teleport on Top includes optional safety checks that can automatically search for safe nearby landing spots, with configurable fallback behavior when no safe position is found.

### OneConfig Support

Meowtils uses OneConfig for its config interface, providing an easy way to customize the mod in-game. Use `/ocfg` to open the OneConfig menu, or `/mt config` to jump directly to the Meowtils settings page.

## Commands

Meowtils includes utility and config commands. The mod overrides the standard `/tp` command on the client side to provide consistent behavior across servers, including support for relative coordinates, rotation, and arithmetic expressions.

Commands have `<required>` and `[optional]` arguments. 
The mod registers these commands:

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
- `/mt tpdist <distance>` or `/mt teleportdistance <distance>` - Set the Teleport Forward distance.
- `/mt color1 <name>` - Set the first chat color for mod messages.
- `/mt color2 <name>` - Set the second chat color for mod messages.
- `/mt list` - Show the available chat colors.

## Installation

1. Install Minecraft Forge for Minecraft 1.8.9.
2. Download the latest Meowtils release JAR [here](https://github.com/meowmeow111112/meowtils/releases/latest).
3. Open your Minecraft `mods` folder:
   - Windows: `%appdata%\.minecraft\mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`
4. Place the Meowtils JAR file into the `mods` folder.
5. Launch Minecraft using your Forge 1.8.9 profile.

The release JAR already includes OneConfig, so no additional installation is required.

## Building from source

```
git clone https://github.com/meowmeow111112/meowtils
cd meowtils
./gradlew build
```

The compiled mod JAR will be written to `build/libs`.
