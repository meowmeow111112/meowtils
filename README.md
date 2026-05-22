# meowtils

meowtils is a client-side utility mod for Minecraft Forge 1.8.9.
It focuses on teleport helpers, checkpoint handling, and parkour-oriented quality-of-life tools.

## Features

- Hotkeys for quick teleporting where you are looking.
- A checkpoint system that uses one hotkey to store a checkpoint and another to return to it.
- Hotbar slot controls for checkpoint set and return, so you can keep the system tied to your preferred slots.
- Utility commands for teleporting, facing changes, and configuration tweaks.
- Lightweight configuration support using OneConfig.

## Commands

The mod registers these commands (arguments enclosed in brackets are optional):

- `/tp <x> <y> <z> [yaw] [pitch]` - Overrides the server's /tp, providing support for relative (~) coordinates, facing, and expressions.
- `/tpf <yaw> [pitch]` - Change your facing direction without moving (~ for relative).
- `/meowtils` or `/mt` - The mod's central command.

`/mt` subcommands:

- `/mt help` - Show the available `/mt` commands.
- `/mt config`, `/mt settings`, `/mt gui` - Open the OneConfig settings GUI.
- `/mt prefix <text>` - Set the chat prefix text.
- `/mt crs <slot>` or `/mt checkpointreturnslot <slot>` - Set the hotbar slot used to return from a checkpoint. Slots are `1` to `9`.
- `/mt css <slot>` or `/mt checkpointsetslot <slot>` - Set the hotbar slot used to place a checkpoint. Slots are `1` to `9`.
- `/mt safetp` or `/mt safeteleport` - Toggle safety checks for teleporting on top.
- `/mt tpdist <distance>` or `/mt teleportforwarddistance <distance>` - Set the forward teleport distance.
- `/mt color1 <name>` - Set the first chat color.
- `/mt color2 <name>` - Set the second chat color.
- `/mt list` - Show the available chat colors.

Use in-game tab completion or command help for the exact syntax and accepted values.

## Installation

1. Install Minecraft Forge 1.8.9 for `11.15.1.2318-1.8.9`.
2. Place the built JAR in your `mods` folder.
3. Launch the Forge profile in Minecraft.

## Building from source

```powershell
git clone <repo-url>
cd meowtils
.\gradlew.bat build
```

The compiled mod JAR will be written to `build/libs`.
