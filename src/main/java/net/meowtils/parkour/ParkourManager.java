package net.meowtils.parkour;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ParkourManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<String, Boolean> houseParkourStates = new HashMap<String, Boolean>();
    private final File stateFile;

    private String currentHouseName;
    private boolean currentHouseParkourActive;
    private boolean pendingHouseJoinRefresh;
    private Object lastWorldReference;

    public ParkourManager() {
        File oneConfigDirectory = new File(new File(mc.mcDataDir, "config"), "OneConfig");
        stateFile = new File(oneConfigDirectory, "meowtils-parkour.properties");
        loadStateFile();
    }

    public void onClientTick() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            currentHouseName = null;
            currentHouseParkourActive = false;
            pendingHouseJoinRefresh = false;
            lastWorldReference = null;
            return;
        }

        if (mc.theWorld != lastWorldReference) {
            lastWorldReference = mc.theWorld;
            pendingHouseJoinRefresh = true;
        }

        if (pendingHouseJoinRefresh || currentHouseName == null) {
            String detectedHouseName = resolveHouseName(pendingHouseJoinRefresh);
            if (detectedHouseName != null) {
                pendingHouseJoinRefresh = false;
                if (!detectedHouseName.equals(currentHouseName)) {
                    currentHouseName = detectedHouseName;
                    currentHouseParkourActive = getStoredHouseState(detectedHouseName);
                }
            }
        }
    }

    public void onClientChatReceived(ClientChatReceivedEvent event) {
        if (event == null || event.message == null || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        String plainMessage = event.message.getUnformattedText();
        if (plainMessage == null) {
            return;
        }

        String trimmedMessage = plainMessage.trim();
        if (trimmedMessage.startsWith("Attempting to teleport you to ")) {
            pendingHouseJoinRefresh = true;
            return;
        }

        if (trimmedMessage.startsWith("Parkour challenge started!")) {
            setCurrentHouseParkourState(true);
            return;
        }

        if (trimmedMessage.startsWith("Parkour challenge failed!") || trimmedMessage.startsWith("Parkour challenge canceled!") || trimmedMessage.startsWith("Parkour challenge cancelled!")) {
            setCurrentHouseParkourState(false);
            return;
        }

        if (isParkourCompletionMessage(trimmedMessage)) {
            setCurrentHouseParkourState(false);
        }
    }

    public boolean shouldBlockTeleportHotkeys() {
        return currentHouseParkourActive;
    }

    private void setCurrentHouseParkourState(boolean active) {
        String houseName = resolveHouseName(pendingHouseJoinRefresh);
        if (houseName == null) {
            return;
        }

        boolean previousState = getStoredHouseState(houseName);
        currentHouseName = houseName;
        currentHouseParkourActive = active;
        houseParkourStates.put(houseName, Boolean.valueOf(active));
        saveStateFile();
    }

    private boolean isParkourCompletionMessage(String message) {
        String playerName = getPlayerNameCandidate();
        if (playerName == null || playerName.length() == 0) {
            return false;
        }

        String completionPrefix = playerName + " completed the parkour in ";
        if (message.startsWith(completionPrefix)) {
            return true;
        }

        String displayName = getPlayerDisplayNameCandidate();
        return displayName != null && displayName.length() > 0 && !displayName.equals(playerName) && message.startsWith(displayName + " completed the parkour in ");
    }

    private String getPlayerNameCandidate() {
        if (mc.thePlayer == null) {
            return null;
        }

        try {
            String playerName = mc.thePlayer.getName();
            return playerName == null ? null : playerName.trim();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String getPlayerDisplayNameCandidate() {
        if (mc.thePlayer == null) {
            return null;
        }

        try {
            IChatComponent displayName = mc.thePlayer.getDisplayName();
            if (displayName == null) {
                return null;
            }

            String text = displayName.getUnformattedText();
            return text == null ? null : text.trim();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveHouseName(boolean requireFreshFooter) {
        if (mc.ingameGUI == null || mc.ingameGUI.getTabList() == null) {
            return requireFreshFooter ? null : currentHouseName;
        }

        GuiPlayerTabOverlay tabOverlay = mc.ingameGUI.getTabList();
        String footerText = readComponentText(tabOverlay, "field_175255_h", "footer");
        String candidate = parseHouseNameFromFooter(footerText);
        if (candidate != null) {
            return candidate;
        }

        return requireFreshFooter ? null : currentHouseName;
    }

    private String readComponentText(Object target, String firstFieldName, String secondFieldName) {
        IChatComponent component = readComponentField(target, firstFieldName);
        if (component == null) {
            component = readComponentField(target, secondFieldName);
        }

        if (component == null) {
            return null;
        }

        String text = component.getUnformattedText();
        return text == null ? null : text.trim();
    }

    private IChatComponent readComponentField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return value instanceof IChatComponent ? (IChatComponent) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String parseHouseNameFromFooter(String text) {
        if (text == null) {
            return null;
        }

        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("You are in ")) {
                    String prefix = "You are in ";
                    String separator = ", by ";
                    int separatorIndex = trimmed.indexOf(separator, prefix.length());
                    if (separatorIndex > prefix.length()) {
                        return trimmed.substring(prefix.length(), separatorIndex).trim();
                    }
                }
            }
        }

        return null;
    }

    private boolean getStoredHouseState(String houseName) {
        Boolean storedState = houseParkourStates.get(houseName);
        return storedState != null && storedState.booleanValue();
    }

    private void loadStateFile() {
        if (!stateFile.exists()) {
            return;
        }

        Properties properties = new Properties();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(stateFile);
            properties.load(new BufferedInputStream(fileInputStream));
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String houseName = entry.getKey() == null ? null : entry.getKey().toString().trim();
                String stateValue = entry.getValue() == null ? null : entry.getValue().toString().trim();
                if (houseName != null && houseName.length() > 0) {
                    houseParkourStates.put(houseName, Boolean.valueOf("true".equalsIgnoreCase(stateValue)));
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void saveStateFile() {
        try {
            File parent = stateFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Properties properties = new Properties();
            for (Map.Entry<String, Boolean> entry : houseParkourStates.entrySet()) {
                properties.setProperty(entry.getKey(), String.valueOf(entry.getValue().booleanValue()));
            }

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(stateFile);
                properties.store(new BufferedOutputStream(fileOutputStream), "Meowtils Parkour State");
            } finally {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            }
        } catch (IOException ignored) {
        }
    }
}