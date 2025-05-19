package com.dreu.planartools;

import com.dreu.planartools.config.ToolsConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.BiConsumer;

import static com.dreu.planartools.PlanarTools.LOGGER;



public class Util {

    @SuppressWarnings("unused")
    public enum LogLevel {
        TRACE(LOGGER::trace),
        DEBUG(LOGGER::debug),
        INFO(LOGGER::info),
        WARN(LOGGER::warn),
        ERROR(LOGGER::error);
        private final BiConsumer<String, Object[]> func;
        LogLevel(BiConsumer<String, Object[]> func) {
            this.func = func;
        }
        public void log(String message, Object... args) {
            func.accept(message, args);
        }
    }

    public record Issue(String message, byte priority) implements Comparable<Issue> {
        @Override
        public int compareTo(@NotNull Issue other) {
            return Byte.compare(other.priority, this.priority);
        }
    }

    public static final ArrayList<Issue> CONFIG_ISSUES = new ArrayList<>();

    public static void addConfigIssue(LogLevel level, byte priority, String message, Object... args) {
//        System.out.printf("Message: " + (message) + "%n", args);
//        System.out.println(message);
//        System.out.println(Arrays.toString(args));
        level.log(message, args);
        String formattable = message.replace("{}", "%s");
        CONFIG_ISSUES.add(new Issue("<" + level.name() + "> -- " + String.format(formattable, args), priority));
        Collections.sort(CONFIG_ISSUES);
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
