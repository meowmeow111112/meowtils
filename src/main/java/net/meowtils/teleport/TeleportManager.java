package net.meowtils.teleport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;

public class TeleportManager {
    private static final String HANDLER_NAME = "meowtils_teleport_sync";
    private static final long TELEPORT_TIMEOUT_MS = 5000;

    private final Minecraft mc = Minecraft.getMinecraft();

    private volatile boolean handlerAttached = false;
    private boolean pendingTeleportRotation = false;
    private float pendingYaw;
    private float pendingPitch;
    private long teleportStartTime = 0;
    private boolean applyRotationAfterNextC06 = false;

    private boolean suppressNextTeleportServerMessage = false;

    public void suppressNextTeleportMessage() {
        suppressNextTeleportServerMessage = true;
    }

    public void setTeleportRotation(float yaw, float pitch, Vec3 expectedPos) {
        pendingTeleportRotation = true;
        pendingYaw = yaw;
        pendingPitch = pitch;
        teleportStartTime = System.currentTimeMillis();
    }

    public boolean shouldSuppressNextMessage() {
        return suppressNextTeleportServerMessage;
    }

    public void clearSuppression() {
        suppressNextTeleportServerMessage = false;
    }

    @SubscribeEvent
    public void onClientChatReceived(ClientChatReceivedEvent event) {
        if (!suppressNextTeleportServerMessage || event == null || event.message == null) {
            return;
        }

        String plainMessage = event.message.getUnformattedText();
        if (plainMessage != null && plainMessage.contains("Teleport")) {
            event.setCanceled(true);
            suppressNextTeleportServerMessage = false;
        }
    }

    @SubscribeEvent
    public void onConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        injectHandler();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        if (pendingTeleportRotation && teleportStartTime > 0 && System.currentTimeMillis() - teleportStartTime > TELEPORT_TIMEOUT_MS) {
            pendingTeleportRotation = false;
            teleportStartTime = 0;
            applyRotationAfterNextC06 = false;
        }

        if (!handlerAttached) {
            injectHandler();
        }
    }

    @SubscribeEvent
    public void onDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        removeHandler();
        pendingTeleportRotation = false;
        teleportStartTime = 0;
        applyRotationAfterNextC06 = false;
    }

    private void injectHandler() {
        Channel channel = getNetworkChannel();
        if (channel == null || channel.pipeline() == null) {
            handlerAttached = false;
            return;
        }

        if (channel.pipeline().get(HANDLER_NAME) != null) {
            handlerAttached = true;
            return;
        }

        final String anchorName = findPacketHandlerName(channel);
        if (anchorName == null) {
            handlerAttached = false;
            return;
        }

        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                Channel currentChannel = getNetworkChannel();
                if (currentChannel == null || currentChannel.pipeline() == null) {
                    handlerAttached = false;
                    return;
                }

                if (currentChannel.pipeline().get(HANDLER_NAME) != null) {
                    handlerAttached = true;
                    return;
                }

                if (currentChannel.pipeline().get(anchorName) == null) {
                    handlerAttached = false;
                    return;
                }

                currentChannel.pipeline().addBefore(anchorName, HANDLER_NAME, new ChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (msg instanceof S08PacketPlayerPosLook) {
                            onTeleportPacketReceived();
                        }
                        super.channelRead(ctx, msg);
                    }

                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                        super.write(ctx, msg, promise);

                        if (applyRotationAfterNextC06 && msg instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
                            applyLocalRotation();
                            applyRotationAfterNextC06 = false;
                        }
                    }
                });
                handlerAttached = true;
            }
        });
    }

    private void removeHandler() {
        Channel channel = getNetworkChannel();
        if (channel == null || channel.pipeline() == null) {
            handlerAttached = false;
            return;
        }

        if (channel.pipeline().get(HANDLER_NAME) != null) {
            channel.eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    Channel currentChannel = getNetworkChannel();
                    if (currentChannel != null && currentChannel.pipeline() != null && currentChannel.pipeline().get(HANDLER_NAME) != null) {
                        currentChannel.pipeline().remove(HANDLER_NAME);
                    }
                    handlerAttached = false;
                }
            });
        } else {
            handlerAttached = false;
        }
    }

    private void onTeleportPacketReceived() {
        if (!pendingTeleportRotation || mc.thePlayer == null) {
            return;
        }
        applyRotationAfterNextC06 = true;
    }

    private void applyLocalRotation() {
        if (mc.thePlayer == null) {
            return;
        }

        mc.thePlayer.rotationYaw = pendingYaw;
        mc.thePlayer.rotationPitch = pendingPitch;
        mc.thePlayer.prevRotationYaw = pendingYaw;
        mc.thePlayer.prevRotationPitch = pendingPitch;
        pendingTeleportRotation = false;
        teleportStartTime = 0;
    }

    private Channel getNetworkChannel() {
        if (mc.getNetHandler() == null) {
            return null;
        }

        NetworkManager networkManager = mc.getNetHandler().getNetworkManager();
        if (networkManager == null) {
            return null;
        }

        try {
            Field channelField = findChannelField(NetworkManager.class);
            if (channelField == null) {
                return null;
            }

            channelField.setAccessible(true);
            return (Channel) channelField.get(networkManager);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private String findPacketHandlerName(Channel channel) {
        if (channel.pipeline().get("packet_handler") != null) {
            return "packet_handler";
        }

        for (String name : channel.pipeline().names()) {
            Object handler = channel.pipeline().get(name);
            if (handler != null && handler.getClass().getSimpleName().toLowerCase().contains("packet")) {
                return name;
            }
        }

        return null;
    }

    private Field findChannelField(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            Field[] fields = current.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if (Channel.class.isAssignableFrom(field.getType())) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }

        return null;
    }
}
