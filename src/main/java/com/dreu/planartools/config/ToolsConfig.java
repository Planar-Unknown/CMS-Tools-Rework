package com.dreu.planartools.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.fml.ModList;

import java.util.*;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.Util.LogLevel.ERROR;
import static com.dreu.planartools.Util.LogLevel.INFO;
import static com.dreu.planartools.Util.*;
import static com.dreu.planartools.config.GeneralConfig.PRESET_FOLDER_NAME;

@SuppressWarnings({"SameParameterValue"})
public class ToolsConfig {
    public static final Tier[] TIERS_BY_ID = {Tiers.WOOD, Tiers.STONE, Tiers.IRON, Tiers.DIAMOND, Tiers.NETHERITE};
    public static final String templateFileName = "config/" + MODID + "/presets/template/tools.toml";
    public static final String TEMPLATE_CONFIG_STRING = """
            # Values not included for Tools will default to the Default power.
            # Power indicates the block Resistance level a tool can overcome.
            # MiningSpeed indicates the rate at which a tool will mine blocks that it can mine.
            # Each block can be configured to choose whether a tools MiningSpeed will be applied.
            
            # Register tool types to use for Tool Power and Block Resistances
            # The order of this list will be the display order of Tooltips
            # You may specify Tooltip Color using 6 digit RGB Hex colors following the Tool Type
            ToolTypes = [
                "Pickaxe:FFD700",
                "Axe:8B4513",
                "Shovel:A9A9A9",
                "Hoe:32CD32",
                "Sword:DC143C",
                "Shears"
            ]
            
            [Tools]
            "minecraft:wooden_pickaxe" = {Pickaxe = 20, MiningSpeed = 2}
            "minecraft:stone_pickaxe" = {Pickaxe = 40, MiningSpeed = 4}
            "minecraft:iron_pickaxe" = {Pickaxe = 60, MiningSpeed = 6}
            "minecraft:golden_pickaxe" = {Pickaxe = 60, MiningSpeed = 12}
            "minecraft:diamond_pickaxe" = {Pickaxe = 80, MiningSpeed = 8}
            "minecraft:netherite_pickaxe" = {Pickaxe = 100, MiningSpeed = 9}
            
            "minecraft:wooden_shovel" = {Shovel = 20, MiningSpeed = 2}
            "minecraft:stone_shovel" = {Shovel = 40, MiningSpeed = 4}
            "minecraft:iron_shovel" = {Shovel = 60, MiningSpeed = 6}
            "minecraft:golden_shovel" = {Shovel = 60, MiningSpeed = 12}
            "minecraft:diamond_shovel" = {Shovel = 80, MiningSpeed = 8}
            "minecraft:netherite_shovel" = {Shovel = 100, MiningSpeed = 9}
            
            "minecraft:wooden_hoe" = {Hoe = 20, MiningSpeed = 2}
            "minecraft:stone_hoe" = {Hoe = 40, MiningSpeed = 4}
            "minecraft:iron_hoe" = {Hoe = 60, MiningSpeed = 6}
            "minecraft:golden_hoe" = {Hoe = 60, MiningSpeed = 12}
            "minecraft:diamond_hoe" = {Hoe = 80, MiningSpeed = 8}
            "minecraft:netherite_hoe" = {Hoe = 100, MiningSpeed = 9}
            
            "minecraft:wooden_axe" = {Axe = 20, MiningSpeed = 2}
            "minecraft:stone_axe" = {Axe = 40, MiningSpeed = 4}
            "minecraft:iron_axe" = {Axe = 60, MiningSpeed = 6}
            "minecraft:golden_axe" = {Axe = 60, MiningSpeed = 12}
            "minecraft:diamond_axe" = {Axe = 80, MiningSpeed = 8}
            "minecraft:netherite_axe" = {Axe = 100, MiningSpeed = 9}
            
            "minecraft:wooden_sword" = {Shears = 20, MiningSpeed = 2}
            "minecraft:stone_sword" = {Shears = 40, MiningSpeed = 4}
            "minecraft:iron_sword" = {Shears = 60, MiningSpeed = 6}
            "minecraft:golden_sword" = {Shears = 60, MiningSpeed = 12}
            "minecraft:diamond_sword" = {Shears = 80, MiningSpeed = 8}
            "minecraft:netherite_sword" = {Shears = 100, MiningSpeed = 9}
            
            "minecraft:shears" = {Shears = 100, MiningSpeed = 12}
            """;
    public static final Config CONFIG = parseFileOrDefault(PRESET_FOLDER_NAME + "tools.toml", TEMPLATE_CONFIG_STRING, false);
    private static final Config TEMPLATE_CONFIG = new TomlParser().parse(TEMPLATE_CONFIG_STRING);

