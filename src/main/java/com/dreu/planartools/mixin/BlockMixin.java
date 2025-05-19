package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import com.google.common.base.Suppliers;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

import static com.dreu.planartools.config.BlocksConfig.BLOCKS;

@Mixin(Block.class)
@SuppressWarnings("unused")
public class BlockMixin {
    private final Supplier<BlocksConfig.Properties> blockProperties = Suppliers.memoize(() -> BLOCKS.get(ForgeRegistries.BLOCKS.getKey(this.asBlock()).toString()));

    @Inject(method = "getExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void onGetExplosionResistance(CallbackInfoReturnable<Float> cir) {
        BlocksConfig.Properties prop = blockProperties.get();
        if (prop != null) {
            float expRes = prop.explosionResistance().orElse(this.asBlock().explosionResistance);
            cir.setReturnValue(expRes == -1 ? Float.POSITIVE_INFINITY : expRes);
        }
    }

    @Shadow
    protected Block asBlock() {
        return null;
    }
}




