package com.dreu.planarcms.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lwjgl.glfw.GLFW;

import static com.dreu.planarcms.PlanarCMS.MODID;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Bus.MOD)
public class ClientModBusEvents {
  public static final String KEY_CATEGORY_TRANSLATION = "key.categories." + MODID;

  public static final KeyMapping TOGGLE_WAILA_KEY_MAPPING = new KeyMapping("key." + MODID + ".toggleWaila", KeyConflictContext.IN_GAME, KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY_TRANSLATION);
  public static final KeyMapping TOGGLE_TOOLTIPS_KEY_MAPPING = new KeyMapping("key." + MODID + ".toggleTooltips", KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY_TRANSLATION);


  @SubscribeEvent
  public static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
    event.register(TOGGLE_WAILA_KEY_MAPPING);
    event.register(TOGGLE_TOOLTIPS_KEY_MAPPING);
  }
}

