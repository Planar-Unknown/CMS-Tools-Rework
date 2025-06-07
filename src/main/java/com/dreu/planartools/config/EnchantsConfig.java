package com.dreu.planartools.config;

import com.dreu.planartools.util.OpposingSets;
import com.electronwill.nightconfig.core.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.config.GeneralConfig.PRESET_FOLDER_NAME;
import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_TYPES;
import static com.dreu.planartools.util.Helpers.*;
import static com.dreu.planartools.util.Helpers.LogLevel.INFO;
import static com.dreu.planartools.util.Helpers.LogLevel.WARN;

public class EnchantsConfig {
  public static final String TEMPLATE_FILE_NAME = "config/" + MODID + "/presets/template/enchants.toml";
  public static final String TEMPLATE_CONFIG_STRING = """
      # Here, you can declare which enchantments can be applied to Tools, specified by Items, Tags, Collections, or Registered Tool Types.
      # Keep in mind that Unbreaking, Mending, and Sweeping edge all have explicit handling, so allowing them on an item may not do anything
      
      # Items are explicit, meaning declared items will always behave as declared, overriding any conflicts
      # Tags and Collections will merge, negative values will win.
      # Tool Types will lose any conflicts, but can be declared with explicit Power Requirements.
      # Power predicates can stack, and a tool will adhere to the highest Power that it can, deferring to lower declared powers as needed
      
      # Collections in this config (denoted by "@") are custom groups of either enchantments or items
      # Create your own collections at: [config/planar_tools/collections/]
      # For example the "@combat" enchant collection can be found at [config/planar_tools/collections/enchants/combat.txt]
      # Enchant Collections may only contain individual enchants
      
      "minecraft:stick" = [           # example of allowing a single item to be enchanted
          "minecraft:looting"
      ]
      
      Arcane = [                      # All items with Arcane power can now have these enchants
          "minecraft:looting",
          "minecraft:fire_aspect"
      ]
      
      "Arcane.50" = [                 # All items with 50 or more Arcane Power can now have Fortune, but can't have Fire Aspect
          "minecraft:fortune",
          "-minecraft:fire_aspect"
      ]
      
      "@golden_tools" = [             # Golden Tools (which have Arcane power in our example config) can not have Looting even though specified in Arcane Power Types above
          "-minecraft:looting"
      ]
      
      Pickaxe = [                     # Any item with Pickaxe Power can be more like a pickaxe now
          "minecraft:efficiency",
          "minecraft:fortune",
          "minecraft:silk_touch"
      ]
      
      Axe = [                         # This makes items declared with Axe power behave like an Axe as a tool, but we don't explicitly allow any of the combat enchants
          "minecraft:efficiency",
          "minecraft:fortune",
          "minecraft:silk_touch"
      ]
      
      Shovel = [
          "minecraft:efficiency",
          "minecraft:fortune",
          "minecraft:silk_touch"
      ]
      
      Hoe = [
          "minecraft:efficiency",
          "minecraft:fortune",
          "minecraft:silk_touch"
      ]
      
      Shears = [
          "minecraft:efficiency"
      ]
      
      Sword = [                       # Any item with Sword power can now have combat enchants
          "@combat"
      ]
      """;

  public static Config CONFIG;

  public static void parse() {
    CONFIG = parseFileOrDefault(PRESET_FOLDER_NAME + "enchants.toml", TEMPLATE_CONFIG_STRING);
  }

  public static final Map<Byte, TreeMap<Integer, OpposingSets<String>>> ENCHANTS_BY_TOOL_TYPE = new TreeMap<>();
  public static final Map<String, OpposingSets<String>> ENCHANTS_BY_ITEM_ID = new HashMap<>();

