package com.dreu.planarcms.util;

import com.dreu.planarcms.config.*;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.dreu.planarcms.PlanarCMS.*;
import static com.dreu.planarcms.config.BlocksConfig.populateBlocks;
import static com.dreu.planarcms.config.EnchantsConfig.populateEnchants;
import static com.dreu.planarcms.config.GeneralConfig.PRESET_FOLDER_NAME;
import static com.dreu.planarcms.config.ToolsConfig.populateToolTypes;
import static com.dreu.planarcms.config.ToolsConfig.populateTools;
import static com.dreu.planarcms.util.Helpers.LogLevel.WARN;
import static java.lang.String.format;


public class Helpers {
    public static boolean displayTooltips = true;
    public static WailaPosition WAILA_POSITION = WailaPosition.INVISIBLE;
    public static boolean configHasBeenParsed = false;
    public static boolean wereIssuesWrittenToFile = false;
    public static final byte MAX_DISPLAYED_ISSUES = 3;

    public enum WailaPosition {
        INVISIBLE,
        LEFT,
        MIDDLE,
        RIGHT;

        public WailaPosition next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public static void toggleTooltipDisplay() {
        displayTooltips = !displayTooltips;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("")
                .append(Component.literal("[" + MODID + "] ").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable( MODID + ".displayTooltips")).append(Component.literal(": "))
                .append(displayTooltips
                    ? Component.translatable("options.on").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("options.off").withStyle(ChatFormatting.RED)
                )
            );
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public static String getItemId(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).toString();
    }

