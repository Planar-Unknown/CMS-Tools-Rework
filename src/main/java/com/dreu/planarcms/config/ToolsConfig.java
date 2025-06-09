package com.dreu.planarcms.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static com.dreu.planarcms.PlanarCMS.MODID;
import static com.dreu.planarcms.config.GeneralConfig.PRESET_FOLDER_NAME;
import static com.dreu.planarcms.util.Helpers.LogLevel.*;
import static com.dreu.planarcms.util.Helpers.*;

@SuppressWarnings({"SameParameterValue"})
public class ToolsConfig {
    public static final String TEMPLATE_FILE_NAME = "config/" + MODID + "/presets/template/tools.toml";
    public static final String TEMPLATE_CONFIG_STRING = """
            # Values not included for Tools will default to the Default power.
            # Power indicates the block Resistance level a tool can overcome.
            # MiningSpeed indicates the rate at which a tool will mine blocks that it can mine.
            # Each block can be configured to choose whether a tools MiningSpeed will be applied.
            
            # Collections in this file (denoted by "@") are custom groups of Items
            # Create your own item collections at: [config/planar_cms/collections/items]
            # For example, the "@golden_tools" collection can be found at [config/planar_cms/collections/items/golden_tools.txt]
            # Item Collections may contain Tags of items or individual items, but may not contain other collections
            
            # Register tool types to use for Tool Power and Block Resistances
            # The order of this list will be the display order of Tooltips
            # You may specify Tooltip Color using 6 digit RGB Hex colors following the Tool Type
            ToolTypes = [
                "Pickaxe:FFD700",
                "Axe:8B4513",
                "Shovel:A9A9A9",
                "Hoe:32CD32",
                "Sword:DC143C",
                "Shears",
                "Arcane:7F00FF"
            ]
            
            [Tools]
            "#minecraft:swords" = {Shears = 20}
            "@golden_tools" = {Arcane = 30}
            
            "minecraft:wooden_pickaxe" = {Pickaxe = 20, MiningSpeed = 2}
            "minecraft:stone_pickaxe" = {Pickaxe = 40, MiningSpeed = 4}
            "minecraft:iron_pickaxe" = {Pickaxe = 60, MiningSpeed = 6}
            "minecraft:golden_pickaxe" = {Pickaxe = 40, MiningSpeed = 12}
            "minecraft:diamond_pickaxe" = {Pickaxe = 80, MiningSpeed = 8}
            "minecraft:netherite_pickaxe" = {Pickaxe = 100, MiningSpeed = 9}
            
            "minecraft:wooden_shovel" = {Shovel = 20, MiningSpeed = 2}
            "minecraft:stone_shovel" = {Shovel = 40, MiningSpeed = 4}
            "minecraft:iron_shovel" = {Shovel = 60, MiningSpeed = 6}
            "minecraft:golden_shovel" = {Shovel = 40, MiningSpeed = 12}
            "minecraft:diamond_shovel" = {Shovel = 80, MiningSpeed = 8}
            "minecraft:netherite_shovel" = {Shovel = 100, MiningSpeed = 9}
            
            "minecraft:wooden_hoe" = {Hoe = 20, MiningSpeed = 2}
            "minecraft:stone_hoe" = {Hoe = 40, MiningSpeed = 4}
            "minecraft:iron_hoe" = {Hoe = 60, MiningSpeed = 6}
            "minecraft:golden_hoe" = {Hoe = 40, MiningSpeed = 12}
            "minecraft:diamond_hoe" = {Hoe = 80, MiningSpeed = 8}
            "minecraft:netherite_hoe" = {Hoe = 100, MiningSpeed = 9}
            
            "minecraft:wooden_axe" = {Axe = 20, MiningSpeed = 2}
            "minecraft:stone_axe" = {Axe = 40, MiningSpeed = 4}
            "minecraft:iron_axe" = {Axe = 60, MiningSpeed = 6}
            "minecraft:golden_axe" = {Axe = 40, MiningSpeed = 12}
            "minecraft:diamond_axe" = {Axe = 80, MiningSpeed = 8}
            "minecraft:netherite_axe" = {Axe = 100, MiningSpeed = 9}
            
            "minecraft:wooden_sword" = {Sword = 20, MiningSpeed = 2}
            "minecraft:stone_sword" = {Sword = 40, MiningSpeed = 4}
            "minecraft:iron_sword" = {Sword = 60, MiningSpeed = 6}
            "minecraft:golden_sword" = {Sword = 40, MiningSpeed = 12}
            "minecraft:diamond_sword" = {Sword = 80, MiningSpeed = 8}
            "minecraft:netherite_sword" = {Sword = 100, MiningSpeed = 9}
            
            "minecraft:shears" = {Shears = 100, MiningSpeed = 10}
            """;

