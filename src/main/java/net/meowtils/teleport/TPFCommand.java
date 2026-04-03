package net.meowtils.teleport;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;

public class TPFCommand implements ICommand {
    private final Minecraft mc = Minecraft.getMinecraft();

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
            sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
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
            sender.addChatMessage(new ChatComponentText("Invalid number format"));
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
    @Override public List addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) { return new ArrayList(); }
    @Override public boolean isUsernameIndex(String[] args, int index) { return false; }
    @Override public int compareTo(ICommand o) { return 0; }
}
