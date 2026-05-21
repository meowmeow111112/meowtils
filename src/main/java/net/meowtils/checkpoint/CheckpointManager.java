package net.meowtils.checkpoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class CheckpointManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    private Vec3 checkpointPos = null;
    private float checkpointYaw = 0, checkpointPitch = 0;
    private boolean hotbarCPActive = true;
    private boolean rightMouseWasDown = false;

    private final KeyBinding cpSetKey = new KeyBinding("Set CP", Keyboard.KEY_K, "Meowtils");
    private final KeyBinding cpReturnKey = new KeyBinding("Return to CP", Keyboard.KEY_L, "Meowtils");
    private final KeyBinding hotbarToggleKey = new KeyBinding("Toggle Hotbar CP System", Keyboard.KEY_H, "Meowtils");

    private TeleportCallback teleportCallback;

    public CheckpointManager(TeleportCallback callback) {
        this.teleportCallback = callback;
    }

    public void register() {
        ClientRegistry.registerKeyBinding(cpSetKey);
        ClientRegistry.registerKeyBinding(cpReturnKey);
        ClientRegistry.registerKeyBinding(hotbarToggleKey);
    }

    public void onKeyInput(String color1, String color2, String reset, String prefix, boolean parkourBlocked) {
        if (mc.thePlayer == null) return;

        if (cpSetKey.isPressed() && !parkourBlocked) setCP(color1, color2, reset, prefix);
        if (cpReturnKey.isPressed() && !parkourBlocked) returnToCP(color1, color2, reset, prefix);
        if (hotbarToggleKey.isPressed() && !parkourBlocked) {
            hotbarCPActive = !hotbarCPActive;
            mc.thePlayer.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Hotbar CP System is now " + (hotbarCPActive ? "ON" : "OFF") + "." + reset));
        }
    }

    public boolean isHotbarCPActive() {
        return hotbarCPActive;
    }

    public void setHotbarCPActive(boolean active, String color1, String color2, String reset, String prefix) {
        if (hotbarCPActive == active) {
            return;
        }

        hotbarCPActive = active;
        if (mc.thePlayer != null && color1 != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Hotbar CP System is now " + (hotbarCPActive ? "ON" : "OFF") + "." + reset));
        }
    }

    public void onClientTick(String color1, String color2, String reset, String prefix, int cpReturnSlot, int cpSetSlot) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!hotbarCPActive) return;

        int slot = mc.thePlayer.inventory.currentItem; // 0-8
        boolean rightMouseDown = org.lwjgl.input.Mouse.isButtonDown(1);

        if (rightMouseDown && !rightMouseWasDown) {
            if (slot == cpReturnSlot) { // slot for return to CP
                returnToCP(color1, color2, reset, prefix);
            } else if (slot == cpSetSlot) { // slot for set CP
                setCP(color1, color2, reset, prefix);
            }
        }

        rightMouseWasDown = rightMouseDown;
    }

    private void setCP(String color1, String color2, String reset, String prefix) {
        checkpointPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        checkpointYaw = mc.thePlayer.rotationYaw;
        checkpointPitch = mc.thePlayer.rotationPitch;
        if (color1 != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "Checkpoint saved!" + reset));
        }
    }

    private void returnToCP(String color1, String color2, String reset, String prefix) {
        if (checkpointPos == null) {
            if (color1 != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(color1 + prefix + color2 + "No checkpoint saved!" + reset));
            }
            return;
        }

        mc.thePlayer.sendChatMessage("/tp " + checkpointPos.xCoord + " " + checkpointPos.yCoord + " " + checkpointPos.zCoord);
        teleportCallback.setTeleportRotation(checkpointYaw, checkpointPitch, checkpointPos);
    }

    public interface TeleportCallback {
        void setTeleportRotation(float yaw, float pitch, Vec3 expectedPos);
    }
}
