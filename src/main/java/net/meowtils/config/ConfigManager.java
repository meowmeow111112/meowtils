package net.meowtils.config;

import net.minecraft.util.EnumChatFormatting;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final String[] COLOR_OPTIONS = new String[] {
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
        "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
        "yellow", "white"
    };

    private String color1 = EnumChatFormatting.GREEN.toString();
    private String color2 = EnumChatFormatting.AQUA.toString();
    private String reset = EnumChatFormatting.RESET.toString();
    private String prefixText = "Meowtils";
    private String prefix = "[" + prefixText + "] ";
    private int cpReturnSlot = 0;   // Hotbar slot for checkpoint return (default: slot 1)
    private int cpSetSlot = 2;      // Hotbar slot for checkpoint set (default: slot 3)
    private boolean topTeleportSafetyChecks = true;
    private int topTeleportSafetyFallbackMode = 0;
    private double tpForwardDistance = 8.0;

    private final Map<String, String> COLOR_MAP = new HashMap<String, String>();
    private final Map<String, Integer> COLOR_INDEX_MAP = new HashMap<String, Integer>();
    private final MeowtilsOneConfig oneConfig;

    public ConfigManager() {
        initializeColorMap();
        oneConfig = new MeowtilsOneConfig();
        syncFromOneConfig();
    }

    private void initializeColorMap() {
        COLOR_MAP.put("black", EnumChatFormatting.BLACK.toString());
        COLOR_MAP.put("dark_blue", EnumChatFormatting.DARK_BLUE.toString());
        COLOR_MAP.put("dark_green", EnumChatFormatting.DARK_GREEN.toString());
        COLOR_MAP.put("dark_aqua", EnumChatFormatting.DARK_AQUA.toString());
        COLOR_MAP.put("dark_red", EnumChatFormatting.DARK_RED.toString());
        COLOR_MAP.put("dark_purple", EnumChatFormatting.DARK_PURPLE.toString());
        COLOR_MAP.put("gold", EnumChatFormatting.GOLD.toString());
        COLOR_MAP.put("gray", EnumChatFormatting.GRAY.toString());
        COLOR_MAP.put("dark_gray", EnumChatFormatting.DARK_GRAY.toString());
        COLOR_MAP.put("blue", EnumChatFormatting.BLUE.toString());
        COLOR_MAP.put("green", EnumChatFormatting.GREEN.toString());
        COLOR_MAP.put("aqua", EnumChatFormatting.AQUA.toString());
        COLOR_MAP.put("red", EnumChatFormatting.RED.toString());
        COLOR_MAP.put("light_purple", EnumChatFormatting.LIGHT_PURPLE.toString());
        COLOR_MAP.put("yellow", EnumChatFormatting.YELLOW.toString());
        COLOR_MAP.put("white", EnumChatFormatting.WHITE.toString());

        for (int i = 0; i < COLOR_OPTIONS.length; i++) {
            COLOR_INDEX_MAP.put(COLOR_OPTIONS[i], i);
        }
    }

    public String getColor1() { syncFromOneConfig(); return color1; }
    public String getColor2() { syncFromOneConfig(); return color2; }
    public String getReset() { return reset; }
    public String getPrefix() { syncFromOneConfig(); return prefix; }
    public String getPrefixText() { syncFromOneConfig(); return prefixText; }
    public int getCpReturnSlot() { syncFromOneConfig(); return cpReturnSlot; }
    public int getCpSetSlot() { syncFromOneConfig(); return cpSetSlot; }
    public boolean isTopTeleportSafetyChecksEnabled() { syncFromOneConfig(); return topTeleportSafetyChecks; }
    public int getTopTeleportSafetyFallbackMode() { syncFromOneConfig(); return topTeleportSafetyFallbackMode; }
    public double getTpForwardDistance() { syncFromOneConfig(); return tpForwardDistance; }
    public Map<String, String> getColorMap() { return COLOR_MAP; }
    public void openConfigGui() { if (oneConfig.isInitialized()) oneConfig.openGui(); }

    public void toggleTopTeleportSafetyChecks() {
        topTeleportSafetyChecks = !topTeleportSafetyChecks;
        oneConfig.topTeleportSafetyChecks = topTeleportSafetyChecks;
        saveConfig();
    }

    public void setColor1(String colorName) {
        if (COLOR_MAP.containsKey(colorName)) {
            color1 = COLOR_MAP.get(colorName);
            oneConfig.color1Index = getColorIndex(colorName);
            saveConfig();
        }
    }

    public void setColor2(String colorName) {
        if (COLOR_MAP.containsKey(colorName)) {
            color2 = COLOR_MAP.get(colorName);
            oneConfig.color2Index = getColorIndex(colorName);
            saveConfig();
        }
    }

    public void setPrefixText(String text) {
        prefixText = text;
        prefix = "[" + prefixText + "] ";
        oneConfig.prefixText = prefixText;
        saveConfig();
    }

    public void setCpReturnSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            cpReturnSlot = slot;
            oneConfig.cpReturnSlot = cpReturnSlot + 1;
            saveConfig();
        }
    }

    public void setCpSetSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            cpSetSlot = slot;
            oneConfig.cpSetSlot = cpSetSlot + 1;
            saveConfig();
        }
    }

    public void setTopTeleportSafetyChecks(boolean enabled) {
        topTeleportSafetyChecks = enabled;
        oneConfig.topTeleportSafetyChecks = topTeleportSafetyChecks;
        saveConfig();
    }

    public void setTopTeleportSafetyFallbackMode(int mode) {
        if (mode >= 0 && mode <= 3) {
            topTeleportSafetyFallbackMode = mode;
            oneConfig.topTeleportSafetyFallbackMode = topTeleportSafetyFallbackMode;
            saveConfig();
        }
    }

    public void setTpForwardDistance(double distance) {
        if (distance > 0.0 || distance == -1.0) {
            tpForwardDistance = distance;
            oneConfig.tpForwardDistance = (float) tpForwardDistance;
            saveConfig();
        }
    }

    private int getColorIndex(String colorName) {
        Integer index = COLOR_INDEX_MAP.get(colorName);
        return index == null ? COLOR_INDEX_MAP.get("green") : index;
    }

    private String getColorNameByIndex(int index) {
        if (index < 0 || index >= COLOR_OPTIONS.length) {
            return "green";
        }

        return COLOR_OPTIONS[index];
    }

    private String getColorNameFromValue(String colorValue) {
        for (Map.Entry<String, String> entry : COLOR_MAP.entrySet()) {
            if (entry.getValue().equals(colorValue)) {
                return entry.getKey();
            }
        }

        return "green";
    }

    private void syncFromOneConfig() {
        String selectedColor1 = getColorNameByIndex(oneConfig.color1Index);
        String selectedColor2 = getColorNameByIndex(oneConfig.color2Index);

        if (!color1.equals(COLOR_MAP.get(selectedColor1))) {
            color1 = COLOR_MAP.get(selectedColor1);
        }

        if (!color2.equals(COLOR_MAP.get(selectedColor2))) {
            color2 = COLOR_MAP.get(selectedColor2);
        }

        if (!prefixText.equals(oneConfig.prefixText)) {
            prefixText = oneConfig.prefixText;
            prefix = "[" + prefixText + "] ";
        }

        int oneConfigReturnSlot = oneConfig.cpReturnSlot - 1;
        int oneConfigSetSlot = oneConfig.cpSetSlot - 1;

        if (oneConfigReturnSlot >= 0 && oneConfigReturnSlot <= 8 && cpReturnSlot != oneConfigReturnSlot) {
            cpReturnSlot = oneConfigReturnSlot;
        }

        if (oneConfigSetSlot >= 0 && oneConfigSetSlot <= 8 && cpSetSlot != oneConfigSetSlot) {
            cpSetSlot = oneConfigSetSlot;
        }

        if (topTeleportSafetyChecks != oneConfig.topTeleportSafetyChecks) {
            topTeleportSafetyChecks = oneConfig.topTeleportSafetyChecks;
        }

        if (topTeleportSafetyFallbackMode != oneConfig.topTeleportSafetyFallbackMode) {
            topTeleportSafetyFallbackMode = oneConfig.topTeleportSafetyFallbackMode;
        }

        if (Math.abs(tpForwardDistance - oneConfig.tpForwardDistance) > 1.0E-6) {
            tpForwardDistance = oneConfig.tpForwardDistance;
        }
    }

    private void saveConfig() {
        if (oneConfig.isInitialized()) {
            oneConfig.save();
        }
    }
}