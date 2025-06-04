package com.dreu.planartools.mixin.enchantments;

import com.dreu.planartools.config.EnchantsConfig;
import com.dreu.planartools.config.ToolsConfig;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.dreu.planartools.config.EnchantsConfig.ENCHANTS_BY_ITEM_ID;
import static com.dreu.planartools.config.EnchantsConfig.ENCHANTS_BY_TOOL_TYPE;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;

@SuppressWarnings({"unused", "DataFlowIssue"})
@Mixin(EnchantCommand.class)
public class EnchantCommandMixin {
  private final EnchantsConfig.OpposingSets<String> ENCHANTMENT_SETS = new EnchantsConfig.OpposingSets<>();

  @Redirect(
      method = "enchant",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"
      )
  )
  private boolean overrideCanEnchant(Enchantment enchantment, ItemStack itemStack) {
    String itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
    ToolsConfig.Properties toolProperties = TOOLS.get(itemId);

    ENCHANTMENT_SETS.clear();

    EnchantsConfig.OpposingSets<String> byItem = ENCHANTS_BY_ITEM_ID.get(itemId);
    if (byItem != null && !byItem.isEmpty()) {
      ENCHANTMENT_SETS.positive().addAll(byItem.positive());
      ENCHANTMENT_SETS.negative().addAll(byItem.negative());
    }

    if (toolProperties != null) {
      for (Byte toolTypeId : toolProperties.powers().keySet()) {
        EnchantsConfig.OpposingSets<String> byTool = ENCHANTS_BY_TOOL_TYPE.get(toolTypeId);
        if (byTool != null && !byTool.isEmpty())
          ENCHANTMENT_SETS.mergeDominantly(byTool);
      }
    }

    if (!ENCHANTMENT_SETS.isEmpty()) {
      String enchantmentName = ForgeRegistries.ENCHANTMENTS.getKey(enchantment).toString();
      if (ENCHANTMENT_SETS.negative().contains(enchantmentName)) {
        ENCHANTMENT_SETS.clear(); return false;}
      if (ENCHANTMENT_SETS.positive().contains(enchantmentName)) {
        ENCHANTMENT_SETS.clear(); return true;}
    }

    ENCHANTMENT_SETS.clear();
    return enchantment.canEnchant(itemStack);
  }
}
