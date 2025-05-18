package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static com.dreu.planartools.config.BlocksConfig.BLOCKS;

@SuppressWarnings("unused")
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockMixin {
    @Shadow private float destroySpeed;

    @Overwrite
    public float getDestroySpeed(BlockGetter level, BlockPos blockPos) {
        @SuppressWarnings("DataFlowIssue") BlocksConfig.Properties blockProperties = BLOCKS.get(ForgeRegistries.BLOCKS.getKey(this.getBlock()).toString());
        return blockProperties != null ? blockProperties.hardness().orElse(this.destroySpeed) : this.destroySpeed;
    }

    @Shadow public Block getBlock() {
        return null;
    }
}