    @SuppressWarnings("DataFlowIssue")
    public static String getBlockId(Block block) {
        return ForgeRegistries.BLOCKS.getKey(block).toString();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static boolean isValidItem(String itemId, Optional<String> collectionName, String fileName) {
        if (!ResourceLocation.isValidResourceLocation(itemId)) {
            addConfigIssue(LogLevel.INFO, (byte) 2, "Not a valid Item ResourceLocation: <{}> declared in {} | Skipping Item...", itemId, collectionName.map(s -> "collection: [" + s + "]").orElseGet(() ->  "config: [" + PRESET_FOLDER_NAME + fileName + "]"));
            return false;
        }
        if (!ModList.get().isLoaded(itemId.split(":")[0])) {
            addConfigIssue(LogLevel.INFO, (byte) 2, "{} declared Tool Power values for <{}> but mod '{{}}' is not loaded | Skipping Item...", collectionName.map(s -> "Collection: [" + s + "]").orElseGet(() ->  "Config: [" + PRESET_FOLDER_NAME + "tools.toml]"), itemId, itemId.split(":")[0]);
            return false;
        }
        if (!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemId))) {
            addConfigIssue(LogLevel.INFO, (byte) 2, "{} declared item <{}> which does not exist, check for typos! | Skipping Item...", collectionName.map(s -> "Collection: [" + s + "]").orElseGet(() ->  "Config: [" + PRESET_FOLDER_NAME + "tools.toml]"), itemId);
            return false;
        }
        return true;
    }
    public static <T> T tryCast(Object value, Class<T> clazz, String key, String fileName) {
        try {
            return clazz.cast(value);
        } catch (Exception e) {
            addConfigIssue(LogLevel.ERROR, (byte) 7, "Value: \"{}\" for '{}' is an invalid type in config [{}] | Expected: '{}' but got: '{}' | Skipping power type...", value, key, PRESET_FOLDER_NAME + fileName, clazz.getSimpleName(), value.getClass().getSimpleName());
            return null;
        }
    }

    public static void parseAndProcessConfig() {
        CONFIG_ISSUES.clear();
        wereIssuesWrittenToFile = false;
        GeneralConfig.parse();
        GeneralConfig.populate();
        if (GeneralConfig.needsRepair) GeneralConfig.repair();
        CollectionsConfig.parseAndPopulate();
        BlocksConfig.parse();
        ToolsConfig.parse();
        EnchantsConfig.parse();
        populateToolTypes();
        populateTagKeys();
        populateTools();
        populateBlocks();
        populateEnchants();
        configHasBeenParsed = true;
        writeConfigIssuesToFile();
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
                    .translatable(MODID + ".logTitle." + title)
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
        if (logFileName.contains(MODID + "/"))
            logFileName = logFileName.replace(MODID + "/", "");
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
                                        Component.translatable(MODID + ".clickToOpen")))
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
                    .append(Component.translatable(MODID + ".issuesDetected").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" {" + MODID + "}: ").withStyle(ChatFormatting.YELLOW))
            );
            for (int i = 0; i < Math.min(MAX_DISPLAYED_ISSUES, CONFIG_ISSUES.size()); i++) {
                player.sendSystemMessage(CONFIG_ISSUES.get(i).message());
            }

            if (CONFIG_ISSUES.size() > MAX_DISPLAYED_ISSUES) {
                player.sendSystemMessage(Component.literal("")
                    .append(Component.literal("[+" + (CONFIG_ISSUES.size() - MAX_DISPLAYED_ISSUES) + "] ").withStyle(ChatFormatting.GREEN))
                    .append(Component.translatable(MODID + ".readMoreAt").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("[config/" + MODID + "/issues.log]").withStyle(style ->
                        style.withColor(0x3381ff)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, Path.of("config/" + MODID + "/issues.log").toAbsolutePath().toString()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(MODID + ".clickToOpen")))
                    ))
                );
            } else {
                player.sendSystemMessage(Component.literal("")
                    .append(Component.translatable(MODID + ".reviewAt").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("[config/" + MODID + "/issues.log]").withStyle(style ->
                        style.withColor(0x3381ff)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, Path.of("config/" + MODID + "/issues.log").toAbsolutePath().toString()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(MODID + ".clickToOpen")))
                    ))
                );
            }
            player.sendSystemMessage(Component.literal("-----------------------------------------------------").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void writeConfigIssuesToFile() {
        if (!wereIssuesWrittenToFile) {
            wereIssuesWrittenToFile = true;
            Collections.sort(CONFIG_ISSUES);
            StringBuilder contents = new StringBuilder();
            for (Issue issue : CONFIG_ISSUES) {
                contents.append(issue.message().getString()).append("\n\n");
            }
            try (FileWriter writer = new FileWriter(Path.of("config/" + MODID + "/issues.log").toAbsolutePath().toString())) {
                //noinspection ResultOfMethodCallIgnored
                Path.of("config/" + MODID + "/").toFile().mkdirs();
                writer.write(contents.toString());
            } catch (IOException io) {
                addConfigIssue(LogLevel.ERROR, (byte) 10, "Unexpected Exception occurred while writing [config/" + MODID + "/issues.log]| Exception: {}", io.getMessage());
            }
        }
    }

    public static Config parseFileOrDefault(String fileName, String defaultConfig) {
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
            return new TomlParser().parse(defaultConfig);
        }
    }

    public static <T> T getOrElse(Config config, String parentKey, String key, T fallback, Class<T> clazz, String fileName) {
        try {
            Object value = config.get(key);
            if (value == null) return fallback;
            return clazz.cast(value);
        } catch (ClassCastException e) {
            addConfigIssue(WARN, (byte) 4,
                "Value: \"{}\" for \"{}.{}\" is an invalid type in config [{}] | Expected: '{}' but got: '{}' | Ignoring property...",
                config.get(key), parentKey, key, PRESET_FOLDER_NAME + fileName,
                clazz.getSimpleName(), config.get(key).getClass().getSimpleName());
            return fallback;
        }
    }

    public static Tier getTierIfPresent(byte toolType, ToolsConfig.Properties toolProperties) {
        Integer power = toolProperties.powers().get(toolType);
        if (power == null || power < 20) return null;
        if (power < 40) return Tiers.WOOD;
        if (power < 60) return Tiers.STONE;
        if (power < 80) return Tiers.IRON;
        if (power < 100) return Tiers.DIAMOND;
        return Tiers.NETHERITE;
    }

    public static <K, V> Map<K, V> mergeMaps(Map<K, V> left, Map<K, V> right, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Map<K, V> mergedMap = new HashMap<>(left);
        right.forEach((key, value) -> mergedMap.merge(key, value, remappingFunction));
        return mergedMap;
    }
}
