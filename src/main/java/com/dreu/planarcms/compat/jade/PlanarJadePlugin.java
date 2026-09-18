package com.dreu.planarcms.compat.jade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import static com.dreu.planarcms.PlanarCMS.MODID;

@WailaPlugin
public class PlanarJadePlugin implements IWailaPlugin {
  public static final ResourceLocation BLOCK_PROPERTIES = new ResourceLocation(MODID, "block_properties");

  @Override
  public void register(IWailaCommonRegistration registration) {
  }

  @Override
  public void registerClient(IWailaClientRegistration registration) {
    registration.registerBlockComponent(PlanarJadeBlockProvider.INSTANCE, Block.class);
  }
}
