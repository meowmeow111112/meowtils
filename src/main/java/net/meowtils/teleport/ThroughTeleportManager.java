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
    private static final double MAX_VERTICAL_ADJUST = 2.0;

    public ThroughTeleportManager(TeleportCallback callback) {
        this.teleportCallback = callback;
    }

    public void register() {
        ClientRegistry.registerKeyBinding(throughTeleportKey);
    }

    public void onKeyInput(String color1, String color2, String reset, String prefix) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (throughTeleportKey.isPressed()) {
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
            double feetY = eyeY - mc.thePlayer.getEyeHeight();
            double feetZ = eyeZ;

            double adjustedFeetY = findAdjustedFeetY(feetX, feetY, feetZ);
            if (Double.isNaN(adjustedFeetY)) {
                continue;
            }

            teleportCallback.suppressNextTeleportMessage();
            mc.thePlayer.sendChatMessage("/tp " + feetX + " " + adjustedFeetY + " " + feetZ);
            return;
        }

        if (color1 != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "No safe position found through the target block!" + reset));
        }
    }

    private boolean isPlayerBoxClear(double x, double standY, double z) {
        double playerHalfWidth = mc.thePlayer.width / 2.0;
        double playerHeight = mc.thePlayer.height;

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

    private double findAdjustedFeetY(double x, double feetY, double z) {
        if (isPlayerBoxClear(x, feetY, z)) {
            return feetY;
        }

        for (double offset = VERTICAL_ADJUST_STEP; offset <= MAX_VERTICAL_ADJUST; offset += VERTICAL_ADJUST_STEP) {
            double lowerY = feetY - offset;
            if (isPlayerBoxClear(x, lowerY, z)) {
                return lowerY;
            }

            double higherY = feetY + offset;
            if (isPlayerBoxClear(x, higherY, z)) {
                return higherY;
            }
        }

        return Double.NaN;
    }

    public interface TeleportCallback {
        void suppressNextTeleportMessage();
    }
}