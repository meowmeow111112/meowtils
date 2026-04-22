package net.meowtils.config;

import net.minecraft.util.EnumChatFormatting;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private String color1 = EnumChatFormatting.GREEN.toString();
    private String color2 = EnumChatFormatting.AQUA.toString();
    private String reset = EnumChatFormatting.RESET.toString();
    private String prefixText = "Meowtils";
    private String prefix = "[" + prefixText + "] ";
    private int cpReturnSlot = 0;   // Hotbar slot for checkpoint return (default: slot 1)
    private int cpSetSlot = 2;      // Hotbar slot for checkpoint set (default: slot 3)
    private boolean topTeleportSafetyChecks = false;
    private double tpForwardDistance = 8.0;

    private final Map<String, String> COLOR_MAP = new HashMap<String, String>();
    private final Path configFile;

    public ConfigManager() {
        // Get the .minecraft/config directory
        String mcPath = System.getProperty("user.home") + File.separator + ".minecraft" + File.separator + "config";
        configFile = Paths.get(mcPath, "meowtils.cfg");
        
        initializeColorMap();
        loadConfig();
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
    }

    public String getColor1() { return color1; }
    public String getColor2() { return color2; }
    public String getReset() { return reset; }
    public String getPrefix() { return prefix; }
    public String getPrefixText() { return prefixText; }
    public int getCpReturnSlot() { return cpReturnSlot; }
    public int getCpSetSlot() { return cpSetSlot; }
    public boolean isTopTeleportSafetyChecksEnabled() { return topTeleportSafetyChecks; }
    public double getTpForwardDistance() { return tpForwardDistance; }
    public Map<String, String> getColorMap() { return COLOR_MAP; }

    public void setColor1(String colorName) {
        if (COLOR_MAP.containsKey(colorName)) {
            color1 = COLOR_MAP.get(colorName);
            saveConfig();
        }
    }

    public void setColor2(String colorName) {
        if (COLOR_MAP.containsKey(colorName)) {
            color2 = COLOR_MAP.get(colorName);
            saveConfig();
        }
    }

    public void setPrefixText(String text) {
        prefixText = text;
        prefix = "[" + prefixText + "] ";
        saveConfig();
    }

    public void setCpReturnSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            cpReturnSlot = slot;
            saveConfig();
        }
    }

    public void setCpSetSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            cpSetSlot = slot;
            saveConfig();
        }
    }

    public void setTopTeleportSafetyChecks(boolean enabled) {
        topTeleportSafetyChecks = enabled;
        saveConfig();
    }

    public void setTpForwardDistance(double distance) {
        if (distance > 0.0 || distance == -1.0) {
            tpForwardDistance = distance;
            saveConfig();
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
                } else if (key.equals("tpForwardDistance")) {
                    if (value.equalsIgnoreCase("infinite")) {
                        tpForwardDistance = -1.0;
                    } else {
                        try {
                            double distance = Double.parseDouble(value);
                            if (distance > 0.0 || distance == -1.0) {
                                tpForwardDistance = distance;
                            }
                        } catch (NumberFormatException e) { }
                    }
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
            writer.write("tpForwardDistance=" + (tpForwardDistance == -1.0 ? "infinite" : tpForwardDistance) + "\n");
            writer.close();
        } catch (IOException e) {
            System.out.println("Failed to save Meowtils config: " + e.getMessage());
        }
    }
}