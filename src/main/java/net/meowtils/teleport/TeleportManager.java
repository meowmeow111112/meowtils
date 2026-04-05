package net.meowtils.teleport;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Vec3;

public class TeleportManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    private boolean pendingTeleportRotation = false;
    private float pendingYaw, pendingPitch;
    private Vec3 expectedTeleportPos = null;
    private long teleportStartTime = 0;
    private boolean useTightTolerance = false;
    private static final long TELEPORT_TIMEOUT_MS = 5000; // 5 seconds
    private static final double NORMAL_COORD_TOLERANCE = 0.1;
    private static final double TIGHT_COORD_TOLERANCE = 1e-8;

    private boolean suppressNextTeleportServerMessage = false;

    public void setTeleportRotation(float yaw, float pitch, Vec3 expectedPos) {
        pendingTeleportRotation = true;
        pendingYaw = yaw;
        pendingPitch = pitch;
        expectedTeleportPos = expectedPos;
        teleportStartTime = System.currentTimeMillis();
        suppressNextTeleportServerMessage = true;

        if (expectedTeleportPos != null) {
            double dx = Math.abs(mc.thePlayer.posX - expectedTeleportPos.xCoord);
            double dy = Math.abs(mc.thePlayer.posY - expectedTeleportPos.yCoord);
            double dz = Math.abs(mc.thePlayer.posZ - expectedTeleportPos.zCoord);
            useTightTolerance = dx <= NORMAL_COORD_TOLERANCE && dy <= NORMAL_COORD_TOLERANCE && dz <= NORMAL_COORD_TOLERANCE;
        } else {
            useTightTolerance = false;
        }
    }

    public boolean shouldSuppressNextMessage() {
        return suppressNextTeleportServerMessage;
    }

    public void clearSuppression() {
        suppressNextTeleportServerMessage = false;
    }

    public void onTeleportMessageReceived() {
        // Primary method: teleport confirmed by server message
        if (pendingTeleportRotation) {
            applyPendingRotation();
        }
    }

    private void applyPendingRotation() {
        mc.thePlayer.rotationYaw = pendingYaw;
        mc.thePlayer.rotationPitch = pendingPitch;
        mc.thePlayer.prevRotationYaw = pendingYaw;
        mc.thePlayer.prevRotationPitch = pendingPitch;
        pendingTeleportRotation = false;
        expectedTeleportPos = null;
        teleportStartTime = 0;
        useTightTolerance = false;
    }

    public void onClientTick() {
        if (mc.thePlayer == null) return;

        // Backup method: coordinate-based detection with timing safeguards
        if (pendingTeleportRotation && expectedTeleportPos != null) {
            long currentTime = System.currentTimeMillis();
            long timeSinceTeleport = currentTime - teleportStartTime;

            double dx = Math.abs(mc.thePlayer.posX - expectedTeleportPos.xCoord);
            double dy = Math.abs(mc.thePlayer.posY - expectedTeleportPos.yCoord);
            double dz = Math.abs(mc.thePlayer.posZ - expectedTeleportPos.zCoord);
            double tolerance = useTightTolerance ? TIGHT_COORD_TOLERANCE : NORMAL_COORD_TOLERANCE;

            if (dx <= tolerance && dy <= tolerance && dz <= tolerance) {
                applyPendingRotation();
            }

            // Timeout: if teleport takes too long, assume it failed and clear state
            if (timeSinceTeleport > TELEPORT_TIMEOUT_MS) {
                pendingTeleportRotation = false;
                expectedTeleportPos = null;
                teleportStartTime = 0;
                useTightTolerance = false;
            }
        }
    }
}
