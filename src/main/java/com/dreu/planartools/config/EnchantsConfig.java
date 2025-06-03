package com.dreu.planartools.config;

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
  //Todo: all the mixins
  public static final String TEMPLATE_FILE_NAME = "config/" + MODID + "/presets/template/enchants.toml";
  public static final String TEMPLATE_CONFIG_STRING = """
      # Here, you can declare which enchantments can be applied to each of your registered tool types.
      # Keep in mind that Unbreaking, Mending, and Sweeping edge all have explicit handling, so allowing them on an item may not do anything
      
      Arcane = [
          "minecraft:looting",
          "minecraft:fortune",
          "minecraft:fire_aspect"
      ]
      
      Pickaxe = [
          "minecraft:efficiency",
          "minecraft:fortune",
          "minecraft:silk_touch"
      ]
      
      Axe = [
          "minecraft:efficiency",
          "minecraft:fortune",
          "minecraft:silk_touch",
          "minecraft:sharpness",
          "minecraft:bane_of_arthropods",
          "minecraft:smite"
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
      
      Sword = [
          "minecraft:sharpness",
          "minecraft:smite",
          "minecraft:bane_of_arthropods",
          "minecraft:knockback",
          "minecraft:fire_aspect",
          "minecraft:looting"
      ]
      """;

  public static Config CONFIG;

  public static void parse() {
    CONFIG = parseFileOrDefault(PRESET_FOLDER_NAME + "enchants.toml", TEMPLATE_CONFIG_STRING);
  }

  public static final Map<Byte, OpposingSets<String>> ENCHANTS_BY_TOOL_TYPE = new HashMap<>();
  public static final Map<String, OpposingSets<String>> ENCHANTS_BY_ITEM_ID = new HashMap<>();

  public static void populateEnchants() {
    ENCHANTS_BY_TOOL_TYPE.clear();
    ENCHANTS_BY_ITEM_ID.clear();
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
      } else if (REGISTERED_TOOL_TYPES.contains(configKey)) {
        ENCHANTS_BY_TOOL_TYPE.put((byte) REGISTERED_TOOL_TYPES.indexOf(configKey), enchantments);
      } else if (configKey.contains(":")) {
        if (isValidItem(configKey, Optional.empty(), "enchants.toml"))
          ENCHANTS_BY_ITEM_ID.merge(configKey, enchantments, OpposingSets::mergeLeftWins);
      } else
        addConfigIssue(LogLevel.INFO, (byte) 2, "\"{}\" used in config: {{}} is NOT a registered tool type, collection, or existing item, check for typos!", configKey, PRESET_FOLDER_NAME + "enchants.toml");
    });
    System.out.println("------------");
    System.out.println("Enchants by Item ID: ");
    System.out.println();
    ENCHANTS_BY_ITEM_ID.forEach((key, value) -> {
      System.out.println("Item is: " + key);
      System.out.println("Negative values:");
      value.negative().forEach(System.out::println);
      System.out.println("Positive values:");
      value.positive().forEach(System.out::println);
      System.out.println("------------");
    });

    System.out.println("------------");
    System.out.println("Enchants by Tool Type: ");
    System.out.println();
    ENCHANTS_BY_TOOL_TYPE.forEach((key, value) -> {
      System.out.println("Tool type is: " + REGISTERED_TOOL_TYPES.get(key));
      System.out.println("Negative values:");
      value.negative().forEach(System.out::println);
      System.out.println("Positive values:");
      value.positive().forEach(System.out::println);
      System.out.println("------------");
    });
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


  public record OpposingSets<T>(Set<T> positive, Set<T> negative) {

    public OpposingSets() {
      this(new HashSet<>(), new HashSet<>());
    }

    @SuppressWarnings("unused")
    public OpposingSets(OpposingSets<T> sets) {
      this(sets.positive, sets.negative);
    }

    public void addPositive(T t) {
      if (!negative.contains(t))
        positive.add(t);
    }

    public void addNegative(T t) {
      negative.add(t);
      positive.remove(t);
    }

    public void clear() {
      positive.clear();
      negative.clear();
    }

    @SuppressWarnings("unused")
    public void addAll(OpposingSets<T> sets) {
      for (T neg : sets.negative()) addNegative(neg);
      for (T pos : sets.positive()) addPositive(pos);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isEmpty() {
      return negative.isEmpty() && positive.isEmpty();
    }

    public void mergeDominantly(OpposingSets<T> incoming) {
      incoming.negative.forEach(t -> {
        if (!this.positive.contains(t))
          this.negative.add(t);
      });
      incoming.positive.forEach(t -> {
        if (!this.negative.contains(t))
          this.positive.add(t);
      });
    }

    public static <T> OpposingSets<T> mergeLeftWins(OpposingSets<T> left, OpposingSets<T> right) {
      OpposingSets<T> merged = new OpposingSets<>();
      merged.negative.addAll(left.negative);
      merged.positive.addAll(left.positive);
      right.negative.forEach(t -> {
        if (!merged.positive.contains(t))
          merged.negative.add(t);
      });
      right.positive.forEach(t -> {
        if (!merged.negative.contains(t))
          merged.positive.add(t);
      });
      return merged;
    }

    public static <T> OpposingSets<T> merge(OpposingSets<T> left, OpposingSets<T> right) {
      if (left == null && right == null) return new OpposingSets<>();
      if (left == null) return right;
      if (right == null) return left;

      OpposingSets<T> merged = new OpposingSets<>();
      merged.negative.addAll(left.negative);
      merged.negative.addAll(right.negative);
      left.positive.forEach(merged::addPositive);
      right.positive.forEach(merged::addPositive);
      return merged;
    }
  }
}
