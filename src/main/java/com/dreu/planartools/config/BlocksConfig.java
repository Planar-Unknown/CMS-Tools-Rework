package com.dreu.planartools.config;

import com.electronwill.nightconfig.core.Config;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.util.Helpers.LogLevel.*;
import static com.dreu.planartools.util.Helpers.*;
import static com.dreu.planartools.config.GeneralConfig.PRESET_FOLDER_NAME;
import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_TYPES;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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
    
    ["minecraft:obsidian"] # A simple example
    DefaultResistance = -1 # Not mineable by default
    Pickaxe = {Resistance = 80, ApplyMiningSpeed = true} # Pickaxes of power 80 or above (Diamond in vanilla) can mine it
    
    # You can specify a tag (denoted by the "#"), and all blocks in that tag will receive the values declared
    ["#minecraft:dirt"]
    DefaultResistance = 0 # Zero indicates no resistance, meaning no power is required to mine it. So any tool, item (or fist) works!
    Shovel = {ApplyMiningSpeed = true} # Even though ANYTHING can mine it, only tools/items that have ShovelPower apply their MiningSpeed.
    
    ["minecraft:moss_block"] # Moss exists in the tag #minecraft:dirt, but you can override any of the values declared for the tag
    Shovel = {ApplyMiningSpeed = false} # Now moss will no longer grant mining speed to shovels but keeps any other values declared for tags
    Hoe = {ApplyMiningSpeed = true}
    
    # You can specify a Block Family (denoted by the "$"), and all blocks in that family will receive the values declared
    ["$minecraft:nether_bricks"] # A Block Family contains all variants of the base block, (e.g., stairs, slabs, walls, etc)
    Hardness = 3.0 # All we've done is made nether brick blocks a little tougher
    ExplosionResistance = 7.0
    # Note that not all mod creators implement block families for blocks with variants.
    
    # You may reference custom collections that can include blocks, block families, and tags
    ["@example/deepslate"] # This collection would be found at [config/planar_tools/collections/example/deepslate.txt]
    DefaultResistance = -1 # We've made all deepslate, its variants, and ores only mineable with stone pickaxes or better.
    Pickaxe = {Resistance = 40, ApplyMiningSpeed = true}
    # You may create your own collections in the collections folder and reference them here just like this.
    
    ["@wood"] # Here is another example collection. This time located at [config/planar_tools/collections/wood.txt]
    DefaultResistance = 0 # We've changed nothing about wood stuff. This is just a nice example of a custom collection
    Axe = {ApplyMiningSpeed = true}
    """;

  public static Config CONFIG;
  public static Map<String, Properties> BLOCKS = new HashMap<>();

  public static void parse() {
    CONFIG = parseFileOrDefault(PRESET_FOLDER_NAME + "blocks.toml", TEMPLATE_CONFIG_STRING);
  }

  public static void populateBlocks() {
    BLOCKS.clear();

    CONFIG.valueMap().forEach((configKey, blockProperties) -> {
      if (configKey.startsWith("#")) {
        handleTagBlocks(configKey, (Config) blockProperties, Optional.empty());
      } else if (configKey.startsWith("$")) {
        handleBlockFamily(configKey, (Config) blockProperties, Optional.empty());
      } else if (configKey.startsWith("@")) {
        handleCollection(configKey, (Config) blockProperties);
      }
    });

    CONFIG.valueMap().forEach((blockId, blockProperties) -> {
      if (blockId.contains("#") || blockId.contains("$") || blockId.contains("@")) return;
      handleSingleBlock(blockId, (Config) blockProperties);
    });
    BLOCKS.keySet().forEach(System.out::println);
  }

  private static void handleCollection(String configKey, Config blockProperties) {
    String collectionId = configKey.substring(1);
    List<String> collection = CollectionsConfig.MAP.get(collectionId);
    if (collection == null) {
      addConfigIssue(WARN, (byte) 4, "Config [{}] declared collection <{}> which does not exist, check for typos! | Skipping Collection...", PRESET_FOLDER_NAME + "blocks.toml", configKey);
      return;
    }
//    System.out.println(configKey + " | " + Arrays.toString(collection.stream().map(string -> string.concat("\n")).toArray()));
    collection.forEach((string) -> {
      if (string.startsWith("#")) {
        handleTagBlocks(string, blockProperties, Optional.of(collectionId));
      } else if (string.startsWith("$")) {
        handleBlockFamily(string, blockProperties, Optional.of(collectionId));
      } else {
        if (blockIsNotValid(string, Optional.of(collectionId))) return;
        addBlock(string, assembleProperties(configKey, blockProperties));
      }
    });

  }

  @SuppressWarnings({"DataFlowIssue", "OptionalUsedAsFieldOrParameterType"})
  private static void handleTagBlocks(String configKey, Config blockProperties, Optional<String> collectionName) {
    String tagId = configKey.substring(1);
    if (!ResourceLocation.isValidResourceLocation(tagId)) {
      addConfigIssue(INFO, (byte) 2, "Not a valid Tag ResourceLocation: <{}> declared in {} | Skipping...", configKey, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + "blocks.toml]"));
      return;
    }
    if (!ForgeRegistries.BLOCKS.tags().isKnownTagName(BlockTags.create(new ResourceLocation(tagId)))) {
      addConfigIssue(INFO, (byte) 2, "Not an existing Block Tag: <{}> declared in {} | Skipping...", configKey, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + "blocks.toml]"));
      return;
    }
    Properties properties = assembleProperties(configKey, blockProperties);
    ForgeRegistries.BLOCKS.tags().getTag(BlockTags.create(new ResourceLocation(tagId))).forEach(block ->
        addBlock(getKey(block), properties)
    );
  }

  private static void handleBlockFamily(String configKey, Config blockProperties, Optional<String> collectionName) {
    String baseBlockId = configKey.substring(1);
    if (!ResourceLocation.isValidResourceLocation(baseBlockId)) {
      addConfigIssue(INFO, (byte) 2, "Not a valid Block ResourceLocation: <{}> declared as a block family base block in {} | Skipping...", configKey, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + "blocks.toml]"));
      return;
    }
    ForgeRegistries.BLOCKS.getDelegate(new ResourceLocation(baseBlockId)).ifPresent(delegate -> {
      if (!BlockFamilies.MAP.containsKey(delegate.get())) {
        addConfigIssue(INFO, (byte) 2, "Not an existing Block Family: <{}> declared in {} | Skipping...", configKey, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + "blocks.toml]"));
        return;
      }

      BlockFamily family = BlockFamilies.MAP.get(delegate.get());
      Properties properties = assembleProperties(configKey, blockProperties);
      family.getVariants().values().forEach(block -> addBlock(getKey(block), properties));
      addBlock(getKey(family.getBaseBlock()), properties);
    });
  }

  private static void handleSingleBlock(String blockId, Config blockPropertiesConfig) {
    if (blockIsNotValid(blockId, Optional.empty())) return;

    BlocksConfig.Properties singleBlockProperties = assembleProperties(blockId, blockPropertiesConfig);
    BLOCKS.merge(blockId, singleBlockProperties, (existing, singleBlock) -> {
      Integer defaultResistance = getOrElse(blockPropertiesConfig, blockId, "DefaultResistance", existing.defaultResistance(), Integer.class);
      Map<Byte, BlocksConfig.ResistanceData> resistanceDataMap = getResistanceDataMapOverride(blockPropertiesConfig, defaultResistance, existing.data());

      for (Map.Entry<Byte, BlocksConfig.ResistanceData> incomingEntry : existing.data().entrySet()) {
        if (!resistanceDataMap.containsKey(incomingEntry.getKey())) {
          resistanceDataMap.put(incomingEntry.getKey(), incomingEntry.getValue());
        }
      }

      return new BlocksConfig.Properties(
          singleBlock.hardness().isPresent() ? singleBlock.hardness() : existing.hardness(),
          singleBlock.explosionResistance().isPresent() ? singleBlock.explosionResistance() : existing.explosionResistance(),
          defaultResistance,
          resistanceDataMap
      );
    });
  }

  private static boolean blockIsNotValid(String blockId, Optional<String> collectionName) {
    if (!ResourceLocation.isValidResourceLocation(blockId)) {
      addConfigIssue(INFO, (byte) 2, "Not a valid Block ResourceLocation: <{}> declared in {} | Skipping Block...", blockId, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + "blocks.toml]"));
      return true;
    }
    if (!ModList.get().isLoaded(blockId.split(":")[0])) {
      addConfigIssue(INFO, (byte) 2, "{} declared Block Resistance values for <{}> but mod '{{}}' is not loaded | Skipping...", collectionName.map(s -> "Collection: [" + s + "]").orElseGet(() ->  "Config: [" + PRESET_FOLDER_NAME + "blocks.toml]"), blockId, blockId.split(":")[0]);
      return true;
    }
    if (!ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(blockId))) {
      addConfigIssue(INFO, (byte) 2, "{} declared block <{}> which does not exist, check for typos! | Skipping Block...", collectionName.map(s -> "Collection: [" + s + "]").orElseGet(() ->  "Config: [" + PRESET_FOLDER_NAME + "blocks.toml]"), blockId);
      return true;
    }
    return false;
  }

  private static Map<Byte, BlocksConfig.ResistanceData> getResistanceDataMapOverride(Config block, int defaultResistance, Map<Byte, BlocksConfig.ResistanceData> right) {
    Map<Byte, BlocksConfig.ResistanceData> resistanceDataMap = new HashMap<>();
    for (Map.Entry<String, Object> property : block.valueMap().entrySet()) {
      switch (property.getKey()) {
        case "DefaultResistance", "ExplosionResistance", "Hardness" -> {
          continue;
        }
        default -> {
          byte toolType = (byte) REGISTERED_TOOL_TYPES.indexOf(property.getKey());
          resistanceDataMap.put(
              toolType,
              new BlocksConfig.ResistanceData(
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
    BLOCKS.merge(key, properties, Properties::merged);
  }

  private static @NotNull Properties assembleProperties(String blockId, Config config) {
    int defaultResistance = getOrElse(config, blockId, "DefaultResistance", 0, Integer.class);
    return new Properties(
        getOptionalFloat(config, "Hardness", blockId),
        getOptionalFloat(config, "ExplosionResistance", blockId),
        defaultResistance,
        getResistanceDataMap(config, defaultResistance)
    );
  }

  private static Map<Byte, ResistanceData> getResistanceDataMap(Config config, int defaultResistance) {
    Map<Byte, ResistanceData> map = new HashMap<>();
    for (Map.Entry<String, Object> entry : config.valueMap().entrySet()) {
      String key = entry.getKey();
      if (isStandardKey(key)) continue;

      if (!REGISTERED_TOOL_TYPES.contains(key)) {
        addConfigIssue(ERROR, (byte) 6, "\"{}\" in config file [{}] is NOT a registered tool type!", key, PRESET_FOLDER_NAME + "blocks.toml");
        continue;
      }

      byte type = (byte) REGISTERED_TOOL_TYPES.indexOf(key);
      Config toolConfig = (Config) entry.getValue();
      map.put(type, new ResistanceData(
          getOrElse(toolConfig, key, "Resistance", defaultResistance, Integer.class),
          getOrElse(toolConfig, key, "ApplyMiningSpeed", false, Boolean.class)
      ));
    }
    return map;
  }

  private static boolean isStandardKey(String key) {
    return key.equals("DefaultResistance") || key.equals("ExplosionResistance") || key.equals("Hardness");
  }

  private static <T> T getOrElse(Config config, String parentKey, String key, T fallback, Class<T> clazz) {
    try {
      Object value = config.get(key);
      if (value == null) return fallback;
      return clazz.cast(value);
    } catch (ClassCastException e) {
      addConfigIssue(WARN, (byte) 4,
          "Value: \"{}\" for \"{}.{}\" is an invalid type in config [{}] | Expected: '{}' but got: '{}' | Ignoring property...",
          config.get(key), parentKey, key, PRESET_FOLDER_NAME + "blocks.toml",
          clazz.getSimpleName(), config.get(key).getClass().getSimpleName());
      return fallback;
    }
  }

  private static Optional<Float> getOptionalFloat(Config values, String key, String parent) {
    Object value = values.get(key);
    if (value instanceof Number number) {
      return Optional.of(number.floatValue());
    } else if (value != null) {
      addConfigIssue(WARN, (byte) 4,
          "Value: \"{}\" for \"{}.{}\" is an invalid type in config [{}] | Expected: 'Float' but got: '{}' | Ignoring property...",
          value, parent, key, PRESET_FOLDER_NAME + "blocks.toml", value.getClass().getSimpleName());
    }
    return Optional.empty();
  }


  @SuppressWarnings("DataFlowIssue")
  private static String getKey(Block block) {
    return ForgeRegistries.BLOCKS.getKey(block).toString();
  }

  public static Properties getBlockProperties(Block block) {
    return BLOCKS.get(getKey(block));
  }

  public record ResistanceData(int resistance, boolean applyMiningSpeed) {}

  public record Properties(Optional<Float> hardness, Optional<Float> explosionResistance, int defaultResistance, Map<Byte, ResistanceData> data) {
    public static Properties merged(Properties left, Properties right) {
      return new Properties(
          mergeOptionalNumbers(left.hardness, right.hardness),
          mergeOptionalNumbers(left.explosionResistance, right.explosionResistance),
          left.defaultResistance() == -1 || right.defaultResistance() == -1
              ? Math.max(left.defaultResistance, right.defaultResistance)
              : Math.min(left.defaultResistance(), right.defaultResistance()),
          mergeMaps(left, right)
      );
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

    private static Map<Byte, ResistanceData> mergeMaps(Properties left, Properties right) {
      Map<Byte, ResistanceData> mergedMap = new HashMap<>(right.data());
      left.data().forEach((key, leftData) ->
          mergedMap.merge(key, leftData, (rightData, newLeftData) -> new ResistanceData(
          Math.min(newLeftData.resistance(), rightData.resistance()),
          newLeftData.applyMiningSpeed() || rightData.applyMiningSpeed()
      )));
      return mergedMap;
    }


    public void write(FriendlyByteBuf buf) {
      buf.writeBoolean(hardness.isPresent());
      hardness.ifPresent(buf::writeFloat);

      buf.writeBoolean(explosionResistance.isPresent());
      explosionResistance.ifPresent(buf::writeFloat);

      buf.writeInt(defaultResistance);
      buf.writeInt(data.size());

      data.forEach((key, value) -> {
        buf.writeByte(key);
        buf.writeInt(value.resistance());
        buf.writeBoolean(value.applyMiningSpeed());
      });
    }

    public static Properties read(FriendlyByteBuf buf) {
      Optional<Float> hardness = buf.readBoolean() ? Optional.of(buf.readFloat()) : Optional.empty();
      Optional<Float> explosionResistance = buf.readBoolean() ? Optional.of(buf.readFloat()) : Optional.empty();
      int defaultResistance = buf.readInt();

      int size = buf.readInt();
      Map<Byte, ResistanceData> map = new HashMap<>();
      for (int i = 0; i < size; i++) {
        byte key = buf.readByte();
        map.put(key, new ResistanceData(buf.readInt(), buf.readBoolean()));
      }
      return new Properties(hardness, explosionResistance, defaultResistance, map);
    }
  }
}
