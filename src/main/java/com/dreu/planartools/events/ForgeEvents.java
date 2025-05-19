package com.dreu.planartools.events;

import com.dreu.planartools.config.ToolsConfig;
import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.config.ToolsConfig.*;
import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@Mod.EventBusSubscriber(modid = MODID, bus = FORGE) @SuppressWarnings("unused")
public class ForgeEvents {
    @SubscribeEvent @SuppressWarnings("DataFlowIssue")
    public static void appendTooltipEvent(RenderTooltipEvent.GatherComponents event) {
        String item = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()).toString();
        if (TOOLS.containsKey(item)) {
            event.getTooltipElements().add(Either.left(Component.translatable("planar_tools.power_title")));
            System.out.println(Arrays.toString(TOOLS.get(item).data()));
            for (ToolsConfig.PowerData data : TOOLS.get(item).data()) {
                event.getTooltipElements().add(Either.left(
                        Component.literal(" ")
                        .append(Component.translatable("planar_tools.powerNames." + REGISTERED_TOOL_TYPES.get(data.toolTypeId()))
                            .withStyle(style -> style.withColor(REGISTERED_TOOL_COLORS.get(data.toolTypeId())))
                        .append(": " + data.power()))

                ));
            }
        }
    }
}
