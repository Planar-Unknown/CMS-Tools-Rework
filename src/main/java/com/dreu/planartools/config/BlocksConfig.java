package com.dreu.planartools.config;

import com.electronwill.nightconfig.core.Config;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.Util.LogLevel.*;
import static com.dreu.planartools.Util.*;
import static com.dreu.planartools.config.GeneralConfig.PRESET_FOLDER_NAME;
import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_TYPES;

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
    Shovel = {Resistance = 40, ApplyMiningSpeed = false}  # Tools with ShovelPower ≥ 40 can mine this block but their MiningSpeed is NOT applied.
    Pickaxe = {Resistance = 20, ApplyMiningSpeed = true}  # Tools with PickaxePower ≥ 20 can mine this block and their MiningSpeed IS applied.
    
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

    public static final Config CONFIG = parseFileOrDefault(PRESET_FOLDER_NAME + "blocks.toml", TEMPLATE_CONFIG_STRING, false);

    public static final Map<String, Properties> BLOCKS = new HashMap<>();
    static {
        CONFIG.valueMap().forEach((blockId, block) -> {
            if (!blockId.contains(":")) {
                addConfigIssue(INFO, (byte) 2, "No namespace found in item id: <{}> declared in config: [{}] | Skipping...", blockId, logFileName(templateFileName));
                return;
            }
            if (ModList.get().isLoaded(blockId.substring(0, blockId.indexOf(":")))) {
                Map<Byte, ResistanceData> resistanceDataMap = new HashMap<>();

                Integer defaultResistance = getOrElse(((Config) block), blockId, "DefaultResistance", 0, Integer.class);

                for (Map.Entry<String, Object> property : ((Config) block).valueMap().entrySet()) {
                    switch (property.getKey()) {
                        case "DefaultResistance", "ExplosionResistance", "Hardness" -> {continue;}
                        default -> resistanceDataMap.put(
                                (byte) REGISTERED_TOOL_TYPES.indexOf(property.getKey()),
                                new ResistanceData(
                                        getOrElse(((Config) property.getValue()), property.getKey(), "Resistance", defaultResistance, Integer.class),
                                        getOrElse(((Config) property.getValue()), property.getKey(), "ApplyMiningSpeed", false, Boolean.class)
                                )
                        );
                    }
                    if (!REGISTERED_TOOL_TYPES.contains(property.getKey())) {
                        addConfigIssue(ERROR, (byte) 6, "\"{}\" in config file [{}] is NOT a registered tool type!", property.getKey(), logFileName(PRESET_FOLDER_NAME + "blocks.toml"));
                    }
                }

                BLOCKS.put(blockId, new Properties(
                        getOptionalFloat(((Config) block), "Hardness"),
                        getOptionalFloat(((Config) block), "ExplosionResistance"),
                        defaultResistance,
                        resistanceDataMap
                ));
            } else
                addConfigIssue(INFO, (byte) 2, "Config [{}] declared Block Resistance values for <{}> when {{}} was not loaded or does not exist in this modpack | Skipping Block...", logFileName(PRESET_FOLDER_NAME + "blocks.toml"), blockId, blockId.substring(0, blockId.indexOf(":")));
        });
    }

    private static <T> T getOrElse(Config config, String parentKey, String key, T fallback, Class<T> clazz) {
        try {
            clazz.cast(config.get(key));
        } catch (Exception e) {
            addConfigIssue(WARN, (byte) 4, "Value: \"{}\" for \"{}.{}\" is an invalid type in config [{}] | Expected: '{}' but got: '{}' | Ignoring property...", config.get(key), parentKey, key, logFileName(PRESET_FOLDER_NAME + "blocks.toml"), Float.class.getTypeName(), config.get(key).getClass().getTypeName());
            return fallback;
        }
        T toReturn = config.get(key);
        return toReturn == null ? fallback : toReturn;
    }

    private static Optional<Float> getOptionalFloat(Config values, String key) {
        try {
            //noinspection RedundantClassCall
            Number.class.cast(values.get(key));
        } catch (Exception e) {
            addConfigIssue(WARN, (byte) 4, "Value: \"{}\" for \"{}\" is an invalid type in config [{}] | Expected: '{}' but got: '{}' | Ignoring property...", values.get(key), key, logFileName(PRESET_FOLDER_NAME + "tools.toml"), Float.class.getTypeName(), values.get(key).getClass().getTypeName());
            return Optional.empty();
        }
        Number explosionResistance = values.get(key);
        return explosionResistance == null ? Optional.empty() : Optional.of(explosionResistance.floatValue());
    }

    @SuppressWarnings("DataFlowIssue")
    public static Properties getBlockProperties(Block block) {
        return BLOCKS.get(ForgeRegistries.BLOCKS.getKey(block).toString());
    }

    public record Properties(Optional<Float> hardness, Optional<Float> explosionResistance, int defaultResistance, Map<Byte, ResistanceData> data) {}
    public record ResistanceData(int resistance, boolean applyMiningSpeed) {}
}
