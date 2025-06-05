package com.dreu.planartools.mixin.enchantments;

import com.dreu.planartools.config.ToolsConfig;
import com.dreu.planartools.util.OpposingSets;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.TreeMap;

import static com.dreu.planartools.config.EnchantsConfig.ENCHANTS_BY_ITEM_ID;
import static com.dreu.planartools.config.EnchantsConfig.ENCHANTS_BY_TOOL_TYPE;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;

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
}
