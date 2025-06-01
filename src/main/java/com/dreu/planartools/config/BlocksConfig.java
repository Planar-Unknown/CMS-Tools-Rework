package com.dreu.planartools.config;

import com.electronwill.nightconfig.core.Config;
import net.minecraft.core.Holder;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.util.Helpers.LogLevel.*;
import static com.dreu.planartools.util.Helpers.*;
import static com.dreu.planartools.config.GeneralConfig.PRESET_FOLDER_NAME;
import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_TYPES;

public class BlocksConfig {
    public static final String templateFileName = "config/" + MODID + "/presets/template/blocks.toml";
    public static final String TEMPLATE_CONFIG_STRING = """
    # Modded Items that override the getDestroySpeed method will not be valid.
    # To request compatibility with a specific mod, let us know in our Discord | https://discord.gg/RrY3rXuAH5
    # Specifically declared blocks will override any values it inherited from specified tags
    
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
    
    # You can specify a tag (denoted by the "#"), and all blocks in that tag will receive the values declared
    ["#minecraft:dirt"]
    DefaultResistance = 0 # Zero indicates no resistance, meaning no power is required to mine it. So any tool, item (or fist) works!
    Shovel = {ApplyMiningSpeed = true} # Even though ANYTHING can mine it, only tools/items that have ShovelPower apply their MiningSpeed.
    
    ["minecraft:moss"] # Moss exists in the tag #minecraft:dirt, but you can override any of the values declared for the tag
    Shovel = {ApplyMiningSpeed = false} # Now moss will no longer grant mining speed to shovels but keeps any other values declared for tags
    Hoe = {ApplyMiningSpeed = true}
    
    ["minecraft:deepslate"]
    DefaultResistance = -1
    Pickaxe = {Resistance = 40, ApplyMiningSpeed = true}
    
    # You can specify a Block Family (denoted by the "$"), and all blocks in that family will receive the values declared
    ["$minecraft:cobbled_deepslate"] # A Block Family contains all variants of the base block, (e.g., stairs, slabs, walls, etc)
    DefaultResistance = -1
    Pickaxe = {Resistance = 40, ApplyMiningSpeed = true}
    
    ["minecraft:obsidian"]
    DefaultResistance = -1
    Pickaxe = {Resistance = 80, ApplyMiningSpeed = true}
    
    ["#minecraft:logs"] # Same as vanilla
    DefaultResistance = 0
    Axe = {ApplyMiningSpeed = true}
    """;

    public static Config CONFIG;

    public static void parse() {
        CONFIG = parseFileOrDefault(PRESET_FOLDER_NAME + "blocks.toml", TEMPLATE_CONFIG_STRING);
    }

    public static Map<String, Properties> BLOCKS = new HashMap<>();

