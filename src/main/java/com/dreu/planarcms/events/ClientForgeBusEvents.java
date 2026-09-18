package com.dreu.planarcms.events;

import com.dreu.planarcms.config.BlocksConfig;
import com.dreu.planarcms.config.DisplayConfig;
import com.dreu.planarcms.util.DisplayHelper;
import com.dreu.planarcms.util.Helpers;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

import static com.dreu.planarcms.PlanarCMS.MODID;
import static com.dreu.planarcms.config.BlocksConfig.BLOCKS;
import static com.dreu.planarcms.config.ToolsConfig.*;
import static com.dreu.planarcms.events.ClientModBusEvents.TOGGLE_TOOLTIPS_KEY_MAPPING;
import static com.dreu.planarcms.events.ClientModBusEvents.TOGGLE_WAILA_KEY_MAPPING;
import static com.dreu.planarcms.util.Helpers.*;
import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@SuppressWarnings({"unused", "DataFlowIssue"})
@Mod.EventBusSubscriber(modid = MODID, bus = FORGE, value = Dist.CLIENT)
public class ClientForgeBusEvents {

  @SubscribeEvent
  public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
    if (Minecraft.getInstance().isLocalServer() && !configHasBeenPopulated) {
      parseAndPopulateConfig();
      configHasBeenPopulated = true;
    }
    DisplayConfig.parse();
    DisplayConfig.populate();
  }

  @SubscribeEvent
  public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
    if (TOGGLE_TOOLTIPS_KEY_MAPPING.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode())))
      toggleTooltipDisplay();
  }

  @SubscribeEvent
  public static void onKeyInput(InputEvent.Key event) {
    if (TOGGLE_WAILA_KEY_MAPPING.consumeClick()) {
      Helpers.WAILA_POSITION = Helpers.WAILA_POSITION.next();
      return;
    }
    if (TOGGLE_TOOLTIPS_KEY_MAPPING.consumeClick())
      Helpers.toggleTooltipDisplay();
  }

  @SubscribeEvent
  public static void renderGuiEvent(RenderGuiEvent event) {
    if (Helpers.WAILA_POSITION == Helpers.WailaPosition.INVISIBLE) return;
    if (ModList.get().isLoaded("jade")) return;

    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult blockHitResult)) return;

    BlockState blockState = mc.level.getBlockState(blockHitResult.getBlockPos());
    if (blockState.isAir()) return;

    List<MutableComponent> wailaComponents = DisplayHelper.getManualWailaComponents(blockState, mc.level, blockHitResult.getBlockPos(), mc.player.getMainHandItem());
    if (wailaComponents.isEmpty()) return;
    drawBox(wailaComponents, event.getGuiGraphics());
  }

  private static void drawBox(List<MutableComponent> components, GuiGraphics guiGraphics) {
    Minecraft mc = Minecraft.getInstance();
    int fontWidth = components.stream()
        .mapToInt(c -> mc.font.width(c.getVisualOrderText()))
        .max()
        .orElse(0);

    int boxWidth = fontWidth + 4;
    int left = switch (Helpers.WAILA_POSITION) {
      case LEFT -> 5;
      case MIDDLE -> (guiGraphics.guiWidth() - boxWidth) / 2;
      case RIGHT -> guiGraphics.guiWidth() - boxWidth - 5;
      case INVISIBLE -> 0;
    };
    int lineHeight = mc.font.lineHeight + 2;
    int boxHeight = lineHeight * components.size();

    TooltipRenderUtil.renderTooltipBackground(guiGraphics, left, 5, boxWidth, boxHeight - 1,
        -1, 0x50000000, 0x50301060, 0x504040FF, 0x50333333);

    for (int i = 0; i < components.size(); i++) {
      guiGraphics.drawString(mc.font, components.get(i), left + 2, 6 + lineHeight * i, 0xFFFFFF);
    }
  }

  @SubscribeEvent
  @SuppressWarnings("DataFlowIssue")
  public static void appendTooltipEvent(RenderTooltipEvent.GatherComponents event) {
    if (!displayTooltips) return;
    String item = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()).toString();
    if (BLOCKS.containsKey(item) && !BLOCKS.get(item).data().isEmpty()) {
      event.getTooltipElements().add(Either.left(Component.translatable(MODID + ".tooltip.resistanceTitle")));
      for (Map.Entry<Byte, BlocksConfig.ToolProfile> data : BLOCKS.get(item).data().entrySet()) {
        event.getTooltipElements().add(Either.left(
            Component.literal(" ")
                .append(Component.literal(REGISTERED_TOOL_TYPES.get(data.getKey())))
                .withStyle(style -> style.withColor(REGISTERED_TOOL_COLORS.get(data.getKey())))
                .append(Component.literal(": " + data.getValue().resistance()))
        ));
      }
    }
    if (TOOLS.containsKey(item) && !TOOLS.get(item).powers().isEmpty()) {
      event.getTooltipElements().add(Either.left(Component.translatable(MODID + ".tooltip.powerTitle")));
      for (Map.Entry<Byte, Integer> powerData : TOOLS.get(item).powers().entrySet()) {
        event.getTooltipElements().add(Either.left(
            Component.literal(" ")
                .append(Component.literal(REGISTERED_TOOL_TYPES.get(powerData.getKey()))
                    .withStyle(style -> style.withColor(REGISTERED_TOOL_COLORS.get(powerData.getKey())))
                    .append(": " + powerData.getValue()))
        ));
      }
    }
  }
}
