package net.meowtils.teleport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class LookTeleportManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    private final KeyBinding lookTeleportKey = new KeyBinding("Look Teleport", Keyboard.KEY_R, "Meowtils");
    private final TeleportCallback teleportCallback;
    private static final double RAY_CAST_DISTANCE = 256.0;

    public LookTeleportManager(TeleportCallback callback) {
        this.teleportCallback = callback;
    }

    public void register() {
        ClientRegistry.registerKeyBinding(lookTeleportKey);
    }

    public void onKeyInput(String color1, String color2, String reset, String prefix) {
        if (mc.thePlayer == null) return;

        if (lookTeleportKey.isPressed()) {
            performLookTeleport(color1, color2, reset, prefix);
        }
    }

    private void performLookTeleport(String color1, String color2, String reset, String prefix) {
        // Cast a ray from the player's eyes along their look direction
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVec = mc.thePlayer.getLook(1.0F);
        Vec3 reachVec = eyePos.addVector(lookVec.xCoord * RAY_CAST_DISTANCE, lookVec.yCoord * RAY_CAST_DISTANCE, lookVec.zCoord * RAY_CAST_DISTANCE);

        // Use the world's rayTraceBlocks to find what block we're looking at
        MovingObjectPosition rayTraceResult = mc.theWorld.rayTraceBlocks(eyePos, reachVec, false, false, false);

        if (rayTraceResult == null || rayTraceResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            if (color1 != null) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    color1 + prefix + color2 + "No block in sight!" + reset));
            }
            return;
        }

        // Get block position and block
        net.minecraft.util.BlockPos pos = rayTraceResult.getBlockPos();
        net.minecraft.block.Block block = mc.theWorld.getBlockState(pos).getBlock();

        // Get collision bounding box for the block
        net.minecraft.util.AxisAlignedBB boundingBox = block.getCollisionBoundingBox(mc.theWorld, pos, mc.theWorld.getBlockState(pos));

        double x, y, z;

        if (boundingBox != null) {
            // Block has a collision box (e.g., solid block, fence, slab)
            // Calculate standable position accounting for adjacent and overhead blocks
            double[] standablePos = findSafeStandablePosition(pos, boundingBox);
            x = standablePos[0];
            y = boundingBox.maxY;
            z = standablePos[1];
        } else {
            // Fallback: center of block
            x = pos.getX() + 0.5;
            y = pos.getY() + 1.0;
            z = pos.getZ() + 0.5;
        }

        // Send teleport command
        mc.thePlayer.sendChatMessage("/tp " + x + " " + y + " " + z);

        // Register the teleport rotation callback
        teleportCallback.setTeleportRotation(
            mc.thePlayer.rotationYaw,
            mc.thePlayer.rotationPitch,
            new Vec3(x, y, z)
        );

        if (color1 != null) {
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                color1 + prefix + color2 + "Teleported!" + reset));
        }
    }

    private double[] findSafeStandablePosition(net.minecraft.util.BlockPos pos, net.minecraft.util.AxisAlignedBB targetBox) {
        double minX = targetBox.minX;
        double maxX = targetBox.maxX;
        double minZ = targetBox.minZ;
        double maxZ = targetBox.maxZ;
        double topY = targetBox.maxY;
        double playerHalfWidth = 0.3;
        double playerHeight = 1.8;

        // Search candidate positions inside the block's top surface area
        int samples = 5;
        for (int xi = 0; xi < samples; xi++) {
            for (int zi = 0; zi < samples; zi++) {
                double x = minX + ((double) xi / (samples - 1)) * (maxX - minX);
                double z = minZ + ((double) zi / (samples - 1)) * (maxZ - minZ);
                net.minecraft.util.AxisAlignedBB playerBox = new net.minecraft.util.AxisAlignedBB(
                    x - playerHalfWidth,
                    topY,
                    z - playerHalfWidth,
                    x + playerHalfWidth,
                    topY + playerHeight,
                    z + playerHalfWidth
                );

                if (isPlayerBoxClear(pos, playerBox)) {
                    return new double[]{x, z};
                }
            }
        }

        // Fallback to center if none of the sampled positions are clear
        return new double[]{(minX + maxX) / 2.0, (minZ + maxZ) / 2.0};
    }

    private boolean isPlayerBoxClear(net.minecraft.util.BlockPos pos, net.minecraft.util.AxisAlignedBB playerBox) {
        // Check blocks in a 3x3 horizontal area and up to 2 blocks above the target block
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    net.minecraft.util.BlockPos checkPos = pos.add(dx, dy, dz);
                    net.minecraft.block.Block block = mc.theWorld.getBlockState(checkPos).getBlock();
                    net.minecraft.util.AxisAlignedBB box = block.getCollisionBoundingBox(mc.theWorld, checkPos, mc.theWorld.getBlockState(checkPos));

                    if (box != null && box.intersectsWith(playerBox)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public interface TeleportCallback {
        void setTeleportRotation(float yaw, float pitch, Vec3 expectedPos);
    }
}

