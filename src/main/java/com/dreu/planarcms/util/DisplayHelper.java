package com.dreu.planarcms.util;

import com.dreu.planarcms.config.BlocksConfig;
import com.dreu.planarcms.config.PowerProfiles;
import com.dreu.planarcms.config.ToolsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

import static com.dreu.planarcms.config.BlocksConfig.getBlockProperties;
import static com.dreu.planarcms.config.DisplayConfig.*;
import static com.dreu.planarcms.config.ToolsConfig.*;

@SuppressWarnings("DataFlowIssue")
public class DisplayHelper {
  private static final String PROPERTY_SEPARATOR = "   ";
  private static final String DEFAULT_LABEL = "Default: ";
  private static final String HELD_LABEL = "Held: ";
  private static final String MINING_SPEED_LABEL = "MiningSpeed";
  private static final String DROPS_LABEL = "Drops";
  private static final String UNMINEABLE_LABEL = "Unmineable";

  public static List<MutableComponent> getManualWailaComponents(BlockState blockState, BlockGetter level, BlockPos blockPos, ItemStack heldStack) {
    ensureLoaded();

    BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
    if (blockProperties == null && !SHOW_UNCONFIGURED_BLOCKS) return List.of();

    List<MutableComponent> tooltipRows = new ArrayList<>();
    if (SHOW_BLOCK_NAME) tooltipRows.add(blockState.getBlock().getName());
    addPlanarRows(tooltipRows, blockState, level, blockPos, heldStack, blockProperties);
    if (SHOW_MOD_NAME) tooltipRows.add(getModName(blockState.getBlock()));
    return tooltipRows;
  }

  public static List<MutableComponent> getJadeComponents(BlockState blockState, BlockGetter level, BlockPos blockPos, ItemStack heldStack) {
    ensureLoaded();
    List<MutableComponent> rows = new ArrayList<>();
    addPlanarRows(rows, blockState, level, blockPos, heldStack, getBlockProperties(blockState.getBlock()));
    return rows;
  }

  private static void addPlanarRows(List<MutableComponent> rows, BlockState blockState, BlockGetter level, BlockPos blockPos, ItemStack heldStack, BlocksConfig.Properties blockProperties) {
    ToolsConfig.Properties toolProperties = heldStack.isEmpty() ? null : getToolProperties(heldStack.getItem());
    HeldToolStatus heldToolStatus = getHeldToolStatus(blockState, level, blockPos, heldStack, blockProperties, toolProperties);

    boolean showDefaultResistance = blockProperties != null && SHOW_DEFAULT_RESISTANCE && blockProperties.defaultResistance() > 0;
    boolean showToolStatus = SHOW_MINING_SPEED || SHOW_DROPS;
    boolean showDefaultToolStatus = showToolStatus && heldToolStatus.displaysDefault();
    if (showDefaultResistance || showDefaultToolStatus) {
      MutableComponent defaultRow = Component.literal("");
      if (showDefaultResistance) {
        defaultRow.append(Component.literal(DEFAULT_LABEL).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(formatResistance(blockProperties.defaultResistance())).withStyle(getDefaultResistanceColor(blockProperties.defaultResistance(), heldToolStatus)));
      }
      if (showDefaultToolStatus)
        appendHeldToolStatus(defaultRow, heldToolStatus, showDefaultResistance);
      rows.add(defaultRow);
    }

    if (showToolStatus && !heldToolStatus.displaysDefault() && !SHOW_TOOL_REQUIREMENTS) {
      MutableComponent statusRow = Component.literal("");
      appendHeldToolStatus(statusRow, heldToolStatus, false);
      rows.add(statusRow);
    }

    if (blockProperties != null) addToolRows(rows, blockProperties, toolProperties, heldToolStatus);

    MutableComponent physicalRow = getPhysicalRow(blockState, level, blockPos, blockProperties, heldToolStatus);
    if (physicalRow != null) rows.add(physicalRow);
  }

  private static void addToolRows(List<MutableComponent> rows, BlocksConfig.Properties block, ToolsConfig.Properties tool, HeldToolStatus status) {
    if (!SHOW_TOOL_REQUIREMENTS || block.data().isEmpty()) return;
    for (int index = 0; index < REGISTERED_TOOL_TYPES.size(); index++) {
      byte type = (byte) index;
      PowerProfiles profiles = block.data().get(type);
      if (profiles == null) continue;
      Integer power = tool == null ? null : tool.powers().get(type);
      BlocksConfig.ToolProfile profile = power == null ? profiles.baseline() : profiles.atPower(power);
      MutableComponent row = getToolRow(type, profile, block, power, status);
      rows.add(row);
      if (power == null && showAdvanceWaila())
        addPowerChangeRows(rows, profiles, block.defaultCanDrop(), ChatFormatting.YELLOW);
    }
  }

