package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.dreu.planartools.config.BlocksConfig.BLOCKS;

@SuppressWarnings("unused")
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {
    @Shadow private float destroySpeed;

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    public void onGetDestroySpeed(BlockGetter level, BlockPos blockPos, CallbackInfoReturnable<Float> cir) {
        ResourceLocation resourceLocation = ForgeRegistries.BLOCKS.getKey(getBlock());
        if (resourceLocation != null) {
            @SuppressWarnings("DataFlowIssue")
            BlocksConfig.Properties blockProperties = BLOCKS.get(resourceLocation.toString());
            cir.setReturnValue(blockProperties != null ? blockProperties.hardness().orElse(this.destroySpeed) : this.destroySpeed);
        }
    }

    private BlockState self() {
        return (BlockState) (Object) this;
    }

    @SuppressWarnings("SameReturnValue")
    @Shadow public Block getBlock() {
        return null;
    }
}
