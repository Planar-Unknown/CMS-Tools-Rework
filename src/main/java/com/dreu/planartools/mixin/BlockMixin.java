package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.dreu.planartools.config.BlocksConfig.getBlockProperties;

@Mixin(Block.class)
@SuppressWarnings("unused")
public class BlockMixin {
    @Inject(method = "getExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void onGetExplosionResistance(CallbackInfoReturnable<Float> cir) {
        BlocksConfig.Properties prop = getBlockProperties(asBlock());
        if (prop != null) {
            float expRes = prop.explosionResistance().orElse(this.asBlock().explosionResistance);
            cir.setReturnValue(expRes == -1 ? Float.POSITIVE_INFINITY : expRes);
        }
    }

    @SuppressWarnings("SameReturnValue")
    @Shadow
    protected Block asBlock() {
        return null;
    }
}




