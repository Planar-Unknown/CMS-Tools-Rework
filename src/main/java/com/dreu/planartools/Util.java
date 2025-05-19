package com.dreu.planartools;

import com.dreu.planartools.config.ToolsConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.mojang.text2speech.Narrator.LOGGER;

public class Util {

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

    public static Tier getTierIfPresent(int toolType, ToolsConfig.Properties toolProperties) {
        int power = toolProperties.data()[toolType].power();
        if (power < 20) return null;
        if (power < 40) return Tiers.WOOD;
        if (power < 60) return Tiers.STONE;
        if (power < 80) return Tiers.IRON;
        if (power < 100) return Tiers.DIAMOND;
        return Tiers.NETHERITE;
    }
}