  public static void populateEnchants() {
    ENCHANTS_BY_TOOL_TYPE.clear();
    ENCHANTS_BY_ITEM_ID.clear();
    Map<String, OpposingSets<String>> singleItems = new HashMap<>();

    CONFIG.valueMap().forEach((configKey, configEnchants) -> {
      //noinspection unchecked
      List<String> enchants = tryCast(configEnchants, List.class, configKey, "enchants.toml");
      if (enchants == null) return;
      if (enchants.isEmpty()) {
        addConfigIssue(LogLevel.INFO, (byte) 1, "Enchantment list for \"{}\" was left empty in config: {{}} | Skipping Tool Type...", configKey, PRESET_FOLDER_NAME + "enchants.toml");
        return;
      }
      OpposingSets<String> enchantments = getOpposingSetsFromList(configKey, enchants);
      if (configKey.startsWith("@")) {
        handleItemCollection(configKey, enchantments);
      } else if (configKey.startsWith("#")) {
        handleItemTag(configKey, enchantments, Optional.empty());
      } else if (configKey.contains(":")) {
        singleItems.put(configKey, enchantments);
      } else if (configKey.contains(".")) {
        handleToolTypeWithPower(configKey, enchantments);
      } else if (REGISTERED_TOOL_TYPES.contains(configKey)) {
        handleToolTypeWithPower(configKey + ".0", enchantments);
      } else addConfigIssue(LogLevel.INFO, (byte) 2, "\"{}\" used in config: {{}} is NOT a registered tool type, collection, or existing item, check for typos!", configKey, PRESET_FOLDER_NAME + "enchants.toml");
    });

    singleItems.forEach((configKey, enchantments) -> {
      if (isValidItem(configKey, Optional.empty(), "enchants.toml"))
        ENCHANTS_BY_ITEM_ID.merge(configKey, enchantments, OpposingSets::mergeRightWins);
    });
  }

  private static void handleToolTypeWithPower(String configKey, OpposingSets<String> enchantments) {
    String[] strings = configKey.split("\\.");
    if (strings.length > 2) {
      addConfigIssue(WARN, (byte) 4, "Key: \"{}\" in Config: [{}] was invalid. There were too many dots! Proper example: \"Pickaxe.20\"", configKey, PRESET_FOLDER_NAME + "enchants.toml");
      return;
    }
    if (!REGISTERED_TOOL_TYPES.contains(strings[0])) {
      addConfigIssue(WARN, (byte) 4, "\"{}\" used in config file [{}] is NOT a registered tool type!", strings[0], PRESET_FOLDER_NAME + "enchants.toml");
      return;
    }
    int power;
    try {
      power = Integer.parseInt(strings[1]);
    } catch (Exception e) {
      addConfigIssue(WARN, (byte) 4, "Key: \"{}\" in Config: [{}] was invalid. Power must be an Integer! Proper example: \"Pickaxe.20\"", strings[0], PRESET_FOLDER_NAME + "enchants.toml");
      return;
    }
    if (power < 0) {
      addConfigIssue(WARN, (byte) 4, "Key: \"{}\" in Config: [{}] was invalid. Power cannot be negative! Proper example: \"Pickaxe.20\"", strings[0], PRESET_FOLDER_NAME + "enchants.toml");
      return;
    }
    byte toolTypeId = (byte) REGISTERED_TOOL_TYPES.indexOf(strings[0]);
    TreeMap<Integer, OpposingSets<String>> treeMap = ENCHANTS_BY_TOOL_TYPE.get(toolTypeId);
    if (treeMap == null)
      ENCHANTS_BY_TOOL_TYPE.put(toolTypeId, new TreeMap<>(Map.of(power, enchantments)));
    else
      treeMap.put(power, enchantments);
  }

  private static void handleItemCollection(String configKey, OpposingSets<String> enchantments) {
    String collectionName = configKey.substring(1);
    List<String> collection = CollectionsConfig.ITEMS_MAP.get(collectionName);
    if (collection == null) {
      addConfigIssue(WARN, (byte) 4, "Config [{}] declared item collection <{}> which does not exist, check for typos! | Skipping Collection...", PRESET_FOLDER_NAME + "enchants.toml", configKey);
      return;
    }
    collection.forEach((member) -> {
      if (member.startsWith("#")) {
        handleItemTag(configKey, enchantments, Optional.of(collectionName));
      } else {
        if (!isValidItem(member, Optional.of(collectionName), "enchants.toml")) return;
        ENCHANTS_BY_ITEM_ID.merge(member, enchantments, OpposingSets::merge);
      }
    });
  }

