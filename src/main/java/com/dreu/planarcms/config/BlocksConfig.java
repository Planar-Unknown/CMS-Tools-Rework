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
       ConfigVersion = 1
       # Create your own rules here. See config/planar_cms/presets/template for a tutorial.
       """;
  }
  public static String getCommentedTemplateConfig() {
    return """
       ConfigVersion = 1
       # DO NOT EDIT THIS TEMPLATE! IT WILL BE RESET!
       # Tutorial examples: copy the rules you want into presets/custom/blocks.toml.
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
       
       # Power entries inherit omitted values; Resistance must still be met.
       # Bonuses add to mining speed (highest eligible bonus wins); 0 clears the bonus.
       ["minecraft:calcite"]
         DefaultResistance = -1
         Pickaxe = [
           {Resistance = 20, ApplyMiningSpeed = false, CanDrop = false},
           {Power = 40, ApplyMiningSpeed = true, CanDrop = true, MiningSpeedBonus = 2.5},
           {Power = 80, MiningSpeedBonus = 0}
         ]

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
    Map<String, Map.Entry<Config, Properties>> singleBlocks = new HashMap<>();
    for (Map.Entry<String, Object> entry : ConfigUpgrades.entries(CONFIG).entrySet()) {
      String configKey = entry.getKey();
      if (!(entry.getValue() instanceof Config propertiesConfig)) {
        addConfigIssue(ERROR, (byte) 6, "Expected a block table for <{}> in [{}] | Unsupported metadata or config format; skipping entry...", configKey, PRESET_FOLDER_NAME + "blocks.toml");
        continue;
      }
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
        singleBlocks.put(configKey, Map.entry(propertiesConfig, properties));
      }
    }

    for (Map.Entry<String, Map.Entry<Config, Properties>> entry : singleBlocks.entrySet()) {
      handleSingleBlock(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue());
    }

    FILTERED_REGISTRY.clear();
    registryHasBeenFilteredByModId = false;
  }

  private static void handleMod(String configKey, Properties properties) {
    String modId = configKey.substring(0, configKey.length() - 2);
    if (isValidMod(modId, Optional.empty(), "blocks.toml")) {
      filterRegistry();
      for (String blockId : FILTERED_REGISTRY.get(modId))
        addBlock(blockId, properties);
    }
  }

  private static void filterRegistry() {
    if (!registryHasBeenFilteredByModId) {
      for (ResourceLocation block : ForgeRegistries.BLOCKS.getKeys())
        FILTERED_REGISTRY.computeIfAbsent(block.getNamespace(), b -> new ArrayList<>()).add(block.toString());
      registryHasBeenFilteredByModId = true;
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
          addBlocksFromMod(sub, negatives);
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
        explicit_positives.add(member);
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
      filterRegistry();
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

  private static void handleSingleBlock(String blockId, Config blockPropertiesConfig, Properties properties) {
    if (!isValidBlock(blockId, Optional.empty())) return;

    BLOCKS.merge(blockId, properties, (existing, singleBlock) -> {
      int defaultResistance = getOrElse(blockPropertiesConfig, blockId, "DefaultResistance", existing.defaultResistance(), Integer.class, "blocks.toml", true);
      boolean defaultCanDrop = getOrElse(blockPropertiesConfig, blockId, "DefaultCanDrop", existing.defaultCanDrop(), Boolean.class, "blocks.toml", true);
      Map<Byte, PowerProfiles> resistanceDataMap = getResistanceDataMapOverride(blockPropertiesConfig, defaultResistance, existing.data(), blockId);

      existing.data().forEach(resistanceDataMap::putIfAbsent);

      return new Properties(
          singleBlock.hardness().isPresent() ? singleBlock.hardness() : existing.hardness(),
          singleBlock.explosionResistance().isPresent() ? singleBlock.explosionResistance() : existing.explosionResistance(),
          defaultResistance,
          defaultCanDrop,
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

  private static Map<Byte, PowerProfiles> getResistanceDataMap(Config config, int defaultResistance) {
    Map<Byte, PowerProfiles> map = new HashMap<>();
    for (Map.Entry<String, Object> entry : config.valueMap().entrySet()) {
      String key = entry.getKey();
      if (isStandardKey(key)) continue;

      int type = REGISTERED_TOOL_TYPES.indexOf(key);
      if (type == -1) {
        addConfigIssue(ERROR, (byte) 6, "\"{}\" in config file [{}] is NOT a registered tool type!", key, PRESET_FOLDER_NAME + "blocks.toml");
        continue;
      }

      PowerProfiles profiles = parseToolProfile(key, entry.getValue(),
          PowerProfiles.constant(new ToolProfile(defaultResistance, true, Optional.empty())));
      if (profiles != null) map.put((byte) type, profiles);
    }
    return map;
  }

  private static Map<Byte, PowerProfiles> getResistanceDataMapOverride(Config block, int defaultResistance, Map<Byte, PowerProfiles> right, String parent) {
    Map<Byte, PowerProfiles> resistanceDataMap = new HashMap<>();
    for (Map.Entry<String, Object> property : block.valueMap().entrySet()) {
      String key = property.getKey();
      if (isStandardKey(key)) continue;

      int toolType = REGISTERED_TOOL_TYPES.indexOf(key);
      if (toolType == -1) {
        addConfigIssue(ERROR, (byte) 6, "\"{}\" used in config file [{}] for <{}> is NOT a registered tool type!", key, PRESET_FOLDER_NAME + "blocks.toml", parent);
        continue;
      }

      PowerProfiles inherited = right.get((byte) toolType);
      if (inherited == null)
        inherited = PowerProfiles.constant(new ToolProfile(defaultResistance, true, Optional.empty()));
      PowerProfiles profiles = parseToolProfile(parent + "." + key, property.getValue(), inherited);
      if (profiles != null) resistanceDataMap.put((byte) toolType, profiles);
    }
    return resistanceDataMap;
  }

  static PowerProfiles parseToolProfile(String parent, Object value, PowerProfiles inherited) {
    if (value instanceof Config config)
      return PowerProfiles.overridden(inherited, parseProfilePatch(parent, config), Map.of());
    if (!(value instanceof List<?> entries) || entries.isEmpty()) {
      return invalidToolProfile(parent, "Expected a tool table or a nonempty array of property tables");
    }

    PowerProfiles.Patch baseline = PowerProfiles.Patch.EMPTY;
    Map<Integer, PowerProfiles.Patch> changes = new TreeMap<>();
    for (int i = 0; i < entries.size(); i++) {
      if (!(entries.get(i) instanceof Config entry))
        return invalidToolProfile(parent, "Each array entry must be a property table");
      for (String key : entry.valueMap().keySet()) {
        if (!key.equals("Power") && !key.equals("Resistance") && !key.equals("ApplyMiningSpeed") && !key.equals("CanDrop") && !key.equals("MiningSpeedBonus"))
          return invalidToolProfile(parent, "Unsupported property: " + key);
      }
      if (entry.get("Power") == null) {
        if (i != 0) return invalidToolProfile(parent, "Only the first entry may omit Power (the baseline)");
        baseline = parseProfilePatch(parent, entry);
        continue;
      }
      Integer power = getOrElse(entry, parent, "Power", null, Integer.class, "blocks.toml", false);
      if (power == null || power < 0)
        return invalidToolProfile(parent, "Power must be a nonnegative integer");
      if (changes.putIfAbsent(power, parseProfilePatch(parent, entry)) != null)
        return invalidToolProfile(parent, "Duplicate Power: " + power);
    }
    return PowerProfiles.overridden(inherited, baseline, changes);
  }

  private static PowerProfiles.Patch parseProfilePatch(String parent, Config config) {
    return new PowerProfiles.Patch(
        Optional.ofNullable(getOrElse(config, parent, "Resistance", null, Integer.class, "blocks.toml", false)),
        Optional.ofNullable(getOrElse(config, parent, "ApplyMiningSpeed", null, Boolean.class, "blocks.toml", false)),
        Optional.ofNullable(getOrElse(config, parent, "CanDrop", null, Boolean.class, "blocks.toml", false)),
        getMiningSpeedBonus(config, parent)
    );
  }

  private static Optional<Float> getMiningSpeedBonus(Config config, String parent) {
    Object value = config.get("MiningSpeedBonus");
    if (value == null) return Optional.empty();
    if (value instanceof Number number && Float.isFinite(number.floatValue()))
      return Optional.of(number.floatValue());
    addConfigIssue(WARN, (byte) 4, "Invalid MiningSpeedBonus <{}> for <{}> in [{}]: expected a finite number | Ignoring property...", value, parent, PRESET_FOLDER_NAME + "blocks.toml");
    return Optional.empty();
  }

  private static PowerProfiles invalidToolProfile(String parent, String reason) {
    addConfigIssue(ERROR, (byte) 6, "Invalid tool properties for <{}> in [{}]: {} | Skipping profile...", parent, PRESET_FOLDER_NAME + "blocks.toml", reason);
    return null;
  }

  private static void addBlock(String blockId, Properties properties) {
    BLOCKS.merge(blockId, properties, Properties::merged);
  }

  private static @NotNull Properties assembleProperties(String configKey, Config config) {
    int defaultResistance = getOrElse(config, configKey, "DefaultResistance", 0, Integer.class, "blocks.toml", false);
    boolean defaultCanDrop = getOrElse(config, configKey, "DefaultCanDrop", true, Boolean.class, "blocks.toml", false);
    return new Properties(
        getOptionalFloat(config, "Hardness", configKey),
        getOptionalFloat(config, "ExplosionResistance", configKey),
        defaultResistance,
        defaultCanDrop,
        getResistanceDataMap(config, defaultResistance)
    );
  }

  private static boolean isStandardKey(String key) {
    return key.equals("DefaultResistance") || key.equals("ExplosionResistance") || key.equals("Hardness") || key.equals("DefaultCanDrop");
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

  public record ToolProfile(int resistance, boolean applyMiningSpeed, Optional<Boolean> canDrop, float miningSpeedBonus) {
    public ToolProfile {
      if (!Float.isFinite(miningSpeedBonus))
        throw new IllegalArgumentException("MiningSpeedBonus must be finite");
      if (miningSpeedBonus == 0) miningSpeedBonus = 0; // Normalize negative zero for profile coalescing.
    }

    public ToolProfile(int resistance, boolean applyMiningSpeed, Optional<Boolean> canDrop) {
      this(resistance, applyMiningSpeed, canDrop, 0);
    }

    public static float applyMiningSpeedBonus(float speed, float bonus) {
      return Math.max(0f, Math.min(Float.MAX_VALUE, speed + bonus));
    }

    public static ToolProfile merged(ToolProfile left, ToolProfile right) {
      return new ToolProfile(
          Math.min(left.resistance(), right.resistance()),
          left.applyMiningSpeed() || right.applyMiningSpeed(),
          left.canDrop.isPresent() ? right.canDrop.isPresent() ? Optional.of(left.canDrop.get() && right.canDrop.get()) : left.canDrop : right.canDrop,
          Math.max(left.miningSpeedBonus(), right.miningSpeedBonus())
      );
    }
  }
  // Each tool type selects a precompiled profile by held power.
  public record Properties(Optional<Float> hardness, Optional<Float> explosionResistance, int defaultResistance, boolean defaultCanDrop, Map<Byte, PowerProfiles> data) {

    public ToolProfile profileFor(byte type, int power) {
      PowerProfiles profiles = data.get(type);
      return profiles == null ? null : profiles.atPower(power);
    }

    public static Properties merged(Properties left, Properties right) {
      return new Properties(
          mergeOptionalNumbers(left.hardness, right.hardness),
          mergeOptionalNumbers(left.explosionResistance, right.explosionResistance),
          left.defaultResistance() == -1 || right.defaultResistance() == -1
              ? Math.max(left.defaultResistance, right.defaultResistance)
              : Math.min(left.defaultResistance(), right.defaultResistance()),
          left.defaultCanDrop && right.defaultCanDrop,
          Helpers.mergeMaps(left.data(), right.data(), PowerProfiles::merged)
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
          : right;
    }

    public void write(FriendlyByteBuf buf) {
      buf.writeBoolean(hardness.isPresent());
      hardness.ifPresent(buf::writeFloat);

      buf.writeBoolean(explosionResistance.isPresent());
      explosionResistance.ifPresent(buf::writeFloat);

      buf.writeInt(defaultResistance);
      buf.writeBoolean(defaultCanDrop);
      buf.writeInt(data.size());

      for (Map.Entry<Byte, PowerProfiles> entry : data.entrySet()) {
        buf.writeByte(entry.getKey());
        entry.getValue().write(buf);
      }
    }

    public static Properties read(FriendlyByteBuf buf) {
      Optional<Float> hardness = buf.readBoolean() ? Optional.of(buf.readFloat()) : Optional.empty();
      Optional<Float> explosionResistance = buf.readBoolean() ? Optional.of(buf.readFloat()) : Optional.empty();
      int defaultResistance = buf.readInt();
      boolean defaultCanDrop = buf.readBoolean();

      int size = buf.readInt();
      if (size < 0 || size > 256 || size > buf.readableBytes() / 15)
        throw new IllegalArgumentException("Invalid tool profile count");
      Map<Byte, PowerProfiles> map = new HashMap<>();
      for (int i = 0; i < size; i++) {
        byte key = buf.readByte();
        if (map.put(key, PowerProfiles.read(buf)) != null)
          throw new IllegalArgumentException("Duplicate tool profile");
      }
      return new Properties(hardness, explosionResistance, defaultResistance, defaultCanDrop, map);
    }
  }
}
