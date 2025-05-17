package com.dreu.planartools.config;

import com.electronwill.nightconfig.core.Config;
import net.minecraftforge.fml.ModList;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.dreu.planartools.PlanarTools.*;
import static com.dreu.planartools.config.GeneralConfig.GLOBAL_DEFAULT_RESISTANCE;

public class BlocksConfig {

    public static final String templateFileName = "config/" + MODID + "/presets/template/blocks.toml";
    public static final String TEMPLATE_CONFIG_STRING = """
    # Modded Items that override the getDestroySpeed method will not be valid.
    # To request compatibility with a specific mod, let us know in our Discord | https://discord.gg/RrY3rXuAH5
    
    # This table shows the default power level of each tier of tool.
    ########################################################################
    # Tier  -> | Wooden |  Stone  |  Iron  |  Gold  | Diamond  | Netherite #
    #----------|--------|---------|--------|--------|----------|-----------#
    # Power -> |   20   |    40   |   60   |   60   |    80    |    100    #
    ########################################################################
    
    # Here is an example of a block that can only be mined with a Power 40+ Shovel OR a Power 20+ Pickaxe:
    ["minecraft:packed_mud"] # Block ID
    Hardness = 1.0 # Affects mining time (e.g., dirt = 0.5, stone = 1.5, bedrock = -1). Only include this if you want to change a block's existing hardness.
    DefaultResistance = -1 # Resistance to unlisted tool types (-1 = unbreakable)
    Shovel = { Resistance = 40, ApplyMiningSpeed = false }  # Tools with ShovelPower ≥ 40 can mine this block but their MiningSpeed is NOT applied.
    Pickaxe = { Resistance = 20, ApplyMiningSpeed = true  }  # Tools with PickaxePower ≥ 20 can mine this block and their MiningSpeed IS applied.
    
    ["minecraft:dirt"]
    DefaultResistance = 0 # Zero indicates no resistance, meaning no power is required to mine it. So any tool, item (or fist) works!
    Shovel = {ApplyMiningSpeed = true} # Even though ANYTHING can mine it, only tools/items that have ShovelPower apply their MiningSpeed.
    
    ["minecraft:deepslate"]
    DefaultResistance = -1
    Pickaxe = {Resistance = 40, ApplyMiningSpeed = true}
    
    ["minecraft:obsidian"]
    DefaultResistance = -1
    Pickaxe = {Resistance = 80, ApplyMiningSpeed = true}
    
    ["minecraft:oak_log"] # Same as vanilla
    DefaultResistance = 0
    Axe = {ApplyMiningSpeed = true}
    """;

    private static final Config CONFIG = parseFileOrDefault(GeneralConfig.PRESET_FOLDER_NAME + "blocks.toml", TEMPLATE_CONFIG_STRING, false);

    public static final Map<String, Properties> BLOCKS = new HashMap<>();
    static {
        CONFIG.valueMap().forEach((itemId, values) -> {
            if (!itemId.contains(":")) {
                LOGGER.warn("No namespace found in item id: [{}] declared in config: [{}] | Skipping...", itemId, templateFileName);
                return;
            }
            if (ModList.get().isLoaded(itemId.substring(0, itemId.indexOf(":")))) {
                int defaultResistance = getResistanceOrDefault((Config) values, itemId);
                Map<String, ToolData> toolDataMap = new HashMap<>();

                putToolData(itemId, (Config) values, toolDataMap, defaultResistance, "Pickaxe");
                putToolData(itemId, (Config) values, toolDataMap, defaultResistance, "Axe");
                putToolData(itemId, (Config) values, toolDataMap, defaultResistance, "Shovel");
                putToolData(itemId, (Config) values, toolDataMap, defaultResistance, "Hoe");
                putToolData(itemId, (Config) values, toolDataMap, defaultResistance, "Shears");

                BLOCKS.put(itemId, new Properties(
                        getOptionalHardness((Config) values),
                        toolDataMap)
                );
            } else
                LOGGER.info("Config [{}] declared Block Resistance values for [{}] when [{}] was not loaded | Skipping Block...", templateFileName, itemId, itemId.substring(0, itemId.indexOf(":")));
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
            LOGGER.warn("Value for {} in {} was not a [{}] | Substituting with default value...", itemId + "." + property, templateFileName, defaultTo.getClass().getTypeName());
            return defaultTo;
        });
    }

    private static int getResistanceOrDefault(Config propertyValues, String itemId) {
        if (!propertyValues.contains("DefaultResistance"))
            return GLOBAL_DEFAULT_RESISTANCE;
        return propertyValues.getIntOrElse("DefaultResistance", () -> {
            LOGGER.warn("Value for {}.DefaultResistance in {} was not an Integer | Substituting with default value...", itemId, templateFileName);
            return GLOBAL_DEFAULT_RESISTANCE;
        });
    }

    private static Optional<Float> getOptionalHardness(Config values) {
        Double hardness = values.get("Hardness");
        return hardness == null ? Optional.empty() : Optional.of(((Number) values.get("Hardness")).floatValue());
    }

    public record Properties(Optional<Float> hardness, Map<String, ToolData> toolDataMap) {}
    public record ToolData(int resistance, boolean applyMiningSpeed) {}

}
