package com.dreu.planartools;

import com.dreu.planartools.config.BlocksConfig;
import com.dreu.planartools.config.GeneralConfig;
import com.dreu.planartools.config.ToolsConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.BiConsumer;

import static com.dreu.planartools.PlanarTools.*;
import static com.dreu.planartools.config.BlocksConfig.populateBlocks;
import static com.dreu.planartools.config.GeneralConfig.populate;
import static com.dreu.planartools.config.ToolsConfig.populateToolTypes;
import static com.dreu.planartools.config.ToolsConfig.populateTools;
import static java.lang.String.format;


public class Util {
public class Helpers {
    public static boolean configHasBeenParsed = false;
    public static boolean wereIssuesWrittenToFile = false;
    public static final byte MAX_DISPLAYED_ISSUES = 3;
    public static int shouldUpdateTime = getUpdateTime();

    public static void parseAndProcessConfig() {
        CONFIG_ISSUES.clear();
        wereIssuesWrittenToFile = false;
        GeneralConfig.parse();
        GeneralConfig.populate();
        if (GeneralConfig.needsRepair) GeneralConfig.repair();
        BlocksConfig.parse();
        ToolsConfig.parse();
        populateToolTypes();
        populateTagKeys();
        populateTools();
        populateBlocks();
        configHasBeenParsed = true;
        writeConfigIssuesToFile();
        shouldUpdateTime = getUpdateTime();
    }

    public static int getUpdateTime() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    @SuppressWarnings("unused")
    public enum LogLevel {
        TRACE(LOGGER::trace, "<TRACE>", 0xAAAAFF, "Fine-grained debug info"),
        DEBUG(LOGGER::debug, "<DEBUG>", 0x55FF55, "General debugging output"),
        INFO(LOGGER::info, "<INFO>", 0xFFFF55, "Something minor went wrong, the mod may not work as expected"),
        WARN(LOGGER::warn, "<WARN>", 0xFFAA00, "Something important failed, the mod will not work as expected"),
        ERROR(LOGGER::error, "<ERROR>", 0xFF5555, "A serious problem occurred, your game may crash at some point");

        private final MutableComponent header;
        private final BiConsumer<String, Object[]> func;

        LogLevel(BiConsumer<String, Object[]> func, String title, int color, String hoverText) {
            this.header = Component.literal("").append(Component
                    .translatable("planar_tools.logTitle." + title)
                    .withStyle(style -> style
                            .withColor(color)
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal(hoverText)
                            ))
                    ));
            this.func = func;
        }

        public void log(String message, Object... args) {
            func.accept("[" + MODID + "] " + message, args);
        }

