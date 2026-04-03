package net.meowtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

@Mod(modid = "meowtils", name = "Meowtils", version = "1.0", clientSideOnly = true)
public class Meowtils {

    private static String color1 = EnumChatFormatting.GREEN.toString();
    private static String color2 = EnumChatFormatting.AQUA.toString();
    private static String reset = EnumChatFormatting.RESET.toString();
    private static String prefixText = "Meowtils";
    private static String prefix = "[" + prefixText + "] ";

    private static final java.util.Map<String, String> COLOR_MAP = new java.util.HashMap<String, String>();

    static {
        COLOR_MAP.put("black", EnumChatFormatting.BLACK.toString());
        COLOR_MAP.put("dark_blue", EnumChatFormatting.DARK_BLUE.toString());
        COLOR_MAP.put("dark_green", EnumChatFormatting.DARK_GREEN.toString());
        COLOR_MAP.put("dark_aqua", EnumChatFormatting.DARK_AQUA.toString());
        COLOR_MAP.put("dark_red", EnumChatFormatting.DARK_RED.toString());
        COLOR_MAP.put("dark_purple", EnumChatFormatting.DARK_PURPLE.toString());
        COLOR_MAP.put("gold", EnumChatFormatting.GOLD.toString());
        COLOR_MAP.put("gray", EnumChatFormatting.GRAY.toString());
        COLOR_MAP.put("dark_gray", EnumChatFormatting.DARK_GRAY.toString());
        COLOR_MAP.put("blue", EnumChatFormatting.BLUE.toString());
        COLOR_MAP.put("green", EnumChatFormatting.GREEN.toString());
        COLOR_MAP.put("aqua", EnumChatFormatting.AQUA.toString());
        COLOR_MAP.put("red", EnumChatFormatting.RED.toString());
        COLOR_MAP.put("light_purple", EnumChatFormatting.LIGHT_PURPLE.toString());
        COLOR_MAP.put("yellow", EnumChatFormatting.YELLOW.toString());
        COLOR_MAP.put("white", EnumChatFormatting.WHITE.toString());
    }

    private final Minecraft mc = Minecraft.getMinecraft();

    private Vec3 checkpointPos = null;
    private float checkpointYaw = 0, checkpointPitch = 0;
    private boolean hotbarCPActive = true;
    private boolean rightMouseWasDown = false;

    private boolean pendingTeleportRotation = false;
    private float pendingYaw, pendingPitch;
    private Vec3 expectedTeleportPos = null;

    private boolean suppressNextTeleportServerMessage = false;

    private final KeyBinding cpSetKey = new KeyBinding("Set CP", Keyboard.KEY_K, "Meowtils");
    private final KeyBinding cpReturnKey = new KeyBinding("Return to CP", Keyboard.KEY_L, "Meowtils");
    private final KeyBinding hotbarToggleKey = new KeyBinding("Toggle Hotbar CP System", Keyboard.KEY_H, "Meowtils");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(cpSetKey);
        ClientRegistry.registerKeyBinding(cpReturnKey);
        ClientRegistry.registerKeyBinding(hotbarToggleKey);

        ClientCommandHandler.instance.registerCommand(new TPCommand());
        ClientCommandHandler.instance.registerCommand(new TPFCommand());
        ClientCommandHandler.instance.registerCommand(new TPUCommand());

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    // ================= CHECKPOINT LOGIC =================

    private void setCP() {
        checkpointPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        checkpointYaw = mc.thePlayer.rotationYaw;
        checkpointPitch = mc.thePlayer.rotationPitch;
        mc.thePlayer.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Checkpoint saved!" + reset));
    }

