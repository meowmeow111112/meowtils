package net.meowtils.teleport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class ForwardTeleportManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    private final KeyBinding forwardTeleportKey = new KeyBinding("Teleport Forward", Keyboard.KEY_G, "Meowtils");
    private final TeleportCallback teleportCallback;
    private static final double MAX_FORWARD_DISTANCE = 256.0;

    public ForwardTeleportManager(TeleportCallback callback) {
        this.teleportCallback = callback;
    }

    public void register() {
        ClientRegistry.registerKeyBinding(forwardTeleportKey);
    }

    public void onKeyInput(String color1, String color2, String reset, String prefix, double configuredDistance) {
        if (mc.thePlayer == null) return;

        if (forwardTeleportKey.isPressed()) {
            performForwardTeleport(color1, color2, reset, prefix, configuredDistance);
        }
    }

    private void performForwardTeleport(String color1, String color2, String reset, String prefix, double configuredDistance) {
        double distance = configuredDistance <= 0.0 ? MAX_FORWARD_DISTANCE : configuredDistance;

        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVec = mc.thePlayer.getLook(1.0F);
        Vec3 targetPos = eyePos.addVector(
            lookVec.xCoord * distance,
            lookVec.yCoord * distance,
            lookVec.zCoord * distance
        ).addVector(0, -mc.thePlayer.getEyeHeight(), 0); // Convert eye-position target back to feet-position target.

        // Register suppression state before sending the teleport command.
        teleportCallback.suppressNextTeleportMessage();
        mc.thePlayer.sendChatMessage("/tp " + targetPos.xCoord + " " + targetPos.yCoord + " " + targetPos.zCoord);
    }

    public interface TeleportCallback {
        void suppressNextTeleportMessage();
    }
}