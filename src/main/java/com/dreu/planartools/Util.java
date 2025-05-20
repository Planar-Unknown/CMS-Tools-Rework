package com.dreu.planartools;

import com.dreu.planartools.config.ToolsConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
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
        TRACE(LOGGER::trace, "<TRACE>", 0xAAAAFF, "Fine-grained debug info"),
        DEBUG(LOGGER::debug, "<DEBUG>", 0x55FF55, "General debugging output"),
        INFO(LOGGER::info, "<INFO>", 0xFFFF55, "Something minor went wrong, but its handled"),
        WARN(LOGGER::warn, "<WARN>", 0xFFAA00, "Something unexpected happened, may or may not cause issues"),
        ERROR(LOGGER::error, "<ERROR>", 0xFF5555, "A serious problem occurred, your game WILL crash at some point");

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
            func.accept(message, args);
        }

        public MutableComponent header() {
            return header;
        }
    }

    public static String logFileName(String fullFileName) {
        String logFileName = fullFileName;
        if (logFileName.contains("config/"))
            logFileName = logFileName.replace("config/", "");
        if (logFileName.contains("planar_tools/"))
            logFileName = logFileName.replace("planar_tools/", "");
        return logFileName;
    }

    public record Issue(Component message, byte priority) implements Comparable<Issue> {
        @Override
        public int compareTo(@NotNull Issue other) {
            return Byte.compare(other.priority, this.priority);
        }
    }

    public static final ArrayList<Issue> CONFIG_ISSUES = new ArrayList<>();

    public static void addConfigIssue(LogLevel level, byte priority, String message, Object... args) {
        level.log(message, args);
        String formattable = message.replace("{}", "%s");
        CONFIG_ISSUES.add(new Issue(level.header().append(formatError(String.format(formattable, args))), priority));
        Collections.sort(CONFIG_ISSUES);
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
                case '>':
                    part = Component.literal(bracketed).withStyle(ChatFormatting.AQUA);
                    break;
                case ']':
                    part = Component.literal(bracketed).withStyle(ChatFormatting.GOLD);
                    if (Files.exists(Path.of("config/planar_tools/" + inner).toAbsolutePath())) {
                        part = part.withStyle(style ->
                                style.withClickEvent(new ClickEvent(
                                        ClickEvent.Action.OPEN_FILE,
                                        Path.of("config/planar_tools/" + inner).toAbsolutePath().toString()
                                )).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("planar_tools.clickToOpen")))
                        );
                    }
                    break;
                case '}':
                    part = Component.literal(bracketed).withStyle(ChatFormatting.YELLOW);
                    break;
                case '"':
                    part = Component.literal(bracketed).withStyle(ChatFormatting.GREEN);
                    break;
                case '\'':
                    part = Component.literal(bracketed).withStyle(ChatFormatting.WHITE);
                    break;
                default:
                    part = Component.literal(bracketed);
            }

            fullComponent.append(part);
            index = end + 1;
        }

        return fullComponent;
    }




//    public static @NotNull MutableComponent formatError(String msg) {
//        MutableComponent fullComponent = Component.empty();
//        msg = " " + msg;
//
//        int index = 0;
//        while (index < msg.length()) {
//            int nextBracket = msg.indexOf('[', index);
//            int nextAngle = msg.indexOf('<', index);
//            int nextCurly = msg.indexOf('{', index);
//
//            int start;
//            char endChar;
//
//            if (nextBracket != -1 && (nextAngle == -1 || nextBracket < nextAngle) && (nextCurly == -1 || nextBracket < nextCurly)) {
//                start = nextBracket;
//                endChar = ']';
//            } else if (nextAngle != -1 && (nextCurly == -1 || nextAngle < nextCurly)) {
//                start = nextAngle;
//                endChar = '>';
//            } else if (nextCurly != -1) {
//                start = nextCurly;
//                endChar = '}';
//            } else {
//                fullComponent.append(Component.literal(msg.substring(index)).withStyle(ChatFormatting.GRAY));
//                break;
//            }
//
//            int end = msg.indexOf(endChar, start);
//            if (end == -1) {
//                fullComponent.append(Component.literal(msg.substring(index)).withStyle(ChatFormatting.GRAY));
//                break;
//            }
//
//            if (start > index) {
//                fullComponent.append(Component.literal(msg.substring(index, start)).withStyle(ChatFormatting.GRAY));
//            }
//
//            String bracketed = msg.substring(start, end + 1);
//            String inner = msg.substring(start + 1, end);
//
//            MutableComponent part;
//            if (endChar == '>') {
//                part = Component.literal(bracketed).withStyle(ChatFormatting.AQUA);
//            } else if (endChar == ']') {
//                part = Component.literal(bracketed).withStyle(ChatFormatting.GOLD);
//                if (Files.exists(Path.of(inner).toAbsolutePath())) {
//                    part = part.withStyle(style ->
//                            style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, Path.of(inner).toAbsolutePath().toString()))
//                    );
//                }
//            } else {
//                part = Component.literal(bracketed).withStyle(ChatFormatting.YELLOW);
//            }
//
//            fullComponent.append(part);
//            index = end + 1;
//        }
//
//        return fullComponent;
//    }


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
