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
    private static final int FALLBACK_CANCEL = 0;
    private static final int FALLBACK_ASCEND = 1;
    private static final int FALLBACK_CENTER = 2;
    private static final int FALLBACK_EDGE = 3;
    private static final double EDGE_CLIP_EPSILON = 1.0E-10;
    private static final double IGNORE_RAY_ADVANCE_STEP = 0.1;
    private static final int IGNORE_RAY_MAX_STEPS = 64;

    public TopTeleportManager(TeleportCallback callback) {
        this.teleportCallback = callback;
    }

    public void register() {
        ClientRegistry.registerKeyBinding(topTeleportKey);
    }

    public void onKeyInput(String color1, String color2, String reset, String prefix, boolean safetyChecksEnabled, int fallbackMode, boolean parkourBlocked) {
        if (mc.thePlayer == null) return;

        if (topTeleportKey.isPressed() && !parkourBlocked) {
            performTopTeleport(color1, color2, reset, prefix, safetyChecksEnabled, fallbackMode);
        }
    }

    private void performTopTeleport(String color1, String color2, String reset, String prefix, boolean safetyChecksEnabled, int fallbackMode) {
        // Cast a ray from the player's eyes along their look direction
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVec = mc.thePlayer.getLook(1.0F);
        Vec3 reachVec = eyePos.addVector(lookVec.xCoord * RAY_CAST_DISTANCE, lookVec.yCoord * RAY_CAST_DISTANCE, lookVec.zCoord * RAY_CAST_DISTANCE);

        // Use the world's rayTraceBlocks to find what block we're looking at,
        // but keep stepping past non-solid "fake" blocks such as cobwebs and pressure plates.
        MovingObjectPosition rayTraceResult = findFirstValidTeleportTarget(eyePos, reachVec, lookVec);

        if (rayTraceResult == null || rayTraceResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            if (color1 != null) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    color1 + prefix + color2 + "No valid block in sight!" + reset));
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
                double[] teleportPos = findSafeTeleportPosition(pos, block, blockState, boundingBox, aimX, aimZ);
                if (teleportPos == null) {
                    teleportPos = resolveSafetyFallback(pos, aimX, aimZ, fallbackMode, color1, color2, reset, prefix);
                }

                if (teleportPos == null) {
                    return;
                }

                x = teleportPos[0];
                y = teleportPos[1];
                z = teleportPos[2];
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

        // Register suppression state before sending the teleport command.
        teleportCallback.suppressNextTeleportMessage();

        // Send teleport command
        mc.thePlayer.sendChatMessage("/tp " + x + " " + y + " " + z);
    }

    private double[] resolveSafetyFallback(net.minecraft.util.BlockPos pos, double preferredX, double preferredZ, int fallbackMode, String color1, String color2, String reset, String prefix) {
        if (fallbackMode == FALLBACK_ASCEND) {
            sendFallbackMessage(color1, color2, reset, prefix, "No safe position found near the target block, ascending.");
            double[] ascended = findSafeTeleportAbove(pos, preferredX, preferredZ);
            if (ascended != null) {
                return ascended;
            }

            sendFallbackMessage(color1, color2, reset, prefix, "No safe position found above the target block, teleport cancelled.");
            return null;
        }

        net.minecraft.block.state.IBlockState blockState = mc.theWorld.getBlockState(pos);
        net.minecraft.block.Block block = blockState.getBlock();
        net.minecraft.util.AxisAlignedBB boundingBox = block.getCollisionBoundingBox(mc.theWorld, pos, blockState);
        if (boundingBox == null) {
            if (color1 != null) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    color1 + prefix + color2 + "No safe position found near the target block!" + reset));
            }
            return null;
        }

        double standY = getStandY(pos, block, boundingBox);

        if (fallbackMode == FALLBACK_CENTER) {
            sendFallbackMessage(color1, color2, reset, prefix, "No safe position found near the target block, teleporting to center.");
            return new double[]{pos.getX() + 0.5, standY, pos.getZ() + 0.5};
        }

        if (fallbackMode == FALLBACK_EDGE) {
            sendFallbackMessage(color1, color2, reset, prefix, "No safe position found near the target block, teleporting to edge.");
            return getEdgeFallbackPosition(pos, boundingBox, standY, preferredX, preferredZ);
        }

        sendFallbackMessage(color1, color2, reset, prefix, "No safe position found near the target block, teleport cancelled.");
        return null;
    }

    private double[] findSafeTeleportPosition(net.minecraft.util.BlockPos pos, net.minecraft.block.Block block, net.minecraft.block.state.IBlockState blockState, net.minecraft.util.AxisAlignedBB targetBox, double preferredX, double preferredZ) {
        double standY = getStandY(pos, block, targetBox);
        double[] standablePos = findSafeStandablePosition(pos, block, blockState, standY, preferredX, preferredZ);
        if (standablePos == null) {
            return null;
        }

        return new double[]{standablePos[0], standY, standablePos[1]};
    }

    private double[] findSafeStandablePosition(net.minecraft.util.BlockPos pos, net.minecraft.block.Block block, net.minecraft.block.state.IBlockState blockState, double standY, double preferredX, double preferredZ) {
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

    private double[] findSafeTeleportAbove(net.minecraft.util.BlockPos startPos, double preferredX, double preferredZ) {
        for (int y = startPos.getY() + 1; y < 256; y++) {
            net.minecraft.util.BlockPos candidatePos = new net.minecraft.util.BlockPos(startPos.getX(), y, startPos.getZ());
            net.minecraft.block.state.IBlockState candidateState = mc.theWorld.getBlockState(candidatePos);
            net.minecraft.block.Block candidateBlock = candidateState.getBlock();
            net.minecraft.util.AxisAlignedBB candidateBox = candidateBlock.getCollisionBoundingBox(mc.theWorld, candidatePos, candidateState);

            if (candidateBox == null) {
                continue;
            }

            double standY = getStandY(candidatePos, candidateBlock, candidateBox);
            double[] standablePos = findSafeStandablePosition(candidatePos, candidateBlock, candidateState, standY, preferredX, preferredZ);
            if (standablePos != null) {
                return new double[]{standablePos[0], standY, standablePos[1]};
            }
        }

        return null;
    }

    private double[] getEdgeFallbackPosition(net.minecraft.util.BlockPos pos, net.minecraft.util.AxisAlignedBB boundingBox, double standY, double preferredX, double preferredZ) {
        double minX = boundingBox.minX;
        double maxX = boundingBox.maxX;
        double minZ = boundingBox.minZ;
        double maxZ = boundingBox.maxZ;
        double playerHalfWidth = mc.thePlayer.width / 2.0;

        double x = clamp(preferredX, minX, maxX);
        double z = clamp(preferredZ, minZ, maxZ);

        double distanceToMinX = Math.abs(x - minX);
        double distanceToMaxX = Math.abs(maxX - x);
        double distanceToMinZ = Math.abs(z - minZ);
        double distanceToMaxZ = Math.abs(maxZ - z);

        double nearestEdgeDistance = Math.min(Math.min(distanceToMinX, distanceToMaxX), Math.min(distanceToMinZ, distanceToMaxZ));

        if (nearestEdgeDistance == distanceToMinX) {
            x = minX - (playerHalfWidth - EDGE_CLIP_EPSILON);
        } else if (nearestEdgeDistance == distanceToMaxX) {
            x = maxX + (playerHalfWidth - EDGE_CLIP_EPSILON);
        } else if (nearestEdgeDistance == distanceToMinZ) {
            z = minZ - (playerHalfWidth - EDGE_CLIP_EPSILON);
        } else {
            z = maxZ + (playerHalfWidth - EDGE_CLIP_EPSILON);
        }

        return new double[]{x, standY, z};
    }

    private MovingObjectPosition findFirstValidTeleportTarget(Vec3 eyePos, Vec3 reachVec, Vec3 lookVec) {
        Vec3 searchStart = eyePos;
        Vec3 stepDirection = lookVec.normalize();

        for (int attempts = 0; attempts < IGNORE_RAY_MAX_STEPS; attempts++) {
            MovingObjectPosition rayTraceResult = mc.theWorld.rayTraceBlocks(searchStart, reachVec, false, false, false);
            if (rayTraceResult == null || rayTraceResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || rayTraceResult.hitVec == null) {
                return rayTraceResult;
            }

            net.minecraft.util.BlockPos pos = rayTraceResult.getBlockPos();
            net.minecraft.block.state.IBlockState blockState = mc.theWorld.getBlockState(pos);
            net.minecraft.block.Block block = blockState.getBlock();

            if (!isIgnoredRayHitBlock(block)) {
                return rayTraceResult;
            }

            searchStart = rayTraceResult.hitVec.addVector(
                stepDirection.xCoord * IGNORE_RAY_ADVANCE_STEP,
                stepDirection.yCoord * IGNORE_RAY_ADVANCE_STEP,
                stepDirection.zCoord * IGNORE_RAY_ADVANCE_STEP
            );
        }

        return null;
    }

    private boolean isIgnoredRayHitBlock(net.minecraft.block.Block block) {
        return block instanceof net.minecraft.block.BlockWeb
            || block instanceof net.minecraft.block.BlockVine
            || block instanceof net.minecraft.block.BlockBasePressurePlate;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void sendFallbackMessage(String color1, String color2, String reset, String prefix, String message) {
        if (color1 == null) {
            return;
        }

        mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(color1 + prefix + color2 + message + reset));
    }

    private boolean isPlayerBoxClear(net.minecraft.util.BlockPos pos, net.minecraft.util.AxisAlignedBB playerBox) {
        net.minecraft.util.AxisAlignedBB expandedPlayerBox = playerBox.expand(COLLISION_EPSILON, 0.0, COLLISION_EPSILON);

        // Use the world's collision query so multipart blocks (panes, walls, fences, stairs)
        // contribute all of their collision boxes, not just one simplified AABB.
        java.util.List<?> collisions = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, expandedPlayerBox);
        return collisions.isEmpty();
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