  public static MutableComponent getBlockToolTooltip(byte type, PowerProfiles profiles, boolean defaultCanDrop) {
    int index = Byte.toUnsignedInt(type);
    MutableComponent row = Component.literal(" ")
        .append(Component.literal(REGISTERED_TOOL_TYPES.get(index)))
        .withStyle(style -> style.withColor(REGISTERED_TOOL_COLORS.get(index)))
        .append(Component.literal(": " + profiles.baseline().resistance()));
    if (profiles.size() > 0 || profiles.baseline().miningSpeedBonus() != 0)
      appendProfileProperties(row, profiles.baseline(), defaultCanDrop, ChatFormatting.GREEN);
    return row;
  }

  public static List<MutableComponent> getPowerChangeRows(PowerProfiles profiles, boolean defaultCanDrop) {
    if (profiles.size() == 0) return List.of();
    List<MutableComponent> rows = new ArrayList<>(profiles.size());
    addPowerChangeRows(rows, profiles, defaultCanDrop, ChatFormatting.GREEN);
    return rows;
  }

  private static void addPowerChangeRows(List<MutableComponent> rows, PowerProfiles profiles, boolean defaultCanDrop, ChatFormatting enabledColor) {
    BlocksConfig.ToolProfile previous = profiles.baseline();
    // These ordered, coalesced snapshots are the immutable summary data, not raw declarations.
    for (int i = 0; i < profiles.size(); i++) {
      BlocksConfig.ToolProfile profile = profiles.profile(i);
      MutableComponent changes = Component.literal("");
      if (profile.resistance() != previous.resistance())
        changes.append(Component.literal(PROPERTY_SEPARATOR + "Resistance: " + formatResistance(profile.resistance())).withStyle(ChatFormatting.GRAY));
      if (SHOW_MINING_SPEED && (profile.applyMiningSpeed() != previous.applyMiningSpeed() || profile.miningSpeedBonus() != previous.miningSpeedBonus())) {
        changes.append(Component.literal(PROPERTY_SEPARATOR));
        appendMiningSpeed(changes, profile.applyMiningSpeed(), profile.miningSpeedBonus(), profile.miningSpeedBonus() != previous.miningSpeedBonus(), enabledColor);
      }
      if (SHOW_DROPS && !profile.canDrop().equals(previous.canDrop()))
        appendProfileDrops(changes, profile, defaultCanDrop, enabledColor);
      if (!changes.getSiblings().isEmpty())
        rows.add(Component.literal("  Power " + profiles.power(i) + "+:").withStyle(ChatFormatting.GRAY).append(changes));
      previous = profile;
    }
  }

  private static void appendProfileProperties(MutableComponent row, BlocksConfig.ToolProfile profile, boolean defaultCanDrop, ChatFormatting enabledColor) {
    if (SHOW_MINING_SPEED) {
      row.append(Component.literal(PROPERTY_SEPARATOR));
      appendMiningSpeed(row, profile.applyMiningSpeed(), profile.miningSpeedBonus(), false, enabledColor);
    }
    if (SHOW_DROPS) appendProfileDrops(row, profile, defaultCanDrop, enabledColor);
  }

  private static void appendProfileDrops(MutableComponent row, BlocksConfig.ToolProfile profile, boolean defaultCanDrop, ChatFormatting enabledColor) {
    boolean canDrop = profile.canDrop().orElse(defaultCanDrop);
    row.append(Component.literal(PROPERTY_SEPARATOR + DROPS_LABEL).withStyle(canDrop ? enabledColor : ChatFormatting.RED));
  }

