package com.dreu.planartools.util;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public record OpposingSets<T>(Set<T> positive, Set<T> negative) {
  public OpposingSets() {
    this(new HashSet<>(), new HashSet<>());
  }

  @SuppressWarnings("unused")
  public OpposingSets(OpposingSets<T> sets) {
    this(sets.positive, sets.negative);
  }

  public void addPositive(T t) {
    if (!negative.contains(t))
      positive.add(t);
  }

  public void addNegative(T t) {
    negative.add(t);
    positive.remove(t);
  }

  public void clear() {
    positive.clear();
    negative.clear();
  }

  @SuppressWarnings("unused")
  public void addAll(OpposingSets<T> sets) {
    for (T neg : sets.negative()) addNegative(neg);
    for (T pos : sets.positive()) addPositive(pos);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isEmpty() {
    return negative.isEmpty() && positive.isEmpty();
  }

  public void mergeDominantly(OpposingSets<T> incoming) {
    incoming.negative.forEach(t -> {
      if (!this.positive.contains(t))
        this.negative.add(t);
    });
    incoming.positive.forEach(t -> {
      if (!this.negative.contains(t))
        this.positive.add(t);
    });
  }

  public static <T> OpposingSets<T> mergeLeftWins(OpposingSets<T> left, OpposingSets<T> right) {
    OpposingSets<T> merged = new OpposingSets<>();
    merged.negative.addAll(left.negative);
    merged.positive.addAll(left.positive);
    right.negative.forEach(t -> {
      if (!merged.positive.contains(t))
        merged.negative.add(t);
    });
    right.positive.forEach(t -> {
      if (!merged.negative.contains(t))
        merged.positive.add(t);
    });
    return merged;
  }

  public static <T> OpposingSets<T> merge(OpposingSets<T> left, OpposingSets<T> right) {
    if (left == null && right == null) return new OpposingSets<>();
    if (left == null) return right;
    if (right == null) return left;

    OpposingSets<T> merged = new OpposingSets<>();
    merged.negative.addAll(left.negative);
    merged.negative.addAll(right.negative);
    left.positive.forEach(merged::addPositive);
    right.positive.forEach(merged::addPositive);
    return merged;
  }
}
