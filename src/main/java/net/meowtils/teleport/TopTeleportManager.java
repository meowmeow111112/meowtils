package net.meowtils.teleport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class TopTeleportManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    private final KeyBinding topTeleportKey = new KeyBinding("Teleport On Top", Keyboard.KEY_R, "Meowtils");
    private final TeleportCallback teleportCallback;
    private static final double RAY_CAST_DISTANCE = 256.0;
    private static final double COLLISION_EPSILON = 1.0E-4;
    private static final double TELEPORT_COORD_SCALE = 1000000.0;

    public TopTeleportManager(TeleportCallback callback) {
        this.teleportCallback = callback;
    }

    public void register() {
        ClientRegistry.registerKeyBinding(topTeleportKey);
    }

    public void onKeyInput(String color1, String color2, String reset, String prefix, boolean safetyChecksEnabled) {
        if (mc.thePlayer == null) return;

        if (topTeleportKey.isPressed()) {
            performTopTeleport(color1, color2, reset, prefix, safetyChecksEnabled);
        }
    }

    private void performTopTeleport(String color1, String color2, String reset, String prefix, boolean safetyChecksEnabled) {
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
        net.minecraft.block.state.IBlockState blockState = mc.theWorld.getBlockState(pos);
        net.minecraft.block.Block block = blockState.getBlock();

        // Get collision bounding box for the block
        net.minecraft.util.AxisAlignedBB boundingBox = block.getCollisionBoundingBox(mc.theWorld, pos, blockState);

        double x, y, z;

        if (boundingBox != null) {
            double standY = getStandY(pos, block, boundingBox);

            double aimX = rayTraceResult.hitVec != null ? rayTraceResult.hitVec.xCoord : (boundingBox.minX + boundingBox.maxX) / 2.0;
            double aimZ = rayTraceResult.hitVec != null ? rayTraceResult.hitVec.zCoord : (boundingBox.minZ + boundingBox.maxZ) / 2.0;
            if (safetyChecksEnabled) {
                double[] standablePos = findSafeStandablePosition(pos, block, blockState, boundingBox, standY, aimX, aimZ);
                if (standablePos == null) {
                    if (color1 != null) {
                        mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                            color1 + prefix + color2 + "No safe position found near the target block!" + reset));
                    }
                    return;
                }
                x = standablePos[0];
                y = standY;
                z = standablePos[1];
            } else {
                x = aimX;
                y = standY;
                z = aimZ;
            }
        } else {
            // Fallback: center of block
            x = pos.getX() + 0.5;
            y = pos.getY() + 1.0;
            z = pos.getZ() + 0.5;
        }

        x = normalizeTeleportCoordinate(x);
        y = normalizeTeleportCoordinate(y);
        z = normalizeTeleportCoordinate(z);

        // Register suppression state before sending the teleport command.
        teleportCallback.suppressNextTeleportMessage();

        // Send teleport command
        mc.thePlayer.sendChatMessage("/tp " + x + " " + y + " " + z);

        if (color1 != null) {
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                color1 + prefix + color2 + "Teleported!" + reset));
        }
    }

    private double[] findSafeStandablePosition(net.minecraft.util.BlockPos pos, net.minecraft.block.Block block, net.minecraft.block.state.IBlockState blockState, net.minecraft.util.AxisAlignedBB targetBox, double standY, double preferredX, double preferredZ) {
        double playerHalfWidth = mc.thePlayer.width / 2.0;
        double playerHeight = 1.8;

        java.util.List<net.minecraft.util.AxisAlignedBB> collisionBoxes = getCollisionBoxes(pos, block, blockState);
        java.util.List<double[]> safePositions = new java.util.ArrayList<double[]>();

        for (net.minecraft.util.AxisAlignedBB collisionBox : collisionBoxes) {
            double minX = collisionBox.minX;
            double maxX = collisionBox.maxX;
            double minZ = collisionBox.minZ;
            double maxZ = collisionBox.maxZ;

            double sampleMinX = minX - playerHalfWidth;
            double sampleMaxX = maxX + playerHalfWidth;
            double sampleMinZ = minZ - playerHalfWidth;
            double sampleMaxZ = maxZ + playerHalfWidth;

            double centerX = (minX + maxX) / 2.0;
            double centerZ = (minZ + maxZ) / 2.0;

            int samples = 5;
            for (int xi = 0; xi < samples; xi++) {
                for (int zi = 0; zi < samples; zi++) {
                    double x = sampleMinX + ((double) xi / (samples - 1)) * (sampleMaxX - sampleMinX);
                    double z = sampleMinZ + ((double) zi / (samples - 1)) * (sampleMaxZ - sampleMinZ);
                    net.minecraft.util.AxisAlignedBB playerBox = new net.minecraft.util.AxisAlignedBB(
                        x - playerHalfWidth,
                        standY,
                        z - playerHalfWidth,
                        x + playerHalfWidth,
                        standY + playerHeight,
                        z + playerHalfWidth
                    );

                    if (isPlayerBoxClear(pos, playerBox)) {
                        safePositions.add(new double[]{x, z});
                    }
                }
            }

            net.minecraft.util.AxisAlignedBB centerPlayerBox = new net.minecraft.util.AxisAlignedBB(
                centerX - playerHalfWidth,
                standY,
                centerZ - playerHalfWidth,
                centerX + playerHalfWidth,
                standY + playerHeight,
                centerZ + playerHalfWidth
            );
            if (isPlayerBoxClear(pos, centerPlayerBox)) {
                safePositions.add(new double[]{centerX, centerZ});
            }
        }

        // Find the position closest to the aimed point on the target block
        if (safePositions.isEmpty()) {
            return null;
        }

        double[] bestPos = null;
        double minDist = Double.MAX_VALUE;
        for (double[] posArr : safePositions) {
            double dx = posArr[0] - preferredX;
            double dz = posArr[1] - preferredZ;
            double dist = dx * dx + dz * dz; // squared distance
            if (dist < minDist) {
                minDist = dist;
                bestPos = posArr;
            }
        }

        return bestPos;
    }

    private boolean isPlayerBoxClear(net.minecraft.util.BlockPos pos, net.minecraft.util.AxisAlignedBB playerBox) {
        net.minecraft.util.AxisAlignedBB expandedPlayerBox = playerBox.expand(COLLISION_EPSILON, 0.0, COLLISION_EPSILON);

        // Use the world's collision query so multipart blocks (panes, walls, fences, stairs)
        // contribute all of their collision boxes, not just one simplified AABB.
        java.util.List<?> collisions = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, expandedPlayerBox);
        return collisions.isEmpty();
    }

    private double normalizeTeleportCoordinate(double coordinate) {
        return Math.round(coordinate * TELEPORT_COORD_SCALE) / TELEPORT_COORD_SCALE;
    }

    private java.util.List<net.minecraft.util.AxisAlignedBB> getCollisionBoxes(net.minecraft.util.BlockPos pos, net.minecraft.block.Block block, net.minecraft.block.state.IBlockState blockState) {
        java.util.List<net.minecraft.util.AxisAlignedBB> collisionBoxes = new java.util.ArrayList<net.minecraft.util.AxisAlignedBB>();
        net.minecraft.util.AxisAlignedBB searchBox = new net.minecraft.util.AxisAlignedBB(
            pos.getX() - 1.0,
            pos.getY() - 1.0,
            pos.getZ() - 1.0,
            pos.getX() + 2.0,
            pos.getY() + 2.0,
            pos.getZ() + 2.0
        );

        block.addCollisionBoxesToList(mc.theWorld, pos, blockState, searchBox, collisionBoxes, mc.thePlayer);

        if (collisionBoxes.isEmpty()) {
            net.minecraft.util.AxisAlignedBB boundingBox = block.getCollisionBoundingBox(mc.theWorld, pos, blockState);
            if (boundingBox != null) {
                collisionBoxes.add(boundingBox);
            }
        }

        return collisionBoxes;
    }

    private double getStandY(net.minecraft.util.BlockPos pos, net.minecraft.block.Block block, net.minecraft.util.AxisAlignedBB boundingBox) {
        if (block instanceof net.minecraft.block.BlockFence || block instanceof net.minecraft.block.BlockWall) {
            return pos.getY() + 1.5;
        }

        return boundingBox.maxY;
    }

    public interface TeleportCallback {
        void suppressNextTeleportMessage();
    }
}

