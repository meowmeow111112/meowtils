package net.meowtils.config;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;

public class MeowtilsCommand implements ICommand {
    private final ConfigManager configManager;

    public MeowtilsCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String getCommandName() { return "meowtils"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "Use /mt help for a list of commands.";
    }

    @Override
    public List getCommandAliases() {
        List<String> aliases = new ArrayList<String>();
        aliases.add("mt");
        return aliases;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String color1 = configManager.getColor1();
        String color2 = configManager.getColor2();
        String reset = configManager.getReset();
        String prefix = configManager.getPrefix();

        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: " + getCommandUsage(sender) + reset));
            return;
        }

        String command = args[0].toLowerCase();

        if (handleListCommand(sender, command, color1, prefix, color2, reset)) {
            return;
        }

        if (handleHelpCommand(sender, command, color1, color2, reset, prefix)) {
            return;
        }

        if (handlePrefixCommand(sender, args, command, color1, color2, reset, prefix)) {
            return;
        }

        if (handleCheckpointReturnSlotCommand(sender, args, command, color1, color2, reset, prefix)) {
            return;
        }

        if (handleCheckpointSetSlotCommand(sender, args, command, color1, color2, reset, prefix)) {
            return;
        }

        if (handleTeleportForwardDistanceCommand(sender, args, command, color1, color2, reset, prefix)) {
            return;
        }

        if (handleColorCommand(sender, args, command, color1, color2, reset, prefix)) {
            return;
        }

        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid arguments. " + getCommandUsage(sender) + reset));
        } else {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Unknown command. " + getCommandUsage(sender) + reset));
        }
    }

    private boolean handleListCommand(ICommandSender sender, String command, String color1, String prefix, String color2, String reset) {
        if (!command.equals("list")) {
            return false;
        }

        StringBuilder sb = new StringBuilder(color1 + prefix + color2 + "Available colors: " + reset);
        for (String colorName : configManager.getColorMap().keySet()) {
            sb.append(colorName).append(", ");
        }
        sb.setLength(sb.length() - 2);
        sender.addChatMessage(new ChatComponentText(sb.toString()));
        return true;
    }

    private boolean handleHelpCommand(ICommandSender sender, String command, String color1, String color2, String reset, String prefix) {
        if (!command.equals("help")) {
            return false;
        }

        sender.addChatMessage(new ChatComponentText(color1 + prefix + "Available commands:" + reset));
        sender.addChatMessage(new ChatComponentText(color1 + "prefix:" + color2 + "Set the chat prefix" + reset));
        sender.addChatMessage(new ChatComponentText(color1 + "crs:" + color2 + "Set the hotbar slot for checkpoint return" + reset));
        sender.addChatMessage(new ChatComponentText(color1 + "css:" + color2 + "Set the hotbar slot for checkpoint set" + reset));
        sender.addChatMessage(new ChatComponentText(color1 + "tpdist:" + color2 + "Set forward teleport distance" + reset));
        sender.addChatMessage(new ChatComponentText(color1 + "color1:" + color2 + "Set the first chat color" + reset));
        sender.addChatMessage(new ChatComponentText(color1 + "color2:" + color2 + "Set the second chat color" + reset));
        sender.addChatMessage(new ChatComponentText(color1 + "list:" + color2 + "Show available colors" + reset));
        return true;
    }

    private boolean handlePrefixCommand(ICommandSender sender, String[] args, String command, String color1, String color2, String reset, String prefix) {
        if (!command.equals("prefix")) {
            return false;
        }

        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: /meowtils prefix <text>" + reset));
            return true;
        }

        StringBuilder newPrefixText = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            newPrefixText.append(args[i]).append(" ");
        }

        configManager.setPrefixText(newPrefixText.toString().trim());
        String updatedPrefix = configManager.getPrefix();

        sender.addChatMessage(new ChatComponentText(color1 + updatedPrefix + color2 + "Prefix set to: [" + configManager.getPrefixText() + "]" + reset));
        return true;
    }

    private boolean handleCheckpointReturnSlotCommand(ICommandSender sender, String[] args, String command, String color1, String color2, String reset, String prefix) {
        if (!command.equals("crs") && !command.equals("checkpointreturnerslot")) {
            return false;
        }

        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: /meowtils crs <slot> (1-9)" + reset));
            return true;
        }

        try {
            int slot = Integer.parseInt(args[1]) - 1;
            if (slot < 0 || slot > 8) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Slot must be between 1 and 9." + reset));
                return true;
            }

            configManager.setCpReturnSlot(slot);
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Checkpoint return slot set to " + (slot + 1) + "." + reset));
        } catch (NumberFormatException e) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid slot number." + reset));
        }
        return true;
    }

    private boolean handleCheckpointSetSlotCommand(ICommandSender sender, String[] args, String command, String color1, String color2, String reset, String prefix) {
        if (!command.equals("css") && !command.equals("checkpointsetslot")) {
            return false;
        }

        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: /meowtils css <slot> (1-9)" + reset));
            return true;
        }

        try {
            int slot = Integer.parseInt(args[1]) - 1;
            if (slot < 0 || slot > 8) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Slot must be between 1 and 9." + reset));
                return true;
            }

            configManager.setCpSetSlot(slot);
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Checkpoint set slot set to " + (slot + 1) + "." + reset));
        } catch (NumberFormatException e) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid slot number." + reset));
        }
        return true;
    }

    private boolean handleTeleportForwardDistanceCommand(ICommandSender sender, String[] args, String command, String color1, String color2, String reset, String prefix) {
        if (!command.equals("tpdist") && !command.equals("teleportforwarddistance")) {
            return false;
        }

        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: /meowtils tpdist <value|infinite>" + reset));
            return true;
        }

        String raw = args[1];
        if (raw.equalsIgnoreCase("infinite")) {
            configManager.setTpForwardDistance(-1.0);
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Teleport forward distance set to infinite." + reset));
            return true;
        }

        try {
            double distance = Double.parseDouble(raw);
            if (distance <= 0.0) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Distance must be greater than 0, or use 'infinite'." + reset));
                return true;
            }

            configManager.setTpForwardDistance(distance);
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Teleport forward distance set to " + distance + "." + reset));
        } catch (NumberFormatException e) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid distance. Use a number or 'infinite'." + reset));
        }
        return true;
    }

    private boolean handleColorCommand(ICommandSender sender, String[] args, String command, String color1, String color2, String reset, String prefix) {
        if (!command.equals("color1") && !command.equals("color2")) {
            return false;
        }

        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid arguments. " + getCommandUsage(sender) + reset));
            return true;
        }

        String colorName = args[1].toLowerCase();
        if (!configManager.getColorMap().containsKey(colorName)) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Unknown color: " + colorName + ". Use /meowtils list to see available colors." + reset));
            return true;
        }

        if (command.equals("color1")) {
            configManager.setColor1(colorName);
            String updatedColor1 = configManager.getColor1();
            sender.addChatMessage(new ChatComponentText(updatedColor1 + prefix + color2 + "Color1 set to " + colorName + "." + reset));
        } else {
            configManager.setColor2(colorName);
            String updatedColor2 = configManager.getColor2();
            sender.addChatMessage(new ChatComponentText(color1 + prefix + updatedColor2 + "Color2 set to " + colorName + "." + reset));
        }

        return true;
    }

    @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
    @Override public List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) { return new ArrayList(); }
    @Override public boolean isUsernameIndex(String[] args, int index) { return false; }
    @Override public int compareTo(ICommand o) { return 0; }
}
