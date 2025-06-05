package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

import static com.dreu.planartools.config.BlocksConfig.getBlockProperties;

@SuppressWarnings("unused")
@Mixin(ExplosionDamageCalculator.class)
public class ExplosionDamageCalculatorMixin {
  @Inject(
      method = "getBlockExplosionResistance",
      at = @At("HEAD"),
      cancellable = true
  )
  private void getBlockExplosionResistanceInject(Explosion explosion, BlockGetter level, BlockPos blockPos, BlockState blockState, FluidState fluidState, CallbackInfoReturnable<Optional<Float>> cir) {
    System.out.println("We are " + blockState.getBlock().getName());
    BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
    if (blockProperties != null) {
      Optional<Float> explosionResistance = blockProperties.explosionResistance();
      if (explosionResistance.isPresent()) {
        float value = explosionResistance.get();
        if (value == -1) cir.setReturnValue(Optional.of(Float.POSITIVE_INFINITY));
        cir.setReturnValue(blockState.isAir() && fluidState.isEmpty() ? Optional.empty() : Optional.of(
            Math.max(value, fluidState.getExplosionResistance(level, blockPos, explosion))
        ));
      } else {
        if (blockProperties.defaultResistance() == -1) cir.setReturnValue(Optional.of(Float.POSITIVE_INFINITY));
      }
    }
  }
}
