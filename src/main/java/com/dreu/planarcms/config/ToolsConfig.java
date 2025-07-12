package com.dreu.planarcms.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import static com.dreu.planarcms.PlanarCMS.MODID;
import static com.dreu.planarcms.config.GeneralConfig.PRESET_FOLDER_NAME;
import static com.dreu.planarcms.util.Helpers.LogLevel.*;
import static com.dreu.planarcms.util.Helpers.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ToolsConfig {
  public static final String TEMPLATE_FILE_NAME = "config/" + MODID + "/presets/template/tools.toml";
  public static final String TEMPLATE_CONFIG_STRING = """
    # See Template for more information
    
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
  public static final String COMMENTED_TEMPLATE_CONFIG_STRING = """
    # DO NOT EDIT THIS TEMPLATE! IT WILL BE RESET!
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

  public static final Map<String, ArrayList<String>> FILTERED_REGISTRY = new HashMap<>();
  private static boolean registryHasBeenFilteredByModId;

  @SuppressWarnings("RedundantClassCall")
  public static void parse() {
    CONFIG = parseFileOrDefault(PRESET_FOLDER_NAME + "tools.toml", TEMPLATE_CONFIG_STRING);
    Object toolTypes = CONFIG.get("ToolTypes");
    if (toolTypes == null) {
      addConfigIssue(ERROR, (byte) 4, "Key \"ToolTypes\" is missing from config [{}] | Using basic Template instead...", PRESET_FOLDER_NAME + "tools.toml");
      CONFIG = new TomlParser().parse(TEMPLATE_CONFIG_STRING);
      return;
    } else {
      try {
        ArrayList.class.cast(toolTypes);
      } catch (Exception e) {
        addConfigIssue(ERROR, (byte) 4, "Value: \"{}\" for \"ToolTypes\" is an invalid type in config [{}] | Expected: 'ArrayList' but got: '{}' | Using basic Template instead...", toolTypes, PRESET_FOLDER_NAME + "tools.toml", toolTypes.getClass().getSimpleName());
        CONFIG = new TomlParser().parse(TEMPLATE_CONFIG_STRING);
        return;
      }
    }
    Object tools = CONFIG.get("Tools");
    if (tools == null) {
      addConfigIssue(ERROR, (byte) 4, "Key \"Tools\" is missing from config [{}] | Using basic Template instead...", PRESET_FOLDER_NAME + "tools.toml");
      CONFIG = new TomlParser().parse(TEMPLATE_CONFIG_STRING);
    } else {
      try {
        Config.class.cast(tools);
      } catch (Exception e) {
        addConfigIssue(ERROR, (byte) 4, "Value: \"{}\" for \"Tools\" is an invalid type in config [{}] | Expected: 'Table' but got: '{}' | Using basic Template instead...", tools, PRESET_FOLDER_NAME + "tools.toml", tools.getClass().getSimpleName());
        CONFIG = new TomlParser().parse(TEMPLATE_CONFIG_STRING);
      }
    }
  }

  public static final ArrayList<String> REGISTERED_TOOL_TYPES = new ArrayList<>();
  public static final ArrayList<Integer> REGISTERED_TOOL_COLORS = new ArrayList<>();

  public static void populateToolTypes() {
    REGISTERED_TOOL_TYPES.clear();
    ((ArrayList<?>) CONFIG.get("ToolTypes")).forEach(toolType -> {
      String entry = toolType.toString();
      String[] parts = entry.split(":");
      REGISTERED_TOOL_TYPES.add(parts[0]);
      if (parts.length == 2) {
        REGISTERED_TOOL_COLORS.add(Integer.parseUnsignedInt(parts[1], 16));
      } else {
        REGISTERED_TOOL_COLORS.add(0xFFFFFF);
      }
    });
  }

  public static Map<String, Properties> TOOLS = new HashMap<>();

  public static void populateTools() {
    TOOLS.clear();
    Map<String, Object> toolsConfig = ((Config) CONFIG.get("Tools")).valueMap();
    Map<String, Config> singleTools = new HashMap<>();
    toolsConfig.forEach((configKey, propertiesConfig) -> {
      Properties properties = assembleProperties(configKey, (Config) propertiesConfig);
      if (configKey.startsWith("#")) {
        handleTag(configKey, properties);
      } else if (configKey.endsWith(":*")) {
        handleMod(configKey, properties);
      } else if (configKey.startsWith("@")) {
        handleCollection(configKey, properties);
      } else if (isValidItem(configKey, Optional.empty(), "tools.toml")) {
          singleTools.put(configKey, (Config) propertiesConfig);
      }
    });
    singleTools.forEach(ToolsConfig::handleSingleItem);
  }

  @SuppressWarnings({"DataFlowIssue"})
  private static void handleTag(String configKey, Properties properties) {
    String tagId = configKey.substring(1);
    if (!ResourceLocation.isValidResourceLocation(tagId)) {
      addConfigIssue(INFO, (byte) 2, "Not a valid Tag ResourceLocation: <{}> declared in {} | Skipping...", configKey, "config: [" + PRESET_FOLDER_NAME + "tools.toml]");
      return;
    }
    if (!ForgeRegistries.ITEMS.tags().isKnownTagName(ItemTags.create(new ResourceLocation(tagId)))) {
      addConfigIssue(INFO, (byte) 2, "Not an existing Item Tag: <{}> declared in {} | Skipping...", configKey,"config: [" + PRESET_FOLDER_NAME + "tools.toml]");
      return;
    }
    ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation(tagId))).forEach(item ->
      addItem(getItemId(item), properties)
    );
  }

  private static void handleCollection(String configKey, Properties properties) {
    String collectionName = configKey.substring(1);
    List<String> collection = CollectionsConfig.ITEMS_MAP.get(collectionName);
    if (collection == null) {
      addConfigIssue(WARN, (byte) 4, "Config [{}] declared item collection <{}> which does not exist, check for typos! | Skipping Collection...", PRESET_FOLDER_NAME + "tools.toml", configKey);
      return;
    }

    List<String> positives = new ArrayList<>();
    Set<String> negatives = new HashSet<>();
    List<String> explicit_positives = new ArrayList<>();

    for (String member : collection) {
      if (member.startsWith("-")) {
        String sub = member.substring(1);
        if (sub.startsWith("#")) {
          addItemsFromTag(configKey, Optional.of(collectionName), negatives);
        } else if (sub.endsWith(":*")) {
          addItemsFromMod(configKey, Optional.of(collectionName), negatives);
        } else if (isValidItem(sub, Optional.of(collectionName), "tools.toml")) {
          negatives.add(sub);
        }
      } else if (member.startsWith("#")) {
        addItemsFromTag(configKey, Optional.of(collectionName), positives);
      } else if (member.endsWith(":*")) {
        addItemsFromMod(configKey, Optional.of(collectionName), positives);
      } else if (isValidItem(member, Optional.of(collectionName), "tools.toml")) {
        explicit_positives.add(member);
      }
    }

    for (String block : positives) {
      if (!negatives.contains(block))
        addItem(block, properties);
    }
    for (String block : explicit_positives) {
      addItem(block, properties);
    }
  }

  private static void handleMod(String configKey, Properties properties) {
    String modId = configKey.substring(0, configKey.length() - 2);
    if (isValidMod(modId, Optional.empty(), "tools.toml")) {
      if (!registryHasBeenFilteredByModId) {
        for (ResourceLocation item : ForgeRegistries.ITEMS.getKeys())
          FILTERED_REGISTRY.computeIfAbsent(item.getNamespace(), b -> new ArrayList<>()).add(item.toString());
        registryHasBeenFilteredByModId = true;
      }
      for (String itemId : FILTERED_REGISTRY.get(modId))
        addItem(itemId, properties);
    }
  }

  private static void addItemsFromMod(String configKey, Optional<String> collectionName, Collection<String> list) {
    String modId = configKey.substring(0, configKey.length() - 2);
    if (isValidMod(modId, collectionName, "tools.toml")) {
      if (!registryHasBeenFilteredByModId) {
        for (ResourceLocation item : ForgeRegistries.ITEMS.getKeys())
          FILTERED_REGISTRY.computeIfAbsent(item.getNamespace(), b -> new ArrayList<>()).add(item.toString());
        registryHasBeenFilteredByModId = true;
      }
      list.addAll(FILTERED_REGISTRY.get(modId));
    }
  }

  @SuppressWarnings("DataFlowIssue")
  private static void addItemsFromTag(String configKey, Optional<String> collectionName, Collection<String> list) {
    if (!isValidTag(configKey, collectionName)) return;
    for (Item item : ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation(configKey.substring(1))))) {
      list.add(getItemId(item));
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
      Integer power = tryCast(value, Integer.class, configKey + "." + property, "tools.toml");
      if (power != null)
        powers.put((byte) REGISTERED_TOOL_TYPES.indexOf(property), power);
    });
    return new Properties(powers, getOptionalInt(toolProperties, "MiningSpeed", configKey));
  }

  @SuppressWarnings("SameParameterValue")
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