        public MutableComponent header() {
            return header.copy();
        }
    }

    public static String abridgeFileName(String fullFileName) {
        String logFileName = fullFileName;
        if (logFileName.contains("config/"))
            logFileName = logFileName.replace("config/", "");
        if (logFileName.contains("planar_tools/"))
            logFileName = logFileName.replace("planar_tools/", "");
        return logFileName;
    }

    public record Issue(LogLevel level, String contents, byte priority) implements Comparable<Issue> {
        @Override
        public int compareTo(@NotNull Issue other) {
            return Byte.compare(other.priority, this.priority);
        }
        public Component message() {
            return level.header().append(formatError(contents));
        }
    }

    public static final ArrayList<Issue> CONFIG_ISSUES = new ArrayList<>();

    public static void addConfigIssue(LogLevel level, byte priority, String message, Object... args) {
        level.log(message, args);
        String toFormat = message.replace("{}", "%s");
        CONFIG_ISSUES.add(new Issue(level, format(toFormat, args), priority));
    }

    public static @NotNull MutableComponent formatError(String msg) {
        MutableComponent fullComponent = Component.empty();
        msg = " " + msg;
        int index = 0;
        while (index < msg.length()) {
            int nextBracket = msg.indexOf('[', index);
            int nextAngle = msg.indexOf('<', index);
            int nextCurly = msg.indexOf('{', index);
            int nextQuote = msg.indexOf('"', index);
            int nextApostrophe = msg.indexOf('\'', index);

            int start = -1;
            char endChar = 0;

            if (nextBracket != -1) {
                start = nextBracket;
                endChar = ']';
            }
            if (nextAngle != -1 && (start == -1 || nextAngle < start)) {
                start = nextAngle;
                endChar = '>';
            }
            if (nextCurly != -1 && (start == -1 || nextCurly < start)) {
                start = nextCurly;
                endChar = '}';
            }
            if (nextQuote != -1 && (start == -1 || nextQuote < start)) {
                start = nextQuote;
                endChar = '"';
            }
            if (nextApostrophe != -1 && (start == -1 || nextApostrophe < start)) {
                start = nextApostrophe;
                endChar = '\'';
            }

            if (start == -1) {
                fullComponent.append(Component.literal(msg.substring(index)).withStyle(ChatFormatting.GRAY));
                break;
            }

            int end = msg.indexOf(endChar, start + 1);
            if (end == -1) {
                fullComponent.append(Component.literal(msg.substring(index)).withStyle(ChatFormatting.GRAY));
                break;
            }

            if (start > index) {
                fullComponent.append(Component.literal(msg.substring(index, start)).withStyle(ChatFormatting.GRAY));
            }

            String bracketed = msg.substring(start, end + 1);
            String inner = msg.substring(start + 1, end);

            MutableComponent part;

            switch (endChar) {
                case '>' -> part = Component.literal(bracketed).withStyle(ChatFormatting.AQUA);
                case ']' -> {
                    try {
                        Paths.get(inner);
                    } catch (Exception ignored) {
                        if (inner.contains(":")) {
                            part = Component.literal(bracketed).withStyle(style -> style.withColor(ChatFormatting.AQUA));
                            break;
                        }
                        part = Component.literal(bracketed).withStyle(style -> style.withColor(ChatFormatting.RED));
                        break;
                    }
                    part = Component.literal(abridgeFileName(bracketed)).withStyle(style -> style.withColor(0x3381ff));
                    if (Files.exists(Path.of(inner).toAbsolutePath())) {
                        part = part.withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                                        Path.of(inner).toAbsolutePath().toString()))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("planar_tools.clickToOpen")))
                        );
                    }
                }
                case '}' -> part = Component.literal(bracketed).withStyle(ChatFormatting.GOLD);
                case '"' -> part = Component.literal(bracketed).withStyle(ChatFormatting.GREEN);
                case '\'' -> part = Component.literal(bracketed).withStyle(ChatFormatting.WHITE);
                default -> part = Component.literal(bracketed);
            }

            fullComponent.append(part);
            index = end + 1;
        }

        return fullComponent;
    }

    public static void sendConfigIssuesInChat(Player player) {
        if (!CONFIG_ISSUES.isEmpty()) {
            Collections.sort(CONFIG_ISSUES);
            player.sendSystemMessage(Component.literal("-----------------------------------------------------").withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(
                Component.literal("[" + CONFIG_ISSUES.size() + "] ").withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable("planar_tools.issuesDetected").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" {" + MODID + "}: ").withStyle(ChatFormatting.YELLOW))
            );
            for (int i = 0; i < Math.min(MAX_DISPLAYED_ISSUES, CONFIG_ISSUES.size()); i++) {
                player.sendSystemMessage(CONFIG_ISSUES.get(i).message());
            }

            if (CONFIG_ISSUES.size() > MAX_DISPLAYED_ISSUES) {
                player.sendSystemMessage(Component.literal("")
                    .append(Component.literal("[+" + (CONFIG_ISSUES.size() - MAX_DISPLAYED_ISSUES) + "] ").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable("planar_tools.readMoreAt").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("[config/" + MODID + "/issues.log]").withStyle(style ->
                        style.withColor(0x3381ff)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, Path.of("config/" + MODID + "/issues.log").toAbsolutePath().toString()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("planar_tools.clickToOpen")))
                    ))
                );
            } else {
                player.sendSystemMessage(Component.literal("")
                    .append(Component.translatable("planar_tools.reviewAt").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("[config/" + MODID + "/issues.log]").withStyle(style ->
                        style.withColor(0x3381ff)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, Path.of("config/" + MODID + "/issues.log").toAbsolutePath().toString()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("planar_tools.clickToOpen")))
                    ))
                );
            }
            player.sendSystemMessage(Component.literal("-----------------------------------------------------").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void writeConfigIssuesToFile() {
        if (!wereIssuesWrittenToFile) {
            wereIssuesWrittenToFile = true;
            StringBuilder contents = new StringBuilder();
            for (Issue issue : CONFIG_ISSUES) {
                contents.append(issue.message().getString()).append("\n\n");
            }
            try (FileWriter writer = new FileWriter(Path.of("config/" + MODID + "/issues.log").toAbsolutePath().toString())) {
                //noinspection ResultOfMethodCallIgnored
                Path.of("config/" + MODID + "/").toFile().mkdirs();
                writer.write(contents.toString());
            } catch (IOException io) {
                addConfigIssue(LogLevel.WARN, (byte) 10, "Unexpected Exception occurred while writing [config/planar_tools/issues.log]| Exception: {}", io.getMessage());
            }
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
            addConfigIssue(LogLevel.ERROR, (byte) 10, "Exception encountered during parsing of config file: [{}]. The hardcoded default config will be used | Exception: {}", fileName, e.getMessage());
            if (rewriteIfFailedToParse) {
                addConfigIssue(LogLevel.INFO, (byte) 2, "Rewriting config file: [{}] in response to parsing failure", fileName);
                try (FileWriter writer = new FileWriter(filePath.toFile().getAbsolutePath())) {
                    writer.write(defaultConfig);
                } catch (IOException io) {
                    addConfigIssue(LogLevel.ERROR, (byte) 10, "Unexpected exception encountered during rewriting of faulty config file: [{}] | Exception: {}", fileName, io.getMessage());
                }
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
