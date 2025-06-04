package com.dreu.planartools.mixin.enchantments;

import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("unused")
@Mixin(EnchantmentMenu.class)
public class EnchantmentMenuMixin {

  @Redirect(
      method = "slotsChanged",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/item/ItemStack;isEnchantable()Z"
      )
  )
  private boolean overrideIsEnchantable(ItemStack itemStack) {
    return true;
  }
}
