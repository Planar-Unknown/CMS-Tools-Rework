package com.dreu.planartools;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.GeneralConfig;
import com.dreu.planartools.config.ToolsConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
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

import static com.dreu.planartools.config.ToolsConfig.REGISTERED_TOOL_TYPES;

@Mod(PlanarTools.MODID)
public class PlanarTools {
    //Todo: Shears mixin
    //Todo: Toggleable Waila
    //Todo: Nbt system for upgrading tools
    //Todo: Tags compatibility, Vanilla tags and Custom TOML / Json tags
    //Todo: Make enchantments work on items that have been given tool types
    /*Todo: Send the following error logs in chat on server start:
        - whenever their custom config or a value in one is not being used
     */
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

    public static Config parseFileOrDefault(String fileName, String defaultConfig, boolean rewriteIfFailedToParse) {
        Path filePath = Path.of(fileName);
        try {
            Files.createDirectories(filePath.getParent());
            return new TomlParser().parse(filePath.toAbsolutePath(),
                    (path, configFormat) -> {
                        FileWriter writer = new FileWriter(path.toFile().getAbsolutePath());
                        writer.write(defaultConfig);
                        writer.close();
                        return true;
                    });
        } catch (Exception e) {
            LOGGER.error("Exception encountered during parsing of config file: [{}]. The hardcoded default config will be used | Exception: {}", fileName, e.getMessage());
            if (rewriteIfFailedToParse) {
                LOGGER.info("Rewriting config file: [{}] in response to parsing failure", fileName);
                try (FileWriter writer = new FileWriter(filePath.toFile().getAbsolutePath())) {
                    writer.write(defaultConfig);
                } catch (IOException io) {
                    LOGGER.error("Exception encountered during rewriting of faulty config file: [{}] | Exception: {}", fileName, io.getMessage());
                }
            } else {
                LOGGER.info("Not rewriting config file: [{}] even though it failed to parse", fileName);
            }
            return new TomlParser().parse(defaultConfig);
        }
    }

    public static void resetTemplate(String fileName, String contents) {
        try (FileWriter writer = new FileWriter(new File(fileName).getAbsolutePath())) {
            String warning = "# DO NOT EDIT THIS TEMPLATE! IT WILL BE RESET!\n";
            writer.write(warning + contents);
        } catch (IOException e) {
            LOGGER.warn("Exception during template replacement: {}", e.getMessage());
        }
    }

    private void preloadConfigs() {
        GeneralConfig.CONFIG.valueMap().put("Preloaded", true);
        ToolsConfig.CONFIG.valueMap().put("Preloaded", true);
        BlocksConfig.CONFIG.valueMap().put("Preloaded", true);
    }
}
