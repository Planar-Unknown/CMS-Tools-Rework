package com.dreu.planarcms.config;

import com.electronwill.nightconfig.core.Config;

import java.io.FileWriter;

import static com.dreu.planarcms.PlanarCMS.MODID;
import static com.dreu.planarcms.util.Helpers.LogLevel.ERROR;
import static com.dreu.planarcms.util.Helpers.LogLevel.WARN;
import static com.dreu.planarcms.util.Helpers.addConfigIssue;
import static com.dreu.planarcms.util.Helpers.parseFileOrDefault;

public class DisplayConfig {
  public static final String fileName = "config/" + MODID + "/display.toml";

  static final String DEFAULT_CONFIG_STRING = """
      # Client-side display settings for the manual Planar Tools overlay and Jade integration.
      # Jade integration is automatic when Jade is installed. These options control which Planar rows are shown.
      
      # Manual overlay only. Jade already shows the target name and owning mod.
      ShowUnconfiguredBlocks = true
      ShowBlockName = true
      ShowModName = true
      
      # Planar block property rows.
      AdvancedWaila = false
      ShowHardness = true
      ShowExplosionResistance = true
      ShowDefaultResistance = true
      ShowDrops = true
      ShowToolRequirements = true
      ShowHeldToolPower = true
      ShowMiningSpeed = true
      ShowUnmineable = true
      """;

  public static Config CONFIG;
  private static boolean configHasBeenPopulated;

  public static boolean SHOW_UNCONFIGURED_BLOCKS;
  public static boolean SHOW_BLOCK_NAME;
  public static boolean SHOW_MOD_NAME;
  public static boolean ADVANCED_WAILA;
  public static boolean SHOW_HARDNESS;
  public static boolean SHOW_EXPLOSION_RESISTANCE;
  public static boolean SHOW_DEFAULT_RESISTANCE;
  public static boolean SHOW_DROPS;
  public static boolean SHOW_TOOL_REQUIREMENTS;
  public static boolean SHOW_HELD_TOOL_POWER;
  public static boolean SHOW_MINING_SPEED;
  public static boolean SHOW_UNMINEABLE;

  public static void ensureLoaded() {
    if (!configHasBeenPopulated) {
      parse();
      populate();
    }
  }

  public static void parse() {
    CONFIG = parseFileOrDefault(fileName, DEFAULT_CONFIG_STRING);
  }

  public static void populate() {
    SHOW_UNCONFIGURED_BLOCKS = getOrDefault("ShowUnconfiguredBlocks", true);
    SHOW_BLOCK_NAME = getOrDefault("ShowBlockName", true);
    SHOW_MOD_NAME = getOrDefault("ShowModName", true);
    ADVANCED_WAILA = getOrDefault("AdvancedWaila", false);
    SHOW_HARDNESS = getOrDefault("ShowHardness", true);
    SHOW_EXPLOSION_RESISTANCE = getOrDefault("ShowExplosionResistance", true);
    SHOW_DEFAULT_RESISTANCE = getOrDefault("ShowDefaultResistance", true);
    SHOW_DROPS = getOrDefault("ShowDrops", true);
    SHOW_TOOL_REQUIREMENTS = getOrDefault("ShowToolRequirements", true);
    SHOW_HELD_TOOL_POWER = getOrDefault("ShowHeldToolPower", true);
    SHOW_MINING_SPEED = getOrDefault("ShowMiningSpeed", true);
    SHOW_UNMINEABLE = getOrDefault("ShowUnmineable", true);
    configHasBeenPopulated = true;
  }

  public static void save(
      boolean showUnconfiguredBlocks,
      boolean ShowBlockName,
      boolean ShowModName,
      boolean advancedWaila,
      boolean showHardness,
      boolean showExplosionResistance,
      boolean showDefaultResistance,
      boolean showDrops,
      boolean showToolRequirements,
      boolean showHeldToolPower,
      boolean showMiningSpeed,
      boolean showUnmineable
  ) {
    try (FileWriter writer = new FileWriter(fileName)) {
      writer.write(
          "# Client-side display settings for the manual Planar Tools overlay and Jade integration.\n" +
          "# Jade integration is automatic when Jade is installed. These options control which Planar rows are shown.\n\n" +
          "# Manual overlay only. Jade already shows the target name and owning mod.\n" +
          "ShowUnconfiguredBlocks = " + showUnconfiguredBlocks + "\n" +
          "ShowBlockName = " + ShowBlockName + "\n" +
          "ShowModName = " + ShowModName + "\n\n" +
          "# Planar block property rows.\n" +
          "AdvancedWaila = " + advancedWaila + "\n" +
          "ShowHardness = " + showHardness + "\n" +
          "ShowExplosionResistance = " + showExplosionResistance + "\n" +
          "ShowDefaultResistance = " + showDefaultResistance + "\n" +
          "ShowDrops = " + showDrops + "\n" +
          "ShowToolRequirements = " + showToolRequirements + "\n" +
          "ShowHeldToolPower = " + showHeldToolPower + "\n" +
          "ShowMiningSpeed = " + showMiningSpeed + "\n" +
          "ShowUnmineable = " + showUnmineable + "\n"
      );
    } catch (Exception e) {
      addConfigIssue(ERROR, (byte) 5, "Encountered exception while saving config file [{}] | Exception: {}", fileName, e.getMessage());
    }
  }

  private static boolean getOrDefault(String key, boolean fallback) {
    Object value = CONFIG.get(key);
    if (value == null) return fallback;
    if (value instanceof Boolean bool) return bool;
    addConfigIssue(WARN, (byte) 4,
        "Value: \"{}\" for \"{}\" is an invalid type in config [{}] | Expected: 'Boolean' but got: '{}' | Using default...",
        value, key, fileName, value.getClass().getSimpleName());
    return fallback;
  }
}
