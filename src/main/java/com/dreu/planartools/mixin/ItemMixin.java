package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Map;

import static com.dreu.planartools.PlanarTools.POWERS;
import static com.dreu.planartools.config.BlocksConfig.BLOCKS;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;

@Mixin(Item.class) @SuppressWarnings("unused")
public class ItemMixin {
    @Overwrite @SuppressWarnings("DataFlowIssue")
    public float getDestroySpeed(ItemStack itemInHand, BlockState blockState) {
        Map.Entry<int[], Integer> toolProperties = TOOLS.get(ForgeRegistries.ITEMS.getKey(itemInHand.getItem()).toString());
        BlocksConfig.Properties blockProperties = BLOCKS.get(ForgeRegistries.BLOCKS.getKey(blockState.getBlock()).toString());
        boolean applyMiningSpeed = false;
        if (blockProperties != null) {
            if (toolProperties != null) {
                boolean canMine = false;
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
            } else {
                for (String power : POWERS) {
                    if (blockProperties.toolDataMap().get(power).resistance() == 0)
                        return 1.0f;
                }
                return 0f;
            }
        } else if (toolProperties != null) {
            //Todo Together: isCorrectToolForDrops check
            return applyMiningSpeed ? toolProperties.getValue() : 1.0f;
        }
        return 1.0f;
    }
}