    @SuppressWarnings("DataFlowIssue")
    public static void populateBlocks() {
        BLOCKS.clear();
        CONFIG.valueMap().forEach((blockId, blockProperties) -> {
            if (blockId.charAt(0) == '#') {
                if (!ResourceLocation.isValidResourceLocation(blockId.substring(1))) {
                    addConfigIssue(INFO, (byte) 2, "Not a valid Tag ResourceLocation: <{}> declared in config: [{}] | Skipping...", blockId, PRESET_FOLDER_NAME + "blocks.toml");
                    return;
                }
                TagKey<Block> tagKey = BlockTags.create(new ResourceLocation(blockId.substring(1)));
                if (ForgeRegistries.BLOCKS.tags().isKnownTagName(tagKey)) {
                    Properties properties = assembleProperties(blockId, (Config) blockProperties);
                    ForgeRegistries.BLOCKS.tags().getTag(tagKey).stream().forEach(tagBlock ->
                        addBlock(ForgeRegistries.BLOCKS.getKey(tagBlock).toString(), properties));
                }
            }
            if (blockId.charAt(0) == '$') {
                if (!ResourceLocation.isValidResourceLocation(blockId.substring(1))) {
                    addConfigIssue(INFO, (byte) 2, "Not a valid Block ResourceLocation: <{}> declared in config: [{}] | Skipping...", blockId, PRESET_FOLDER_NAME + "blocks.toml");
                    return;
                }
                Optional<Holder.Reference<Block>> blockDelegate = ForgeRegistries.BLOCKS.getDelegate(new ResourceLocation(blockId.substring(1)));
                if (blockDelegate.isPresent()) {
                    if (BlockFamilies.MAP.containsKey(blockDelegate.get().get())) {
                        BlockFamily family = BlockFamilies.MAP.get(blockDelegate.get().get());
                        Properties properties = assembleProperties(blockId, (Config) blockProperties);
                        family.getVariants().values().forEach(familyBlock ->
                            addBlock(ForgeRegistries.BLOCKS.getKey(familyBlock).toString(), properties));
                        addBlock(ForgeRegistries.BLOCKS.getKey(family.getBaseBlock()).toString(), properties);
                    }
                }
            }
        });

        CONFIG.valueMap().forEach((blockId, blockProperties) -> {
            Config blockPropertiesConfig = (Config) blockProperties;
            if (blockId.contains("#") || blockId.contains("$")) return;
            if (!ResourceLocation.isValidResourceLocation(blockId)) {
                addConfigIssue(INFO, (byte) 2, "Not a valid Block ResourceLocation: <{}> declared in config: [{}] | Skipping...", blockId, PRESET_FOLDER_NAME + "blocks.toml");
                return;
            }
            if (!ModList.get().isLoaded(blockId.substring(0, blockId.indexOf(":")))) {
                addConfigIssue(INFO, (byte) 2, "Config [{}] declared Block Resistance values for <{}> when {{}} was not loaded or does not exist in this modpack | Skipping Block...", PRESET_FOLDER_NAME + "blocks.toml", blockId, blockId.substring(0, blockId.indexOf(":")));
                return;
            }
            if (BLOCKS.containsKey(blockId)) {
                Properties right = BLOCKS.get(blockId);
                Integer defaultResistance = getOrElse(blockPropertiesConfig, blockId, "DefaultResistance", right.defaultResistance(), Integer.class);
                Map<Byte, ResistanceData> resistanceDataMap = getResistanceDataMapOverride(blockPropertiesConfig, defaultResistance, right.data());
                for (Map.Entry<Byte, ResistanceData> rightEntry : right.data().entrySet()) {
                    if (!resistanceDataMap.containsKey(rightEntry.getKey())) {
                        resistanceDataMap.put(rightEntry.getKey(), rightEntry.getValue());
                    }
                }
                Optional<Float> leftHardness = getOptionalFloat(blockPropertiesConfig, "Hardness", blockId);
                Optional<Float> leftExplosionResistance = getOptionalFloat(blockPropertiesConfig, "ExplosionResistance", blockId);

                BLOCKS.put(blockId, new Properties(
                    leftHardness.isPresent() ? leftHardness : right.hardness(),
                    leftExplosionResistance.isPresent() ? leftExplosionResistance : right.explosionResistance(),
                    defaultResistance,
                    resistanceDataMap
                ));
            } else {
                addBlock(blockId, assembleProperties(blockId, blockPropertiesConfig));
            }
        });
    }

    private static Map<Byte, ResistanceData> getResistanceDataMapOverride(Config block, int defaultResistance, Map<Byte, ResistanceData> right) {
        Map<Byte, ResistanceData> resistanceDataMap = new HashMap<>();
        for (Map.Entry<String, Object> property : block.valueMap().entrySet()) {
            switch (property.getKey()) {
                case "DefaultResistance", "ExplosionResistance", "Hardness" -> {continue;}
                default -> {
                    byte toolType = (byte) REGISTERED_TOOL_TYPES.indexOf(property.getKey());
                    resistanceDataMap.put(
                        toolType,
                        new ResistanceData(
                            getOrElse(((Config) property.getValue()), property.getKey(), "Resistance", right.containsKey(toolType) ? right.get(toolType).resistance() : defaultResistance, Integer.class),
                            getOrElse(((Config) property.getValue()), property.getKey(), "ApplyMiningSpeed", right.containsKey(toolType) && right.get(toolType).applyMiningSpeed(), Boolean.class)
                        )
                    );
                }
            }
            if (!REGISTERED_TOOL_TYPES.contains(property.getKey())) {
                addConfigIssue(ERROR, (byte) 6, "\"{}\" in config file [{}] is NOT a registered tool type!", property.getKey(), PRESET_FOLDER_NAME + "blocks.toml");
            }
        }
        return resistanceDataMap;
    }


    private static void addBlock(String key, Properties properties) {
        if (BLOCKS.containsKey(key)) {
            BLOCKS.put(key, Properties.merged(properties, BLOCKS.get(key)));
        } else {
            BLOCKS.put(key, properties);
        }

    }

    private static @NotNull Properties assembleProperties(String blockId, Config block) {
        Integer defaultResistance = getOrElse(block, blockId, "DefaultResistance", 0, Integer.class);
        Map<Byte, ResistanceData> resistanceDataMap = getResistanceDataMap(block, defaultResistance);
        return new Properties(
            getOptionalFloat(block, "Hardness", blockId),
            getOptionalFloat(block, "ExplosionResistance", blockId),
            defaultResistance,
            resistanceDataMap
        );
    }

