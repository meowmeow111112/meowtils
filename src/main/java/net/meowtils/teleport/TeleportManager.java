package net.meowtils.teleport;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Vec3;

public class TeleportManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    private boolean pendingTeleportRotation = false;
    private float pendingYaw, pendingPitch;
    private Vec3 expectedTeleportPos = null;

    private boolean suppressNextTeleportServerMessage = false;

    public void setTeleportRotation(float yaw, float pitch, Vec3 expectedPos) {
        pendingTeleportRotation = true;
        pendingYaw = yaw;
        pendingPitch = pitch;
        expectedTeleportPos = expectedPos;
        suppressNextTeleportServerMessage = true;
    }

    public boolean shouldSuppressNextMessage() {
        return suppressNextTeleportServerMessage;
    }

    public void clearSuppression() {
        suppressNextTeleportServerMessage = false;
    }

    public void onClientTick() {
        if (mc.thePlayer == null) return;

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
    }
}
