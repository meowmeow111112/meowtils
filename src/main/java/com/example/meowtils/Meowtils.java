package com.example.meowtils;

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
    //Message formatting variables
    private static String color1 = EnumChatFormatting.GREEN.toString(); // Green - for prefix
    private static String color2 = EnumChatFormatting.AQUA.toString(); // Aqua - for messages
    private static String reset = EnumChatFormatting.RESET.toString();
    private static String prefixText = "Meowtils";
    private static String prefix = "[" + prefixText + "] ";

    // Color map for named colors
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

    // Checkpoint storage
    private Vec3 checkpointPos = null;
    private float checkpointYaw = 0, checkpointPitch = 0;
    private boolean hotbarCPActive = true; // toggleable hotbar CP system
    private boolean rightMouseWasDown = false; // to detect right-click press

    // Pending rotation after server teleport confirmation
    private boolean pendingTeleportRotation = false;
    private float pendingYaw, pendingPitch;
    private Vec3 expectedTeleportPos = null;

    // Teleport message suppression
    private boolean suppressNextTeleportServerMessage = false;

    // Keybinds
    private final KeyBinding cpSetKey = new KeyBinding("Set CP", Keyboard.KEY_K, "TPPlus Hotbar CP");
    private final KeyBinding cpReturnKey = new KeyBinding("Return to CP", Keyboard.KEY_L, "TPPlus Hotbar CP");
    private final KeyBinding hotbarToggleKey = new KeyBinding("Toggle Hotbar CP System", Keyboard.KEY_H, "TPPlus Hotbar CP");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Register keybinds
        ClientRegistry.registerKeyBinding(cpSetKey);
        ClientRegistry.registerKeyBinding(cpReturnKey);
        ClientRegistry.registerKeyBinding(hotbarToggleKey);

        // Register commands
        ClientCommandHandler.instance.registerCommand(new TPCommand());
        ClientCommandHandler.instance.registerCommand(new TPFCommand());
        ClientCommandHandler.instance.registerCommand(new SetCPCommand());
        ClientCommandHandler.instance.registerCommand(new GoCPCommand());
        ClientCommandHandler.instance.registerCommand(new TPUCommand());

        // Register this class for event handling
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent event) {
        if (!suppressNextTeleportServerMessage) return;

        String plain = event.message.getUnformattedText();
        if (plain.contains("Teleporting you to") || plain.contains("Teleporting you")) {
            event.setCanceled(true);
            suppressNextTeleportServerMessage = false;
        }
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (mc.thePlayer == null) return;

        if (cpSetKey.isPressed()) setCP();
        if (cpReturnKey.isPressed()) returnToCP();
        if (hotbarToggleKey.isPressed()) {
            hotbarCPActive = !hotbarCPActive;
            mc.thePlayer.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Hotbar CP System is now " + (hotbarCPActive ? "ON" : "OFF") + "." + reset));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Handle pending rotation after server teleport confirmation
        if (pendingTeleportRotation && expectedTeleportPos != null) {
            double dx = Math.abs(mc.thePlayer.posX - expectedTeleportPos.xCoord);
            double dy = Math.abs(mc.thePlayer.posY - expectedTeleportPos.yCoord);
            double dz = Math.abs(mc.thePlayer.posZ - expectedTeleportPos.zCoord);

            if (dx <= 0.1 && dy <= 0.1 && dz <= 0.1) {
                mc.thePlayer.rotationYaw = pendingYaw;
                mc.thePlayer.rotationPitch = pendingPitch;
                mc.thePlayer.prevRotationYaw = pendingYaw;
                mc.thePlayer.prevRotationPitch = pendingPitch;
                pendingTeleportRotation = false;
                expectedTeleportPos = null;
            }
        }

        if (!hotbarCPActive) return;

        int slot = mc.thePlayer.inventory.currentItem; // 0-8
        boolean rightMouseDown = Mouse.isButtonDown(1);

        if (rightMouseDown && !rightMouseWasDown) {
            if (slot == 0) { // slot 1: return to CP
                returnToCP();
            } else if (slot == 2) { // slot 3: set CP
                setCP();
            }
        }

        rightMouseWasDown = rightMouseDown;
    }

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

        // Send /tp command to server for position
        mc.thePlayer.sendChatMessage("/tp " + checkpointPos.xCoord + " " + checkpointPos.yCoord + " " + checkpointPos.zCoord);
        suppressNextTeleportServerMessage = true;

        // Schedule rotation when confirmed teleport happens (reactive)
        pendingTeleportRotation = true;
        pendingYaw = checkpointYaw;
        pendingPitch = checkpointPitch;
        expectedTeleportPos = checkpointPos;
    }

    // -------------------- COMMANDS --------------------

    private class TPCommand implements ICommand {
        @Override
        public String getCommandName() { return "tpp"; }

        @Override
        public String getCommandUsage(ICommandSender sender) { return "/tpp <x> <y> <z> [yaw] [pitch] (use ~ for relative)"; }

        @Override
        public List getCommandAliases() { return new ArrayList<String>(); }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 3 || args.length > 5) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: " + getCommandUsage(sender) + reset));
                return;
            }

            try {
                // Parse coordinates
                double x = parseCoordinate(args[0], mc.thePlayer.posX, false);
                double y = parseCoordinate(args[1], mc.thePlayer.posY, false);
                double z = parseCoordinate(args[2], mc.thePlayer.posZ, false);

                float yaw = mc.thePlayer.rotationYaw;
                float pitch = mc.thePlayer.rotationPitch;

                if (args.length >= 4) {
                    yaw = (float) parseCoordinate(args[3], mc.thePlayer.rotationYaw, true);
                }
                if (args.length == 5) {
                    pitch = (float) parseCoordinate(args[4], mc.thePlayer.rotationPitch, true);
                }

                // Send /tp command to server
                mc.thePlayer.sendChatMessage("/tp " + x + " " + y + " " + z);
                suppressNextTeleportServerMessage = true;

                // Schedule rotation when confirmed teleport happens (reactive)
                pendingTeleportRotation = true;
                pendingYaw = yaw;
                pendingPitch = pitch;
                expectedTeleportPos = new Vec3(x, y, z);

            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid number format" + reset));
            }
        }

        private double parseCoordinate(String arg, double current, boolean isRotation) throws NumberFormatException {
            if (arg.startsWith("~")) {
                String value = arg.substring(1);
                double offset = value.isEmpty() ? 0 : Double.parseDouble(value);
                return current + offset;
            } else {
                return Double.parseDouble(arg);
            }
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) { return new ArrayList<String>(); }
        @Override
        public boolean isUsernameIndex(String[] args, int index) { return false; }
        @Override
        public int compareTo(ICommand o) { return 0; }
    }

    private class TPFCommand implements ICommand {
        @Override
        public String getCommandName() { return "tpf"; }

        @Override
        public String getCommandUsage(ICommandSender sender) { return "/tpf <yaw> [pitch] (use ~ for relative)"; }

        @Override
        public List getCommandAliases() { return new ArrayList<String>(); }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 1 || args.length > 2) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: " + getCommandUsage(sender) + reset));
                return;
            }

            try {
                float yaw = (float) parseCoordinate(args[0], mc.thePlayer.rotationYaw, true);
                float pitch = mc.thePlayer.rotationPitch;

                if (args.length == 2) {
                    pitch = (float) parseCoordinate(args[1], mc.thePlayer.rotationPitch, true);
                }

                // Set rotation directly
                mc.thePlayer.rotationYaw = yaw;
                mc.thePlayer.rotationPitch = pitch;
                mc.thePlayer.prevRotationYaw = yaw;
                mc.thePlayer.prevRotationPitch = pitch;

            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid number format" + reset));
            }
        }

        private double parseCoordinate(String arg, double current, boolean isRotation) throws NumberFormatException {
            if (arg.startsWith("~")) {
                String value = arg.substring(1);
                double offset = value.isEmpty() ? 0 : Double.parseDouble(value);
                return current + offset;
            } else {
                return Double.parseDouble(arg);
            }
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) { return new ArrayList<String>(); }
        @Override
        public boolean isUsernameIndex(String[] args, int index) { return false; }
        @Override
        public int compareTo(ICommand o) { return 0; }
    }

    private class SetCPCommand implements ICommand {
        @Override
        public String getCommandName() { return "setcp"; }
        @Override
        public String getCommandUsage(ICommandSender sender) { return "/setcp"; }
        @Override
        public List getCommandAliases() { return new ArrayList<String>(); }
        @Override
        public void processCommand(ICommandSender sender, String[] args) { setCP(); }
        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) { return new ArrayList<String>(); }
        @Override
        public boolean isUsernameIndex(String[] args, int index) { return false; }
        @Override
        public int compareTo(ICommand o) { return 0; }
    }

    private class GoCPCommand implements ICommand {
        @Override
        public String getCommandName() { return "gocp"; }
        @Override
        public String getCommandUsage(ICommandSender sender) { return "/gocp"; }
        @Override
        public List getCommandAliases() { return new ArrayList<String>(); }
        @Override
        public void processCommand(ICommandSender sender, String[] args) { returnToCP(); }
        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) { return new ArrayList<String>(); }
        @Override
        public boolean isUsernameIndex(String[] args, int index) { return false; }
        @Override
        public int compareTo(ICommand o) { return 0; }
    }

    private class TPUCommand implements ICommand {
        @Override
        public String getCommandName() { return "tpu"; }

        @Override
        public String getCommandUsage(ICommandSender sender) { return "/tpu <color1|color2> <color_name> | /tpu prefix <text> | /tpu list"; }

        @Override
        public List getCommandAliases() { return new ArrayList<String>(); }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length == 0) {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: " + getCommandUsage(sender) + reset));
                return;
            }

            if (args[0].equalsIgnoreCase("list")) {
                StringBuilder sb = new StringBuilder(color1 + prefix + color2 + "Available colors: " + reset);
                for (String colorName : COLOR_MAP.keySet()) {
                    sb.append(colorName).append(", ");
                }
                sb.setLength(sb.length() - 2); // remove last comma
                sender.addChatMessage(new ChatComponentText(sb.toString()));
                return;
            }

            if (args[0].equalsIgnoreCase("prefix")) {
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Usage: /tpu prefix <text>" + reset));
                    return;
                }
                StringBuilder newPrefixText = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    newPrefixText.append(args[i]).append(" ");
                }
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
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Unknown color: " + colorName + ". Use /tpu list to see available colors." + reset));
                return;
            }

            if (colorVar.equals("color1")) {
                color1 = COLOR_MAP.get(colorName);
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Color1 set to " + colorName + "." + reset));
            } else if (colorVar.equals("color2")) {
                color2 = COLOR_MAP.get(colorName);
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Color2 set to " + colorName + "." + reset));
            } else {
                sender.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Invalid color variable. Use color1 or color2." + reset));
            }
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) { return new ArrayList<String>(); }
        @Override
        public boolean isUsernameIndex(String[] args, int index) { return false; }
        @Override
        public int compareTo(ICommand o) { return 0; }
    }
}