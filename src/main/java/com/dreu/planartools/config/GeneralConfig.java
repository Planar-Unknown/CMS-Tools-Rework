package com.dreu.planartools.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;

import java.io.FileWriter;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.util.Helpers.LogLevel.ERROR;
import static com.dreu.planartools.util.Helpers.LogLevel.WARN;
import static com.dreu.planartools.util.Helpers.*;

public class GeneralConfig {
    public static boolean needsRepair;
    public static final String fileName = "config/" + MODID + "/general.toml";

    public static void repair() {
        try (FileWriter writer = new FileWriter(fileName)){
            writer.write(
                    "# To reset this config to default, delete this file and rerun the game.\n\n" +
                    "UseGlobalDefault = " + USE_GLOBAL_DEFAULT + "\n" +
                    "GlobalDefaultResistance = " + GLOBAL_DEFAULT_RESISTANCE + "\n\n" +
                    "# To use a custom preset, create a folder in: config/planar_tools/presets/[your-preset-name]\n" +
                    "# Add both blocks.toml and tools.toml to your preset folder.\n" +
                    "# Use the \"template\" preset in config/planar_tools/presets/template as an example.\n" +
                    "Preset = \"" + PRESET + "\""
            );
        } catch (IOException io) {
            addConfigIssue(ERROR, (byte) 5, "Encountered exception while writing repaired config file [{}] | Exception: {}", fileName, io.getMessage());
        }
    }

    static final String DEFAULT_CONFIG_STRING = """
            # To reset this config to default, delete this file and rerun the game.
            
            UseGlobalDefault = true
            GlobalDefaultResistance = -1
            
            # To use a custom preset, create a folder in: config/planar_tools/presets/[your-preset-name]
            # Add both blocks.toml and tools.toml to your preset folder.
            # Use the "template" preset in config/planar_tools/presets/template as an example.
            Preset = "custom"
            
            # When true, the config will be parsed every time you join a world. This is so modpack
            # developers can test changes without needing to constantly restart the whole game.
            # If a server has this enabled it will parse every time any player joins.
            Hotswappable = false # This should be false for regular gameplay.
            """;

    public static final Config DEFAULT_CONFIG = new TomlParser().parse(DEFAULT_CONFIG_STRING);

    public static Config CONFIG;
    public static void parse() {
        CONFIG = parseFileOrDefault(fileName, DEFAULT_CONFIG_STRING, true);
    }

    public static int GLOBAL_DEFAULT_RESISTANCE;
    public static boolean USE_GLOBAL_DEFAULT;
    private static String PRESET;
    public static String PRESET_FOLDER_NAME;
    public static boolean HOTSWAPPABLE;

    public static void populate() {
        USE_GLOBAL_DEFAULT = getOrDefault("UseGlobalDefault", Boolean.class);
        GLOBAL_DEFAULT_RESISTANCE = USE_GLOBAL_DEFAULT ? getOrDefault("GlobalDefaultResistance", Integer.class) : 0;
        PRESET = getOrDefault("Preset", String.class);
        PRESET_FOLDER_NAME = String.format("config/%s/presets/%s/", MODID, PRESET);
        HOTSWAPPABLE = getOrDefault("Hotswappable", Boolean.class);
    }

    public static <T> T getOrDefault(String key, Class<T> clazz) {
        try {
            if ((CONFIG.get(key) == null)) {
                addConfigIssue(WARN, (byte) 4, "Key \"{}\" is missing from config [{}] | Marking config file for repair...", key, logFileName(fileName));
                needsRepair = true;
                return clazz.cast(DEFAULT_CONFIG.get(key));
            }
            return clazz.cast(CONFIG.get(key));
        } catch (Exception e) {
            addConfigIssue(WARN, (byte) 4, "Value: \"{}\" for \"{}\" is an invalid type in config [{}] | Expected: '{}' but got: '{}' | Marking config file for repair...", CONFIG.get(key), key, logFileName(fileName), clazz.getTypeName(), CONFIG.get(key).getClass().getTypeName());
            needsRepair = true;
            return clazz.cast(DEFAULT_CONFIG.get(key));
        }
    }
}
