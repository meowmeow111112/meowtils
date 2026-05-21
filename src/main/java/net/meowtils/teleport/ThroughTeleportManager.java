package net.meowtils.teleport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class ThroughTeleportManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    private final KeyBinding throughTeleportKey = new KeyBinding("Teleport Through", Keyboard.KEY_V, "Meowtils");
    private final TeleportCallback teleportCallback;
    private static final double RAY_CAST_DISTANCE = 256.0;
    private static final double COLLISION_EPSILON = 1.0E-4;
    private static final double FACE_OFFSET = 0.05;
    private static final double SEARCH_STEP = 0.01;

    private static final double VERTICAL_ADJUST_STEP = 0.05;
    private static final double SNAP_SCALE = 16.0;
    private static final double LILYPAD_HEIGHT = 0.015625;
    private static final double Y_EPSILON = 1.0E-8;

    public ThroughTeleportManager(TeleportCallback callback) {
        this.teleportCallback = callback;
    }

    public void register() {
        ClientRegistry.registerKeyBinding(throughTeleportKey);
    }

    public void onKeyInput(String color1, String color2, String reset, String prefix, boolean parkourBlocked) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (throughTeleportKey.isPressed() && !parkourBlocked) {
            performThroughTeleport(color1, color2, reset, prefix);
        }
    }

    private void performThroughTeleport(String color1, String color2, String reset, String prefix) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVec = mc.thePlayer.getLook(1.0F);
        Vec3 reachVec = eyePos.addVector(lookVec.xCoord * RAY_CAST_DISTANCE, lookVec.yCoord * RAY_CAST_DISTANCE, lookVec.zCoord * RAY_CAST_DISTANCE);

        MovingObjectPosition rayTraceResult = mc.theWorld.rayTraceBlocks(eyePos, reachVec, false, false, false);

        if (rayTraceResult == null || rayTraceResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || rayTraceResult.hitVec == null) {
            if (color1 != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "No block in sight!" + reset));
            }
            return;
        }

        Vec3 faceNormal = new Vec3(
            rayTraceResult.sideHit.getFrontOffsetX(),
            rayTraceResult.sideHit.getFrontOffsetY(),
            rayTraceResult.sideHit.getFrontOffsetZ()
        );
        Vec3 throughDirection = new Vec3(-faceNormal.xCoord, -faceNormal.yCoord, -faceNormal.zCoord);
        Vec3 searchStart = rayTraceResult.hitVec.addVector(throughDirection.xCoord * FACE_OFFSET, throughDirection.yCoord * FACE_OFFSET, throughDirection.zCoord * FACE_OFFSET);
        BlockPos blockPos = rayTraceResult.getBlockPos();
        double centerX = blockPos.getX() + 0.5D;
        double centerZ = blockPos.getZ() + 0.5D;

        for (double distance = 0.0; distance <= RAY_CAST_DISTANCE; distance += SEARCH_STEP) {
            double eyeX = rayTraceResult.hitVec.xCoord;
            double eyeY = rayTraceResult.hitVec.yCoord;
            double eyeZ = rayTraceResult.hitVec.zCoord;

            switch (rayTraceResult.sideHit) {
                case WEST:
                case EAST:
                    eyeX = searchStart.xCoord + throughDirection.xCoord * distance;
                    eyeZ = centerZ;
                    break;
                case NORTH:
                case SOUTH:
                    eyeX = centerX;
                    eyeZ = searchStart.zCoord + throughDirection.zCoord * distance;
                    break;
                case DOWN:
                case UP:
                    eyeX = centerX;
                    eyeY = searchStart.yCoord + throughDirection.yCoord * distance;
                    eyeZ = centerZ;
                    break;
                default:
                    break;
            }

            double feetX = eyeX;
            double feetZ = eyeZ;

            // Only attempt teleport when a zero-height probe (point) is clear at the sampled line.
            if (!isProbePointClear(eyeX, eyeY, eyeZ)) {
                continue;
            }

            double eyeHeight = mc.thePlayer.getEyeHeight();
            double playerHeight = mc.thePlayer.height;

            // Candidate anchor positions relative to probe Y:
            //  - eyeAlignedFeet: feet such that eye is at probe Y
            //  - feetAtProbe: feet such that feet are at probe Y (highest feet)
            //  - headAlignedFeet: feet such that head/top is at probe Y (lowest feet)
            double eyeAlignedFeet = eyeY - eyeHeight;
            double feetAtProbe = eyeY;
            double headAlignedFeet = eyeY - playerHeight;

            // If eye-aligned placement is valid, prefer it (preserves where the eye was aimed).
            if (isPlayerBoxClear(feetX, eyeAlignedFeet, feetZ)) {
                teleportCallback.suppressNextTeleportMessage();
                mc.thePlayer.sendChatMessage("/tp " + feetX + " " + eyeAlignedFeet + " " + feetZ);
                return;
            }

            // Fast-head fallback: place head directly under the block above the current probe block.
            BlockPos probeBlockPos = new BlockPos(eyeX, eyeY, eyeZ);
            double blockAboveProbeY = probeBlockPos.getY() + 1.0;
            double feetWithHeadUnderBlockAbove = blockAboveProbeY - playerHeight;
            if (isPlayerBoxClear(feetX, feetWithHeadUnderBlockAbove, feetZ)) {
                teleportCallback.suppressNextTeleportMessage();
                mc.thePlayer.sendChatMessage("/tp " + feetX + " " + feetWithHeadUnderBlockAbove + " " + feetZ);
                return;
            }

            // Otherwise, perform a stepped search starting at the head-aligned feet (lowest)
            // and move up toward feetAtProbe so we prefer landing on a block (feet above floor).
            double step = VERTICAL_ADJUST_STEP;
            if (headAlignedFeet <= feetAtProbe) {
                for (double y = headAlignedFeet; y <= feetAtProbe; y += step) {
                    double candidateY = getMarchCandidateY(feetX, y, feetZ);
                    if (!Double.isNaN(candidateY)) {
                        teleportCallback.suppressNextTeleportMessage();
                        mc.thePlayer.sendChatMessage("/tp " + feetX + " " + candidateY + " " + feetZ);
                        return;
                    }
                }
            } else {
                for (double y = headAlignedFeet; y >= feetAtProbe; y -= step) {
                    double candidateY = getMarchCandidateY(feetX, y, feetZ);
                    if (!Double.isNaN(candidateY)) {
                        teleportCallback.suppressNextTeleportMessage();
                        mc.thePlayer.sendChatMessage("/tp " + feetX + " " + candidateY + " " + feetZ);
                        return;
                    }
                }
            }
        }

        if (color1 != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "No safe position found through the target block!" + reset));
        }
    }

    private boolean isPlayerBoxClear(double x, double standY, double z) {
        return isPlayerBoxClear(x, standY, z, mc.thePlayer.height);
    }

    

    private boolean isProbePointClear(double x, double y, double z) {
        double playerHalfWidth = mc.thePlayer.width / 2.0;

        AxisAlignedBB probeBox = new AxisAlignedBB(
            x - playerHalfWidth,
            y,
            z - playerHalfWidth,
            x + playerHalfWidth,
            y,
            z + playerHalfWidth
        );

        AxisAlignedBB expandedProbeBox = probeBox.expand(COLLISION_EPSILON, 0.0, COLLISION_EPSILON);
        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, expandedProbeBox).isEmpty();
    }

    private boolean isPlayerBoxClear(double x, double standY, double z, double playerHeight) {
        double playerHalfWidth = mc.thePlayer.width / 2.0;

        AxisAlignedBB playerBox = new AxisAlignedBB(
            x - playerHalfWidth,
            standY,
            z - playerHalfWidth,
            x + playerHalfWidth,
            standY + playerHeight,
            z + playerHalfWidth
        );

        AxisAlignedBB expandedPlayerBox = playerBox.expand(COLLISION_EPSILON, 0.0, COLLISION_EPSILON);
        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, expandedPlayerBox).isEmpty();
    }

    private double getMarchCandidateY(double x, double y, double z) {
        double snappedY = Math.floor((y * SNAP_SCALE) + Y_EPSILON) / SNAP_SCALE;
        if (isPlayerBoxClear(x, snappedY, z)) {
            return snappedY;
        }

        // If snappedY is on a whole block level (.. .00), try lilypad-height offset.
        if (Math.abs(snappedY - Math.floor(snappedY + Y_EPSILON)) <= Y_EPSILON) {
            double lilyAdjustedY = snappedY + LILYPAD_HEIGHT;
            if (isPlayerBoxClear(x, lilyAdjustedY, z)) {
                return lilyAdjustedY;
            }
        }

        return Double.NaN;
    }

    public interface TeleportCallback {
        void suppressNextTeleportMessage();
    }
}