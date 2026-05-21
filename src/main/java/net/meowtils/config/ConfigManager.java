package net.meowtils.config;

import net.minecraft.util.EnumChatFormatting;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final Path configFile;

    public ConfigManager() {
        // Get the .minecraft/config directory
        String mcPath = System.getProperty("user.home") + File.separator + ".minecraft" + File.separator + "config";
        configFile = Paths.get(mcPath, "meowtils.cfg");
        
        initializeColorMap();
        oneConfig = new MeowtilsOneConfig();
        loadConfig();
        syncOneConfigFromState();
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
    public void openConfigGui() { oneConfig.openGui(); }

    public void toggleTopTeleportSafetyChecks() {
        topTeleportSafetyChecks = !topTeleportSafetyChecks;
        MeowtilsOneConfig.topTeleportSafetyChecks = topTeleportSafetyChecks;
        saveConfig();
    }

    public void setColor1(String colorName) {
        if (COLOR_MAP.containsKey(colorName)) {
            color1 = COLOR_MAP.get(colorName);
            MeowtilsOneConfig.color1Index = getColorIndex(colorName);
            saveConfig();
        }
    }

    public void setColor2(String colorName) {
        if (COLOR_MAP.containsKey(colorName)) {
            color2 = COLOR_MAP.get(colorName);
            MeowtilsOneConfig.color2Index = getColorIndex(colorName);
            saveConfig();
        }
    }

    public void setPrefixText(String text) {
        prefixText = text;
        prefix = "[" + prefixText + "] ";
        MeowtilsOneConfig.prefixText = prefixText;
        saveConfig();
    }

    public void setCpReturnSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            cpReturnSlot = slot;
            MeowtilsOneConfig.cpReturnSlot = cpReturnSlot + 1;
            saveConfig();
        }
    }

    public void setCpSetSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            cpSetSlot = slot;
            MeowtilsOneConfig.cpSetSlot = cpSetSlot + 1;
            saveConfig();
        }
    }

    public void setTopTeleportSafetyChecks(boolean enabled) {
        topTeleportSafetyChecks = enabled;
        MeowtilsOneConfig.topTeleportSafetyChecks = topTeleportSafetyChecks;
        saveConfig();
    }

    public void setTopTeleportSafetyFallbackMode(int mode) {
        if (mode >= 0 && mode <= 3) {
            topTeleportSafetyFallbackMode = mode;
            MeowtilsOneConfig.topTeleportSafetyFallbackMode = topTeleportSafetyFallbackMode;
            saveConfig();
        }
    }

    public void setTpForwardDistance(double distance) {
        if (distance > 0.0 || distance == -1.0) {
            tpForwardDistance = distance;
            MeowtilsOneConfig.tpForwardDistance = (float) tpForwardDistance;
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

    private void syncOneConfigFromState() {
        MeowtilsOneConfig.color1Index = getColorIndex(getColorNameFromValue(color1));
        MeowtilsOneConfig.color2Index = getColorIndex(getColorNameFromValue(color2));
        MeowtilsOneConfig.prefixText = prefixText;
        MeowtilsOneConfig.cpReturnSlot = cpReturnSlot + 1;
        MeowtilsOneConfig.cpSetSlot = cpSetSlot + 1;
        MeowtilsOneConfig.topTeleportSafetyChecks = topTeleportSafetyChecks;
        MeowtilsOneConfig.topTeleportSafetyFallbackMode = topTeleportSafetyFallbackMode;
        MeowtilsOneConfig.tpForwardDistance = (float) tpForwardDistance;
    }

    private void syncFromOneConfig() {
        String selectedColor1 = getColorNameByIndex(MeowtilsOneConfig.color1Index);
        String selectedColor2 = getColorNameByIndex(MeowtilsOneConfig.color2Index);

        if (!color1.equals(COLOR_MAP.get(selectedColor1))) {
            color1 = COLOR_MAP.get(selectedColor1);
        }

        if (!color2.equals(COLOR_MAP.get(selectedColor2))) {
            color2 = COLOR_MAP.get(selectedColor2);
        }

        if (!prefixText.equals(MeowtilsOneConfig.prefixText)) {
            prefixText = MeowtilsOneConfig.prefixText;
            prefix = "[" + prefixText + "] ";
        }

        int oneConfigReturnSlot = MeowtilsOneConfig.cpReturnSlot - 1;
        int oneConfigSetSlot = MeowtilsOneConfig.cpSetSlot - 1;

        if (oneConfigReturnSlot >= 0 && oneConfigReturnSlot <= 8 && cpReturnSlot != oneConfigReturnSlot) {
            cpReturnSlot = oneConfigReturnSlot;
        }

        if (oneConfigSetSlot >= 0 && oneConfigSetSlot <= 8 && cpSetSlot != oneConfigSetSlot) {
            cpSetSlot = oneConfigSetSlot;
        }

        if (topTeleportSafetyChecks != MeowtilsOneConfig.topTeleportSafetyChecks) {
            topTeleportSafetyChecks = MeowtilsOneConfig.topTeleportSafetyChecks;
        }

        if (topTeleportSafetyFallbackMode != MeowtilsOneConfig.topTeleportSafetyFallbackMode) {
            topTeleportSafetyFallbackMode = MeowtilsOneConfig.topTeleportSafetyFallbackMode;
        }

        if (Math.abs(tpForwardDistance - MeowtilsOneConfig.tpForwardDistance) > 1.0E-6) {
            tpForwardDistance = MeowtilsOneConfig.tpForwardDistance;
        }
    }

    private void loadConfig() {
        try {
            if (!Files.exists(configFile)) {
                // Create default config if it doesn't exist
                saveConfig();
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(configFile.toFile()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;

                String key = parts[0].trim();
                String value = parts[1].trim();

                if (key.equals("color1")) {
                    if (COLOR_MAP.containsValue(value)) {
                        color1 = value;
                    }
                } else if (key.equals("color2")) {
                    if (COLOR_MAP.containsValue(value)) {
                        color2 = value;
                    }
                } else if (key.equals("prefixText")) {
                    prefixText = value;
                    prefix = "[" + prefixText + "] ";
                } else if (key.equals("cpReturnSlot")) {
                    try {
                        int slot = Integer.parseInt(value) - 1; // Convert 1-9 to 0-8
                        if (slot >= 0 && slot <= 8) cpReturnSlot = slot;
                    } catch (NumberFormatException e) { }
                } else if (key.equals("cpSetSlot")) {
                    try {
                        int slot = Integer.parseInt(value) - 1; // Convert 1-9 to 0-8
                        if (slot >= 0 && slot <= 8) cpSetSlot = slot;
                    } catch (NumberFormatException e) { }
                } else if (key.equals("topTeleportSafetyChecks") || key.equals("lookTeleportSafetyChecks")) {
                    topTeleportSafetyChecks = value.equalsIgnoreCase("true");
                } else if (key.equals("topTeleportSafetyFallbackMode")) {
                    try {
                        int mode = Integer.parseInt(value);
                        if (mode >= 0 && mode <= 3) {
                            topTeleportSafetyFallbackMode = mode;
                        }
                    } catch (NumberFormatException e) { }
                } else if (key.equals("tpForwardDistance")) {
                    try {
                        double distance = Double.parseDouble(value);
                        if (distance > 0.0) {
                            tpForwardDistance = distance;
                        }
                    } catch (NumberFormatException e) { }
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Failed to load Meowtils config: " + e.getMessage());
        }
    }

    private void saveConfig() {
        try {
            // Create directory if it doesn't exist
            Files.createDirectories(configFile.getParent());

            BufferedWriter writer = new BufferedWriter(new FileWriter(configFile.toFile()));
            writer.write("# Meowtils Configuration\n");
            writer.write("color1=" + color1 + "\n");
            writer.write("color2=" + color2 + "\n");
            writer.write("prefixText=" + prefixText + "\n");
            writer.write("cpReturnSlot=" + (cpReturnSlot + 1) + "\n");
            writer.write("cpSetSlot=" + (cpSetSlot + 1) + "\n");
            writer.write("topTeleportSafetyChecks=" + topTeleportSafetyChecks + "\n");
            writer.write("topTeleportSafetyFallbackMode=" + topTeleportSafetyFallbackMode + "\n");
            writer.write("tpForwardDistance=" + tpForwardDistance + "\n");
            writer.close();
        } catch (IOException e) {
            System.out.println("Failed to save Meowtils config: " + e.getMessage());
        }
    }
}