  @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "DataFlowIssue"})
  private static void handleItemTag(String configKey, OpposingSets<String> sets, Optional<String> collectionName) {
    String tagId = configKey.substring(1);
    if (!ResourceLocation.isValidResourceLocation(tagId)) {
      addConfigIssue(INFO, (byte) 2, "Not a valid Tag ResourceLocation: <{}> declared in {} | Skipping...", configKey, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() -> "config: [" + PRESET_FOLDER_NAME + "enchants.toml]"));
      return;
    }
    if (!ForgeRegistries.ITEMS.tags().isKnownTagName(ItemTags.create(new ResourceLocation(tagId)))) {
      addConfigIssue(INFO, (byte) 2, "Not an existing Item Tag: <{}> declared in {} | Skipping...", configKey, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() -> "config: [" + PRESET_FOLDER_NAME + "enchants.toml]"));
      return;
    }
    ForgeRegistries.ITEMS.tags().getTag(ItemTags.create(new ResourceLocation(tagId))).forEach(item ->
      ENCHANTS_BY_ITEM_ID.merge(getItemId(item), sets, OpposingSets::merge));
  }

  private static @NotNull OpposingSets<String> getOpposingSetsFromList(String configKey, List<String> enchants) {
    OpposingSets<String> enchantments = new OpposingSets<>();
    for (String enchant : enchants) {
      if (enchant.startsWith("-")) {
        enchant = enchant.substring(1);
        if (enchant.startsWith("@")) {
          addEnchantmentsFromCollection(enchant, enchantments, true);
        } else if (isValidEnchant(enchant, Optional.empty(), "for: \"" + configKey + "\""))
          enchantments.addNegative(enchant);
      } else if (enchant.startsWith("@")) {
        addEnchantmentsFromCollection(enchant, enchantments, false);
      } else if (isValidEnchant(enchant, Optional.empty(), configKey)) {
        enchantments.addPositive(enchant);
      }
    }
    return enchantments;
  }

  private static void addEnchantmentsFromCollection(String configKey, OpposingSets<String> enchantments, boolean invert) {
    String collectionName = configKey.substring(1);
    List<String> collection = CollectionsConfig.ENCHANTS_MAP.get(collectionName);
    if (collection == null) {
      addConfigIssue(WARN, (byte) 4, "Config [{}] declared item collection <{}> which does not exist, check for typos! | Skipping Collection...", PRESET_FOLDER_NAME + "enchants.toml", configKey);
      return;
    }
    collection.forEach((enchant) -> {
      if (isValidEnchant(enchant, Optional.of(collectionName), ""))
        if (invert) enchantments.addNegative(enchant);
        else enchantments.addPositive(enchant);
    });
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static boolean isValidEnchant(String enchant, Optional<String> collectionName, String key) {
    if (!ResourceLocation.isValidResourceLocation(enchant)) {
      addConfigIssue(INFO, (byte) 2, "Not a valid Enchant ResourceLocation: <{}> declared in {} | Skipping...", enchant, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() -> "config: [" + PRESET_FOLDER_NAME + "tools.toml]"));
      return false;
    }
    if (!ModList.get().isLoaded(enchant.split(":")[0])) {
      addConfigIssue(INFO, (byte) 2, "{} declared enchantment: <{}>{}, but mod '{{}}' is not loaded | Skipping Item...", collectionName.map(s -> "Collection: [" + s + "]").orElseGet(() -> "Config: [" + PRESET_FOLDER_NAME + "enchants.toml]"), enchant, key, enchant.split(":")[0]);
      return false;
    }
    if (!ForgeRegistries.ENCHANTMENTS.containsKey(new ResourceLocation(enchant))) {
      addConfigIssue(INFO, (byte) 2, "{} declared non-existent enchantment: <{}>{}, check for typos! | Skipping Item...", collectionName.map(s -> "Collection: [" + s + "]").orElseGet(() -> "Config: [" + PRESET_FOLDER_NAME + "tools.toml]"), enchant, key);
      return false;
    }
    return true;
  }
}
