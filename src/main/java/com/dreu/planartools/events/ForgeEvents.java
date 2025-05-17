package com.dreu.planartools.events;

import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.PlanarTools.POWERS;
import static com.dreu.planartools.config.ToolsConfig.TOOLS;
import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@Mod.EventBusSubscriber(modid = MODID, bus = FORGE) @SuppressWarnings("unused")
public class ForgeEvents {

    @SubscribeEvent @SuppressWarnings("DataFlowIssue")
    public static void appendTooltipEvent(RenderTooltipEvent.GatherComponents event) {
        String item = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()).toString();
        if (TOOLS.containsKey(item)) {
            int[] powers = TOOLS.get(item).getKey();
            for (int i = 0; i < powers.length; i++) {
                if (powers[i] > 0) {
                    event.getTooltipElements().add(Either.left(Component.translatable("planar_tools.powerNames." + POWERS[i]).append(": " + powers[i])));
                }
            }
        }
    }
}
