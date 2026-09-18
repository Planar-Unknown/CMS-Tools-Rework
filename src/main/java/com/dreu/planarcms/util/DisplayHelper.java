package com.dreu.planarcms.util;

import com.dreu.planarcms.config.BlocksConfig;
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

    List<MutableComponent> planarRows = getPlanarRows(blockState, level, blockPos, heldStack);
    if (planarRows.isEmpty() && !SHOW_UNCONFIGURED_BLOCKS) return List.of();

    List<MutableComponent> tooltipRows = new ArrayList<>();
    if (SHOW_BLOCK_NAME) tooltipRows.add(blockState.getBlock().getName());
    tooltipRows.addAll(planarRows);
    if (SHOW_MOD_NAME) tooltipRows.add(getModName(blockState.getBlock()));
    return tooltipRows;
  }

  public static List<MutableComponent> getJadeComponents(BlockState blockState, BlockGetter level, BlockPos blockPos, ItemStack heldStack) {
    ensureLoaded();
    return getPlanarRows(blockState, level, blockPos, heldStack);
  }

  private static List<MutableComponent> getPlanarRows(BlockState blockState, BlockGetter level, BlockPos blockPos, ItemStack heldStack) {
    Block block = blockState.getBlock();
    List<MutableComponent> planarRows = new ArrayList<>();
    BlocksConfig.Properties blockProperties = getBlockProperties(block);
    ToolsConfig.Properties toolProperties = heldStack.isEmpty() ? null : getToolProperties(heldStack.getItem());
    HeldToolStatus heldToolStatus = getHeldToolStatus(blockState, level, blockPos, heldStack, blockProperties, toolProperties);

    boolean showDefaultResistance = blockProperties != null && SHOW_DEFAULT_RESISTANCE && blockProperties.defaultResistance() > 0;
    boolean showDefaultToolStatus = blockProperties != null && !ADVANCED_WAILA && heldToolStatus.displaysDefault() && (SHOW_MINING_SPEED || SHOW_DROPS);
    if (showDefaultResistance || showDefaultToolStatus) {
      MutableComponent defaultRow = Component.literal("");
      if (showDefaultResistance) {
        defaultRow.append(Component.literal(DEFAULT_LABEL).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(formatResistance(blockProperties.defaultResistance())).withStyle(getDefaultResistanceColor(blockProperties.defaultResistance(), heldToolStatus)));
      }
      if (showDefaultToolStatus)
        appendHeldToolStatus(defaultRow, heldToolStatus, showDefaultResistance);
      planarRows.add(defaultRow);
    }

    if (blockProperties != null && SHOW_TOOL_REQUIREMENTS && !blockProperties.data().isEmpty()) {
      for (byte toolType = 0; toolType < REGISTERED_TOOL_TYPES.size(); toolType++) {
        BlocksConfig.ToolProfile toolProfile = blockProperties.data().get(toolType);
        if (toolProfile != null) planarRows.add(getToolRow(toolType, toolProfile, blockProperties, toolProperties, heldToolStatus));
      }
    }

    MutableComponent heldToolRow = getHeldToolRow(heldToolStatus, blockProperties, toolProperties);
    if (heldToolRow != null && (blockProperties == null || blockProperties.data().isEmpty())) planarRows.add(heldToolRow);

    MutableComponent physicalRow = getPhysicalRow(blockState, level, blockPos, blockProperties, heldToolStatus);
    if (physicalRow != null) planarRows.add(physicalRow);

    return planarRows;
  }

  private static MutableComponent getToolRow(byte toolType, BlocksConfig.ToolProfile toolProfile, BlocksConfig.Properties blockProperties, ToolsConfig.Properties toolProperties, HeldToolStatus heldToolStatus) {
    MutableComponent row = Component.literal("")
        .append(Component.literal(REGISTERED_TOOL_TYPES.get(toolType))
            .withStyle(style -> style.withColor(REGISTERED_TOOL_COLORS.get(toolType))))
        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
        .append(Component.literal(formatResistance(toolProfile.resistance())).withStyle(getToolStatusColor(toolType, toolProfile, toolProperties)));

    if (ADVANCED_WAILA && SHOW_HELD_TOOL_POWER && toolProperties != null && toolProperties.powers().containsKey(toolType)) {
      int heldPower = toolProperties.powers().get(toolType);
      row.append(Component.literal(PROPERTY_SEPARATOR + HELD_LABEL).withStyle(ChatFormatting.GRAY))
          .append(Component.literal(String.valueOf(heldPower)).withStyle(heldPower >= toolProfile.resistance() && toolProfile.resistance() >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    if (!ADVANCED_WAILA && heldToolStatus.displayedToolType() == toolType) {
      appendHeldToolStatus(row, heldToolStatus);
    }

    if (ADVANCED_WAILA && SHOW_MINING_SPEED) {
      row.append(Component.literal(PROPERTY_SEPARATOR + MINING_SPEED_LABEL).withStyle(toolProfile.applyMiningSpeed() ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    if (ADVANCED_WAILA && SHOW_DROPS) {
      row.append(Component.literal(PROPERTY_SEPARATOR + DROPS_LABEL).withStyle(getCanDropFormatting(toolProfile.canDrop().orElse(blockProperties.defaultCanDrop()))));
      if (toolProfile.canDrop().isEmpty())
        row.append(Component.literal(" default").withStyle(ChatFormatting.GRAY));
    }

    return row;
  }

  private static MutableComponent getHeldToolRow(HeldToolStatus heldToolStatus, BlocksConfig.Properties blockProperties, ToolsConfig.Properties toolProperties) {
    if (blockProperties != null && toolProperties != null) return null;

    if (!SHOW_MINING_SPEED && !SHOW_DROPS) {
      return SHOW_UNMINEABLE && !heldToolStatus.canMine()
          ? Component.literal(UNMINEABLE_LABEL).withStyle(ChatFormatting.RED)
          : null;
    }

    MutableComponent row = Component.literal("");
    boolean appended = false;

    if (!heldToolStatus.canMine() && SHOW_UNMINEABLE) {
      row.append(Component.literal(UNMINEABLE_LABEL).withStyle(ChatFormatting.RED));
      appended = true;
    }

    if (SHOW_MINING_SPEED) {
      if (appended) row.append(Component.literal(PROPERTY_SEPARATOR));
      row.append(Component.literal(MINING_SPEED_LABEL).withStyle(heldToolStatus.applyMiningSpeed() ? ChatFormatting.GREEN : ChatFormatting.RED));
      appended = true;
    }

    if (SHOW_DROPS) {
      if (appended) row.append(Component.literal(PROPERTY_SEPARATOR));
      row.append(Component.literal(DROPS_LABEL).withStyle(getCanDropFormatting(heldToolStatus.canDrop())));
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
      row.append(Component.literal(MINING_SPEED_LABEL).withStyle(status.applyMiningSpeed() ? ChatFormatting.GREEN : ChatFormatting.RED));
      appended = true;
    }

    if (SHOW_DROPS) {
      if (appended) row.append(Component.literal(PROPERTY_SEPARATOR));
      row.append(Component.literal(DROPS_LABEL).withStyle(getCanDropFormatting(status.canDrop())));
    }
  }

  private static HeldToolStatus getHeldToolStatus(BlockState blockState, BlockGetter level, BlockPos blockPos, ItemStack heldStack, BlocksConfig.Properties blockProperties, ToolsConfig.Properties toolProperties) {
    if (blockProperties == null) {
      boolean canMine = blockState.getDestroySpeed(level, blockPos) != -1.0F;
      boolean hasCorrectToolForDrops = canMine && hasCorrectToolForDrops(blockState, heldStack);
      boolean applyMiningSpeed = canMine && heldStack.getDestroySpeed(blockState) > 1.0F;
      return new HeldToolStatus(canMine, applyMiningSpeed, hasCorrectToolForDrops, (byte) -1, true);
    }

    boolean canMine = blockProperties.defaultResistance() == 0;
    boolean applyMiningSpeed = false;
    boolean canDrop = blockProperties.defaultCanDrop();
    boolean toolAllowsDrops = false;
    boolean displaysDefault = true;
    byte displayedToolType = -1;
    int strongestSuccessfulPower = Integer.MIN_VALUE;
    int strongestRelevantPower = Integer.MIN_VALUE;

    if (toolProperties != null) {
      for (var heldPower : toolProperties.powers().entrySet()) {
        BlocksConfig.ToolProfile toolProfile = blockProperties.data().get(heldPower.getKey());
        if (toolProfile != null) {
          if (heldPower.getValue() > strongestRelevantPower) {
            strongestRelevantPower = heldPower.getValue();
            displayedToolType = heldPower.getKey();
          }
          int resistance = toolProfile.resistance();
          if (resistance >= 0) {
            if (heldPower.getValue() >= resistance) {
              canMine = true;
              if (heldPower.getValue() > strongestSuccessfulPower) {
                strongestSuccessfulPower = heldPower.getValue();
                displayedToolType = heldPower.getKey();
              }
              displaysDefault = false;
              if (toolProfile.applyMiningSpeed()) applyMiningSpeed = true;
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

    if (!canMine && displayedToolType != -1)
      displaysDefault = false;
    return new HeldToolStatus(canMine, applyMiningSpeed, canMine && canDrop, displayedToolType, displaysDefault);
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

    if (SHOW_HARDNESS) {
      if (appended) physicalRow.append(Component.literal(PROPERTY_SEPARATOR));
      float hardness = blockProperties != null && blockProperties.hardness().isPresent()
          ? blockProperties.hardness().get()
          : blockState.getDestroySpeed(level, blockPos);
      physicalRow.append(Component.literal("Hardness: ").withStyle(ChatFormatting.GRAY))
          .append(Component.literal(formatFloat(hardness)).withStyle(ChatFormatting.WHITE));
      appended = true;
    }

    if (SHOW_EXPLOSION_RESISTANCE) {
      if (appended) physicalRow.append(Component.literal(PROPERTY_SEPARATOR));
      float explosionResistance = blockProperties != null && blockProperties.explosionResistance().isPresent()
          ? blockProperties.explosionResistance().get()
          : blockState.getBlock().getExplosionResistance();
      physicalRow.append(Component.literal("Blast: ").withStyle(ChatFormatting.GRAY))
          .append(Component.literal(formatExplosionResistance(explosionResistance)).withStyle(ChatFormatting.WHITE));
      appended = true;
    }

    return appended ? physicalRow : null;
  }

  private static ChatFormatting getToolStatusColor(byte toolType, BlocksConfig.ToolProfile toolProfile, ToolsConfig.Properties toolProperties) {
    if (toolProperties == null || !toolProperties.powers().containsKey(toolType)) return ChatFormatting.GRAY;
    int heldPower = toolProperties.powers().get(toolType);
    if (toolProfile.resistance() < 0 || heldPower < toolProfile.resistance()) return ChatFormatting.RED;
    return toolProfile.applyMiningSpeed() ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
  }

  private static ChatFormatting getDefaultResistanceColor(int resistance, HeldToolStatus heldToolStatus) {
    if (!heldToolStatus.canMine()) return ChatFormatting.RED;
    if (resistance <= 0) return ChatFormatting.GREEN;
    return ChatFormatting.YELLOW;
  }

  private static ChatFormatting getCanDropFormatting(boolean value) {
    return value ? ChatFormatting.GREEN : ChatFormatting.RED;
  }

  private static String formatResistance(int resistance) {
    return resistance < 0 ? "unmineable" : String.valueOf(resistance);
  }

  private static String formatExplosionResistance(float resistance) {
    return resistance == -1 ? "unbreakable" : formatFloat(resistance);
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

  private record HeldToolStatus(boolean canMine, boolean applyMiningSpeed, boolean canDrop, byte displayedToolType, boolean displaysDefault) {}
}
