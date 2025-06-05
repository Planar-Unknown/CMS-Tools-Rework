package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.ToolsConfig;
import com.dreu.planartools.util.OpposingSets;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.TreeMap;

import static com.dreu.planartools.PlanarTools.TAG_KEYS_BY_TOOL_TYPE;
import static com.dreu.planartools.config.BlocksConfig.getBlockProperties;
import static com.dreu.planartools.config.EnchantsConfig.ENCHANTS_BY_ITEM_ID;
import static com.dreu.planartools.config.EnchantsConfig.ENCHANTS_BY_TOOL_TYPE;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;
import static com.dreu.planartools.config.ToolsConfig.getToolProperties;
import static com.dreu.planartools.util.Helpers.getTierIfPresent;

@SuppressWarnings({"unused", "DataFlowIssue"})
@Mixin(ItemStack.class)
public class ItemStackMixin {

  @Redirect(
      method = "isEnchantable",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/Item;isEnchantable(Lnet/minecraft/world/item/ItemStack;)Z"
      )
  )
  private boolean redirectItemIsEnchantable(Item item, ItemStack itemStack) {
    System.out.println("We are at: ItemStickyMixin yay!");
    String itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
    OpposingSets<String> byItem = ENCHANTS_BY_ITEM_ID.getOrDefault(itemId, new OpposingSets<>());
    if (!byItem.positive().isEmpty()) return true;

    ToolsConfig.Properties toolProperties = TOOLS.get(itemId);
    if (toolProperties != null) {
      for (Byte toolType : toolProperties.powers().keySet()) {
        TreeMap<Integer, OpposingSets<String>> treeMap = ENCHANTS_BY_TOOL_TYPE.get(toolType);
        if (treeMap != null && !treeMap.isEmpty()) {
          for (OpposingSets<String> enchantments : treeMap.descendingMap().values()) {
            if (!enchantments.positive().isEmpty()) {
              for (String enchant : enchantments.positive()) {
                if (!byItem.negative().contains(enchant)) return true;
              }
            }
          }
        }
      }
    }

    return item.isEnchantable(itemStack);
  }

  @Redirect(
      method = "getDestroySpeed",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/Item;getDestroySpeed(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)F"
      )
  )
  private float redirectGetDestroySpeed(Item item, ItemStack itemStack, BlockState blockState) {
    BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
    ToolsConfig.Properties toolProperties = getToolProperties(item);
    if (blockProperties != null) {
      boolean applyMiningSpeed = false;
      if (toolProperties != null) {
        boolean canMine = false;
        for (Map.Entry<Byte, Integer> powerData : toolProperties.powers().entrySet()) {
          BlocksConfig.ResistanceData resistanceData = blockProperties.data().get(powerData.getKey());
          if (resistanceData != null) {
            int resistance = resistanceData.resistance();
            if (resistance >= 0 && powerData.getValue() >= resistance) {
              canMine = true;
              if (resistanceData.applyMiningSpeed()) {
                applyMiningSpeed = true;
              }
            }
          } else {
            int defaultResistance = blockProperties.defaultResistance();
            if (defaultResistance != -1 && powerData.getValue() >= defaultResistance)
              canMine = true;
          }
        }
        if (canMine) {
          if (applyMiningSpeed) {
            if (toolProperties.miningSpeed().isPresent())
              return Float.valueOf(toolProperties.miningSpeed().get());
          } else
            return 1f;
        } else
          return 0f;
      } else {
        return blockProperties.defaultResistance() == 0 ? 1f : 0f;
      }
    } else if (toolProperties != null) {
      return isCorrectToolForDrops(blockState) ? toolProperties.miningSpeed().orElse(1) : 1.0f;
    }
    return item.getDestroySpeed(itemStack, blockState);
  }

  @Redirect(
      method = "isCorrectToolForDrops",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/Item;isCorrectToolForDrops(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)Z",
          remap = false
      )
  )
  private boolean redirectIsCorrectToolForDrops(Item item, ItemStack itemStack, BlockState blockState) {
    BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
    ToolsConfig.Properties toolProperties = getToolProperties(item);
    if (toolProperties != null) {
      if (blockProperties != null) {
        for (Map.Entry<Byte, Integer> powerData : toolProperties.powers().entrySet()) {
          BlocksConfig.ResistanceData resistanceData = blockProperties.data().get(powerData.getKey());
          if (resistanceData != null) {
            if (resistanceData.resistance() >= 0 && powerData.getValue() >= resistanceData.resistance()) {
              return true;
            }
          }
        }
      } else {
        for (Map.Entry<Byte, Integer> powerData : toolProperties.powers().entrySet()) {
          TagKey<Block> tag = TAG_KEYS_BY_TOOL_TYPE.get(powerData.getKey());
          if (blockState.is(tag)) {
            var tier = getTierIfPresent(powerData.getKey(), toolProperties);
            if (tier != null && TierSortingRegistry.isCorrectTierForDrops(tier, blockState)) {
              return true;
            }
          }
        }
      }
    }
    return item.isCorrectToolForDrops(itemStack, blockState);
  }

  @SuppressWarnings("SameReturnValue")
  @Shadow
  public boolean isCorrectToolForDrops(BlockState blockState) {
    return false;
  }
}