    public static Config CONFIG;
    private static final Config TEMPLATE_CONFIG = new TomlParser().parse(TEMPLATE_CONFIG_STRING);

    public static void parse() {
        CONFIG = parseFileOrDefault(PRESET_FOLDER_NAME + "tools.toml", TEMPLATE_CONFIG_STRING);
    }

    public static final ArrayList<String> REGISTERED_TOOL_TYPES = new ArrayList<>();
    public static final ArrayList<Integer> REGISTERED_TOOL_COLORS = new ArrayList<>();

    public static void populateToolTypes() {
        REGISTERED_TOOL_TYPES.clear();
        //noinspection unchecked
        getOrDefault("ToolTypes", ArrayList.class).forEach(toolType -> {
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

    public static Map<String, Properties> TOOLS = new HashMap<>();
    public static void populateTools() {
        TOOLS.clear();
        Map<String, Object> toolsConfig = getOrDefault("Tools", Config.class).valueMap();
        Map<String, Config> singleTools = new HashMap<>();
        toolsConfig.forEach((configKey, toolProperties) -> {
            if (configKey.startsWith("#")) {
                handleTag(configKey, (Config) toolProperties, Optional.empty());
            } else if (configKey.startsWith("@")) {
                handleCollection(configKey, (Config) toolProperties);
            } else {
                singleTools.put(configKey, (Config) toolProperties);
            }
        });
        singleTools.forEach(ToolsConfig::handleSingleItem);
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "DataFlowIssue"})
    private static void handleTag(String configKey, Config toolProperties, Optional<String> collectionName) {
        String tagId = configKey.substring(1);
        if (!ResourceLocation.isValidResourceLocation(tagId)) {
            addConfigIssue(INFO, (byte) 2, "Not a valid Tag ResourceLocation: <{}> declared in {} | Skipping...", configKey, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + "tools.toml]"));
            return;
        }
        if (!ForgeRegistries.ITEMS.tags().isKnownTagName(ItemTags.create(new ResourceLocation(tagId)))) {
            addConfigIssue(INFO, (byte) 2, "Not an existing Item Tag: <{}> declared in {} | Skipping...", configKey, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + "tools.toml]"));
            return;
        }
        ToolsConfig.Properties properties = assembleProperties(configKey, toolProperties);
        ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation(tagId))).forEach(item ->
            addItem(getItemId(item), properties)
        );
    }

    private static void handleCollection(String configKey, Config toolProperties) {
        String collectionName = configKey.substring(1);
        List<String> collection = CollectionsConfig.ITEMS_MAP.get(collectionName);
        if (collection == null) {
            addConfigIssue(WARN, (byte) 4, "Config [{}] declared item collection <{}> which does not exist, check for typos! | Skipping Collection...", PRESET_FOLDER_NAME + "tools.toml", configKey);
            return;
        }
        collection.forEach((string) -> {
            if (string.startsWith("#")) {
                handleTag(string, toolProperties, Optional.of(collectionName));
            } else {
                if (!isValidItem(string, Optional.of(collectionName), "tools.toml")) return;
                addItem(string, assembleProperties(configKey, toolProperties));
            }
        });
    }

    private static void addItem(String key, Properties properties) {
        TOOLS.merge(key, properties, Properties::merged);
    }

    private static Properties assembleProperties(String configKey, Config toolProperties) {
        Map<Byte, Integer> powers = new HashMap<>();
        toolProperties.valueMap().forEach((property, value) -> {
            if (property.equals("MiningSpeed")) return;
            if (!REGISTERED_TOOL_TYPES.contains(property)) {
                addConfigIssue(ERROR, (byte) 6, "\"{}\" used in config file [{}] for <{}> is NOT a registered tool type!", property, PRESET_FOLDER_NAME + "blocks.toml", configKey);
                return;
            }
            powers.put((byte) REGISTERED_TOOL_TYPES.indexOf(property), tryCast(value, Integer.class, configKey + "." + property, "tools.toml"));
        });
        return new Properties(powers, getOptionalInt(toolProperties, "MiningSpeed", configKey));
    }

    private static Optional<Integer> getOptionalInt(Config values, String key, String parent) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return Optional.of(number.intValue());
        } else if (value != null) {
            addConfigIssue(WARN, (byte) 4,
                "Value: \"{}\" for \"{}.{}\" is an invalid type in config [{}] | Expected: 'Integer' but got: '{}' | Ignoring property...",
                value, parent, key, PRESET_FOLDER_NAME + "tools.toml", value.getClass().getSimpleName());
        }
        return Optional.empty();
    }

    private static void handleSingleItem(String itemId, Config toolProperties) {
        if (!isValidItem(itemId, Optional.empty(), "tools.toml")) return;

        Properties singleItemProperties = assembleProperties(itemId, toolProperties);

        TOOLS.merge(itemId, singleItemProperties, (existing, singleItem) -> {
            Map<Byte, Integer> mergedMap = new HashMap<>(existing.powers());
            mergedMap.putAll(singleItem.powers());
            return new Properties(mergedMap, singleItem.miningSpeed().isPresent() ? singleItem.miningSpeed() : existing.miningSpeed());
        });
    }

    private static <T> T getOrDefault(String key, Class<T> clazz) {
        try {
            if ((CONFIG.get(key) == null)) {
                addConfigIssue(ERROR, (byte) 4, "Key \"{}\" is missing from config [{}] | Using basic Template instead...", key, PRESET_FOLDER_NAME + "tools.toml");
                return clazz.cast(TEMPLATE_CONFIG.get(key));
            }
            return clazz.cast(CONFIG.get(key));
        } catch (Exception e) {
            addConfigIssue(ERROR, (byte) 4, "Value: \"{}\" for '{}' is an invalid type in config [{}] | Expected: '{}' but got: '{}' | Using basic Template instead...", CONFIG.get(key), key, PRESET_FOLDER_NAME + "tools.toml", clazz.getSimpleName(), CONFIG.get(key).getClass().getSimpleName());
            return clazz.cast(TEMPLATE_CONFIG.get(key));
        }
    }

    public static Properties getToolProperties(Item item) {
        return TOOLS.get(getItemId(item));
    }

    // powers is a map of ToolTypeID to ToolPower
    public record Properties(Map<Byte, Integer> powers, Optional<Integer> miningSpeed) {

        public static Properties merged(Properties left, Properties right) {
            return new Properties(
                mergeMaps(left.powers(), right.powers(), Math::max),
                left.miningSpeed().isPresent() && right.miningSpeed().isPresent()
                    ? Optional.of(Math.max(left.miningSpeed().get(), right.miningSpeed().get()))
                    : left.miningSpeed().isPresent() ? left.miningSpeed()
                    : right.miningSpeed()
            );
        }

        public void writeToBuffer(FriendlyByteBuf buf) {
            buf.writeInt(powers().size());
            for (Map.Entry<Byte, Integer> entry : powers.entrySet()) {
                buf.writeByte(entry.getKey());
                buf.writeInt(entry.getValue());
            }
            buf.writeBoolean(miningSpeed.isPresent());
            miningSpeed().ifPresent(buf::writeInt);
        }

        public static Properties readFromBuffer(FriendlyByteBuf buf) {
            Map<Byte, Integer> powers = new HashMap<>();
            int bounds = buf.readInt();
            for (int i = 0; i < bounds; i++)
                powers.put(buf.readByte(), buf.readInt());
            return new Properties(powers, buf.readBoolean() ? Optional.of(buf.readInt()) : Optional.empty());
        }
    }
}