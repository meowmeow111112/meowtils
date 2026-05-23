package net.meowtils.debug;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.ChatComponentText;

import java.lang.reflect.Field;

public class OutgoingPacketTraceManager {
    private static final Logger LOGGER = LogManager.getLogger("MeowtilsPacketTrace");
    private static final String HANDLER_NAME = "meowtils_outgoing_packet_trace";

    private final Minecraft mc = Minecraft.getMinecraft();

    private volatile boolean enabled = false;
    private volatile boolean handlerAttached = false;
    private static final int MAX_CHAT_LEN = 240;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            injectHandler();
        } else {
            removeHandler();
        }
    }

    public String getStatusText() {
        return "Outgoing packet trace is " + (enabled ? "ON" : "OFF") + " (attached: " + (handlerAttached ? "yes" : "no") + ")";
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
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                        if (enabled && msg instanceof Packet) {
                            String text = describePacket((Packet<?>) msg);
                            postToChat(trimForChat(text));
                        }
                        super.write(ctx, msg, promise);
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

    private String describePacket(Packet<?> packet) {
        StringBuilder sb = new StringBuilder(packet.getClass().getSimpleName());
        sb.append(" {");

        Field[] fields = collectFields(packet.getClass());
        int added = 0;
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            Object value = readFieldValue(field, packet);
            if (value == null || !isPrintableValue(value)) {
                continue;
            }

            if (added > 0) {
                sb.append(", ");
            }

            sb.append(field.getName()).append("=").append(value);
            added++;

            if (added >= 8) {
                sb.append(", ...");
                break;
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private Field[] collectFields(Class<?> type) {
        java.util.ArrayList<Field> fields = new java.util.ArrayList<Field>();
        Class<?> current = type;
        while (current != null) {
            Field[] declared = current.getDeclaredFields();
            for (int i = 0; i < declared.length; i++) {
                Field field = declared[i];
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields.toArray(new Field[fields.size()]);
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

    private void postToChat(final String text) {
        if (mc.thePlayer == null) return;
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
        if (text.length() <= MAX_CHAT_LEN) return text;
        return text.substring(0, MAX_CHAT_LEN - 3) + "...";
    }
}