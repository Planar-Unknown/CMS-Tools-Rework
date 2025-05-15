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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Mod(PlanarTools.MODID)
public class PlanarTools {
    public static final String[] POWERS = {"Pickaxe", "Axe", "Shovel", "Hoe", "Shears"};
    public static final int PICKAXE = 0, AXE = 1, SHOVEL = 2, HOE = 3, SHEARS = 4;
    public static final String MODID = "planar_tools";
    public static final Logger LOGGER = LogUtils.getLogger();
    public PlanarTools() {
        if (ToolsConfig.needsRepair) repairConfig(ToolsConfig.fileName);
        if (BlocksConfig.needsRepair) repairConfig(BlocksConfig.fileName);
    }

    public static Config parseFileOrDefault(String fileName, String defaultConfig) {
        try {
            Files.createDirectories(Path.of("config/" + MODID));}
        catch (Exception ignored) {}

        return new TomlParser().parse(Path.of(fileName).toAbsolutePath(),
                (path, configFormat) -> {
                    FileWriter writer = new FileWriter(path.toFile().getAbsolutePath());
                    writer.write(defaultConfig);
                    writer.close();
                    return true;
                });
    }

    public static void repairConfig(String fileName) {
        LOGGER.warn("An issue was found with config: {} | You can find a copy of faulty config at: {} | Repairing...", fileName, fileName.replace(".toml", "_faulty.toml"));
        Path sourcePath = Paths.get(fileName);
        Path destinationPath = Paths.get(fileName.replace(".toml", "_faulty.toml"));
        try {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Exception during faulty config caching: {}", e.getMessage());
        }
        try (FileWriter writer = new FileWriter(new File(fileName).getAbsolutePath())) {
            StringBuilder contents = new StringBuilder("# To reset this config to default, delete this file and rerun the game.");
            //Todo: properly build contents
            contents.append("haha");
            writer.write(contents.toString());
        } catch (IOException e) {
            LOGGER.warn("Exception during config repair: {}", e.getMessage());
        }
    }
}
