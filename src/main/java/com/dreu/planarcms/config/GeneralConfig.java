package com.dreu.planarcms.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;

import java.nio.file.Path;

import static com.dreu.planarcms.PlanarCMS.MODID;
import static com.dreu.planarcms.util.Helpers.*;
import static com.dreu.planarcms.util.Helpers.LogLevel.WARN;

public class GeneralConfig {
    public static boolean needsRepair;
    public static final String fileName = "config/" + MODID + "/general.toml";

    public static void repair() {
        if (CONFIG.contains(ConfigUpgrades.VERSION)) save(DEFAULT_WAILA_POSITION, PRESET);
    }

    public static boolean save(WailaPosition defaultWailaPosition, String preset) {
        return save(defaultWailaPosition, preset, ENABLE_ADVANCED_WAILA_KEYBIND, ADVANCED_WAILA_KEY_MODE);
    }

    public static boolean save(WailaPosition defaultWailaPosition, String preset, boolean enableAdvancedWailaKeybind, AdvancedWailaKeyMode advancedWailaKeyMode) {
        Config config = new TomlParser().parse(DEFAULT_CONFIG_STRING);
        config.set("DefaultWailaPosition", defaultWailaPosition.toString().toLowerCase());
        config.set("Preset", preset);
        config.set("EnableAdvancedWailaKeybind", enableAdvancedWailaKeybind);
        config.set("AdvancedWailaKeyMode", advancedWailaKeyMode.toString().toLowerCase());
        return ConfigUpgrades.save(Path.of(fileName), ConfigUpgrades.format(config), 1);
    }

    static final String DEFAULT_CONFIG_STRING = """
            ConfigVersion = 1
            # To reset this config to default, delete this file and rerun the game.
            
            # Where the waila will be located by default. Valid values are "left", "middle", "right", or "invisible"
            DefaultWailaPosition = "middle"

            # Client-only Advanced Waila keybind. Rebind Left Shift in Minecraft Controls.
            # Ignored while AdvancedWaila is enabled in display.toml.
            EnableAdvancedWailaKeybind = true
            # "hold" shows Advanced while pressed; "toggle" switches locally until pressed again.
            # Toggle state is not saved and resets on config reload or leaving the world.
            AdvancedWailaKeyMode = "hold"
            
            # To use a custom preset, create a folder in: config/planar_cms/presets/[your-preset-name]
            # Add blocks.toml, tools.toml, and enchants.toml to your preset folder.
            # Use the "template" preset in config/planar_cms/presets/template as an example.
            Preset = "custom"
            """;

    public static final Config DEFAULT_CONFIG = new TomlParser().parse(DEFAULT_CONFIG_STRING);

    public static Config CONFIG;
    public static void parse() {
        needsRepair = false;
        CONFIG = parseFileOrDefault(fileName, DEFAULT_CONFIG_STRING);
    }

    private static String PRESET;
    public static String PRESET_FOLDER_NAME;
    public static WailaPosition DEFAULT_WAILA_POSITION;
    public static boolean ENABLE_ADVANCED_WAILA_KEYBIND = true;
    public static AdvancedWailaKeyMode ADVANCED_WAILA_KEY_MODE = AdvancedWailaKeyMode.HOLD;

    public enum AdvancedWailaKeyMode { HOLD, TOGGLE }

    public static void populate() {
        PRESET = getOrDefault("Preset", String.class);
        PRESET_FOLDER_NAME = String.format("config/%s/presets/%s/", MODID, PRESET);
        DEFAULT_WAILA_POSITION = wailaPosFromString(getOrDefault("DefaultWailaPosition", String.class));
        WAILA_POSITION = DEFAULT_WAILA_POSITION;
        // These optional v1 additions must not force a rewrite of existing configs.
        ENABLE_ADVANCED_WAILA_KEYBIND = !CONFIG.contains("EnableAdvancedWailaKeybind") || getOrDefault("EnableAdvancedWailaKeybind", Boolean.class);
        ADVANCED_WAILA_KEY_MODE = CONFIG.contains("AdvancedWailaKeyMode")
            ? advancedWailaKeyModeFromString(getOrDefault("AdvancedWailaKeyMode", String.class)) : AdvancedWailaKeyMode.HOLD;
        DisplayConfig.resetAdvancedWailaKey();
    }

    public static String getPreset() {
        return PRESET;
    }

    public static <T> T getOrDefault(String key, Class<T> clazz) {
        try {
            if ((CONFIG.get(key) == null)) {
                addConfigIssue(WARN, (byte) 4, "Key \"{}\" is missing from config [{}] | Marking config file for repair...", key, fileName);
                needsRepair = true;
                return clazz.cast(DEFAULT_CONFIG.get(key));
            }
            return clazz.cast(CONFIG.get(key));
        } catch (Exception e) {
            addConfigIssue(WARN, (byte) 4, "Value: \"{}\" for \"{}\" is an invalid type in config [{}] | Expected: '{}' but got: '{}' | Marking config file for repair...", CONFIG.get(key), key, fileName, clazz.getSimpleName(), CONFIG.get(key).getClass().getSimpleName());
            needsRepair = true;
            return clazz.cast(DEFAULT_CONFIG.get(key));
        }
    }

    public static WailaPosition wailaPosFromString(String name) {
        for (WailaPosition position : WailaPosition.values())
            if (position.toString().equalsIgnoreCase(name)) return position;
        addConfigIssue(LogLevel.INFO, (byte) 1, "'DefaultWailaPosition' is an invalid value in config: [{}] | Expected: \"left\", \"middle\", \"right\", or \"invisible\" but got: \"{}\" | Marking config file for repair...", fileName, name);
        GeneralConfig.needsRepair = true;
        return WailaPosition.MIDDLE;
    }

    public static AdvancedWailaKeyMode advancedWailaKeyModeFromString(String name) {
        for (AdvancedWailaKeyMode mode : AdvancedWailaKeyMode.values())
            if (mode.toString().equalsIgnoreCase(name)) return mode;
        addConfigIssue(WARN, (byte) 4, "'AdvancedWailaKeyMode' is an invalid value in config: [{}] | Expected: \"hold\" or \"toggle\" but got: \"{}\" | Marking config file for repair...", fileName, name);
        needsRepair = true;
        return AdvancedWailaKeyMode.HOLD;
    }
}
