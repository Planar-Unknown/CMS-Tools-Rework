package com.dreu.planartools.events;

import com.dreu.planartools.Util;
import com.dreu.planartools.config.ToolsConfig;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.Util.CONFIG_ISSUES;
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

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!CONFIG_ISSUES.isEmpty()) {
                player.sendSystemMessage(
                        Component.literal("[" + CONFIG_ISSUES.size() + "] ").withStyle(ChatFormatting.RED)
                                .append(Component.translatable("planar_tools.issuesDetected").withStyle(ChatFormatting.RED))
                                .append(" {" + MODID + "}: ").withStyle(ChatFormatting.YELLOW)
                );

                for (Util.Issue issue : CONFIG_ISSUES) {
                    player.sendSystemMessage(Component.literal("--------------------------------------").withStyle(ChatFormatting.LIGHT_PURPLE));
                    String msg = issue.message();
                    MutableComponent fullComponent = Component.empty();

                    int index = 0;
                    while (index < msg.length()) {
                        int start = msg.indexOf('[', index);
                        if (start == -1) {
                            fullComponent.append(Component.literal(msg.substring(index)));
                            break;
                        }
                        int end = msg.indexOf(']', start);
                        if (end == -1) {
                            fullComponent.append(Component.literal(msg.substring(index)));
                            break;
                        }

                        if (start > index)
                            fullComponent.append(Component.literal(msg.substring(index, start)));

                        String bracketed = msg.substring(start, end + 1);
                        String inner = msg.substring(start + 1, end);
                        ChatFormatting color = inner.length() > 40 ? ChatFormatting.GOLD : ChatFormatting.AQUA;

                        fullComponent.append(Component.literal(bracketed).withStyle(color));

                        index = end + 1;
                    }

                    player.sendSystemMessage(fullComponent);
                }
            }
        }
    }
}
