package net.meowtils.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;

public class MeowtilsOneConfig extends Config {
    @Dropdown(
        name = "Chat Color 1",
        category = "Chat",
        options = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white"
        }
    )
    public static int color1Index = 10;

    @Dropdown(
        name = "Chat Color 2",
        category = "Chat",
        options = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white"
        }
    )
    public static int color2Index = 11;

    @Text(name = "Chat Prefix", category = "Chat")
    public static String prefixText = "meow";

    @Number(name = "Checkpoint Return Slot", category = "Checkpoint", min = 1, max = 9, step = 1)
    public static int cpReturnSlot = 1;

    @Number(name = "Checkpoint Set Slot", category = "Checkpoint", min = 1, max = 9, step = 1)
    public static int cpSetSlot = 3;

    @Switch(name = "Top Teleport Safety Checks", category = "Teleport")
    public static boolean topTeleportSafetyChecks = true;

    @Slider(name = "Forward Teleport Distance", category = "Teleport", min = 1.0f, max = 256.0f, step = 1)
    public static float tpForwardDistance = 8.0f;

    public MeowtilsOneConfig() {
        super(new Mod("Meowtils", ModType.UTIL_QOL), "meowtils.json");
        initialize();
    }
}