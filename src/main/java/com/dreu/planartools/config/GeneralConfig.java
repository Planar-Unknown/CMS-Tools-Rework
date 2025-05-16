package com.dreu.planartools.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;

import static com.dreu.planartools.PlanarTools.*;

public class GeneralConfig {
    public static boolean needsRepair;
    public static final String fileName = "config/" + MODID + "/general.toml";

    static final String DEFAULT_CONFIG_STRING = """
            # To reset this config to default, delete this file and rerun the game.
            
            
            UseGlobalDefault = true
            GlobalDefaultResistance = -1
            
            # To use a custom preset, create a folder in: config/planar_tools/presets/[your-preset-name]
            # Add both blocks.toml and tools.toml to your preset folder.
            # Use the "template" preset in config/planar_tools/presets/template as an example.
            Preset = "template"
            """;

    private static final Config CONFIG = parseFileOrDefault(fileName, DEFAULT_CONFIG_STRING);
    private static final Config DEFAULT_CONFIG = new TomlParser().parse(DEFAULT_CONFIG_STRING);

    public static final boolean USE_GLOBAL_DEFAULT = getOrDefault("UseGlobalDefault", Boolean.class);
    public static final int GLOBAL_DEFAULT_RESISTANCE = USE_GLOBAL_DEFAULT ? getOrDefault("GlobalDefaultResistance", Integer.class) : 0;
    public static final String PRESET_FOLDER_NAME = String.format("config/%s/presets/%s/", MODID, getOrDefault("Preset", String.class));

    private static <T> T getOrDefault(String key, Class<T> clazz) {
        try {
            if ((CONFIG.get(key) == null)) {
                LOGGER.error("Key [{}] is missing from Config: [{}] | Marking config file for repair...", key, fileName);
                needsRepair = true;
                return clazz.cast(DEFAULT_CONFIG.get(key));
            }
            return clazz.cast(CONFIG.get(key));
        } catch (Exception e) {
            LOGGER.error("Value: [{}] for [{}] is an invalid type in Config: {} | Expected: [{}] but got: [{}] | Marking config file for repair...", CONFIG.get(key), key, fileName, clazz.getTypeName(), CONFIG.get(key).getClass().getTypeName());
            needsRepair = true;
            return clazz.cast(DEFAULT_CONFIG.get(key));
        }
    }
}
