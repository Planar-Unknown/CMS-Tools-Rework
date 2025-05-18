package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.ToolsConfig;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.Supplier;

import static com.dreu.planartools.PlanarTools.TAG_KEYS_BY_TOOL_TYPE;
import static com.dreu.planartools.config.BlocksConfig.getBlockProperties;
import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_TYPES;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;

@Mixin(Item.class)
@SuppressWarnings("unused")
public class ItemMixin {
    @SuppressWarnings("DataFlowIssue")
    private final Item self = (Item) (Object) this;
    @SuppressWarnings("DataFlowIssue")
    Supplier<ToolsConfig.Properties> toolProperties = () -> TOOLS.get(ForgeRegistries.ITEMS.getKey(self).toString());

    @Overwrite
    public float getDestroySpeed(ItemStack itemInHand, BlockState blockState) {
        BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
        if (blockProperties != null) {
            boolean applyMiningSpeed = false;
            if (toolProperties != null) {
                boolean canMine = false;
                for (ToolsConfig.PowerData data : toolProperties.get().data()) {
                    String toolType = REGISTERED_TOOL_TYPES.get(data.toolTypeId());
                    int resistance = blockProperties.get(toolType).resistance();
                    if (resistance >= 0 && data.power() >= resistance) {
                        canMine = true;
                        if (blockProperties.get(toolType).applyMiningSpeed()) {
                            applyMiningSpeed = true;
                        }
                    }
                }
                return canMine ? applyMiningSpeed ? toolProperties.get().miningSpeed() : 1.0f : 0.0f;
            } else {
                for (String toolType : REGISTERED_TOOL_TYPES) {
                    if (blockProperties.get(toolType) != null && blockProperties.get(toolType).resistance() == 0)
                        return 1.0f;
                }
                return 0f;
            }
        } else if (toolProperties != null) {
            return isCorrectToolForDrops(blockState) ? toolProperties.get().miningSpeed() : 1.0f;
        }
        return 1.0f;
    }

    @Overwrite
    public boolean isCorrectToolForDrops(BlockState blockState) {
        BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
        if (toolProperties != null) {
            if (blockProperties != null) {
                for (ToolsConfig.PowerData data : toolProperties.get().data()) {
                    BlocksConfig.BlockData toolData = blockProperties.get(REGISTERED_TOOL_TYPES.get(data.toolTypeId()));
                    if (toolData.applyMiningSpeed()) {
                        if (toolData.resistance() >= 0 && data.power() >= toolData.resistance()) {
                            return true;
                        }
                    }
                }
            } else {
                for (ToolsConfig.PowerData data : toolProperties.get().data()) {
                    TagKey<Block> tag = TAG_KEYS_BY_TOOL_TYPE[data.toolTypeId()];
                    if (blockState.is(tag) && getTierIfPresent(data.toolTypeId()) != null && TierSortingRegistry.isCorrectTierForDrops(getTierIfPresent(data.toolTypeId()), blockState))
                        return true;
                }
            }
        }
        return false;
    }

    private Tier getTierIfPresent(int toolType) {
        if (toolProperties != null) {
            int power = toolProperties.get().data()[toolType].power();
            if (power < 20) return null;
            if (power < 40) return Tiers.WOOD;
            if (power < 60) return Tiers.STONE;
            if (power < 80) return Tiers.IRON;
            if (power < 100) return Tiers.DIAMOND;
            return Tiers.NETHERITE;
        }
        return null;
    }

}
