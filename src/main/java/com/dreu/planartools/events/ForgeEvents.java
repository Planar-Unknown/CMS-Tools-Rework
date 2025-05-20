package com.dreu.planartools.events;

import com.dreu.planartools.Util;
import com.dreu.planartools.config.ToolsConfig;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.Util.*;
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
        if (!wereIssuesWrittenToFile) {
            StringBuilder contents = new StringBuilder();
            for (Util.Issue issue : CONFIG_ISSUES) {
                contents.append(issue.message().getString()).append("\n");
            }
            try (FileWriter writer = new FileWriter(Path.of("config/" + MODID + "/issues.log").toAbsolutePath().toString())) {
                Path.of("config/" + MODID + "/").toFile().mkdirs();
                writer.write(contents.toString());
            } catch (IOException io) {
                addConfigIssue(LogLevel.WARN, (byte) 10, "Unexpected Exception occurred while writing [config/planar_tools/issues.log]| Exception: {}", io.getMessage());
            }
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!CONFIG_ISSUES.isEmpty()) {
                Collections.sort(CONFIG_ISSUES);
                player.sendSystemMessage(Component.literal("-----------------------------------------------------").withStyle(ChatFormatting.LIGHT_PURPLE));
                player.sendSystemMessage(
                    Component.literal("[" + CONFIG_ISSUES.size() + "] ").withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("planar_tools.issuesDetected").withStyle(ChatFormatting.RED))
                        .append(Component.literal(" {" + MODID + "}: ").withStyle(ChatFormatting.YELLOW))
                );
                for (int i = 0; i < Math.min(MAX_DISPLAYED_ISSUES, CONFIG_ISSUES.size()); i++) {
                    System.out.println(CONFIG_ISSUES.get(i).message().getString());
                    player.sendSystemMessage(CONFIG_ISSUES.get(i).message());
                }
                if (CONFIG_ISSUES.size() > MAX_DISPLAYED_ISSUES) {
                    player.sendSystemMessage(Component.literal("")
                        .append(Component.literal("[+" + (CONFIG_ISSUES.size() - MAX_DISPLAYED_ISSUES) + "] ").withStyle(ChatFormatting.GREEN))
                        .append(Component.translatable("planar_tools.readMoreAt").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("[config/" + MODID + "/issues.log]").withStyle(style ->
                            style.withColor(0x3381ff)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, Path.of("config/" + MODID + "/issues.log").toAbsolutePath().toString()))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("planar_tools.clickToOpen")))
                        ))
                    );
                }
                player.sendSystemMessage(Component.literal("-----------------------------------------------------").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }
}
