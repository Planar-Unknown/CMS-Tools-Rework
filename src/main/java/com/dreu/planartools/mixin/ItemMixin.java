package com.dreu.planartools.mixin;

import com.dreu.planartools.config.BlocksConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.dreu.planartools.PlanarTools.POWERS;
import static com.dreu.planartools.config.BlocksConfig.getBlockProperties;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;

@Mixin(Item.class) @SuppressWarnings("unused")
public class ItemMixin {
    @SuppressWarnings("DataFlowIssue") private final Item self = (Item)(Object) this;
    @SuppressWarnings("DataFlowIssue")
    Supplier<Map.Entry<int[], Integer>> toolProperties = () -> TOOLS.get(ForgeRegistries.ITEMS.getKey(self).toString());

//    Set<Pair<String, TagKey<Block>>> blockTags = new HashSet<>();

    private Set<Pair<String, TagKey<Block>>> getTagKeys() {
        Set<Pair<String, TagKey<Block>>> tagKeys = new HashSet<>();
        if (toolProperties != null) {
            int[] powers = toolProperties.get().getKey();
            for (int i = 0; i < powers.length; i++) {
                if (powers[i] > 0) {
                    TagKey<Block> tag = switch (POWERS[i]) {
                        case "Axe" -> BlockTags.MINEABLE_WITH_AXE;
                        case "Shovel" -> BlockTags.MINEABLE_WITH_SHOVEL;
                        case "Hoe" -> BlockTags.MINEABLE_WITH_HOE;
                        case "Pickaxe" -> BlockTags.MINEABLE_WITH_PICKAXE;
                        default -> null;
                    };
                    if (tag != null)
                        tagKeys.add(Pair.of(POWERS[i], tag));
                }
            }
        }
        return tagKeys;
    }

    @Overwrite
    public float getDestroySpeed(ItemStack itemInHand, BlockState blockState) {
        BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
        if (blockProperties != null) {
            boolean applyMiningSpeed = false;
            if (toolProperties != null) {
                boolean canMine = false;
                for (int i = 0; i < toolProperties.get().getKey().length; i++) {
                    int resistance = blockProperties.toolDataMap().get(POWERS[i]).resistance();
                    if (resistance >= 0 && toolProperties.get().getKey()[i] >= resistance) {
                        canMine = true;
                        if (blockProperties.toolDataMap().get(POWERS[i]).applyMiningSpeed()) {
                            applyMiningSpeed = true;
                        }
                    }
                }
                return canMine ? applyMiningSpeed ? toolProperties.get().getValue() : 1.0f : 0.0f;
            } else {
                for (String power : POWERS) {
                    if (blockProperties.toolDataMap().get(power).resistance() == 0)
                        return 1.0f;
                }
                return 0f;
            }
        } else if (toolProperties != null) {
            return isCorrectToolForDrops(blockState) ? toolProperties.get().getValue() : 1.0f;
        }
        return 1.0f;
    }

    @Overwrite
    public boolean isCorrectToolForDrops(BlockState blockState) {
        BlocksConfig.Properties blockProperties = getBlockProperties(blockState.getBlock());
        if (toolProperties != null) {
            int[] powers = toolProperties.get().getKey();
            if (blockProperties != null) {
                for (int i = 0; i < powers.length; i++) {
                    BlocksConfig.ToolData toolData = blockProperties.toolDataMap().get(POWERS[i]);
                    if (toolData.applyMiningSpeed()) {
                        if (toolData.resistance() >= 0 && powers[i] >= toolData.resistance()) {
                            return true;
                        }
                    }
                }
            } else {
                for (Pair<String, TagKey<Block>> tag : getTagKeys()) {
                    if (blockState.is(tag.getRight()) && getTierIfPresent(tag.getLeft()) != null && TierSortingRegistry.isCorrectTierForDrops(getTierIfPresent(tag.getLeft()), blockState))
                        return true;
                }
            }
        }
        return false;
    }

    private Tier getTierIfPresent(String toolType) {
        if (toolProperties != null) {
            int index = switch (toolType) {
                case "Axe" -> 1;
                case "Shovel" -> 2;
                case "Hoe" -> 3;
                case "Shears" -> 4;
                case "Sword" -> 5;
                default -> 0;
            };
            int power = toolProperties.get().getKey()[index];
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
