package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.ToolsConfig;
import com.google.common.base.Suppliers;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
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
import static com.dreu.planartools.Util.getTierIfPresent;
import static com.dreu.planartools.config.BlocksConfig.getBlockProperties;
import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_TYPES;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;

@SuppressWarnings("unused")
@Mixin(ShearsItem.class)
public class ShearsItemMixin {
    @SuppressWarnings("DataFlowIssue")
    private Item self() {
        return (Item) (Object) this;
    }
    @SuppressWarnings("DataFlowIssue")
    final Supplier<ToolsConfig.Properties> toolProperties = Suppliers.memoize(() -> TOOLS.get(ForgeRegistries.ITEMS.getKey(self()).toString()));

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void onGetDestroySpeed(ItemStack itemInHand, BlockState blockState, CallbackInfoReturnable<Float> cir) {
        BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
        if (blockProperties != null) {
            boolean applyMiningSpeed = false;
            if (toolProperties.get() != null) {
                boolean canMine = false;
                for (ToolsConfig.PowerData powerData : toolProperties.get().data()) {
                    BlocksConfig.ResistanceData resistanceData = blockProperties.data().get(powerData.toolTypeId());
                    if (resistanceData != null) {
                        int resistance = resistanceData.resistance();
                        if (resistance >= 0 && powerData.power() >= resistance) {
                            canMine = true;
                            if (resistanceData.applyMiningSpeed()) {
                                applyMiningSpeed = true;
                            }
                        }
                    } else {
                        int defaultResistance = blockProperties.defaultResistance();
                        if (defaultResistance != -1 && powerData.power() >= defaultResistance)
                            canMine = true;
                    }
                }
                cir.setReturnValue(canMine ? (applyMiningSpeed ? toolProperties.get().miningSpeed() : 1.0f) : 0.0f);
            } else {
                for (String toolType : REGISTERED_TOOL_TYPES) {
                    BlocksConfig.ResistanceData data = blockProperties.data().get((byte) REGISTERED_TOOL_TYPES.indexOf(toolType));
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
                for (ToolsConfig.PowerData powerData : toolProperties.get().data()) {
                    BlocksConfig.ResistanceData resistanceData = blockProperties.data().get(powerData.toolTypeId());
                    if (resistanceData != null && resistanceData.applyMiningSpeed()) {
                        if (resistanceData.resistance() >= 0 && powerData.power() >= resistanceData.resistance()) {
                            cir.setReturnValue(true);
                        }
                    }
                }
            } else {
                for (ToolsConfig.PowerData data : toolProperties.get().data()) {
                    TagKey<Block> tag = TAG_KEYS_BY_TOOL_TYPE.get(data.toolTypeId());
                    if (blockState.is(tag)) {
                        var tier = getTierIfPresent(data.toolTypeId(), toolProperties.get());
                        if (tier != null && TierSortingRegistry.isCorrectTierForDrops(tier, blockState)) {
                            cir.setReturnValue(true);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("SameReturnValue")
    @Shadow
    public boolean isCorrectToolForDrops(BlockState blockState) {return false;}
}
