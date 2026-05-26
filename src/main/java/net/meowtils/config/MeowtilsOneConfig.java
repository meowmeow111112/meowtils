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
            "Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple",
            "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple",
            "Yellow", "White"
        }
    )
    public int color1Index = 10;

    @Dropdown(
        name = "Chat Color 2",
        category = "Chat",
        options = {
            "Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple",
            "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple",
            "Yellow", "White"
        }
    )
    public int color2Index = 11;

    @Text(name = "Chat Prefix", category = "Chat")
    public String prefixText = "meow";

    @Number(name = "Checkpoint Return Slot", category = "Checkpoint", min = 1, max = 9, step = 1)
    public int cpReturnSlot = 1;

    @Number(name = "Checkpoint Set Slot", category = "Checkpoint", min = 1, max = 9, step = 1)
    public int cpSetSlot = 3;

    @Switch(name = "Top Teleport Safety Checks", category = "Teleport")
    public boolean topTeleportSafetyChecks = true;

    @Dropdown(
        name = "Unsafe Teleport Fallback",
        category = "Teleport",
        options = {
            "Cancel teleport",
            "Ascend until safe",
            "Teleport to center",
            "Teleport to edge"
        }
    )
    public int topTeleportSafetyFallbackMode = 0;

    @Slider(name = "Forward Teleport Distance", category = "Teleport", min = 1.0f, max = 256.0f, step = 1)
    public float tpForwardDistance = 8.0f;
    private boolean initialized;
    private boolean teleportFallbackDependencyRegistered;

    public MeowtilsOneConfig() {
        super(new Mod("Meowtils", ModType.UTIL_QOL), "meowtils.json");
        try {
            initialize();
            initialized = true;
        } catch (Throwable throwable) {
            initialized = false;
            System.err.println("Meowtils: OneConfig failed to initialize, continuing with in-memory defaults.");
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void initialize() {
        super.initialize();

        if (!teleportFallbackDependencyRegistered) {
            addDependency("topTeleportSafetyFallbackMode", "topTeleportSafetyChecks");
            teleportFallbackDependencyRegistered = true;
        }
    }
}