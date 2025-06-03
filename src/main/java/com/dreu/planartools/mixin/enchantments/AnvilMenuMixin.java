package com.dreu.planartools.mixin.enchantments;

import com.dreu.planartools.config.EnchantsConfig.OpposingSets;
import com.dreu.planartools.config.ToolsConfig;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.dreu.planartools.config.EnchantsConfig.ENCHANTS_BY_ITEM_ID;
import static com.dreu.planartools.config.EnchantsConfig.ENCHANTS_BY_TOOL_TYPE;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;

@SuppressWarnings({"DataFlowIssue", "unused"})
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
  private final OpposingSets<String> ENCHANTMENTS = new OpposingSets<>();

  @Redirect(
      method = "createResult",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"
      )
  )
  private boolean overrideCanEnchant(Enchantment enchantment, ItemStack itemStack) {
    String itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
    ToolsConfig.Properties toolProperties = TOOLS.get(itemId);

    ENCHANTMENTS.clear();

    OpposingSets<String> byItem = ENCHANTS_BY_ITEM_ID.get(itemId);
    if (byItem != null && !byItem.isEmpty()) {
      ENCHANTMENTS.positive().addAll(byItem.positive());
      ENCHANTMENTS.negative().addAll(byItem.negative());
    }

    if (toolProperties != null) {
      for (Byte toolTypeId : toolProperties.powers().keySet()) {
        OpposingSets<String> byTool = ENCHANTS_BY_TOOL_TYPE.get(toolTypeId);
        if (byTool != null && !byTool.isEmpty())
          ENCHANTMENTS.mergeDominantly(byTool);
      }
    }

    if (!ENCHANTMENTS.isEmpty()) {
      String enchantmentName = ForgeRegistries.ENCHANTMENTS.getKey(enchantment).toString();
      if (ENCHANTMENTS.negative().contains(enchantmentName)) {ENCHANTMENTS.clear(); return false;}
      if (ENCHANTMENTS.positive().contains(enchantmentName)) {ENCHANTMENTS.clear(); return true;}
    }

    ENCHANTMENTS.clear();
    return enchantment.canEnchant(itemStack);
  }
}