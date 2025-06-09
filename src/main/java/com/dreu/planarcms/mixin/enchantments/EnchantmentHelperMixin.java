package com.dreu.planarcms.mixin.enchantments;

import com.dreu.planarcms.config.ToolsConfig;
import com.dreu.planarcms.util.OpposingSets;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.TreeMap;

import static com.dreu.planarcms.config.EnchantsConfig.ENCHANTS_BY_ITEM_ID;
import static com.dreu.planarcms.config.EnchantsConfig.ENCHANTS_BY_TOOL_TYPE;
import static com.dreu.planarcms.config.ToolsConfig.TOOLS;

@SuppressWarnings({"DataFlowIssue", "unused"})
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

  @Redirect(
      method = "getAvailableEnchantmentResults",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/enchantment/Enchantment;canApplyAtEnchantingTable(Lnet/minecraft/world/item/ItemStack;)Z",
          remap = false
      )
  )
  private static boolean redirectCanApplyAtEnchantingTable(Enchantment enchantment, ItemStack itemStack, int levelCost, ItemStack originalStack, boolean allowTreasure) {
    String itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
    String enchantId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment).toString();
    ToolsConfig.Properties toolProperties = TOOLS.get(itemId);

    OpposingSets<String> byItem = ENCHANTS_BY_ITEM_ID.getOrDefault(itemId, new OpposingSets<>());
    if (byItem != null && !byItem.isEmpty()) {
      if (toolProperties != null) {
        for (Map.Entry<Byte, Integer> toolTypeData : toolProperties.powers().entrySet()) {
          TreeMap<Integer, OpposingSets<String>> treeMap = ENCHANTS_BY_TOOL_TYPE.get(toolTypeData.getKey());
          if (treeMap != null && !treeMap.isEmpty()) {
            for (OpposingSets<String> opposingSets : treeMap.descendingMap().tailMap(toolTypeData.getValue()).values()) {
              if (opposingSets.negative().contains(enchantId)) {
                if (!byItem.positive().contains(enchantId))
                  return false;
              }
              if (opposingSets.positive().contains(enchantId)) {
                if (!byItem.negative().contains(enchantId))
                  return true;
              }
            }
          }
        }
      }
      if (byItem.negative().contains(enchantId)) return false;
      if (byItem.positive().contains(enchantId)) return true;
    } else if (toolProperties != null) {
      for (Map.Entry<Byte, Integer> toolTypeData : toolProperties.powers().entrySet()) {
        TreeMap<Integer, OpposingSets<String>> treeMap = ENCHANTS_BY_TOOL_TYPE.get(toolTypeData.getKey());
        if (treeMap != null && !treeMap.isEmpty()) {
          for (OpposingSets<String> opposingSets : treeMap.descendingMap().tailMap(toolTypeData.getValue()).values()) {
            if (opposingSets.negative().contains(enchantId))
              return false;
            if (opposingSets.positive().contains(enchantId))
              return true;
          }
        }
      }
    }
    return enchantment.canApplyAtEnchantingTable(itemStack);
  }

  @Redirect(
      method = "getEnchantmentCost",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/ItemStack;getEnchantmentValue()I",
          remap = false
      )
  )
  private static int getEnchantmentCostRedirect(ItemStack itemStack) {
    return itemStack.getEnchantmentValue() == 0 ? 10 : itemStack.getEnchantmentValue();
  }

  @Redirect(
      method = "selectEnchantment",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/ItemStack;getEnchantmentValue()I",
          remap = false
      )
  )
  private static int selectEnchantmentRedirect(ItemStack itemStack) {
    return itemStack.getEnchantmentValue() == 0 ? 10 : itemStack.getEnchantmentValue();
  }
}

