package com.dreu.planarcms.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dreu.planarcms.util.Helpers.LogLevel.ERROR;
import static com.dreu.planarcms.util.Helpers.addConfigIssue;

/** File schema upgrades only. Each map entry transforms n into n+1. */
public final class ConfigUpgrades {
  public static final String VERSION = "ConfigVersion";
  private static final Pattern VERSION_LINE = Pattern.compile("(?m)^[ \\t]*(?:ConfigVersion|\"ConfigVersion\"|'ConfigVersion')[ \\t]*=[^\\r\\n#]*");

  private ConfigUpgrades() {}

  public static Config load(Path file, String defaults, int latest, Map<Integer, Consumer<Config>> steps) {
    CommentedConfig current;
    String savedContents;
    try {
      if (!Files.exists(file)) {
        current = parse(defaults);
        current.set(VERSION, latest);
        try {
          write(file, withVersion(defaults, current));
        } catch (Exception e) {
          report(file, "Cannot save new config; using defaults in memory", e);
        }
        return current;
      }
      savedContents = Files.readString(file);
      current = parse(savedContents);
    } catch (Exception e) {
      report(file, "Cannot parse config; using defaults without replacing the file", e);
      return parse(defaults);
    }

    final int storedVersion;
    try {
      storedVersion = version(current, latest);
    } catch (IllegalArgumentException e) {
      report(file, "Unsupported config version; leaving file unchanged and using defaults", e);
      return parse(defaults);
    }
    int version = storedVersion;
    while (version < latest) {
      try {
        Consumer<Config> step = version == 0 ? config -> {} : steps.get(version);
        if (step == null) throw new IllegalArgumentException("Missing upgrade " + version + " -> " + (version + 1));
        // Reparse for a deep candidate copy: a failed step cannot mutate the last saved config.
        CommentedConfig candidate = parse(savedContents);
        step.accept(candidate);
        candidate.set(VERSION, version + 1);
        String contents = withVersion(savedContents, candidate);
        if (!candidate.equals(parse(contents))) throw new IllegalArgumentException("Upgrade did not survive TOML round trip");
        Path backup = file.resolveSibling(file.getFileName() + ".pre-upgrade.bak");
        if (!Files.exists(backup)) write(backup, savedContents);
        if (!Files.isRegularFile(backup)) throw new IOException("Backup is not a regular file: " + backup);
        write(file, contents);
        current = candidate;
        savedContents = contents;
        version++;
      } catch (Exception e) {
        report(file, "Upgrade " + version + " -> " + (version + 1) + " stopped; retaining last saved config", e);
        break;
      }
    }
    return current;
  }

  private static int version(Config config, int latest) {
    if (!config.contains(VERSION)) return 0;
    Object value = config.get(VERSION);
    if (!(value instanceof Integer number) || number < 1 || number > latest)
      throw new IllegalArgumentException("Unsupported " + VERSION + " = " + value + " (latest " + latest + ")");
    return number;
  }

  static CommentedConfig parse(String contents) {
    CommentedConfig config = CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
    new TomlParser().parse(new StringReader(contents), config, ParsingMode.REPLACE);
    return config;
  }

  static Map<String, Object> entries(Config config) {
    // Recreate the original parser's source order, excluding metadata before the map can resize.
    Map<String, Object> entries = Config.<Object>getDefaultMapCreator(false).get();
    config.valueMap().forEach((key, value) -> {
      if (!key.equals(VERSION)) entries.put(key, value);
    });
    return entries;
  }

  static String format(Config config) {
    TomlWriter writer = new TomlWriter();
    // Keep scalar property tables compact when saving edited settings.
    writer.setWriteTableInlinePredicate(table -> table.valueMap().values().stream()
        .noneMatch(value -> value instanceof Config || value instanceof java.util.List<?>));
    writer.setIndentArrayElementsPredicate(list -> !list.isEmpty() && list.get(0) instanceof Config);
    writer.setIndent("  ");
    return writer.writeToString(config);
  }

  private static String withVersion(String original, Config candidate) {
    String assignment = VERSION + " = " + candidate.getInt(VERSION);
    Config previous = parse(original);
    if (!previous.contains(VERSION)) {
      String stamped = assignment + "\n" + original;
      if (candidate.equals(parse(stamped))) return stamped;
    } else {
      // NightConfig 3.6 expands arrays into repeated tables. A metadata-only upgrade keeps
      // the source text, but only after the structured parser verifies the entire result.
      Matcher matches = VERSION_LINE.matcher(original);
      while (matches.find()) {
        String stamped = original.substring(0, matches.start()) + assignment + original.substring(matches.end());
        if (candidate.equals(parse(stamped))) return stamped;
      }
    }
    return format(candidate);
  }

  public static boolean save(Path file, String contents, int latest) {
    try {
      if (Files.exists(file)) version(parse(Files.readString(file)), latest);
      Config candidate = parse(contents);
      if (version(candidate, latest) != latest) throw new IllegalArgumentException("Save must include the current version");
      write(file, contents);
      return true;
    } catch (Exception e) {
      report(file, "Cannot save config; leaving existing file unchanged", e);
      return false;
    }
  }

  private static void write(Path file, String contents) throws IOException {
    Path absolute = file.toAbsolutePath();
    Files.createDirectories(absolute.getParent());
    Path temporary = Files.createTempFile(absolute.getParent(), absolute.getFileName() + ".", ".tmp");
    try {
      Files.writeString(temporary, contents);
      try {
        Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static void report(Path file, String message, Exception e) {
    addConfigIssue(ERROR, (byte) 10, "{} [{}] | Exception: {}", message, file, e.getMessage());
  }
}
