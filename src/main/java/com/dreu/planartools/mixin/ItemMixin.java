package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.ToolsConfig;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

import static com.dreu.planartools.PlanarTools.TAG_KEYS_BY_TOOL_TYPE;
import static com.dreu.planartools.config.BlocksConfig.getBlockProperties;
import static com.dreu.planartools.config.ToolsConfig.getToolProperties;
import static com.dreu.planartools.util.Helpers.getTierIfPresent;

@Mixin(Item.class)
@SuppressWarnings("unused")
public class ItemMixin {
  @SuppressWarnings("DataFlowIssue")
  private Item self() {
    return (Item) (Object) this;
  }

  @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
  private void onGetDestroySpeed(ItemStack itemInHand, BlockState blockState, CallbackInfoReturnable<Float> cir) {
    BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
    ToolsConfig.Properties toolProperties = getToolProperties(self());
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
              cir.setReturnValue(Float.valueOf(toolProperties.miningSpeed().get()));
          } else
            cir.setReturnValue(1f);
        } else
          cir.setReturnValue(0f);
      } else {
        cir.setReturnValue(blockProperties.defaultResistance() == 0 ? 1f : 0f);
      }
    } else if (toolProperties != null) {
      cir.setReturnValue(isCorrectToolForDrops(blockState) ? toolProperties.miningSpeed().orElse(1) : 1.0f);
    }
  }

  @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
  private void onIsCorrectToolForDrops(BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
    BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
    ToolsConfig.Properties toolProperties = getToolProperties(self());
    if (toolProperties != null) {
      if (blockProperties != null) {
        for (Map.Entry<Byte, Integer> powerData : toolProperties.powers().entrySet()) {
          BlocksConfig.ResistanceData resistanceData = blockProperties.data().get(powerData.getKey());
          if (resistanceData != null) {
            if (resistanceData.resistance() >= 0 && powerData.getValue() >= resistanceData.resistance()) {
              cir.setReturnValue(true);
            }
          }
        }
      } else {
        for (Map.Entry<Byte, Integer> powerData : toolProperties.powers().entrySet()) {
          TagKey<Block> tag = TAG_KEYS_BY_TOOL_TYPE.get(powerData.getKey());
          if (blockState.is(tag)) {
            var tier = getTierIfPresent(powerData.getKey(), toolProperties);
            if (tier != null && TierSortingRegistry.isCorrectTierForDrops(tier, blockState)) {
              cir.setReturnValue(true);
            }
          }
        }
      }
    }
  }

  @SuppressWarnings("SameReturnValue")
  @Shadow
  public boolean isCorrectToolForDrops(BlockState blockState) {
    return false;
  }
}