    private void returnToCP() {
        if (checkpointPos == null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "No checkpoint saved!" + reset));
            return;
        }

        mc.thePlayer.sendChatMessage("/tp " + checkpointPos.xCoord + " " + checkpointPos.yCoord + " " + checkpointPos.zCoord);
        suppressNextTeleportServerMessage = true;

        pendingTeleportRotation = true;
        pendingYaw = checkpointYaw;
        pendingPitch = checkpointPitch;
        expectedTeleportPos = checkpointPos;
    }

    // ================= COMMANDS =================

    private class TPCommand implements ICommand {
        @Override
        public String getCommandName() { return "tp"; }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/tp <x> <y> <z> [yaw] [pitch] (use ~ for relative)";
        }

        @Override
        public List getCommandAliases() { return new ArrayList(); }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 3 || args.length > 5) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: " + getCommandUsage(sender) + reset));
                return;
            }

            try {
                double x = parseCoordinate(args[0], mc.thePlayer.posX);
                double y = parseCoordinate(args[1], mc.thePlayer.posY);
                double z = parseCoordinate(args[2], mc.thePlayer.posZ);

                float yaw = mc.thePlayer.rotationYaw;
                float pitch = mc.thePlayer.rotationPitch;

                if (args.length >= 4)
                    yaw = (float) parseCoordinate(args[3], mc.thePlayer.rotationYaw);
                if (args.length == 5)
                    pitch = (float) parseCoordinate(args[4], mc.thePlayer.rotationPitch);

                mc.thePlayer.sendChatMessage("/tp " + x + " " + y + " " + z);
                suppressNextTeleportServerMessage = true;

                pendingTeleportRotation = true;
                pendingYaw = yaw;
                pendingPitch = pitch;
                expectedTeleportPos = new Vec3(x, y, z);

            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid number format" + reset));
            }
        }

        private double parseCoordinate(String arg, double current) {
            if (arg.startsWith("~")) {
                String value = arg.substring(1);
                double offset = value.isEmpty() ? 0 : Double.parseDouble(value);
                return current + offset;
            }
            return Double.parseDouble(arg);
        }

        @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
        @Override public List addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) { return new ArrayList(); }
        @Override public boolean isUsernameIndex(String[] args, int index) { return false; }
        @Override public int compareTo(ICommand o) { return 0; }
    }

    private class TPFCommand implements ICommand {
        @Override
        public String getCommandName() { return "tpf"; }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/tpf <yaw> [pitch] (use ~ for relative)";
        }

        @Override
        public List getCommandAliases() { return new ArrayList(); }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 1 || args.length > 2) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: " + getCommandUsage(sender) + reset));
                return;
            }

            try {
                float yaw = (float) parseCoordinate(args[0], mc.thePlayer.rotationYaw);
                float pitch = mc.thePlayer.rotationPitch;

                if (args.length == 2)
                    pitch = (float) parseCoordinate(args[1], mc.thePlayer.rotationPitch);

                mc.thePlayer.rotationYaw = yaw;
                mc.thePlayer.rotationPitch = pitch;
                mc.thePlayer.prevRotationYaw = yaw;
                mc.thePlayer.prevRotationPitch = pitch;

            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid number format" + reset));
            }
        }

        private double parseCoordinate(String arg, double current) {
            if (arg.startsWith("~")) {
                String value = arg.substring(1);
                double offset = value.isEmpty() ? 0 : Double.parseDouble(value);
                return current + offset;
            }
            return Double.parseDouble(arg);
        }

        @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
        @Override public List addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) { return new ArrayList(); }
        @Override public boolean isUsernameIndex(String[] args, int index) { return false; }
        @Override public int compareTo(ICommand o) { return 0; }
    }

    private class TPUCommand implements ICommand {
        @Override
        public String getCommandName() { return "meowtils"; }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/meowtils <color1|color2> <color_name> | /meowtils prefix <text> | /meowtils list";
        }

        @Override
        public List getCommandAliases() {
            List<String> aliases = new ArrayList<String>();
            aliases.add("mt");
            return aliases;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length == 0) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: " + getCommandUsage(sender) + reset));
                return;
            }

            if (args[0].equalsIgnoreCase("list")) {
                StringBuilder sb = new StringBuilder(color1 + prefix + color2 + "Available colors: " + reset);
                for (String colorName : COLOR_MAP.keySet())
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

                prefixText = newPrefixText.toString().trim();
                prefix = "[" + prefixText + "] ";

                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Prefix set to: [" + prefixText + "]" + reset));
                return;
            }

            if (args.length < 2) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid arguments. " + getCommandUsage(sender) + reset));
                return;
            }

            String colorVar = args[0].toLowerCase();
            String colorName = args[1].toLowerCase();

            if (!COLOR_MAP.containsKey(colorName)) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Unknown color: " + colorName + ". Use /meowtils list to see available colors." + reset));
                return;
            }

            if (colorVar.equals("color1")) {
                color1 = COLOR_MAP.get(colorName);
            } else if (colorVar.equals("color2")) {
                color2 = COLOR_MAP.get(colorName);
            } else {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid color variable. Use color1 or color2." + reset));
                return;
            }

            sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Updated successfully." + reset));
        }

        @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
        @Override public List addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) { return new ArrayList(); }
        @Override public boolean isUsernameIndex(String[] args, int index) { return false; }
        @Override public int compareTo(ICommand o) { return 0; }
    }
}