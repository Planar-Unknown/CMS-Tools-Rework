package com.dreu.planartools.events;

import com.dreu.planartools.util.Helpers;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.*;
import org.lwjgl.glfw.GLFW;

import static com.dreu.planartools.PlanarTools.MODID;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientEvents {
  public static final String KEY_CATEGORY_TRANSLATION = "key.categories." + MODID;
  public static final String TOGGLE_WAILA_KEY_TRANSLATION = "key." + MODID + ".toggleWaila";
  public static final String TOGGLE_TOOLTIPS_KEY_TRANSLATION = "key." + MODID + ".toggleTooltips";

  public static final KeyMapping TOGGLE_WAILA_KEY_MAPPING = new KeyMapping(TOGGLE_WAILA_KEY_TRANSLATION, KeyConflictContext.IN_GAME, KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY_TRANSLATION);
  public static final KeyMapping TOGGLE_TOOLTIPS_KEY_MAPPING = new KeyMapping(TOGGLE_TOOLTIPS_KEY_TRANSLATION, KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY_TRANSLATION);

  @SubscribeEvent
  public static void onKeyInput(InputEvent.Key event) {
    if (TOGGLE_WAILA_KEY_MAPPING.consumeClick()) {
      Helpers.WAILA_POSITION = Helpers.WAILA_POSITION.next();
      return;
    }
    if (TOGGLE_TOOLTIPS_KEY_MAPPING.consumeClick())
      Helpers.toggleTooltipDisplay();
  }

  @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Bus.MOD)
  public static class ClientModBusEvents {
    @SubscribeEvent
    public static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
      event.register(TOGGLE_WAILA_KEY_MAPPING);
      event.register(TOGGLE_TOOLTIPS_KEY_MAPPING);
    }
  }
}

