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
            // Calculate standable position accounting for adjacent blocks
            double[] standablePos = calculateStandablePosition(pos, boundingBox);
            x = standablePos[0];
            y = boundingBox.maxY + pos.getY();
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

    private double[] calculateStandablePosition(net.minecraft.util.BlockPos pos, net.minecraft.util.AxisAlignedBB targetBox) {
        // Start with the target block's bounding box as the standable area
        double minX = targetBox.minX;
        double maxX = targetBox.maxX;
        double minZ = targetBox.minZ;
        double maxZ = targetBox.maxZ;

        // Check all 4 horizontal adjacent blocks
        net.minecraft.util.BlockPos[] adjacentPositions = {
            pos.offset(net.minecraft.util.EnumFacing.NORTH),
            pos.offset(net.minecraft.util.EnumFacing.SOUTH),
            pos.offset(net.minecraft.util.EnumFacing.EAST),
            pos.offset(net.minecraft.util.EnumFacing.WEST)
        };

        for (net.minecraft.util.BlockPos adjPos : adjacentPositions) {
            net.minecraft.block.Block adjBlock = mc.theWorld.getBlockState(adjPos).getBlock();
            net.minecraft.util.AxisAlignedBB adjBox = adjBlock.getCollisionBoundingBox(mc.theWorld, adjPos, mc.theWorld.getBlockState(adjPos));

            if (adjBox != null) {
                // Offset the adjacent box to world coordinates relative to target block
                double adjMinX = adjBox.minX + (adjPos.getX() - pos.getX());
                double adjMaxX = adjBox.maxX + (adjPos.getX() - pos.getX());
                double adjMinZ = adjBox.minZ + (adjPos.getZ() - pos.getZ());
                double adjMaxZ = adjBox.maxZ + (adjPos.getZ() - pos.getZ());

                // Check if adjacent block overlaps with target block's area
                if (adjMinX < maxX && adjMaxX > minX && adjMinZ < maxZ && adjMaxZ > minZ) {
                    // Adjacent block overlaps - constrain standable area away from it
                    if (adjPos.getX() > pos.getX()) {
                        // Adjacent is to the east - constrain max X
                        maxX = Math.min(maxX, adjMinX);
                    } else if (adjPos.getX() < pos.getX()) {
                        // Adjacent is to the west - constrain min X
                        minX = Math.max(minX, adjMaxX);
                    }

                    if (adjPos.getZ() > pos.getZ()) {
                        // Adjacent is to the south - constrain max Z
                        maxZ = Math.min(maxZ, adjMinZ);
                    } else if (adjPos.getZ() < pos.getZ()) {
                        // Adjacent is to the north - constrain min Z
                        minZ = Math.max(minZ, adjMaxZ);
                    }
                }
            }
        }

        // Calculate center of the remaining standable area
        double centerX = (minX + maxX) / 2.0 + pos.getX();
        double centerZ = (minZ + maxZ) / 2.0 + pos.getZ();

        return new double[]{centerX, centerZ};
    }

    public interface TeleportCallback {
        void setTeleportRotation(float yaw, float pitch, Vec3 expectedPos);
    }
}

