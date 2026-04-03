package net.meowtils;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import net.meowtils.checkpoint.CheckpointManager;
import net.meowtils.config.ConfigManager;
import net.meowtils.config.MeowtilsCommand;
import net.meowtils.teleport.TeleportManager;
import net.meowtils.teleport.TPCommand;
import net.meowtils.teleport.TPFCommand;

@Mod(modid = "meowtils", name = "Meowtils", version = "1.0", clientSideOnly = true)
public class Meowtils {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final ConfigManager configManager = new ConfigManager();
    private final TeleportManager teleportManager = new TeleportManager();
    private final CheckpointManager checkpointManager = new CheckpointManager(new CheckpointManager.TeleportCallback() {
        @Override
        public void setTeleportRotation(float yaw, float pitch, net.minecraft.util.Vec3 expectedPos) {
            teleportManager.setTeleportRotation(yaw, pitch, expectedPos);
        }
    });

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        checkpointManager.register();

        ClientCommandHandler.instance.registerCommand(new TPCommand(teleportManager));
        ClientCommandHandler.instance.registerCommand(new TPFCommand());
        ClientCommandHandler.instance.registerCommand(new MeowtilsCommand(configManager));

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    // ================= EVENT HANDLERS =================

    @SubscribeEvent
    public void onClientChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent event) {
        if (!teleportManager.shouldSuppressNextMessage()) return;

        String plain = event.message.getUnformattedText();
        if (plain.contains("Teleporting you to") || plain.contains("Teleporting you")) {
            event.setCanceled(true);
            teleportManager.clearSuppression();
        }
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (mc.thePlayer == null) return;
        checkpointManager.onKeyInput(configManager.getColor1(), configManager.getColor2(), configManager.getReset(), configManager.getPrefix());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        teleportManager.onClientTick();
        checkpointManager.onClientTick();
    }
}