package com.dreu.planartools.events;

import com.dreu.planartools.Util;
import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.ToolsConfig;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.Util.*;
import static com.dreu.planartools.config.BlocksConfig.getBlockProperties;
import static com.dreu.planartools.config.ToolsConfig.*;
import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@Mod.EventBusSubscriber(modid = MODID, bus = FORGE)
@SuppressWarnings({"unused", "DataFlowIssue"})
public class ForgeEvents {

    @SubscribeEvent
    public static void renderGuiEvent(RenderGuiEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.hitResult instanceof BlockHitResult blockHitResult)) return;

        Block block = mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock();
        if (block.equals(Blocks.AIR)) return;

        List<MutableComponent> componentsList = new ArrayList<>();
        componentsList.add(block.getName());

        BlocksConfig.Properties blockProperties = getBlockProperties(block);
        if (blockProperties != null) {
            ToolsConfig.Properties toolProperties = TOOLS.get(
                    ForgeRegistries.ITEMS.getKey(mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem()).toString()
            );

            Map<Byte, Integer> toolPowers = new HashMap<>();
            if (toolProperties != null) {
                for (ToolsConfig.PowerData data : toolProperties.data()) {
                    toolPowers.put(data.toolTypeId(), data.power());
                }
            }

            MutableComponent builder = Component.literal("");
            boolean first = true;

            for (Map.Entry<Byte, BlocksConfig.ResistanceData> entry : blockProperties.data().entrySet()) {
                if (!first) builder.append("   ");
                first = false;

                int toolPower = toolPowers.getOrDefault(entry.getKey(), 0);
                ChatFormatting color = (toolPower >= entry.getValue().resistance())
                        ? ChatFormatting.GREEN : blockProperties.defaultResistance() == 0 ? ChatFormatting.YELLOW : ChatFormatting.RED;

                builder.append(Component.literal(REGISTERED_TOOL_TYPES.get(entry.getKey())).withStyle(ChatFormatting.GRAY))
                        .append(" ")
                        .append(Component.literal(String.valueOf(entry.getValue().resistance())).withStyle(color));
            }

            componentsList.add(builder);
        }

        componentsList.add(Component.literal(
                ModList.get().getModContainerById(ForgeRegistries.BLOCKS.getKey(block).getNamespace())
                        .map(mod -> mod.getModInfo().getDisplayName())
                        .orElse(ForgeRegistries.BLOCKS.getKey(block).getNamespace())
        ).withStyle(ChatFormatting.ITALIC, ChatFormatting.BLUE));

        drawBox(componentsList, event.getGuiGraphics());
    }

    private static void drawBox(List<MutableComponent> components, GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        int fontWidth = components.stream()
                .mapToInt(c -> mc.font.width(c.getVisualOrderText()))
                .max()
                .orElse(0);

        int boxWidth = fontWidth + 4;
        int left = (guiGraphics.guiWidth() - boxWidth) / 2;
        int lineHeight = mc.font.lineHeight + 2;
        int boxHeight = lineHeight * components.size();

        TooltipRenderUtil.renderTooltipBackground(guiGraphics, left, 5, boxWidth, boxHeight - 1,
                -1, 0x50000000, 0x50301060, 0x504040FF, 0x50333333);

        for (int i = 0; i < components.size(); i++) {
            guiGraphics.drawString(mc.font, components.get(i), left + 2, 6 + lineHeight * i, 0xFFFFFF);
        }
    }

    @SubscribeEvent @SuppressWarnings("DataFlowIssue")
    public static void appendTooltipEvent(RenderTooltipEvent.GatherComponents event) {
        String item = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()).toString();
        if (TOOLS.containsKey(item)) {
            event.getTooltipElements().add(Either.left(Component.translatable("planar_tools.power_title")));
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!wereIssuesWrittenToFile) {
            wereIssuesWrittenToFile = true;
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
