package com.dreu.planartools.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import static com.dreu.planartools.PlanarTools.MODID;

public class KeyBinds {
  public static final String KEY_CATEGORY_TRANSLATION = "key.categories." + MODID;
  public static final String TOGGLE_WAILA_KEY_TRANSLATION = "key." + MODID + ".toggleWaila";

  public static final KeyMapping TOGGLE_WAILA_KEY_MAPPING = new KeyMapping(TOGGLE_WAILA_KEY_TRANSLATION, KeyConflictContext.IN_GAME, KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY_TRANSLATION);
}
