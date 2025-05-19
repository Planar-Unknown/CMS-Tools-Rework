package com.dreu.planartools;

import com.dreu.planartools.config.ToolsConfig;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

public class Util {
    public static Tier getTierIfPresent(int toolType, ToolsConfig.Properties toolProperties) {
        int power = toolProperties.data()[toolType].power();
        if (power < 20) return null;
        if (power < 40) return Tiers.WOOD;
        if (power < 60) return Tiers.STONE;
        if (power < 80) return Tiers.IRON;
        if (power < 100) return Tiers.DIAMOND;
        return Tiers.NETHERITE;
    }
}
