package dev.sbs.minecraftapi.asset;

import org.jetbrains.annotations.NotNull;

/**
 * A single asset namespace entry with its filesystem path, source identifier, and vanilla flag.
 */
public record AssetNamespace(@NotNull String name, @NotNull String path, @NotNull String sourceId, boolean vanilla) {}
