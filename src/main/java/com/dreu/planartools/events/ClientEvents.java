package com.dreu.planartools.events;

import com.dreu.planartools.util.Helpers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.*;

import static com.dreu.planartools.util.KeyBinds.TOGGLE_WAILA_KEY_MAPPING;
import static com.dreu.planartools.PlanarTools.MODID;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientEvents {
  @SubscribeEvent
  public static void onKeyInput(InputEvent.Key event) {
    if (TOGGLE_WAILA_KEY_MAPPING.consumeClick()) {
      Helpers.WAILA_POSITION = Helpers.WAILA_POSITION.next();
    }
  }

  @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = Bus.MOD)
  public static class ClientModBusEvents {
    @SubscribeEvent
    public static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
      event.register(TOGGLE_WAILA_KEY_MAPPING);
    }
  }
}

