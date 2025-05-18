package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.ToolsConfig;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static com.dreu.planartools.config.BlocksConfig.BLOCKS;
import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_TYPES;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;

@Mixin(DiggerItem.class) @SuppressWarnings("unused")
public class DiggerItemMixin extends TieredItem {

    private final TagKey<Block> blocks;
    private final float speed;

    //dummy constructor to trick the pre-compiler
    public DiggerItemMixin(Tier tier, TagKey<Block> blocks, Properties properties) {
        super(tier, properties);
        this.blocks = blocks;
        this.speed = tier.getSpeed();
    }

    @Overwrite @SuppressWarnings("DataFlowIssue")
    public float getDestroySpeed(ItemStack itemInHand, BlockState blockState) {
        ToolsConfig.Properties toolProperties = TOOLS.get(ForgeRegistries.ITEMS.getKey(itemInHand.getItem()).toString());
        BlocksConfig.Properties blockProperties = BLOCKS.get(ForgeRegistries.BLOCKS.getKey(blockState.getBlock()).toString());
        if (toolProperties != null && blockProperties != null) {
            boolean canMine = false;
            boolean applyMiningSpeed = false;
            for (ToolsConfig.PowerData data : toolProperties.data()) {
                int resistance = blockProperties.get(REGISTERED_TOOL_TYPES.get(data.toolTypeId())).resistance();
                if (resistance >= 0 && data.power() >= resistance) {
                    canMine = true;
                    if (blockProperties.get(REGISTERED_TOOL_TYPES.get(data.toolTypeId())).applyMiningSpeed()) {
                        applyMiningSpeed = true;
                    }
                }
            }
            return canMine ? applyMiningSpeed ? toolProperties.miningSpeed() : 1.0f : 0.0f;
        }
        return blockState.is(blocks) ? speed : 1.0F;
    }
}
