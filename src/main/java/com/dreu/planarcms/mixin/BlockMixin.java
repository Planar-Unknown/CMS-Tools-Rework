package com.dreu.planarcms.mixin;

import com.dreu.planarcms.config.BlocksConfig;
import com.dreu.planarcms.config.ToolsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;

import static com.dreu.planarcms.config.BlocksConfig.getBlockProperties;
import static com.dreu.planarcms.config.ToolsConfig.getToolProperties;

@Mixin(Block.class)
public abstract class BlockMixin {
  @Inject(
      method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Z)V",
      at = @At("HEAD"),
      cancellable = true,
      remap = false
  )
  private static void onDropResources(BlockState blockState, Level level, BlockPos blockPos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack itemStack, boolean dropXp, CallbackInfo ci) {
    if (level instanceof ServerLevel serverLevel) {
      boolean shouldDrop = true;
      BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
      if (blockProperties != null) {
        shouldDrop = blockProperties.defaultCanDrop();
        ToolsConfig.Properties toolProperties = getToolProperties(itemStack.getItem());
        if (toolProperties != null) {
          for (Map.Entry<Byte, Integer> powerData : toolProperties.powers().entrySet()) {
            BlocksConfig.ToolProfile toolProfile = blockProperties.data().get(powerData.getKey());
            if (toolProfile != null) {
              if (toolProfile.resistance() >= 0 && powerData.getValue() >= toolProfile.resistance()) {
                if (toolProfile.canDrop().isPresent()) {
                  if (toolProfile.canDrop().get()) {
                    shouldDrop = true; // If any resistance is successfully met and allows drops, then break and allow
                    break;
                  } else {
                    shouldDrop = false; // If a resistance is successfully met and does not allow drops, set drops not allowed and continue to check other resistances
                  }
                }
              }
            }
          }
        }
        if (!shouldDrop)
          ci.cancel();
      }
    }
  }
}

