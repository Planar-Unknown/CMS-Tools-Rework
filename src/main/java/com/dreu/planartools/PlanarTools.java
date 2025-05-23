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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static com.dreu.planartools.Util.LogLevel.WARN;
import static com.dreu.planartools.Util.addConfigIssue;
import static com.dreu.planartools.config.BlocksConfig.populateBlocks;
import static com.dreu.planartools.config.ToolsConfig.*;

@Mod(PlanarTools.MODID)
public class PlanarTools {
    //Todo: Toggleable Waila
    //Todo: Nbt system for upgrading tools
    //Todo: Tags compatibility, Vanilla tags and Custom TOML / Json tags
    //Todo: Make enchantments work on items that have been given tool types
    //Eventually make blocks store their destroy progress
    public static final String MODID = "planar_tools";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("unchecked") public static final ArrayList<TagKey<Block>> TAG_KEYS_BY_TOOL_TYPE = new ArrayList<>();

    public PlanarTools() {
        populateToolTypes();
        populateTagKeys();
        if (GeneralConfig.needsRepair) GeneralConfig.repair();
        populateTools();
        populateBlocks();
//        preloadConfigs();
        resetTemplate(BlocksConfig.templateFileName, BlocksConfig.TEMPLATE_CONFIG_STRING);
        resetTemplate(ToolsConfig.templateFileName, ToolsConfig.TEMPLATE_CONFIG_STRING);
    }

    private void populateTagKeys() {
        for (int i = 0; i < REGISTERED_TOOL_TYPES.size(); i++) {
            String toolType = REGISTERED_TOOL_TYPES.get(i);
            String tagKeyName = "mineable/" + toolType.toLowerCase();
            TAG_KEYS_BY_TOOL_TYPE.add(BlockTags.create(new ResourceLocation(tagKeyName)));
        }
    }

    public static void resetTemplate(String fileName, String contents) {
        try {
            Files.createDirectories(Path.of(fileName).getParent());
            FileWriter writer = new FileWriter(new File(fileName).getAbsolutePath());
            writer.write("# DO NOT EDIT THIS TEMPLATE! IT WILL BE RESET!\n" + contents);
            writer.close();
        } catch (IOException e) {
            addConfigIssue(WARN, (byte) 5, "Exception during template replacement: {}", e.getMessage());
        }
    }
}
