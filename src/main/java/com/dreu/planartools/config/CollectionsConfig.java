package com.dreu.planartools.config;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.*;

import static com.dreu.planartools.PlanarTools.LOGGER;
import static com.dreu.planartools.PlanarTools.MODID;

public class CollectionsConfig {
  public static final String COLLECTIONS_FOLDER = "config/" + MODID + "/collections";
  public static final String DEEPSLATE_EXAMPLE_TEMPLATE = """
      // This is a collection, it can be referenced in your blocks.toml using its file path after the collections folder
      // For example this collection can be referenced with ["@example/deepslate"] because it is [config/planar_tools/collections/example/deepslate.txt]
      // File extensions (e.g., .txt, .cfg, .toml) can be anything. It doesn't matter.
      
      minecraft:deepslate
      $minecraft:cobbled_deepslate
      $minecraft:polished_deepslate
      $minecraft:deepslate_bricks
      $minecraft:deepslate_tiles
      #forge:ores_in_ground/deepslate
      """;

  public static final String WOOD_EXAMPLE_TEMPLATE = """
      // This is a collection, it can be referenced in your blocks.toml using its file path after the collections folder
      // For example this collection can be referenced with ["@wood"] because it is [config/planar_tools/collections/wood.txt]
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

  public static final Map<String, List<String>> MAP = new HashMap<>();

  public static void parseAndPopulate() {
      if (new File(COLLECTIONS_FOLDER).mkdirs()) {
        //noinspection ResultOfMethodCallIgnored
        new File(COLLECTIONS_FOLDER + "/example").mkdirs();
        try (FileWriter fileWriter = new FileWriter(COLLECTIONS_FOLDER + "/example/deepslate.txt")) {
          fileWriter.write(DEEPSLATE_EXAMPLE_TEMPLATE);
          MAP.put("example/deepslate", List.of(
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
        try (FileWriter fileWriter = new FileWriter(COLLECTIONS_FOLDER + "/wood.txt")) {
          fileWriter.write(WOOD_EXAMPLE_TEMPLATE);
          MAP.put("wood", List.of(
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
              )
          );
        } catch (Exception e) {
          LOGGER.error(e.getMessage());
        }
      } else {
        populateMapFromFilesRecursively(COLLECTIONS_FOLDER);
      }
  }

  @SuppressWarnings({"DataFlowIssue"})
  private static void populateMapFromFilesRecursively(String fileName){
    File currentFile = new File(fileName);
    if (currentFile.isFile()){
      try {
        List<String> contents = Files.readAllLines(currentFile.toPath()).stream().filter(s -> !(s.startsWith("/") || s.isEmpty())).toList();
        MAP.put(removeExtension(fileName.substring(COLLECTIONS_FOLDER.length() + 1)), contents);
      } catch (Exception e) {
        LOGGER.error(e.getMessage());
      }
    } else {
      for (File nextFile : currentFile.listFiles()) {
        populateMapFromFilesRecursively(fileName + "/" + nextFile.getName());
      }
    }
  }

  public static String removeExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
  }
}
