package com.dreu.planarcms.config;

import com.electronwill.nightconfig.core.Config;

import java.nio.file.Path;

import static com.dreu.planarcms.PlanarCMS.MODID;
import static com.dreu.planarcms.util.Helpers.LogLevel.WARN;
import static com.dreu.planarcms.util.Helpers.addConfigIssue;
import static com.dreu.planarcms.util.Helpers.parseFileOrDefault;

public class DisplayConfig {
  public static final String fileName = "config/" + MODID + "/display.toml";

  static final String DEFAULT_CONFIG_STRING = """
      ConfigVersion = 1
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
  private static boolean advancedWailaKeyActive;
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
    resetAdvancedWailaKey();
    configHasBeenPopulated = true;
  }

  public static boolean isAdvancedWaila() {
    return ADVANCED_WAILA || (GeneralConfig.ENABLE_ADVANCED_WAILA_KEYBIND && advancedWailaKeyActive);
  }

  public static void updateAdvancedWailaKey(boolean pressed) {
    if (ADVANCED_WAILA || !GeneralConfig.ENABLE_ADVANCED_WAILA_KEYBIND) {
      resetAdvancedWailaKey();
    } else if (GeneralConfig.ADVANCED_WAILA_KEY_MODE == GeneralConfig.AdvancedWailaKeyMode.HOLD) {
      advancedWailaKeyActive = pressed;
    } else if (pressed) {
      advancedWailaKeyActive = !advancedWailaKeyActive;
    }
  }

  public static void resetAdvancedWailaKey() {
    advancedWailaKeyActive = false;
  }

  public static boolean save(
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
    return ConfigUpgrades.save(Path.of(fileName),
          "ConfigVersion = 1\n" +
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
          "ShowUnmineable = " + showUnmineable + "\n", 1);
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
