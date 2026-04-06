package dev.sbs.minecraftapi.asset;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration options for loading Minecraft assets via {@link MinecraftAssetFactory}.
 *
 * <p>Either {@code versionId} or {@code assetsDirectory} must be set, but not both.
 * When {@code versionId} is set, assets are downloaded and cached under {@code cacheRoot}.
 * When {@code assetsDirectory} is set, assets are loaded from a pre-existing extracted directory.
 */
@Getter
public final class MinecraftAssetOptions {

    private static final @NotNull Path DEFAULT_CACHE_ROOT = Path.of(System.getProperty("user.dir"), "cache");

    private final @Nullable String versionId;
    private final @Nullable String assetsDirectory;
    private final @NotNull Path cacheRoot;
    private final @NotNull List<String> texturePackDirectories;

    private MinecraftAssetOptions(@Nullable String versionId, @Nullable String assetsDirectory,
                                   @NotNull Path cacheRoot, @NotNull List<String> texturePackDirectories) {
        this.versionId = versionId;
        this.assetsDirectory = assetsDirectory;
        this.cacheRoot = cacheRoot;
        this.texturePackDirectories = Collections.unmodifiableList(texturePackDirectories);
    }

    /**
     * Creates a new builder for constructing asset options.
     *
     * @return a new builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for constructing {@link MinecraftAssetOptions} instances.
     */
    public static final class Builder {

        private @Nullable String versionId;
        private @Nullable String assetsDirectory;
        private @NotNull Path cacheRoot = DEFAULT_CACHE_ROOT;
        private @NotNull List<String> texturePackDirectories = new ArrayList<>();

        /**
         * Sets the Minecraft version to download and cache.
         *
         * @param versionId the version (e.g. "1.21.4"), or null for the latest release
         * @return this builder
         */
        public @NotNull Builder withVersionId(@Nullable String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * Sets the pre-existing extracted assets directory to load from.
         *
         * @param assetsDirectory the Minecraft assets root (the {@code minecraft/} directory)
         * @return this builder
         */
        public @NotNull Builder withAssetsDirectory(@NotNull String assetsDirectory) {
            this.assetsDirectory = assetsDirectory;
            return this;
        }

        /**
         * Sets the root directory for version-specific asset caches.
         *
         * @param cacheRoot the cache root directory
         * @return this builder
         */
        public @NotNull Builder withCacheRoot(@NotNull Path cacheRoot) {
            this.cacheRoot = cacheRoot;
            return this;
        }

        /**
         * Sets the directories to scan for resource packs.
         *
         * @param directories the texture pack directories
         * @return this builder
         */
        public @NotNull Builder withTexturePackDirectories(@NotNull List<String> directories) {
            this.texturePackDirectories = new ArrayList<>(directories);
            return this;
        }

        /**
         * Sets the directories to scan for resource packs.
         *
         * @param directories the texture pack directories
         * @return this builder
         */
        public @NotNull Builder withTexturePackDirectories(@NotNull Iterable<String> directories) {
            this.texturePackDirectories = new ArrayList<>();
            directories.forEach(this.texturePackDirectories::add);
            return this;
        }

        public @NotNull MinecraftAssetOptions build() {
            return new MinecraftAssetOptions(versionId, assetsDirectory, cacheRoot, texturePackDirectories);
        }
    }
}
