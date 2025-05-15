package com.dreu.planartools.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraftforge.fml.ModList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import static com.dreu.planartools.PlanarTools.*;

@SuppressWarnings({"SameParameterValue"})
public class ToolsConfig {
    public static boolean needsRepair = false;
    public static final String fileName = "config/" + MODID + "/tools.toml";
    static final String DEFAULT_CONFIG_STRING = """
            # To reset this config to default, delete this file and rerun the game.
            # Values not included for Tools will default to the Default power.
            # Items not registered by their mod as Tools won't be valid.
            Default = {PickaxePower = -1, AxePower = -1, ShovelPower = -1, HoePower = -1, ShearPower = -1, MiningSpeed = 1}
            
            [Tools]
            "minecraft:wooden_pickaxe" = {PickaxePower = 20, MiningSpeed = 2}
            "minecraft:stone_pickaxe" = {PickaxePower = 40, MiningSpeed = 4}
            "minecraft:iron_pickaxe" = {PickaxePower = 60, MiningSpeed = 6}
            "minecraft:golden_pickaxe" = {PickaxePower = 60, MiningSpeed = 12}
            "minecraft:diamond_pickaxe" = {PickaxePower = 80, MiningSpeed = 8}
            "minecraft:netherite_pickaxe" = {PickaxePower = 100, MiningSpeed = 9}
            
            "minecraft:wooden_shovel" = {ShovelPower = 20, MiningSpeed = 2}
            "minecraft:stone_shovel" = {ShovelPower = 40, MiningSpeed = 4}
            "minecraft:iron_shovel" = {ShovelPower = 60, MiningSpeed = 6}
            "minecraft:golden_shovel" = {ShovelPower = 60, MiningSpeed = 12}
            "minecraft:diamond_shovel" = {ShovelPower = 80, MiningSpeed = 8}
            "minecraft:netherite_shovel" = {ShovelPower = 100, MiningSpeed = 9}
            
            "minecraft:wooden_hoe" = {HoePower = 20, MiningSpeed = 2}
            "minecraft:stone_hoe" = {HoePower = 40, MiningSpeed = 4}
            "minecraft:iron_hoe" = {HoePower = 60, MiningSpeed = 6}
            "minecraft:golden_hoe" = {HoePower = 60, MiningSpeed = 12}
            "minecraft:diamond_hoe" = {HoePower = 80, MiningSpeed = 8}
            "minecraft:netherite_hoe" = {HoePower = 100, MiningSpeed = 9}
            
            "minecraft:wooden_axe" = {AxePower = 20, MiningSpeed = 2}
            "minecraft:stone_axe" = {AxePower = 40, MiningSpeed = 4}
            "minecraft:iron_axe" = {AxePower = 60, MiningSpeed = 6}
            "minecraft:golden_axe" = {AxePower = 60, MiningSpeed = 12}
            "minecraft:diamond_axe" = {AxePower = 80, MiningSpeed = 8}
            "minecraft:netherite_axe" = {AxePower = 100, MiningSpeed = 9}
            
            "minecraft:wooden_sword" = {ShearPower = 20, MiningSpeed = 2}
            "minecraft:stone_sword" = {ShearPower = 40, MiningSpeed = 4}
            "minecraft:iron_sword" = {ShearPower = 60, MiningSpeed = 6}
            "minecraft:golden_sword" = {ShearPower = 60, MiningSpeed = 12}
            "minecraft:diamond_sword" = {ShearPower = 80, MiningSpeed = 8}
            "minecraft:netherite_sword" = {ShearPower = 100, MiningSpeed = 9}
            
            "minecraft:shears" = {ShearPower = 100, MiningSpeed = 12}
            """;
    private static final Config CONFIG = parseFileOrDefault(fileName, DEFAULT_CONFIG_STRING);
    private static final Config DEFAULT_CONFIG = new TomlParser().parse(DEFAULT_CONFIG_STRING);

    public static final Map<String, Integer> DEFAULT_POWERS = Map.of(
        "PickaxePower", getOrDefault("Default.PickaxePower", Integer.class),
        "AxePower", getOrDefault("Default.AxePower", Integer.class),
        "ShovelPower", getOrDefault("Default.ShovelPower", Integer.class),
        "HoePower", getOrDefault("Default.HoePower", Integer.class),
        "ShearPower", getOrDefault("Default.ShearPower", Integer.class)
    );
    
    public static final Map<String, Map.Entry<int[], Integer>> TOOLS = new HashMap<>();
    static {
        getOrDefault("Tools", Config.class).valueMap().forEach((itemId, values) -> {
            if (!itemId.contains(":")) {
                LOGGER.warn("No namespace found in item id: [{}] declared in config: [{}] | Skipping...", itemId, fileName);
                return;
            }
            if (ModList.get().isLoaded(itemId.substring(0, itemId.indexOf(":")))) {
                TOOLS.put(itemId, Map.entry(
                    new int[]{
                        getPropertyOrDefault((Config) values, "PickaxePower", itemId),
                        getPropertyOrDefault((Config) values, "AxePower", itemId),
                        getPropertyOrDefault((Config) values, "ShovelPower", itemId),
                        getPropertyOrDefault((Config) values, "HoePower", itemId),
                        getPropertyOrDefault((Config) values, "ShearPower", itemId)
                    },
                    getPropertyOrDefault((Config) values, "MiningSpeed", itemId))
            );
            } else
                LOGGER.info("Config [{}] declared tool power values for [{}] when [{}] was not loaded | Skipping Item...", fileName, itemId, itemId.substring(0, itemId.indexOf(":")));
        });
    }

    private static int getPropertyOrDefault(Config propertyValues, String property, String itemId) {
        if (!propertyValues.contains(property))
            return DEFAULT_POWERS.get(property);
        return propertyValues.getIntOrElse(property, () -> {
            LOGGER.warn("Value for {} in {} was not an Integer | Substituting with default value...", itemId + "." + property, fileName);
            return DEFAULT_POWERS.get(property);
        });
    }

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