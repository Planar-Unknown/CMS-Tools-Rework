package com.dreu.planartools.util;

import java.util.function.Supplier;

import static com.dreu.planartools.util.Helpers.shouldUpdateTime;

@SuppressWarnings("unused")
public class CachedSupplier<T> {
  private int lastUpdateTime;
  private final Supplier<T> supplier;
  private T cache = null;

  private CachedSupplier (Supplier<T> delegate) {
    this.supplier = delegate;
  }

  public T get() {
    if (lastUpdateTime != shouldUpdateTime) {
      lastUpdateTime = shouldUpdateTime;
      return cache = supplier.get();
    }
    return cache == null ? cache = supplier.get() : cache;
  }

  public T getFresh() {
    return cache = supplier.get();
  }

  public void clear() {
    cache = null;
  }

  public static <T> CachedSupplier<T> of(Supplier<T> delegate) {
    return new CachedSupplier<>(delegate);
  }
}
