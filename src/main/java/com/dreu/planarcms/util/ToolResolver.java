package com.dreu.planarcms.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Collections;

import static com.dreu.planarcms.config.ToolsConfig.REGISTERED_TOOL_TYPES;
import static com.dreu.planarcms.config.ToolsConfig.getToolProperties;

public final class ToolResolver {
  private static final Tiers[] ANCHORS = {
      Tiers.WOOD, Tiers.GOLD, Tiers.STONE, Tiers.IRON, Tiers.DIAMOND, Tiers.NETHERITE
  };
  private static final int[] ANCHOR_POWERS = {20, 30, 40, 60, 80, 100};

  private ToolResolver() {}

  public static float getMiningSpeed(ItemStack stack, BlockState state) {
    if (stack.isEmpty()) return 1f;

    var item = stack.getItem();
    var configured = getToolProperties(item);
    if (configured != null) {
      return configured.miningSpeed().isPresent()
          ? configured.miningSpeed().get() : item.getDestroySpeed(stack, state);
    }
    if (item instanceof TieredItem tiered) {
      return tiered.getTier().getSpeed();
    }
    return item.getDestroySpeed(stack, state);
  }

  public static Map<Byte, Integer> getPowers(ItemStack stack) {
    if (stack.isEmpty()) return Map.of();
    var configured = getToolProperties(stack.getItem());
    if (configured != null) return configured.powers();

    List<Byte> types = getToolTypes(stack);
    if (types.isEmpty()) return Map.of();
    OptionalInt tierPower = stack.getItem() instanceof TieredItem tiered
        ? getTierPower(tiered.getTier()) : OptionalInt.empty();
    Map<Byte, Integer> powers = new LinkedHashMap<>();
    for (byte type : types) {
      if (REGISTERED_TOOL_TYPES.get(Byte.toUnsignedInt(type)).equals("Shears")) {
        powers.put(type, 100);
      } else if (tierPower.isPresent()) {
        powers.put(type, tierPower.getAsInt());
      }
    }
    return powers.isEmpty() ? Map.of() : Collections.unmodifiableMap(powers);
  }

  public static OptionalInt getTierPower(Tier tier) {
    if (tier == null) return OptionalInt.empty();
    for (int i = 0; i < ANCHORS.length; i++) {
      if (tier == ANCHORS[i]) return OptionalInt.of(ANCHOR_POWERS[i]);
    }

    List<Tier> sorted = TierSortingRegistry.getSortedTiers();
    int tierIndex = -1;
    int nextAnchor = 0;
    int lowerPower = 0;
    int upperPower = -1;
    int netheriteIndex = -1;
    for (int index = 0; index < sorted.size(); index++) {
      Tier current = sorted.get(index);
      if (current == tier) {
        if (tierIndex >= 0) return OptionalInt.empty();
        tierIndex = index;
      }
      if (current instanceof Tiers) {
        if (nextAnchor >= ANCHORS.length || current != ANCHORS[nextAnchor]) {
          return OptionalInt.empty();
        }
        int power = ANCHOR_POWERS[nextAnchor++];
        if (tierIndex < 0) lowerPower = power;
        else if (upperPower < 0) upperPower = power;
        if (current == Tiers.NETHERITE) netheriteIndex = index;
      }
    }
    if (tierIndex < 0 || nextAnchor != ANCHORS.length) return OptionalInt.empty();
    if (upperPower >= 0) return OptionalInt.of((lowerPower + upperPower) / 2);

    long power = 100L + 10L * (tierIndex - netheriteIndex);
    return OptionalInt.of((int) Math.min(Integer.MAX_VALUE, power));
  }

  public static List<Byte> getToolTypes(ItemStack stack) {
    if (stack.isEmpty()) return List.of();

    List<Byte> types = new ArrayList<>();
    for (int index = 0; index < REGISTERED_TOOL_TYPES.size(); index++) {
      ToolAction action = diggingAction(REGISTERED_TOOL_TYPES.get(index));
      if (action != null && stack.canPerformAction(action)) {
        types.add((byte) index);
      }
    }
    return List.copyOf(types);
  }

  private static ToolAction diggingAction(String toolType) {
    return switch (toolType) {
      case "Pickaxe" -> ToolActions.PICKAXE_DIG;
      case "Axe" -> ToolActions.AXE_DIG;
      case "Shovel" -> ToolActions.SHOVEL_DIG;
      case "Hoe" -> ToolActions.HOE_DIG;
      case "Sword" -> ToolActions.SWORD_DIG;
      case "Shears" -> ToolActions.SHEARS_DIG;
      default -> null;
    };
  }
}
