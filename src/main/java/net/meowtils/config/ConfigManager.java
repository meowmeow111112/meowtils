package net.meowtils.config;

import net.minecraft.util.EnumChatFormatting;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private String color1 = EnumChatFormatting.GREEN.toString();
    private String color2 = EnumChatFormatting.AQUA.toString();
    private String reset = EnumChatFormatting.RESET.toString();
    private String prefixText = "Meowtils";
    private String prefix = "[" + prefixText + "] ";

    private final Map<String, String> COLOR_MAP = new HashMap<String, String>();

    public ConfigManager() {
        initializeColorMap();
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
    public Map<String, String> getColorMap() { return COLOR_MAP; }

    public void setColor1(String colorName) {
        if (COLOR_MAP.containsKey(colorName)) {
            color1 = COLOR_MAP.get(colorName);
        }
    }

    public void setColor2(String colorName) {
        if (COLOR_MAP.containsKey(colorName)) {
            color2 = COLOR_MAP.get(colorName);
        }
    }

    public void setPrefixText(String text) {
        prefixText = text;
        prefix = "[" + prefixText + "] ";
    }
}
