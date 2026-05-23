package net.meowtils.debug;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PacketDebugManager {
    public enum OutputMode {
        LOG,
        CHAT,
        BOTH
    }

    private static final Logger LOGGER = LogManager.getLogger("MeowtilsPacketDebug");
    private static final String HANDLER_NAME = "meowtils_packet_debug_inbound";
    private static final int MAX_FIELDS = 8;
    private static final int MAX_CHAT_LEN = 220;

    private final Minecraft mc = Minecraft.getMinecraft();

    private volatile boolean enabled = false;
    private volatile OutputMode mode = OutputMode.LOG;
    private volatile boolean handlerAttached = false;
    private int packetCount = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public OutputMode getMode() {
        return mode;
    }

    public int getPacketCount() {
        return packetCount;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            injectHandler();
        } else {
            removeHandler();
        }
    }

    public void setMode(OutputMode mode) {
        if (mode != null) {
            this.mode = mode;
        }
    }

    public String getStatusText() {
        return "Packet debug is " + (enabled ? "ON" : "OFF") + " (attached: " + (handlerAttached ? "yes" : "no") + ", mode: " + mode.name().toLowerCase() + ", captured: " + packetCount + ")";
    }

    @SubscribeEvent
    public void onConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (enabled) {
            injectHandler();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        if (enabled && !handlerAttached) {
            injectHandler();
        }
    }

    @SubscribeEvent
    public void onDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        removeHandler();
        packetCount = 0;
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
                        if (enabled && msg instanceof Packet) {
                            onIncomingPacket((Packet<?>) msg);
                        }
                        super.channelRead(ctx, msg);
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

    private void onIncomingPacket(Packet<?> packet) {
        packetCount++;
        String details = "IN #" + packetCount + " " + buildPacketSummary(packet);

        if (mode == OutputMode.LOG || mode == OutputMode.BOTH) {
            LOGGER.info(details);
        }

        if (mode == OutputMode.CHAT || mode == OutputMode.BOTH) {
            postToChat(trimForChat(details));
        }
    }

    private void postToChat(final String text) {
        if (mc.thePlayer == null) {
            return;
        }

        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText(text));
                }
            }
        });
    }

    private String trimForChat(String text) {
        if (text.length() <= MAX_CHAT_LEN) {
            return text;
        }
        return text.substring(0, MAX_CHAT_LEN - 3) + "...";
    }

    private String buildPacketSummary(Packet<?> packet) {
        Class<?> type = packet.getClass();
        StringBuilder sb = new StringBuilder(type.getSimpleName());
        sb.append(" {");

        Field[] fields = type.getDeclaredFields();
        int added = 0;
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Object value = readFieldValue(field, packet);
            if (value == null || !isPrintableValue(value)) {
                continue;
            }

            if (added > 0) {
                sb.append(", ");
            }

            sb.append(field.getName()).append("=").append(value);
            added++;

            if (added >= MAX_FIELDS) {
                sb.append(", ...");
                break;
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private Object readFieldValue(Field field, Packet<?> packet) {
        try {
            field.setAccessible(true);
            return field.get(packet);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private boolean isPrintableValue(Object value) {
        return value instanceof Number
            || value instanceof Boolean
            || value instanceof Character
            || value instanceof String
            || value instanceof Enum;
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