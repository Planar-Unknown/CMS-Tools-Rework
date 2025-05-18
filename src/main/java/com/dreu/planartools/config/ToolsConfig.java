package com.dreu.planartools.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dreu.planartools.PlanarTools.*;
import static com.dreu.planartools.config.GeneralConfig.PRESET_FOLDER_NAME;
import static java.lang.String.format;

@SuppressWarnings({"SameParameterValue"})
public class ToolsConfig {
    public static final Tier[] TIERS_BY_ID = {Tiers.WOOD, Tiers.STONE, Tiers.IRON, Tiers.DIAMOND, Tiers.NETHERITE};
    public static boolean needsRepair = false;
    public static final String templateFileName = "config/" + MODID + "/presets/template/tools.toml";
    public static final String TEMPLATE_CONFIG_STRING = """
            # Values not included for Tools will default to the Default power.
            # Power indicates the block Resistance level a tool can overcome.
            # MiningSpeed indicates the rate at which a tool will mine blocks that it can mine.
            # Each block can be configured to choose whether a tools MiningSpeed will be applied.
            ToolTypes = ["Pickaxe", "Axe", "Shovel", "Hoe", "Shear", "Sword"]
            
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
            
            "minecraft:wooden_sword" = {Shear = 20, MiningSpeed = 2}
            "minecraft:stone_sword" = {Shear = 40, MiningSpeed = 4}
            "minecraft:iron_sword" = {Shear = 60, MiningSpeed = 6}
            "minecraft:golden_sword" = {Shear = 60, MiningSpeed = 12}
            "minecraft:diamond_sword" = {Shear = 80, MiningSpeed = 8}
            "minecraft:netherite_sword" = {Shear = 100, MiningSpeed = 9}
            
            "minecraft:shears" = {Shear = 100, MiningSpeed = 12}
            """;
    public static final Config CONFIG = parseFileOrDefault(PRESET_FOLDER_NAME + "tools.toml", TEMPLATE_CONFIG_STRING, false);
    private static final Config TEMPLATE_CONFIG = new TomlParser().parse(TEMPLATE_CONFIG_STRING);

    @SuppressWarnings("unchecked") public static final ArrayList<String> REGISTERED_TOOL_TYPES = getOrDefault("ToolTypes", ArrayList.class);

    public static final Map<String, Properties> TOOLS = new HashMap<>();
    static {
        getOrDefault("Tools", Config.class).valueMap().forEach((itemId, tool) -> {
            if (!itemId.contains(":")) {
                LOGGER.warn("No namespace found in item id: [{}] declared in config: [{}] | Skipping...", itemId, templateFileName);
                return;
            }
            if (ModList.get().isLoaded(itemId.substring(0, itemId.indexOf(":")))) {
                List<PowerData> powerDataList = new ArrayList<>();
                Integer miningSpeed = 1;
                for (Map.Entry<String, Object> property : ((Config) tool).valueMap().entrySet()) {
                    if (property.getKey().equals("MiningSpeed")) miningSpeed = (Integer) property.getValue();
                    else {
                        if (!REGISTERED_TOOL_TYPES.contains(property.getKey())) {
                            throw new IllegalStateException(format("[%s] in config file [%s] is not a registered tool type", property.getKey(), PRESET_FOLDER_NAME + "tools.toml"));
                        }
                        powerDataList.add(new PowerData(
                                (byte) REGISTERED_TOOL_TYPES.indexOf(property.getKey()),
                                (byte) Mth.clamp(Math.floor((int)property.getValue()*0.05), 0, TIERS_BY_ID.length),
                                (int) property.getValue()
                        ));

                    }
                }
                TOOLS.put(itemId, new Properties(
                    powerDataList.toArray(new PowerData[0]),
                    miningSpeed
                ));
            } else LOGGER.info("Config [{}] declared tool power values for [{}] when [{}] was not loaded | Skipping Item...", templateFileName, itemId, itemId.substring(0, itemId.indexOf(":")));
        });
    }

    private static <T> T getOrDefault(String key, Class<T> clazz) {
        try {
            if ((CONFIG.get(key) == null)) {
                LOGGER.error("Key [{}] is missing from Config: [{}] | Marking config file for repair...", key, templateFileName);
                needsRepair = true;
                return clazz.cast(TEMPLATE_CONFIG.get(key));
            }
            return clazz.cast(CONFIG.get(key));
        } catch (Exception e) {
            LOGGER.error("Value: [{}] for [{}] is an invalid type in Config: {} | Expected: [{}] but got: [{}] | Marking config file for repair...", CONFIG.get(key), key, templateFileName, clazz.getTypeName(), CONFIG.get(key).getClass().getTypeName());
            needsRepair = true;
            return clazz.cast(TEMPLATE_CONFIG.get(key));
        }
    }

    public record Properties(PowerData[] data, int miningSpeed) {}
    public record PowerData(byte toolTypeId, byte tierId, int power) {}
}