  private static MutableComponent getToolRow(byte toolType, BlocksConfig.ToolProfile toolProfile, BlocksConfig.Properties blockProperties, Integer heldPower, HeldToolStatus heldToolStatus) {
    int index = Byte.toUnsignedInt(toolType);
    MutableComponent row = Component.literal("")
        .append(Component.literal(REGISTERED_TOOL_TYPES.get(index))
            .withStyle(style -> style.withColor(REGISTERED_TOOL_COLORS.get(index))))
        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
        .append(Component.literal(formatResistance(toolProfile.resistance())).withStyle(getToolStatusColor(toolProfile, heldPower)));

    if (showAdvanceWaila() && SHOW_HELD_TOOL_POWER && heldPower != null) {
      row.append(Component.literal(PROPERTY_SEPARATOR + HELD_LABEL).withStyle(ChatFormatting.GRAY))
          .append(Component.literal(String.valueOf(heldPower)).withStyle(heldPower >= toolProfile.resistance() && toolProfile.resistance() >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    if (!showAdvanceWaila() && !heldToolStatus.displaysDefault() && heldToolStatus.displayedToolType() == toolType) {
      appendHeldToolStatus(row, heldToolStatus);
    }

    if (showAdvanceWaila()) {
      boolean qualified = heldToolStatus.canMine() && heldPower != null && toolProfile.resistance() >= 0 && heldPower >= toolProfile.resistance();
      appendProfileProperties(row, toolProfile, blockProperties.defaultCanDrop(), qualified ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
    }

    return row;
  }

  private static void appendHeldToolStatus(MutableComponent row, HeldToolStatus status) {
    appendHeldToolStatus(row, status, true);
  }

  private static void appendHeldToolStatus(MutableComponent row, HeldToolStatus status, boolean prependSeparator) {
    boolean appended = prependSeparator;

    if (SHOW_MINING_SPEED) {
      if (appended) row.append(Component.literal(PROPERTY_SEPARATOR));
      appendMiningSpeed(row, status.applyMiningSpeed(), status.miningSpeedBonus(), false,
          status.applyMiningSpeed() ? ChatFormatting.GREEN : ChatFormatting.RED);
      appended = true;
    }

    if (SHOW_DROPS) {
      if (appended) row.append(Component.literal(PROPERTY_SEPARATOR));
      row.append(Component.literal(DROPS_LABEL).withStyle(getCanDropFormatting(status.canDrop())));
    }
  }

  private static HeldToolStatus getHeldToolStatus(BlockState blockState, BlockGetter level, BlockPos blockPos, ItemStack heldStack, BlocksConfig.Properties blockProperties, ToolsConfig.Properties toolProperties) {
    float actualSpeed = heldStack.getDestroySpeed(blockState);
    boolean physicallyMineable = blockState.getDestroySpeed(level, blockPos) >= 0 && actualSpeed > 0;
    if (blockProperties == null) {
      boolean canMine = physicallyMineable;
      boolean hasCorrectToolForDrops = canMine && hasCorrectToolForDrops(blockState, heldStack);
      boolean applyMiningSpeed = canMine && actualSpeed > 1.0F;
      return new HeldToolStatus(canMine, applyMiningSpeed, hasCorrectToolForDrops, (byte) -1, true, 0);
    }

    boolean canMine = blockProperties.defaultResistance() == 0;
    boolean canDrop = blockProperties.defaultCanDrop();
    boolean toolAllowsDrops = false;
    boolean displaysDefault = true;
    byte displayedToolType = -1;
    int strongestSuccessfulPower = Integer.MIN_VALUE;
    float miningSpeedBonus = 0;
    boolean hasQualifiedBonus = false;

    if (toolProperties != null) {
      for (var heldPower : toolProperties.powers().entrySet()) {
        BlocksConfig.ToolProfile toolProfile = blockProperties.profileFor(heldPower.getKey(), heldPower.getValue());
        if (toolProfile != null) {
          int resistance = toolProfile.resistance();
          if (resistance >= 0) {
            if (heldPower.getValue() >= resistance) {
              canMine = true;
              miningSpeedBonus = hasQualifiedBonus ? Math.max(miningSpeedBonus, toolProfile.miningSpeedBonus()) : toolProfile.miningSpeedBonus();
              hasQualifiedBonus = true;
              if (heldPower.getValue() > strongestSuccessfulPower
                  || heldPower.getValue() == strongestSuccessfulPower
                  && Byte.toUnsignedInt(heldPower.getKey()) < Byte.toUnsignedInt(displayedToolType)) {
                strongestSuccessfulPower = heldPower.getValue();
                displayedToolType = heldPower.getKey();
              }
              displaysDefault = false;
              if (toolProfile.canDrop().isPresent()) {
                if (toolProfile.canDrop().get()) {
                  canDrop = true;
                  toolAllowsDrops = true;
                } else if (!toolAllowsDrops) {
                  canDrop = false;
                }
              }
            } else if (heldPower.getValue() >= blockProperties.defaultResistance() && blockProperties.defaultResistance() != -1) {
              canMine = true;
              if (strongestSuccessfulPower == Integer.MIN_VALUE)
                displaysDefault = true;
            }
          }
        } else {
          int defaultResistance = blockProperties.defaultResistance();
          if (defaultResistance != -1 && heldPower.getValue() >= defaultResistance) {
            canMine = true;
            if (strongestSuccessfulPower == Integer.MIN_VALUE)
              displaysDefault = true;
          }
        }
      }
    }

    canMine &= physicallyMineable;
    boolean applyMiningSpeed = canMine && actualSpeed > 1.0F;
    canDrop &= hasCorrectToolForDrops(blockState, heldStack);

    return new HeldToolStatus(canMine, applyMiningSpeed, canMine && canDrop, displayedToolType, displaysDefault, miningSpeedBonus);
  }

  private static boolean hasCorrectToolForDrops(BlockState blockState, ItemStack heldStack) {
    return !blockState.requiresCorrectToolForDrops() || heldStack.isCorrectToolForDrops(blockState);
  }

  private static MutableComponent getPhysicalRow(BlockState blockState, BlockGetter level, BlockPos blockPos, BlocksConfig.Properties blockProperties, HeldToolStatus heldToolStatus) {
    if (!SHOW_UNMINEABLE && !SHOW_HARDNESS && !SHOW_EXPLOSION_RESISTANCE) return null;

    MutableComponent physicalRow = Component.literal("");
    boolean appended = false;

    if (SHOW_UNMINEABLE && !heldToolStatus.canMine()) {
      physicalRow.append(Component.literal(UNMINEABLE_LABEL).withStyle(ChatFormatting.RED));
      appended = true;
    }

    if (SHOW_EXPLOSION_RESISTANCE) {
      if (appended) physicalRow.append(Component.literal(PROPERTY_SEPARATOR));
      float explosionResistance = blockProperties != null && blockProperties.explosionResistance().isPresent()
          ? blockProperties.explosionResistance().get()
          : blockProperties != null && blockProperties.defaultResistance() == -1
              ? -1
          : blockState.getBlock().getExplosionResistance();
      if (explosionResistance == -1 || explosionResistance == Float.POSITIVE_INFINITY) {
        physicalRow.append(Component.literal("Blast Immune").withStyle(ChatFormatting.RED));
      } else {
        physicalRow.append(Component.literal("Blast: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(formatFloat(explosionResistance)).withStyle(ChatFormatting.WHITE));
      }
      appended = true;
    }

    if (SHOW_HARDNESS) {
      float hardness = blockProperties != null && blockProperties.hardness().isPresent()
          ? blockProperties.hardness().get()
          : blockState.getDestroySpeed(level, blockPos);
      if (showAdvanceWaila() || !(hardness == -1.0F && SHOW_UNMINEABLE && !heldToolStatus.canMine())) {
        if (appended) physicalRow.append(Component.literal(PROPERTY_SEPARATOR));
        physicalRow.append(Component.literal("Hardness: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(formatFloat(hardness)).withStyle(ChatFormatting.WHITE));
        appended = true;
      }
    }

    return appended ? physicalRow : null;
  }

  private static ChatFormatting getToolStatusColor(BlocksConfig.ToolProfile toolProfile, Integer heldPower) {
    if (heldPower == null) return ChatFormatting.GRAY;
    if (toolProfile.resistance() < 0 || heldPower < toolProfile.resistance()) return ChatFormatting.RED;
    return toolProfile.applyMiningSpeed() || toolProfile.miningSpeedBonus() > 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
  }

  private static ChatFormatting getDefaultResistanceColor(int resistance, HeldToolStatus heldToolStatus) {
    if (!heldToolStatus.canMine()) return ChatFormatting.RED;
    if (resistance <= 0) return ChatFormatting.GREEN;
    return ChatFormatting.YELLOW;
  }

  private static ChatFormatting getCanDropFormatting(boolean value) {
    return value ? ChatFormatting.GREEN : ChatFormatting.RED;
  }

  private static void appendMiningSpeed(MutableComponent row, boolean applyMiningSpeed, float bonus, boolean showZero, ChatFormatting enabledColor) {
    row.append(Component.literal(MINING_SPEED_LABEL).withStyle(applyMiningSpeed || bonus > 0 ? enabledColor : ChatFormatting.RED));
    if (bonus != 0 || showZero)
      row.append(Component.literal((bonus < 0 ? " " : " +") + formatFloat(bonus)).withStyle(ChatFormatting.WHITE));
  }

  private static String formatResistance(int resistance) {
    return resistance < 0 ? "unmineable" : String.valueOf(resistance);
  }

  private static String formatFloat(float value) {
    return value == (long) value ? String.valueOf((long) value) : String.valueOf(value);
  }

  private static MutableComponent getModName(Block block) {
    String modId = ForgeRegistries.BLOCKS.getKey(block).getNamespace();
    return Component.literal(ModList.get().getModContainerById(modId)
        .map(mod -> mod.getModInfo().getDisplayName())
        .orElse(modId)
    ).withStyle(ChatFormatting.ITALIC, ChatFormatting.BLUE);
  }

  private record HeldToolStatus(boolean canMine, boolean applyMiningSpeed, boolean canDrop, byte displayedToolType, boolean displaysDefault, float miningSpeedBonus) {}
}
