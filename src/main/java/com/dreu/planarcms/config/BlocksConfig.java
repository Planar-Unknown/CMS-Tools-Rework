package com.dreu.planarcms.config;

import com.dreu.planarcms.util.Helpers;
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

import static com.dreu.planarcms.PlanarCMS.MODID;
import static com.dreu.planarcms.config.GeneralConfig.PRESET_FOLDER_NAME;
import static com.dreu.planarcms.config.ToolsConfig.REGISTERED_TOOL_TYPES;
import static com.dreu.planarcms.util.Helpers.LogLevel.*;
import static com.dreu.planarcms.util.Helpers.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class BlocksConfig {
  //Todo: add comment to template for Forced tool types
  public static final String TEMPLATE_FILE_NAME = "config/" + MODID + "/presets/template/blocks.toml";
  public static String getTemplateConfigString() {
    return """
       # See Template for more information
       
       ["minecraft:packed_mud"]
         Hardness = 1.0
         DefaultResistance = -1
         Shovel = {Resistance = 40, ApplyMiningSpeed = false}
         Pickaxe = {Resistance = 20, ApplyMiningSpeed = true}
       
       ["minecraft:amethyst_block"]
         DefaultResistance = -1
         Arcane = {Resistance = 30, ApplyMiningSpeed = true}
       
       ["#minecraft:dirt"]
         DefaultResistance = 0
         Shovel = {ApplyMiningSpeed = true}
       
       ["minecraft:moss_block"]
         Shovel = {ApplyMiningSpeed = false}
         Hoe = {ApplyMiningSpeed = true}
       
       ["$minecraft:nether_bricks"]
         Hardness = 3.0
         ExplosionResistance = 7.0
       
       ["@example/deepslate"]
         DefaultResistance = -1
         Pickaxe = {Resistance = 40, ApplyMiningSpeed = true}
       
       ["@wood"]
         DefaultResistance = 0
         Axe = {ApplyMiningSpeed = true}
       
       ["#minecraft:wool"]
         Shears = {Resistance = 100, ApplyMiningSpeed = true}
       
       ["minecraft:cobweb"]
         Shears = {Resistance = 20, ApplyMiningSpeed = true}
       """;
  }
  public static String getCommentedTemplateConfig() {
    return """
       # DO NOT EDIT THIS TEMPLATE! IT WILL BE RESET!
       # Collections in this file (denoted by "@") are custom groups of Blocks
       # Create your own collections at: [config/planar_cms/collections/blocks]
       # For example, the "@example/deepslate" collection can be found at [config/planar_cms/collections/blocks/example/deepslate.txt]
       # Block Collections may contain Tags of blocks or individual blocks, but may not contain other collections
       # Specifically declared blocks will override any values it inherited from specified tags or collections
       
       # DefaultResistance = -1 will make a block not destroyed by explosions
       # Unless ExplosionResistance is also specified. ExplosionResistance = -1 makes it indestructible by explosions
       
       # This table shows the default power level of each tier of tool.
       ########################################################################
       # Tier  -> | Wooden |  Stone  |  Iron  |  Gold  | Diamond  | Netherite #
       #----------|--------|---------|--------|--------|----------|-----------#
       # Power -> |   20   |    40   |   60   |   40   |    80    |    100    #
       ########################################################################
       
       ["minecraft:packed_mud"]                                  # Here is an example of a block that can only be mined with a Power 40+ Shovel OR a Power 20+ Pickaxe:
         Hardness = 1.0                                          # Affects mining time (e.g., dirt = 0.5, stone = 1.5, bedrock = -1). Only include this if you want to change a block's existing hardness.
         DefaultResistance = -1                                  # Resistance to unlisted tool types (-1 = unbreakable)
         Shovel = {Resistance = 40, ApplyMiningSpeed = false}    # Tools with ShovelPower ≥ 40 can mine this block but their MiningSpeed is NOT applied.
         Pickaxe = {Resistance = 20, ApplyMiningSpeed = true}    # Tools with PickaxePower ≥ 20 can mine this block and their MiningSpeed IS applied.
       
       ["minecraft:amethyst_block"]                              # A simple example of making Amethyst only mineable with Golden Tools
         DefaultResistance = -1                                  # Not mineable by default
         Arcane = {Resistance = 30, ApplyMiningSpeed = true}     # Tools with Arcane power 30 or above can mine it
                                                                 # Note that in the tools.toml, we have given all @golden_tools Arcane power of 30
       
                                                                 # You can specify a tag (denoted by the "#"), and all blocks in that tag will receive the values declared
       ["#minecraft:dirt"]
         DefaultResistance = 0                                   # Zero indicates no resistance, meaning no power is required to mine it. So any tool, item (or fist) works!
         Shovel = {ApplyMiningSpeed = true}                      # Even though ANYTHING can mine it, only tools/items that have ShovelPower apply their MiningSpeed.
       
       ["minecraft:moss_block"]                                  # Moss exists in the tag #minecraft:dirt, but you can override any of the values declared for the tag
         Shovel = {ApplyMiningSpeed = false}                     # Now moss will no longer grant mining speed to shovels but keeps any other values declared for tags
         Hoe = {ApplyMiningSpeed = true}
       
                                                                 # You can specify a Block Family (denoted by the "$"), and all blocks in that family will receive the values declared
       ["$minecraft:nether_bricks"]                              # A Block Family contains all variants of the base block, (e.g., stairs, slabs, walls)
         Hardness = 3.0                                          # All we've done is made nether brick blocks a little tougher
         ExplosionResistance = 7.0                               # Note that not all mod creators implement block families for blocks with variants.
       
                                                                 # You may reference custom collections that can include blocks, block families, and tags
       ["@example/deepslate"]                                    # This collection would be found at [config/planar_cms/collections/blocks/example/deepslate.txt]
         DefaultResistance = -1                                  # We've made all deepslate, its variants, and ores only mineable with stone pickaxes or better.
         Pickaxe = {Resistance = 40, ApplyMiningSpeed = true}
       
       ["@wood"]                                                 # You may create your own collections in the collections folder and reference them here just like this.
         DefaultResistance = 0                                   # Here is another example collection. This time located at [config/planar_cms/collections/wood.txt]
         Axe = {ApplyMiningSpeed = true}                         # We've changed nothing about wood stuff. This is just a nice example of a custom collection
       
       ["#minecraft:wool"]                                       # Swords in tools.toml example were declared with 20 Shears power, so they will not mine Wool quickly
         Shears = {Resistance = 100, ApplyMiningSpeed = true}
       
       ["minecraft:cobweb"]                                      # Swords will however mine Cobwebs quickly
         Shears = {Resistance = 20, ApplyMiningSpeed = true}
       """;
  }

  public static Config CONFIG;
  public static Map<String, Properties> BLOCKS = new HashMap<>();

  public static final Map<String, ArrayList<String>> FILTERED_REGISTRY = new HashMap<>();
  private static boolean registryHasBeenFilteredByModId;

  public static void parse() {
    CONFIG = parseFileOrDefault(PRESET_FOLDER_NAME + "blocks.toml", getTemplateConfigString());
  }

  public static void populateBlocks() {
    BLOCKS.clear();
    Map<String, Config> singleBlocks = new HashMap<>();

    for (Map.Entry<String, Object> entry : CONFIG.valueMap().entrySet()) {
      String configKey = entry.getKey();
      Config propertiesConfig = (Config) entry.getValue();
      Properties properties = assembleProperties(configKey, propertiesConfig);
      if (configKey.startsWith("#")) {
        handleTag(configKey, properties);
      } else if (configKey.startsWith("$")) {
        handleBlockFamily(configKey, properties);
      } else if (configKey.startsWith("@")) {
        handleCollection(configKey, properties);
      } else if (configKey.endsWith(":*")) {
        handleMod(configKey, properties);
      } else if (isValidBlock(configKey, Optional.empty())) {
        singleBlocks.put(configKey, propertiesConfig);
      }
    }

    singleBlocks.forEach(BlocksConfig::handleSingleBlock);

    FILTERED_REGISTRY.clear();
    registryHasBeenFilteredByModId = false;
  }

  private static void handleMod(String configKey, Properties properties) {
    String modId = configKey.substring(0, configKey.length() - 2);
    if (isValidMod(modId, Optional.empty(), "blocks.toml")) {
      if (!registryHasBeenFilteredByModId) {
        for (ResourceLocation block : ForgeRegistries.BLOCKS.getKeys())
          FILTERED_REGISTRY.computeIfAbsent(block.getNamespace(), b -> new ArrayList<>()).add(block.toString());
        registryHasBeenFilteredByModId = true;
      }
      for (String blockId : FILTERED_REGISTRY.get(modId))
        addBlock(blockId, properties);
    }
  }

  private static void handleCollection(String configKey, Properties properties) {
    String collectionName = configKey.substring(1);
    List<String> collection = CollectionsConfig.BLOCKS_MAP.get(collectionName);
    if (collection == null) {
      addConfigIssue(WARN, (byte) 4, "Config [{}] declared block collection <{}> which does not exist, check for typos! | Skipping Collection...", PRESET_FOLDER_NAME + "blocks.toml", configKey);
      return;
    }

    List<String> positives = new ArrayList<>();
    Set<String> negatives = new HashSet<>();
    List<String> explicit_positives = new ArrayList<>();

    for (String member : collection) {
      if (member.startsWith("-")) {
        String sub = member.substring(1);
        if (sub.startsWith("#")) {
          addBlocksFromTag(sub, Optional.of(collectionName), negatives);
        } else if (sub.startsWith("$")) {
          addBlocksFromFamily(sub, Optional.of(collectionName), negatives);
        } else if (sub.endsWith(":*")) {
          addBlocksFromMod(member, negatives);
        } else if (isValidBlock(sub, Optional.of(collectionName))) {
          negatives.add(sub);
        }
      } else if (member.startsWith("#")) {
        addBlocksFromTag(member, Optional.of(collectionName), positives);
      } else if (member.startsWith("$")) {
        addBlocksFromFamily(member, Optional.of(collectionName), positives);
      } else if (member.endsWith(":*")) {
        addBlocksFromMod(member, positives);
      } else if (isValidBlock(member, Optional.of(collectionName))) {
        explicit_positives.add(configKey);
      }
    }

    for (String block : positives) {
      if (!negatives.contains(block))
        addBlock(block, properties);
    }
    for (String block : explicit_positives) {
      addBlock(block, properties);
    }
  }

  private static void addBlocksFromMod(String string, Collection<String> list) {
    String modId = string.substring(0, string.length() - 2);
    if (isValidMod(modId, Optional.empty(), "blocks.toml")) {
      if (!registryHasBeenFilteredByModId) {
        for (ResourceLocation block : ForgeRegistries.BLOCKS.getKeys())
          FILTERED_REGISTRY.computeIfAbsent(block.getNamespace(), b -> new ArrayList<>()).add(block.toString());
        registryHasBeenFilteredByModId = true;
      }
      list.addAll(FILTERED_REGISTRY.get(modId));
    }
  }

  private static void addBlocksFromFamily(String configKey, Optional<String> collectionName, Collection<String> list) {
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
      for (Block block : family.getVariants().values())
        list.add(getBlockId(block));
      list.add(getBlockId(family.getBaseBlock()));
    });
  }

  @SuppressWarnings("DataFlowIssue")
  private static void addBlocksFromTag(String configKey, Optional<String> collectionName, Collection<String> list) {
    if (!isValidTag(configKey, collectionName)) return;
    for (Block block : ForgeRegistries.BLOCKS.tags().getTag(BlockTags.create(new ResourceLocation(configKey.substring(1))))) {
      list.add(getBlockId(block));
    }
  }

  @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "DataFlowIssue"})
  private static boolean isValidTag(String configKey, Optional<String> collectionName) {
    String tagId = configKey.substring(1);
    if (!ResourceLocation.isValidResourceLocation(tagId)) {
      addConfigIssue(INFO, (byte) 2, "Not a valid Tag ResourceLocation: <{}> declared in {} | Skipping...", configKey, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + "blocks.toml]"));
      return false;
    }
    if (!ForgeRegistries.BLOCKS.tags().isKnownTagName(BlockTags.create(new ResourceLocation(tagId)))) {
      addConfigIssue(INFO, (byte) 2, "Not an existing Block Tag: <{}> declared in {} | Skipping...", configKey, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + "blocks.toml]"));
      return false;
    }
    return true;
  }

  @SuppressWarnings("DataFlowIssue")
  private static void handleTag(String configKey, Properties properties) {
    if (!isValidTag(configKey, Optional.empty())) return;
    for (Block block : ForgeRegistries.BLOCKS.tags().getTag(BlockTags.create(new ResourceLocation(configKey.substring(1)))))
      addBlock(getBlockId(block), properties);
  }

  private static void handleBlockFamily(String configKey, Properties properties) {
    String baseBlockId = configKey.substring(1);
    if (!ResourceLocation.isValidResourceLocation(baseBlockId)) {
      addConfigIssue(INFO, (byte) 2, "Not a valid Block ResourceLocation: <{}> declared as a block family base block in {} | Skipping...", configKey,"config: [" + PRESET_FOLDER_NAME + "blocks.toml]");
      return;
    }
    ForgeRegistries.BLOCKS.getDelegate(new ResourceLocation(baseBlockId)).ifPresent(delegate -> {
      if (!BlockFamilies.MAP.containsKey(delegate.get())) {
        addConfigIssue(INFO, (byte) 2, "Not an existing Block Family: <{}> declared in {} | Skipping...", configKey,"config: [" + PRESET_FOLDER_NAME + "blocks.toml]");
        return;
      }

      BlockFamily family = BlockFamilies.MAP.get(delegate.get());
      for (Block block : family.getVariants().values()) {
        addBlock(getBlockId(block), properties);
      }
      addBlock(getBlockId(family.getBaseBlock()), properties);
    });
  }

  private static void handleSingleBlock(String blockId, Config blockPropertiesConfig) {
    if (!isValidBlock(blockId, Optional.empty())) return;

    Properties singleBlockProperties = assembleProperties(blockId, blockPropertiesConfig);
    BLOCKS.merge(blockId, singleBlockProperties, (existing, singleBlock) -> {
      Integer defaultResistance = getOrElse(blockPropertiesConfig, blockId, "DefaultResistance", existing.defaultResistance(), Integer.class, "blocks.toml");
      Map<Byte, ResistanceData> resistanceDataMap = getResistanceDataMapOverride(blockPropertiesConfig, defaultResistance, existing.data(), blockId);

      existing.data().forEach(resistanceDataMap::putIfAbsent);

      return new Properties(
          singleBlock.hardness().isPresent() ? singleBlock.hardness() : existing.hardness(),
          singleBlock.explosionResistance().isPresent() ? singleBlock.explosionResistance() : existing.explosionResistance(),
          defaultResistance,
          resistanceDataMap
      );
    });
  }

  private static boolean isValidBlock(String blockId, Optional<String> collectionName) {
    if (!ResourceLocation.isValidResourceLocation(blockId)) {
      addConfigIssue(INFO, (byte) 2, "Not a valid Block ResourceLocation: <{}> declared in {} | Skipping Block...", blockId, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + "blocks.toml]"));
      return false;
    }
    if (!ModList.get().isLoaded(blockId.split(":")[0])) {
      addConfigIssue(INFO, (byte) 2, "{} declared Block Resistance values for <{}> but mod '{{}}' is not loaded | Skipping...", collectionName.map(s -> "Collection: [" + s + "]").orElseGet(() ->  "Config: [" + PRESET_FOLDER_NAME + "blocks.toml]"), blockId, blockId.split(":")[0]);
      return false;
    }
    if (!ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(blockId))) {
      addConfigIssue(INFO, (byte) 2, "{} declared block <{}> which does not exist, check for typos! | Skipping Block...", collectionName.map(s -> "Collection: [" + s + "]").orElseGet(() ->  "Config: [" + PRESET_FOLDER_NAME + "blocks.toml]"), blockId);
      return false;
    }
    return true;
  }

  private static Map<Byte, ResistanceData> getResistanceDataMapOverride(Config block, int defaultResistance, Map<Byte, ResistanceData> right, String parent) {
    Map<Byte, ResistanceData> resistanceDataMap = new HashMap<>();
    for (Map.Entry<String, Object> property : block.valueMap().entrySet()) {
      switch (property.getKey()) {
        case "DefaultResistance", "ExplosionResistance", "Hardness" -> {
          continue;
        }
        default -> {
          byte toolType = (byte) REGISTERED_TOOL_TYPES.indexOf(property.getKey());
          resistanceDataMap.put(
              toolType,
              new ResistanceData(
                  getOrElse(((Config) property.getValue()), property.getKey(), "Resistance", right.containsKey(toolType) ? right.get(toolType).resistance() : defaultResistance, Integer.class, "blocks.toml"),
                  getOrElse(((Config) property.getValue()), property.getKey(), "ApplyMiningSpeed", right.containsKey(toolType) && right.get(toolType).applyMiningSpeed(), Boolean.class, "blocks.toml")
              )
          );
        }
      }
      if (!REGISTERED_TOOL_TYPES.contains(property.getKey())) {
        addConfigIssue(ERROR, (byte) 6, "\"{}\" used in config file [{}] for <{}> is NOT a registered tool type!", property.getKey(), PRESET_FOLDER_NAME + "blocks.toml", parent);
      }
    }
    return resistanceDataMap;
  }

  private static void addBlock(String blockId, Properties properties) {
    BLOCKS.merge(blockId, properties, Properties::merged);
  }

  private static @NotNull Properties assembleProperties(String configKey, Config config) {
    int defaultResistance = getOrElse(config, configKey, "DefaultResistance", 0, Integer.class, "blocks.toml");
    return new Properties(
        getOptionalFloat(config, "Hardness", configKey),
        getOptionalFloat(config, "ExplosionResistance", configKey),
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
          getOrElse(toolConfig, key, "Resistance", defaultResistance, Integer.class, "blocks.toml"),
          getOrElse(toolConfig, key, "ApplyMiningSpeed", false, Boolean.class, "blocks.toml")
      ));
    }
    return map;
  }

  private static boolean isStandardKey(String key) {
    return key.equals("DefaultResistance") || key.equals("ExplosionResistance") || key.equals("Hardness");
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

  public static Properties getBlockProperties(Block block) {
    return BLOCKS.get(getBlockId(block));
  }
  //Todo: Make Resistance an optional

  public record ResistanceData(int resistance, boolean applyMiningSpeed) {}
  // powers is a map of ToolTypeID to ResistanceData(resistance, applyMiningSpeed)
  public record Properties(Optional<Float> hardness, Optional<Float> explosionResistance, int defaultResistance, Map<Byte, ResistanceData> data) {

    public static Properties merged(Properties left, Properties right) {
      return new Properties(
          mergeOptionalNumbers(left.hardness, right.hardness),
          mergeOptionalNumbers(left.explosionResistance, right.explosionResistance),
          left.defaultResistance() == -1 || right.defaultResistance() == -1
              ? Math.max(left.defaultResistance, right.defaultResistance)
              : Math.min(left.defaultResistance(), right.defaultResistance()),
          Helpers.mergeMaps(left.data(), right.data(),
              (rightData, newLeftData) -> new ResistanceData(
                Math.min(newLeftData.resistance(), rightData.resistance()),
                newLeftData.applyMiningSpeed() || rightData.applyMiningSpeed()
          ))
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

    public void write(FriendlyByteBuf buf) {
      buf.writeBoolean(hardness.isPresent());
      hardness.ifPresent(buf::writeFloat);

      buf.writeBoolean(explosionResistance.isPresent());
      explosionResistance.ifPresent(buf::writeFloat);

      buf.writeInt(defaultResistance);
      buf.writeInt(data.size());

      for (Map.Entry<Byte, ResistanceData> entry : data.entrySet()) {
        buf.writeByte(entry.getKey());
        buf.writeInt(entry.getValue().resistance());
        buf.writeBoolean(entry.getValue().applyMiningSpeed());
      }
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
