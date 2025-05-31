package com.dreu.planartools;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.GeneralConfig;
import com.dreu.planartools.config.ToolsConfig;
import com.dreu.planartools.network.PacketHandler;
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
import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_TYPES;

@Mod(PlanarTools.MODID)
public class PlanarTools {
    //Todo: Test cachedsuppliers
    //Todo: Check if items/blocks exist before adding them
    //Todo: lastUpdateTimePacket
    //Todo: Toggleable Waila
    //Todo: Tags compatibility, Vanilla tags and Custom TOML / Json tags
    //Todo: Make enchantments work on items that have been given tool types
    //Todo: Nbt system for upgrading tools
    //Todo: modpack creator configurable enchants that interact with the Nbt system
    //Eventually make blocks store their destroy progress
    public static final String MODID = "planar_tools";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ArrayList<TagKey<Block>> TAG_KEYS_BY_TOOL_TYPE = new ArrayList<>();

    public PlanarTools() {
        PacketHandler.register();
        GeneralConfig.parse();
        GeneralConfig.populate();
        ToolsConfig.parse();
        BlocksConfig.parse();
        resetTemplate(BlocksConfig.templateFileName, BlocksConfig.TEMPLATE_CONFIG_STRING);
        resetTemplate(ToolsConfig.templateFileName, ToolsConfig.TEMPLATE_CONFIG_STRING);
    }

    public static void populateTagKeys() {
        TAG_KEYS_BY_TOOL_TYPE.clear();
        for (String toolType : REGISTERED_TOOL_TYPES) {
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
