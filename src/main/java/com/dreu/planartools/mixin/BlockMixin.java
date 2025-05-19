package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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
public class BlockMixin extends BlockBehaviour {
    private final Supplier<BlocksConfig.Properties> blockProperties = () -> BLOCKS.get(ForgeRegistries.BLOCKS.getKey(this.asBlock()).toString());

    @Inject(method = "getExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void onGetExplosionResistance(CallbackInfoReturnable<Float> cir) {
        if (blockProperties.get() != null) {
            float expRes = blockProperties.get().explosionResistance().orElse(this.explosionResistance);
            cir.setReturnValue(expRes == -1 ? Float.POSITIVE_INFINITY : expRes);
        }
    }

    @Shadow
    public Item asItem() {return null;}

    @Shadow
    protected Block asBlock() {return null;}

    public BlockMixin(Properties properties, short dummy) {
        super(properties);
    }
}




