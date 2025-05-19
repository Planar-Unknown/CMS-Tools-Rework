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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void onGetDestroySpeed(ItemStack itemInHand, BlockState blockState, CallbackInfoReturnable<Float> cir) {
        BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
        if (blockProperties != null) {
            boolean applyMiningSpeed = false;
            if (toolProperties.get() != null) {
                boolean canMine = false;
                for (ToolsConfig.PowerData data : toolProperties.get().data()) {
                    String toolType = REGISTERED_TOOL_TYPES.get(data.toolTypeId());
                    BlocksConfig.BlockData blockData = blockProperties.get(toolType);
                    if (blockData != null) {
                        int resistance = blockProperties.get(toolType).resistance();
                        if (resistance >= 0 && data.power() >= resistance) {
                            canMine = true;
                            if (blockData.applyMiningSpeed()) {
                                applyMiningSpeed = true;
                            }
                        }
                    }
                }
                cir.setReturnValue(canMine ? (applyMiningSpeed ? toolProperties.get().miningSpeed() : 1.0f) : 0.0f);
            } else {
                for (String toolType : REGISTERED_TOOL_TYPES) {
                    BlocksConfig.BlockData data = blockProperties.get(toolType);
                    if (data != null && data.resistance() == 0) {
                        cir.setReturnValue(1.0f);
                    }
                }
                cir.setReturnValue(0f);
            }
        } else if (toolProperties.get() != null) {
            cir.setReturnValue(isCorrectToolForDrops(blockState) ? toolProperties.get().miningSpeed() : 1.0f);
        }
    }


    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void onIsCorrectToolForDrops(BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
        BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
        if (toolProperties.get() != null) {
            if (blockProperties != null) {
                for (ToolsConfig.PowerData data : toolProperties.get().data()) {
                    BlocksConfig.BlockData toolData = blockProperties.get(REGISTERED_TOOL_TYPES.get(data.toolTypeId()));
                    if (toolData != null && toolData.applyMiningSpeed()) {
                        if (toolData.resistance() >= 0 && data.power() >= toolData.resistance()) {
                            cir.setReturnValue(true);
                        }
                    }
                }
            } else {
                for (ToolsConfig.PowerData data : toolProperties.get().data()) {
                    TagKey<Block> tag = TAG_KEYS_BY_TOOL_TYPE[data.toolTypeId()];
                    if (blockState.is(tag)) {
                        var tier = getTierIfPresent(data.toolTypeId());
                        if (tier != null && TierSortingRegistry.isCorrectTierForDrops(tier, blockState)) {
                            cir.setReturnValue(true);
                        }
                    }
                }
            }
        }
    }


    private Tier getTierIfPresent(int toolType) {
        if (toolProperties.get() != null) {
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

    @Shadow
    public boolean isCorrectToolForDrops(BlockState blockState) {return false;}
}
