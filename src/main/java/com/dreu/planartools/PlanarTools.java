package com.dreu.planartools;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.GeneralConfig;
import com.dreu.planartools.config.ToolsConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.dreu.planartools.Util.LogLevel.WARN;
import static com.dreu.planartools.Util.addConfigIssue;
import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_TYPES;

@Mod(PlanarTools.MODID)
public class PlanarTools {
    //Todo: Toggleable Waila
    //Todo: Nbt system for upgrading tools
    //Todo: Tags compatibility, Vanilla tags and Custom TOML / Json tags
    //Todo: Make enchantments work on items that have been given tool types
    //Todo: Make log file for all config issues to be printed in
    //Eventually make blocks store their destroy progress
    public static final String MODID = "planar_tools";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("unchecked") public static final TagKey<Block>[] TAG_KEYS_BY_TOOL_TYPE = new TagKey[REGISTERED_TOOL_TYPES.size()];


    public PlanarTools() {
        populateTagKeys();
        preloadConfigs();
        if (GeneralConfig.needsRepair) GeneralConfig.repair();
        resetTemplate(BlocksConfig.templateFileName, BlocksConfig.TEMPLATE_CONFIG_STRING);
        resetTemplate(ToolsConfig.templateFileName, ToolsConfig.TEMPLATE_CONFIG_STRING);
    }

    private void populateTagKeys() {
        for (int i = 0; i < REGISTERED_TOOL_TYPES.size(); i++) {
            String toolType = REGISTERED_TOOL_TYPES.get(i);
            String tagKeyName = "mineable/" + toolType.toLowerCase();
            TAG_KEYS_BY_TOOL_TYPE[i] = BlockTags.create(new ResourceLocation(tagKeyName));
        }
    }

    public static void resetTemplate(String fileName, String contents) {
        try (FileWriter writer = new FileWriter(new File(fileName).getAbsolutePath())) {
            String warning = "# DO NOT EDIT THIS TEMPLATE! IT WILL BE RESET!\n";
            writer.write(warning + contents);
        } catch (IOException e) {
            addConfigIssue(WARN, (byte) 5, "Exception during template replacement: {}", e.getMessage());
        }
    }

    private void preloadConfigs() {
        GeneralConfig.CONFIG.valueMap().put("Preloaded", true);
        ToolsConfig.CONFIG.valueMap().put("Preloaded", true);
        BlocksConfig.CONFIG.valueMap().put("Preloaded", true);
    }
}
