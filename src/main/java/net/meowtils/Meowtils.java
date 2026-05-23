package net.meowtils;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.util.ChatComponentText;

import net.meowtils.checkpoint.CheckpointManager;
import net.meowtils.config.ConfigManager;
import net.meowtils.config.MeowtilsCommand;
import net.meowtils.debug.PacketDebugManager;
import net.meowtils.parkour.ParkourManager;
import net.meowtils.teleport.TeleportManager;
import net.meowtils.teleport.TPCommand;
import net.meowtils.teleport.TPFCommand;
import net.meowtils.teleport.TopTeleportManager;
import net.meowtils.teleport.ForwardTeleportManager;
import net.meowtils.teleport.ThroughTeleportManager;

@Mod(modid = "meowtils", name = "Meowtils", version = "1.0", clientSideOnly = true)
public class Meowtils {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final ConfigManager configManager = new ConfigManager();
    private final ParkourManager parkourManager = new ParkourManager();
    private final TeleportManager teleportManager = new TeleportManager();
    private final PacketDebugManager packetDebugManager = new PacketDebugManager();
    private final CheckpointManager checkpointManager = new CheckpointManager(new CheckpointManager.TeleportCallback() {
        @Override
        public void setTeleportRotation(float yaw, float pitch, net.minecraft.util.Vec3 expectedPos) {
            teleportManager.setTeleportRotation(yaw, pitch, expectedPos);
        }
    });
    private final TopTeleportManager topTeleportManager = new TopTeleportManager(new TopTeleportManager.TeleportCallback() {
        @Override
        public void suppressNextTeleportMessage() {
            teleportManager.suppressNextTeleportMessage();
        }
    });
    private final ForwardTeleportManager forwardTeleportManager = new ForwardTeleportManager(new ForwardTeleportManager.TeleportCallback() {
        @Override
        public void suppressNextTeleportMessage() {
            teleportManager.suppressNextTeleportMessage();
        }
    });
    private final ThroughTeleportManager throughTeleportManager = new ThroughTeleportManager(new ThroughTeleportManager.TeleportCallback() {
        @Override
        public void suppressNextTeleportMessage() {
            teleportManager.suppressNextTeleportMessage();
        }
    });

    private boolean parkourHotbarWasEnabledBeforeBlock;
    private boolean lastParkourBlocked;
    private int parkourHotbarRestoreCooldown;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        checkpointManager.register();
        topTeleportManager.register();
        forwardTeleportManager.register();
        throughTeleportManager.register();

        ClientCommandHandler.instance.registerCommand(new TPCommand(teleportManager));
        ClientCommandHandler.instance.registerCommand(new TPFCommand());
        ClientCommandHandler.instance.registerCommand(new MeowtilsCommand(configManager, packetDebugManager));

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(packetDebugManager);
    }

    // ================= EVENT HANDLERS =================

    @SubscribeEvent
    public void onClientChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent event) {
        parkourManager.onClientChatReceived(event);

        if (!teleportManager.shouldSuppressNextMessage()) return;

        String plain = event.message.getUnformattedText();
        if (plain.contains("Teleport")) {
            event.setCanceled(true);
            teleportManager.onTeleportMessageReceived();
            teleportManager.clearSuppression();
        }
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (mc.thePlayer == null) return;

        boolean parkourBlocked = parkourManager.shouldBlockTeleportHotkeys();
        checkpointManager.onKeyInput(
            configManager.getColor1(),
            configManager.getColor2(),
            configManager.getReset(),
            configManager.getPrefix(),
            parkourBlocked
        );
        topTeleportManager.onKeyInput(
            configManager.getColor1(),
            configManager.getColor2(),
            configManager.getReset(),
            configManager.getPrefix(),
            configManager.isTopTeleportSafetyChecksEnabled(),
            configManager.getTopTeleportSafetyFallbackMode(),
            parkourBlocked
        );
        forwardTeleportManager.onKeyInput(
            configManager.getColor1(),
            configManager.getColor2(),
            configManager.getReset(),
            configManager.getPrefix(),
            configManager.getTpForwardDistance(),
            parkourBlocked
        );
        throughTeleportManager.onKeyInput(
            configManager.getColor1(),
            configManager.getColor2(),
            configManager.getReset(),
            configManager.getPrefix(),
            parkourBlocked
        );
    }

    @SubscribeEvent
    public void onMouseInput(MouseEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        boolean parkourBlocked = parkourManager.shouldBlockTeleportHotkeys();
        boolean consumed = checkpointManager.onMouseInput(
            configManager.getColor1(),
            configManager.getColor2(),
            configManager.getReset(),
            configManager.getPrefix(),
            configManager.getCpReturnSlot(),
            configManager.getCpSetSlot(),
            parkourBlocked
        );

        if (consumed) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        parkourManager.onClientTick();
        updateParkourHotbarState();
        teleportManager.onClientTick();
    }

    private void updateParkourHotbarState() {
        boolean parkourBlocked = parkourManager.shouldBlockTeleportHotkeys();

        if (parkourBlocked && !lastParkourBlocked) {
            parkourHotbarWasEnabledBeforeBlock = checkpointManager.isHotbarCPActive();
            parkourHotbarRestoreCooldown = 0;
            if (parkourHotbarWasEnabledBeforeBlock) {
                checkpointManager.setHotbarCPActive(false, configManager.getColor1(), configManager.getColor2(), configManager.getReset(), configManager.getPrefix());
            }
        } else if (!parkourBlocked && lastParkourBlocked) {
            parkourHotbarRestoreCooldown = parkourHotbarWasEnabledBeforeBlock ? 2 : 0;
        } else if (!parkourBlocked && parkourHotbarRestoreCooldown > 0) {
            parkourHotbarRestoreCooldown--;
            if (parkourHotbarRestoreCooldown == 0) {
                if (parkourHotbarWasEnabledBeforeBlock) {
                    checkpointManager.setHotbarCPActive(true, configManager.getColor1(), configManager.getColor2(), configManager.getReset(), configManager.getPrefix());
                }
                parkourHotbarWasEnabledBeforeBlock = false;
            }
        } else if (parkourBlocked) {
            parkourHotbarRestoreCooldown = 0;
        }

        lastParkourBlocked = parkourBlocked;
    }
}