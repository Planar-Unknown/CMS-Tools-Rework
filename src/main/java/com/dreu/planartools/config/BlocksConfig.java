package com.dreu.planartools.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.dreu.planartools.PlanarTools.*;
import static com.dreu.planartools.PlanarTools.LOGGER;

public class BlocksConfig {
    public static boolean needsRepair = false;
    public static final String fileName = "config/" + MODID + "/blocks.toml";

    static final String DEFAULT_CONFIG_STRING = """
    # To reset this config to default, delete this file and rerun the game.
    # Items not registered by their mod as Tools won't be valid.
    # Default Power will be based on required tool type and tier
    # that the block was originally registered with.
    # This table shows the default power level of each tier of tool.
    ###############################################################
    # Tier  -> | Wooden |  Stone  |  Iron  | Diamond  | Netherite #
    #----------|--------|---------|--------|----------|-----------#
    # Power -> |   20   |    40   |   60   |    80    |    100    #
    ###############################################################
    
    [[Blocks]]
    ["minecraft:dirt"]
    Hardness = 0.5
    DefaultResistance = 0
    Shovel = {Resistance = 0, ApplyMiningSpeed = true}
    
    ["minecraft:stone"]
    Hardness = 1.5
    DefaultResistance = -1
    Pickaxe = {Resistance = 20, ApplyMiningSpeed = true}
    
    """;

    public static final Map<String, Integer> DEFAULT_PROPERTIES = Map.of(
            "Hardness", 1000,
            "DefaultResistance", -1
            );

    private static final Config CONFIG = parseFileOrDefault(fileName, DEFAULT_CONFIG_STRING);
    private static final Config DEFAULT_CONFIG = new TomlParser().parse(DEFAULT_CONFIG_STRING);

    public static final Map<String, Properties> BLOCKS = new HashMap<>();
    static {
        CONFIG.valueMap().forEach((itemId, values) -> {
            if (!itemId.contains(":")) {
                LOGGER.warn("No namespace found in item id: [{}] declared in config: [{}] | Skipping...", itemId, fileName);
                return;
            }
            if (ModList.get().isLoaded(itemId.substring(0, itemId.indexOf(":")))) {
                int defaultResistance = getPropertyOrDefault((Config) values, "DefaultResistance", itemId);
                Map<String, ToolData> toolDataMap = new HashMap<>();

                putToolData(itemId, (Config) values, toolDataMap, defaultResistance, "Pickaxe");
                putToolData(itemId, (Config) values, toolDataMap, defaultResistance, "Axe");
                putToolData(itemId, (Config) values, toolDataMap, defaultResistance, "Shovel");
                putToolData(itemId, (Config) values, toolDataMap, defaultResistance, "Hoe");
                putToolData(itemId, (Config) values, toolDataMap, defaultResistance, "Shears");

                BLOCKS.put(itemId, new Properties(
                        getPropertyOrDefault((Config) values, "Hardness", itemId),
                        toolDataMap)
                );
            } else
                LOGGER.info("Config [{}] declared Block Resistance values for [{}] when [{}] was not loaded | Skipping Block...", fileName, itemId, itemId.substring(0, itemId.indexOf(":")));
        });
    }

    private static void putToolData(String itemId, Config values, Map<String, ToolData> toolDataMap, int defaultResistance, String tool) {
        toolDataMap.put(tool, new ToolData(
                getPropertyOrElse(values, tool + ".Resistance", itemId, defaultResistance),
                getPropertyOrElse(values, tool + ".ApplyMiningSpeed", itemId, false)
        ));
    }

    private static <T> T getPropertyOrElse(Config propertyValues, String property, String itemId, T defaultTo) {
        if (!propertyValues.contains(property))
            return defaultTo;
        return propertyValues.getOrElse(property, () -> {
            LOGGER.warn("Value for {} in {} was not a [{}] | Substituting with default value...", itemId + "." + property, fileName, defaultTo.getClass().getTypeName());
            return defaultTo;
        });
    }

    private static int getPropertyOrDefault(Config propertyValues, String property, String itemId) {
        if (!propertyValues.contains(property))
            return DEFAULT_PROPERTIES.get(property);
        return propertyValues.getIntOrElse(property, () -> {
            LOGGER.warn("Value for {} in {} was not an Integer | Substituting with default value...", itemId + "." + property, fileName);
            return DEFAULT_PROPERTIES.get(property);
        });
    }

//    private static <T> T getOrDefault(String key, Class<T> clazz) {
//        try {
//            if ((CONFIG.get(key) == null)) {
//                LOGGER.error("Key [{}] is missing from Config: [{}] | Marking config file for repair...", key, fileName);
//                needsRepair = true;
//                return clazz.cast(DEFAULT_CONFIG.get(key));
//            }
//            return clazz.cast(CONFIG.get(key));
//        } catch (Exception e) {
//            LOGGER.error("Value: [{}] for [{}] is an invalid type in Config: {} | Expected: [{}] but got: [{}] | Marking config file for repair...", CONFIG.get(key), key, fileName, clazz.getTypeName(), CONFIG.get(key).getClass().getTypeName());
//            needsRepair = true;
//            return clazz.cast(DEFAULT_CONFIG.get(key));
//        }
//    }

    public record Properties(int hardness, Map<String, ToolData> toolDataMap) {}
    public record ToolData(int resistance, boolean applyMiningSpeed) {}

}
