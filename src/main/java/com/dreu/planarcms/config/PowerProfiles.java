package com.dreu.planarcms.config;

import com.dreu.planarcms.config.BlocksConfig.ToolProfile;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

public final class PowerProfiles {
  private static final int[] NO_POWERS = new int[0];
  private final int[] powers;
  private final ToolProfile[] profiles;

  private PowerProfiles(int[] powers, ToolProfile[] profiles) {
    this.powers = powers;
    this.profiles = profiles;
  }

  public static PowerProfiles constant(ToolProfile profile) {
    return new PowerProfiles(NO_POWERS, new ToolProfile[] {Objects.requireNonNull(profile)});
  }

  public static PowerProfiles of(ToolProfile baseline, Map<Integer, ToolProfile> changes) {
    if (changes.isEmpty()) return constant(baseline);
    int[] powers = new int[changes.size()];
    ToolProfile[] profiles = new ToolProfile[changes.size() + 1];
    profiles[0] = Objects.requireNonNull(baseline);
    int size = 0;
    for (var change : new TreeMap<>(changes).entrySet()) {
      if (change.getKey() < 0) throw new IllegalArgumentException("Power cannot be negative");
      ToolProfile profile = Objects.requireNonNull(change.getValue());
      if (profile.equals(profiles[size])) continue;
      powers[size++] = change.getKey();
      profiles[size] = profile;
    }
    return new PowerProfiles(size == 0 ? NO_POWERS : Arrays.copyOf(powers, size), Arrays.copyOf(profiles, size + 1));
  }

  public ToolProfile atPower(int power) {
    int low = 0;
    int high = powers.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (powers[middle] <= power) low = middle + 1;
      else high = middle;
    }
    return profiles[low];
  }

  public ToolProfile baseline() {
    return profiles[0];
  }

  public int size() {
    return powers.length;
  }

  public int power(int index) {
    return powers[index];
  }

  public ToolProfile profile(int index) {
    return profiles[index + 1];
  }

  public void write(FriendlyByteBuf buf) {
    buf.writeInt(powers.length);
    writeProfile(buf, profiles[0]);
    for (int i = 0; i < powers.length; i++) {
      buf.writeInt(powers[i]);
      writeProfile(buf, profiles[i + 1]);
    }
  }

  public static PowerProfiles read(FriendlyByteBuf buf) {
    int size = buf.readInt();
    // Baseline: 10 bytes minimum; each threshold adds 4 bytes plus another profile.
    if (size < 0 || buf.readableBytes() < 10 || size > (buf.readableBytes() - 10) / 14)
      throw new IllegalArgumentException("Invalid power profile count");
    ToolProfile[] profiles = new ToolProfile[size + 1];
    int[] powers = size == 0 ? NO_POWERS : new int[size];
    profiles[0] = readProfile(buf);
    int previous = -1;
    for (int i = 0; i < size; i++) {
      int power = buf.readInt();
      if (power <= previous) throw new IllegalArgumentException("Power thresholds must be nonnegative and ascending");
      powers[i] = power;
      profiles[i + 1] = readProfile(buf);
      previous = power;
    }
    return new PowerProfiles(powers, profiles);
  }

  private static void writeProfile(FriendlyByteBuf buf, ToolProfile profile) {
    buf.writeInt(profile.resistance());
    buf.writeBoolean(profile.applyMiningSpeed());
    buf.writeBoolean(profile.canDrop().isPresent());
    profile.canDrop().ifPresent(buf::writeBoolean);
    buf.writeFloat(profile.miningSpeedBonus());
  }

  private static ToolProfile readProfile(FriendlyByteBuf buf) {
    return new ToolProfile(buf.readInt(), buf.readBoolean(), buf.readBoolean() ? Optional.of(buf.readBoolean()) : Optional.empty(), buf.readFloat());
  }

  public static PowerProfiles merged(PowerProfiles left, PowerProfiles right) {
    TreeSet<Integer> powers = new TreeSet<>();
    for (int power : left.powers) powers.add(power);
    for (int power : right.powers) powers.add(power);
    Map<Integer, ToolProfile> changes = new TreeMap<>();
    for (int power : powers) {
      changes.put(power, ToolProfile.merged(left.atPower(power), right.atPower(power)));
    }
    return of(ToolProfile.merged(left.baseline(), right.baseline()), changes);
  }

  public static PowerProfiles overridden(PowerProfiles inherited, Patch baseline, Map<Integer, Patch> changes) {
    TreeSet<Integer> powers = new TreeSet<>(changes.keySet());
    for (int power : inherited.powers) powers.add(power);
    Map<Integer, ToolProfile> profiles = new TreeMap<>();
    Patch active = baseline;
    // Keep the explicit fields, not a resolved snapshot: omitted fields must follow inherited changes.
    for (int power : powers) {
      Patch change = changes.get(power);
      if (change != null) active = active.overridden(change);
      profiles.put(power, active.apply(inherited.atPower(power)));
    }
    return of(baseline.apply(inherited.baseline()), profiles);
  }

  public record Patch(Optional<Integer> resistance, Optional<Boolean> applyMiningSpeed, Optional<Boolean> canDrop, Optional<Float> miningSpeedBonus) {
    public static final Patch EMPTY = new Patch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public Patch(Optional<Integer> resistance, Optional<Boolean> applyMiningSpeed, Optional<Boolean> canDrop) {
      this(resistance, applyMiningSpeed, canDrop, Optional.empty());
    }

    public ToolProfile apply(ToolProfile inherited) {
      return new ToolProfile(
          resistance.orElse(inherited.resistance()),
          applyMiningSpeed.orElse(inherited.applyMiningSpeed()),
          canDrop.isPresent() ? canDrop : inherited.canDrop(),
          miningSpeedBonus.orElse(inherited.miningSpeedBonus())
      );
    }

    public Patch overridden(Patch incoming) {
      return new Patch(
          incoming.resistance.isPresent() ? incoming.resistance : resistance,
          incoming.applyMiningSpeed.isPresent() ? incoming.applyMiningSpeed : applyMiningSpeed,
          incoming.canDrop.isPresent() ? incoming.canDrop : canDrop,
          incoming.miningSpeedBonus.isPresent() ? incoming.miningSpeedBonus : miningSpeedBonus
      );
    }
  }
}
