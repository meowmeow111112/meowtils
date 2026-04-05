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
        return "/meowtils <color1|color2> <color_name> | /meowtils prefix <text> | /meowtils crs <slot> | /meowtils css <slot> | /meowtils list";
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

        if (args[0].equalsIgnoreCase("list")) {
            StringBuilder sb = new StringBuilder(color1 + prefix + color2 + "Available colors: " + reset);
            for (String colorName : configManager.getColorMap().keySet())
                sb.append(colorName).append(", ");
            sb.setLength(sb.length() - 2);
            sender.addChatMessage(new ChatComponentText(sb.toString()));
            return;
        }

        if (args[0].equalsIgnoreCase("prefix")) {
            if (args.length < 2) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: /meowtils prefix <text>" + reset));
                return;
            }

            StringBuilder newPrefixText = new StringBuilder();
            for (int i = 1; i < args.length; i++)
                newPrefixText.append(args[i]).append(" ");

            configManager.setPrefixText(newPrefixText.toString().trim());
            prefix = configManager.getPrefix();

            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Prefix set to: [" + configManager.getPrefixText() + "]" + reset));
            return;
        }

        if (args[0].equalsIgnoreCase("crs") || args[0].equalsIgnoreCase("checkpointreturnerslot")) {
            if (args.length < 2) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: /meowtils crs <slot> (1-9)" + reset));
                return;
            }

            try {
                int slot = Integer.parseInt(args[1]) - 1; // Convert 1-9 to 0-8
                if (slot < 0 || slot > 8) {
                    sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Slot must be between 1 and 9." + reset));
                    return;
                }
                configManager.setCpReturnSlot(slot);
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Checkpoint return slot set to " + (slot + 1) + "." + reset));
            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid slot number." + reset));
            }
            return;
        }

        if (args[0].equalsIgnoreCase("css") || args[0].equalsIgnoreCase("checkpointsetslot")) {
            if (args.length < 2) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: /meowtils css <slot> (1-9)" + reset));
                return;
            }

            try {
                int slot = Integer.parseInt(args[1]) - 1; // Convert 1-9 to 0-8
                if (slot < 0 || slot > 8) {
                    sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Slot must be between 1 and 9." + reset));
                    return;
                }
                configManager.setCpSetSlot(slot);
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Checkpoint set slot set to " + (slot + 1) + "." + reset));
            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid slot number." + reset));
            }
            return;
        }

        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid arguments. " + getCommandUsage(sender) + reset));
            return;
        }

        String colorVar = args[0].toLowerCase();
        String colorName = args[1].toLowerCase();

        if (!configManager.getColorMap().containsKey(colorName)) {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Unknown color: " + colorName + ". Use /meowtils list to see available colors." + reset));
            return;
        }

        if (colorVar.equals("color1")) {
            configManager.setColor1(colorName);
            color1 = configManager.getColor1();
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Color1 set to " + colorName + "." + reset));
        } else if (colorVar.equals("color2")) {
            configManager.setColor2(colorName);
            color2 = configManager.getColor2();
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Color2 set to " + colorName + "." + reset));
        } else {
            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid color variable. Use color1 or color2." + reset));
        }
    }

    @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
    @Override public List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) { return new ArrayList(); }
    @Override public boolean isUsernameIndex(String[] args, int index) { return false; }
    @Override public int compareTo(ICommand o) { return 0; }
}