    private static @NotNull Map<Byte, ResistanceData> getResistanceDataMap(Config block, Integer defaultResistance) {
        Map<Byte, ResistanceData> resistanceDataMap = new HashMap<>();
        for (Map.Entry<String, Object> property : block.valueMap().entrySet()) {
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
                addConfigIssue(ERROR, (byte) 6, "\"{}\" in config file [{}] is NOT a registered tool type!", property.getKey(), PRESET_FOLDER_NAME + "blocks.toml");
            }
        }
        return resistanceDataMap;
    }

    private static <T> T getOrElse(Config config, String parentKey, String key, T fallback, Class<T> clazz) {
        try {
            clazz.cast(config.get(key));
        } catch (Exception e) {
            addConfigIssue(WARN, (byte) 4, "Value: \"{}\" for \"{}.{}\" is an invalid type in config [{}] | Expected: '{}' but got: '{}' | Ignoring property...", config.get(key), parentKey, key, PRESET_FOLDER_NAME + "blocks.toml", clazz.getSimpleName(), config.get(key).getClass().getSimpleName());
            return fallback;
        }
        T toReturn = config.get(key);
        return toReturn == null ? fallback : toReturn;
    }

    private static Optional<Float> getOptionalFloat(Config values, String key, String parent) {
        try {
            //noinspection RedundantClassCall
            Number.class.cast(values.get(key));
        } catch (Exception e) {
            addConfigIssue(WARN, (byte) 4, "Value: \"{}\" for \"{}.{}\" is an invalid type in config [{}] | Expected: 'Float' but got: '{}' | Ignoring property...", values.get(key), parent, key, PRESET_FOLDER_NAME + "blocks.toml", values.get(key).getClass().getSimpleName());
            return Optional.empty();
        }
        Number explosionResistance = values.get(key);
        return explosionResistance == null ? Optional.empty() : Optional.of(explosionResistance.floatValue());
    }

    @SuppressWarnings("DataFlowIssue")
    public static Properties getBlockProperties(Block block) {
        return BLOCKS.get(ForgeRegistries.BLOCKS.getKey(block).toString());
    }

    public record Properties(Optional<Float> hardness, Optional<Float> explosionResistance, int defaultResistance, Map<Byte, ResistanceData> data) {

        public static Properties merged(Properties left, Properties right) {
            Optional<Float> hardness = mergeOptionalNumbers(left.hardness(), right.hardness());
            Optional<Float> explosionResistance = mergeOptionalNumbers(left.explosionResistance(), right.explosionResistance());
            int defaultResistance = Math.max(left.defaultResistance(), right.defaultResistance());
            return new Properties(hardness, explosionResistance, defaultResistance, mergeResistanceDataMaps(left, right));
        }

        private static Map<Byte, ResistanceData> mergeResistanceDataMaps(Properties left, Properties right) {
            Map<Byte, ResistanceData> newMap = right.data();
            for (Map.Entry<Byte, ResistanceData> leftEntry : left.data().entrySet()) {
                if (newMap.containsKey(leftEntry.getKey())) {
                    ResistanceData rightResData = right.data().get(leftEntry.getKey());
                    ResistanceData newResData = new ResistanceData(
                        Math.min(leftEntry.getValue().resistance(), rightResData.resistance()),
                        leftEntry.getValue().applyMiningSpeed() || rightResData.applyMiningSpeed()
                    );
                    newMap.put(leftEntry.getKey(), newResData);
                } else {
                    newMap.put(leftEntry.getKey(), leftEntry.getValue());
                }
            }
            return newMap;
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private static <T extends Number> Optional<T> mergeOptionalNumbers(Optional<T> left, Optional<T> right) {
            return left.isPresent()
                ? right.isPresent()
                    ? left.get().doubleValue() >= right.get().doubleValue()
                        ? left
                        : right
                    : left
                : Optional.empty();
        }


        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(this.hardness().isPresent());
            this.hardness().ifPresent(buf::writeFloat);

            buf.writeBoolean(this.explosionResistance().isPresent());
            this.explosionResistance().ifPresent(buf::writeFloat);

            buf.writeInt(this.defaultResistance());

            Map<Byte, ResistanceData> map = this.data();
            buf.writeInt(map.size());
            for (Map.Entry<Byte, ResistanceData> entry : map.entrySet()) {
                buf.writeByte(entry.getKey());
                buf.writeInt(entry.getValue().resistance());
                buf.writeBoolean(entry.getValue().applyMiningSpeed());
            }
        }

        public static Properties read(FriendlyByteBuf buf) {
            Optional<Float> hardness = buf.readBoolean() ? Optional.of(buf.readFloat()) : Optional.empty();
            Optional<Float> explosionResistance = buf.readBoolean() ? Optional.of(buf.readFloat()) : Optional.empty();
            int defaultResistance = buf.readInt();

            int mapSize = buf.readInt();
            Map<Byte, ResistanceData> map = new HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                byte key = buf.readByte();
                ResistanceData value = new ResistanceData(buf.readInt(), buf.readBoolean());
                map.put(key, value);
            }
            return new Properties(hardness, explosionResistance, defaultResistance, map);
        }
    }
    public record ResistanceData(int resistance, boolean applyMiningSpeed) {}
}
