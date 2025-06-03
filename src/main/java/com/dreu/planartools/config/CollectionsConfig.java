package com.dreu.planartools.config;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dreu.planartools.PlanarTools.LOGGER;
import static com.dreu.planartools.PlanarTools.MODID;

public class CollectionsConfig {
  public static final String BLOCK_COLLECTIONS_FOLDER = "config/" + MODID + "/collections/blocks";
  public static final String ITEM_COLLECTIONS_FOLDER = "config/" + MODID + "/collections/items";
  public static final String ENCHANT_COLLECTIONS_FOLDER = "config/" + MODID + "/collections/enchants";

  public static final String COMBAT_EXAMPLE_TEMPLATE = """
      // This is a collection of Enchantments, it can be referenced your enchantments.toml using its file path (after the collections folder)
      // For example, this collection can be referenced with "@combat" because it is at [config/planar_tools/collections/enchants/combat.txt]
      // File extensions (e.g., .txt, .cfg, .toml) can be anything. It doesn't matter.
      
      minecraft:sharpness
      minecraft:bane_of_arthropods
      minecraft:smite
      minecraft:looting
      minecraft:fire_aspect
      minecraft:knockback
      """;

  public static final String DEEPSLATE_EXAMPLE_TEMPLATE = """
      // This is a collection of blocks, it can be referenced in your blocks.toml using its file path (after the collections folder)
      // For example this collection can be referenced with ["@example/deepslate"] because it is at [config/planar_tools/collections/blocks/example/deepslate.txt]
      // File extensions (e.g., .txt, .cfg, .toml) can be anything. It doesn't matter.
      
      minecraft:deepslate
      $minecraft:cobbled_deepslate
      $minecraft:polished_deepslate
      $minecraft:deepslate_bricks
      $minecraft:deepslate_tiles
      #forge:ores_in_ground/deepslate
      """;

  public static final String WOOD_EXAMPLE_TEMPLATE = """
      // This is a collection of blocks, it can be referenced in your blocks.toml using its file path (after the collections folder)
      // For example this collection can be referenced with ["@wood"] because it is [config/planar_tools/collections/blocks/wood.txt]
      // File extensions (e.g., .txt, .cfg, .toml) can be anything. It doesn't matter.
      
      // Minecraft Tags
      #minecraft:logs
      #minecraft:planks
      #minecraft:wooden_buttons
      #minecraft:wooden_doors
      #minecraft:wooden_fences
      #minecraft:wooden_pressure_plates
      #minecraft:wooden_slabs
      #minecraft:wooden_stairs
      #minecraft:wooden_trapdoors
      
      // Minecraft Block Families
      $minecraft:dark_oak_planks
      $minecraft:oak_planks
      $minecraft:acacia_planks
      $minecraft:birch_planks
      $minecraft:jungle_planks
      $minecraft:spruce_planks
      $minecraft:mangrove_planks
      $minecraft:cherry_planks
      $minecraft:warped_planks
      $minecraft:crimson_planks
      
      // Forge Tags
      #forge:fence_gates/wooden
      #forge:fences/wooden
      #forge:chests/wooden
      #forge:barrels/wooden
      """;

  public static final String GOLDEN_TOOLS_TEMPLATE = """
      // This is a collection of items, it can be referenced in your tools.toml or enchantments.toml using its file path after the collections folder
      // For example this collection can be referenced with ["@golden_tools"] because it is [config/planar_tools/collections/items/golden_tools.txt]
      // File extensions (e.g., .txt, .cfg, .toml) can be anything. It doesn't matter.
      
      minecraft:golden_axe
      minecraft:golden_pickaxe
      minecraft:golden_shovel
      minecraft:golden_hoe
      minecraft:golden_sword
      """;

  public static final Map<String, List<String>> BLOCKS_MAP = new HashMap<>();
  public static final Map<String, List<String>> ITEMS_MAP = new HashMap<>();
  public static final Map<String, List<String>> ENCHANTS_MAP = new HashMap<>();

