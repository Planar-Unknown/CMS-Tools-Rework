package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.*;

import static com.dreu.planartools.PlanarTools.POWERS;
import static com.dreu.planartools.config.BlocksConfig.BLOCKS;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;

@Mixin(DiggerItem.class)
public class DiggerItemMixin extends TieredItem {

    private final TagKey<Block> blocks;
    private final float speed;

    //dummy constructor to trick the pre-compiler
    public DiggerItemMixin(Tier tier, TagKey<Block> blocks, Properties properties) {
        super(tier, properties);
        this.blocks = blocks;
        this.speed = tier.getSpeed();
    }

    @Overwrite
    public float getDestroySpeed(ItemStack itemInHand, BlockState blockState) {
        System.out.println("working!");
        Map.Entry<int[], Integer> toolProperties = TOOLS.get(ForgeRegistries.ITEMS.getKey(itemInHand.getItem()).toString());
        BlocksConfig.Properties blockProperties = BLOCKS.get(ForgeRegistries.BLOCKS.getKey(blockState.getBlock()).toString());
        if (toolProperties != null && blockProperties != null) {
            boolean canMine = false;
            boolean applyMiningSpeed = false;
            for (int i = 0; i < toolProperties.getKey().length; i++) {
                int resistance = blockProperties.toolDataMap().get(POWERS[i]).resistance();
                if (resistance >= 0 && toolProperties.getKey()[i] >= resistance) {
                    canMine = true;
                    if (blockProperties.toolDataMap().get(POWERS[i]).applyMiningSpeed()) {
                        applyMiningSpeed = true;
                    }
                }
            }
            return canMine ? applyMiningSpeed ? toolProperties.getValue() : 1.0f : 0.0f;
        }
        return blockState.is(this.blocks) ? this.speed : 1.0F;
    }
}
