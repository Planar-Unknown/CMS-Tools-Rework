package com.dreu.planartools;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.ToolsConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(PlanarTools.MODID)
public class PlanarTools {
    public static final String[] POWERS = {"Pickaxe", "Axe", "Shovel", "Hoe", "Shears"};
    public static final String MODID = "planar_tools";
    public static final Logger LOGGER = LogUtils.getLogger();
    public PlanarTools() {
        //Todo: if (GeneralConfig.needsRepair) GeneralConfig.repair();
        resetTemplate(BlocksConfig.templateFileName, BlocksConfig.TEMPLATE_CONFIG_STRING);
        resetTemplate(ToolsConfig.templateFileName, ToolsConfig.TEMPLATE_CONFIG_STRING);
    }

    public static Config parseFileOrDefault(String fileName, String defaultConfig) {
        Path filePath = Path.of(fileName);
        try {
            Files.createDirectories(filePath.getParent());}
        catch (Exception ignored) {}
        return new TomlParser().parse(filePath.toAbsolutePath(),
                (path, configFormat) -> {
                    FileWriter writer = new FileWriter(path.toFile().getAbsolutePath());
                    writer.write(defaultConfig);
                    writer.close();
                    return true;
                });
    }

    public static void resetTemplate(String fileName, String contents) {
        try (FileWriter writer = new FileWriter(new File(fileName).getAbsolutePath())) {
            writer.write(contents);
        } catch (IOException e) {
            LOGGER.warn("Exception during template replacement: {}", e.getMessage());
        }
    }
}