    public static final ArrayList<String> REGISTERED_TOOL_TYPES = new ArrayList<>();
    public static final ArrayList<Integer> REGISTERED_TOOL_COLORS = new ArrayList<>();

    static {
        //noinspection unchecked
        getOrDefault("ToolTypes", ArrayList.class).forEach((toolType) -> {
            String entry = toolType.toString();
            String[] parts = entry.split(":");
            REGISTERED_TOOL_TYPES.add(parts[0]);
            if (parts.length == 2){
                REGISTERED_TOOL_COLORS.add(Integer.parseUnsignedInt(parts[1], 16));
            } else {
                REGISTERED_TOOL_COLORS.add(0xFFFFFF);
            }
        });
    }

    public static final Map<String, Properties> TOOLS = new HashMap<>();
    static {
        getOrDefault("Tools", Config.class).valueMap().forEach((itemId, tool) -> {
            if (!itemId.contains(":")) {
                addConfigIssue(INFO, (byte) 2, "No namespace found in item id: <{}> declared in config: [{}] | Skipping...", itemId, logFileName(templateFileName));
                return;
            }
            if (ModList.get().isLoaded(itemId.substring(0, itemId.indexOf(":")))) {
                List<PowerData> powerDataList = new ArrayList<>();
                Integer miningSpeed = 1;
                for (Map.Entry<String, Object> property : ((Config) tool).valueMap().entrySet()) {
                    if (property.getKey().equals("MiningSpeed")) miningSpeed = (Integer) property.getValue();
                    else {
                        if (!REGISTERED_TOOL_TYPES.contains(property.getKey())) {
                            addConfigIssue(ERROR, (byte) 6, "\"{}\" declared for <{}> in config file [{}] is NOT a registered tool type!", property.getKey(), itemId, logFileName(PRESET_FOLDER_NAME + "tools.toml"));
                        }
                        powerDataList.add(new PowerData(
                                (byte) REGISTERED_TOOL_TYPES.indexOf(property.getKey()),
                                (byte) Mth.clamp(Math.floor((int)property.getValue()*0.05), 0, TIERS_BY_ID.length),
                                (int) property.getValue()
                        ));

                    }
                }
                powerDataList.sort(Comparator.comparingInt(PowerData::toolTypeId));

                TOOLS.put(itemId, new Properties(
                    powerDataList.toArray(new PowerData[0]),
                    miningSpeed
                ));
            } else addConfigIssue(INFO, (byte) 2, "Config [{}] declared tool power values for <{}> when {{}} was not loaded or does not exist in this modpack | Skipping Item...", logFileName(templateFileName), itemId, itemId.substring(0, itemId.indexOf(":")));
        });
    }

    private static <T> T getOrDefault(String key, Class<T> clazz) {
        try {
            if ((CONFIG.get(key) == null)) {
                addConfigIssue(ERROR, (byte) 4, "Key \"{}\" is missing from config [{}] | Using basic Template instead...", key, logFileName(PRESET_FOLDER_NAME + "tools.toml"));
                return clazz.cast(TEMPLATE_CONFIG.get(key));
            }
            return clazz.cast(CONFIG.get(key));
        } catch (Exception e) {
            addConfigIssue(ERROR, (byte) 4, "Value: \"{}\" for \"{}\" is an invalid type in config [{}] | Expected: '{}' but got: '{}' | Using basic Template instead...", CONFIG.get(key), key, logFileName(PRESET_FOLDER_NAME + "tools.toml"), clazz.getTypeName(), CONFIG.get(key).getClass().getTypeName());
            return clazz.cast(TEMPLATE_CONFIG.get(key));
        }
    }

    public record Properties(PowerData[] data, int miningSpeed) {}
    public record PowerData(byte toolTypeId, byte tierId, int power) {}
}