  public static void parseAndPopulate() {
    if (new File(ENCHANT_COLLECTIONS_FOLDER).mkdirs()) {
      try (FileWriter fileWriter = new FileWriter(ENCHANT_COLLECTIONS_FOLDER + "/combat.txt")) {
        fileWriter.write(COMBAT_EXAMPLE_TEMPLATE);
        ENCHANTS_MAP.put("combat", List.of(
            "minecraft:sharpness",
            "minecraft:bane_of_arthropods",
            "minecraft:smite",
            "minecraft:looting",
            "minecraft:fire_aspect",
            "minecraft:knockback"
        ));
      } catch (Exception e) {
        LOGGER.error(e.getMessage());
      }
    }
    if (new File(ITEM_COLLECTIONS_FOLDER).mkdirs()) {
      try (FileWriter fileWriter = new FileWriter(ITEM_COLLECTIONS_FOLDER + "/golden_tools.txt")) {
        fileWriter.write(GOLDEN_TOOLS_TEMPLATE);
        ITEMS_MAP.put("golden_tools", List.of(
            "minecraft:golden_axe",
            "minecraft:golden_pickaxe",
            "minecraft:golden_shovel",
            "minecraft:golden_hoe",
            "minecraft:golden_sword"
        ));
      } catch (Exception e) {
        LOGGER.error(e.getMessage());
      }
    }
    if (new File(BLOCK_COLLECTIONS_FOLDER).mkdirs()) {
      //noinspection ResultOfMethodCallIgnored
      new File(BLOCK_COLLECTIONS_FOLDER + "/example").mkdirs();
      try (FileWriter fileWriter = new FileWriter(BLOCK_COLLECTIONS_FOLDER + "/example/deepslate.txt")) {
        fileWriter.write(DEEPSLATE_EXAMPLE_TEMPLATE);
        BLOCKS_MAP.put("example/deepslate", List.of(
            "minecraft:deepslate",
            "$minecraft:cobbled_deepslate",
            "$minecraft:polished_deepslate",
            "$minecraft:deepslate_bricks",
            "$minecraft:deepslate_tiles",
            "#forge:ores_in_ground/deepslate"
        ));
      } catch (Exception e) {
        LOGGER.error(e.getMessage());
      }
      try (FileWriter fileWriter = new FileWriter(BLOCK_COLLECTIONS_FOLDER + "/wood.txt")) {
        fileWriter.write(WOOD_EXAMPLE_TEMPLATE);
        BLOCKS_MAP.put("wood", List.of(
            "#minecraft:logs",
            "#minecraft:planks",
            "#minecraft:wooden_buttons",
            "#minecraft:wooden_doors",
            "#minecraft:wooden_fences",
            "#minecraft:wooden_pressure_plates",
            "#minecraft:wooden_slabs",
            "#minecraft:wooden_stairs",
            "#minecraft:wooden_trapdoors",
            "$minecraft:dark_oak_planks",
            "$minecraft:oak_planks",
            "$minecraft:acacia_planks",
            "$minecraft:birch_planks",
            "$minecraft:jungle_planks",
            "$minecraft:spruce_planks",
            "$minecraft:mangrove_planks",
            "$minecraft:cherry_planks",
            "$minecraft:warped_planks",
            "$minecraft:crimson_planks",
            "#forge:fence_gates/wooden",
            "#forge:fences/wooden",
            "#forge:chests/wooden",
            "#forge:barrels/wooden"
        ));
      } catch (Exception e) {
        LOGGER.error(e.getMessage());
      }
    } else {
      populateMapFromFilesRecursively(ENCHANT_COLLECTIONS_FOLDER, ENCHANTS_MAP, ENCHANT_COLLECTIONS_FOLDER.length() + 1);
      populateMapFromFilesRecursively(ITEM_COLLECTIONS_FOLDER, ITEMS_MAP, ITEM_COLLECTIONS_FOLDER.length() + 1);
      populateMapFromFilesRecursively(BLOCK_COLLECTIONS_FOLDER, BLOCKS_MAP, BLOCK_COLLECTIONS_FOLDER.length() + 1);
    }
  }

  @SuppressWarnings({"DataFlowIssue"})
  private static void populateMapFromFilesRecursively(String fileName, Map<String, List<String>> map, int subStringStart) {
    File currentFile = new File(fileName);
    if (currentFile.isFile()) {
      try {
        List<String> contents = Files.readAllLines(currentFile.toPath()).stream().filter(s -> !(s.startsWith("/") || s.isEmpty())).toList();
        if (contents.isEmpty()) {
          //Todo addConfigIssue found empty collection at
        }
        map.put(removeExtension(fileName.substring(subStringStart)), contents);
      } catch (Exception e) {
        LOGGER.error(e.getMessage());
      }
    } else {
      for (File nextFile : currentFile.listFiles()) {
        populateMapFromFilesRecursively(fileName + "/" + nextFile.getName(), map, subStringStart);
      }
    }
  }

  public static String removeExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
  }
}
