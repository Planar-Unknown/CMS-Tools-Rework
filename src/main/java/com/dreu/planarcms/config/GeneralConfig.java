package com.dreu.planarcms.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;

import java.io.FileWriter;

import static com.dreu.planarcms.PlanarCMS.MODID;
import static com.dreu.planarcms.util.Helpers.*;
import static com.dreu.planarcms.util.Helpers.LogLevel.ERROR;
import static com.dreu.planarcms.util.Helpers.LogLevel.WARN;

public class GeneralConfig {
    public static boolean needsRepair;
    public static final String fileName = "config/" + MODID + "/general.toml";

    public static void repair() {
        try (FileWriter writer = new FileWriter(fileName)){
            writer.write(
                    "# To reset this config to default, delete this file and rerun the game.\n\n" +
                    "# Where the waila will be located by default. Valid values are \"left\", \"middle\", \"right\", or \"invisible\"\n" +
                    "DefaultWailaPosition = \"" + DEFAULT_WAILA_POSITION.toString().toLowerCase() + "\"\n\n" +
                    "# To use a custom preset, create a folder in: config/planar_cms/presets/[your-preset-name]\n" +
                    "# Add blocks.toml, tools.toml, and enchants.toml to your preset folder.\n" +
                    "# Use the \"template\" preset in config/planar_cms/presets/template as an example.\n" +
                    "Preset = \"" + PRESET + "\""
            );
        } catch (Exception e) {
            addConfigIssue(ERROR, (byte) 5, "Encountered exception while writing repaired config file [{}] | Exception: {}", fileName, e.getMessage());
        }
    }

    public static void save(WailaPosition defaultWailaPosition, String preset) {
        try (FileWriter writer = new FileWriter(fileName)){
            writer.write(
                    "# To reset this config to default, delete this file and rerun the game.\n\n" +
                    "# Where the waila will be located by default. Valid values are \"left\", \"middle\", \"right\", or \"invisible\"\n" +
                    "DefaultWailaPosition = \"" + defaultWailaPosition.toString().toLowerCase() + "\"\n\n" +
                    "# To use a custom preset, create a folder in: config/planar_cms/presets/[your-preset-name]\n" +
                    "# Add blocks.toml, tools.toml, and enchants.toml to your preset folder.\n" +
                    "# Use the \"template\" preset in config/planar_cms/presets/template as an example.\n" +
                    "Preset = \"" + preset + "\""
            );
        } catch (Exception e) {
            addConfigIssue(ERROR, (byte) 5, "Encountered exception while saving config file [{}] | Exception: {}", fileName, e.getMessage());
        }
    }

    static final String DEFAULT_CONFIG_STRING = """
            # To reset this config to default, delete this file and rerun the game.
            
            # Where the waila will be located by default. Valid values are "left", "middle", "right", or "invisible"
            DefaultWailaPosition = "middle"
            
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

    public static void populate() {
        PRESET = getOrDefault("Preset", String.class);
        PRESET_FOLDER_NAME = String.format("config/%s/presets/%s/", MODID, PRESET);
        DEFAULT_WAILA_POSITION = wailaPosFromString(getOrDefault("DefaultWailaPosition", String.class));
        WAILA_POSITION = DEFAULT_WAILA_POSITION;
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
}
