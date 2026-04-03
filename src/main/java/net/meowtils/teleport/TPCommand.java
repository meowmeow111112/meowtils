package net.meowtils.teleport;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TPCommand implements ICommand {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final TeleportManager teleportManager;

    public TPCommand(TeleportManager teleportManager) {
        this.teleportManager = teleportManager;
    }

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
            forwardToServer(args);
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
            teleportManager.setTeleportRotation(yaw, pitch, new Vec3(x, y, z));

        } catch (NumberFormatException e) {
            forwardToServer(args);
        }
    }

    private void forwardToServer(String[] args) {
        StringBuilder cmd = new StringBuilder("/tp");
        for (String arg : args) {
            cmd.append(" ").append(arg);
        }
        mc.thePlayer.sendChatMessage(cmd.toString());
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
    @Override public List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) { return new ArrayList(); }
    @Override public boolean isUsernameIndex(String[] args, int index) { return false; }
    @Override public int compareTo(ICommand o) { return 0; }
}
