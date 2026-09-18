package com.dreu.planarcms.compat.jade;

import com.dreu.planarcms.util.DisplayHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class PlanarJadeBlockProvider implements IBlockComponentProvider {
  public static final PlanarJadeBlockProvider INSTANCE = new PlanarJadeBlockProvider();

  private PlanarJadeBlockProvider() {}

  @Override
  public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
    Minecraft minecraft = Minecraft.getInstance();
    ItemStack heldStack = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
    for (MutableComponent row : DisplayHelper.getJadeComponents(accessor.getBlockState(), accessor.getLevel(), accessor.getPosition(), heldStack)) {
      tooltip.add(row);
    }
  }

  @Override
  public ResourceLocation getUid() {
    return PlanarJadePlugin.BLOCK_PROPERTIES;
  }
}
