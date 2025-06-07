package com.dreu.planartools.events;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.ToolsConfig;
import com.dreu.planartools.util.Helpers;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dreu.planartools.PlanarTools.MODID;
import static com.dreu.planartools.config.BlocksConfig.BLOCKS;
import static com.dreu.planartools.config.BlocksConfig.getBlockProperties;
import static com.dreu.planartools.config.ToolsConfig.*;
import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_COLORS;
import static com.dreu.planartools.events.ClientModBusEvents.TOGGLE_TOOLTIPS_KEY_MAPPING;
import static com.dreu.planartools.events.ClientModBusEvents.TOGGLE_WAILA_KEY_MAPPING;
import static com.dreu.planartools.util.Helpers.displayTooltips;
import static com.dreu.planartools.util.Helpers.toggleTooltipDisplay;
import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MODID, bus = FORGE, value = Dist.CLIENT)
public class ClientForgeBusEvents {
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
    //Todo: display "Unmineable" when default resistance is -1 and no resistances are present
    if (Helpers.WAILA_POSITION == Helpers.WailaPosition.INVISIBLE) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || !(mc.hitResult instanceof BlockHitResult blockHitResult)) return;

    Block block = mc.level.getBlockState(blockHitResult.getBlockPos()).getBlock();
    if (block.equals(Blocks.AIR)) return;

    List<MutableComponent> componentsList = new ArrayList<>();
    componentsList.add(block.getName());

    BlocksConfig.Properties blockProperties = getBlockProperties(block);
    if (blockProperties != null && !blockProperties.data().isEmpty()) {
      ToolsConfig.Properties toolProperties = TOOLS.get(
          ForgeRegistries.ITEMS.getKey(mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem()).toString()
      );

      MutableComponent builder = Component.literal("");
      boolean first = true;

      for (Map.Entry<Byte, BlocksConfig.ResistanceData> entry : blockProperties.data().entrySet()) {
        if (!first) builder.append("   ");
        first = false;
        ChatFormatting color;

        if (toolProperties != null && toolProperties.powers().containsKey(entry.getKey())) {
          int toolPower = toolProperties.powers().get(entry.getKey());
          color = entry.getValue().resistance() == -1 || toolPower < entry.getValue().resistance()
              ? ChatFormatting.RED
              : entry.getValue().applyMiningSpeed()
              ? ChatFormatting.GREEN
              : ChatFormatting.YELLOW;
        } else {
          color = blockProperties.defaultResistance() == 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        }


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
      event.getTooltipElements().add(Either.left(Component.translatable("planar_tools.tooltip.resistanceTitle")));
      for (Map.Entry<Byte, BlocksConfig.ResistanceData> data : BLOCKS.get(item).data().entrySet()) {
        event.getTooltipElements().add(Either.left(
            Component.literal(" ")
                .append(Component.translatable("planar_tools.powerNames." + REGISTERED_TOOL_TYPES.get(data.getKey())))
                .withStyle(style -> style.withColor(REGISTERED_TOOL_COLORS.get(data.getKey())))
                .append(Component.literal(": " + data.getValue().resistance()))
        ));
      }
    }
    if (TOOLS.containsKey(item) && !TOOLS.get(item).powers().isEmpty()) {
      event.getTooltipElements().add(Either.left(Component.translatable("planar_tools.tooltip.powerTitle")));
      for (Map.Entry<Byte, Integer> powerData : TOOLS.get(item).powers().entrySet()) {
        event.getTooltipElements().add(Either.left(
            Component.literal(" ")
                .append(Component.translatable("planar_tools.powerNames." + REGISTERED_TOOL_TYPES.get(powerData.getKey()))
                    .withStyle(style -> style.withColor(REGISTERED_TOOL_COLORS.get(powerData.getKey())))
                    .append(": " + powerData.getValue()))
        ));
      }
    }
  }
}
