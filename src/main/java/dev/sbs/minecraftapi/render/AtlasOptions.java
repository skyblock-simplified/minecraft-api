package dev.sbs.minecraftapi.render;

import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Immutable options for atlas generation, controlling asset paths, output layout,
 * content filtering, texture pack selection, and rendering mode.
 *
 * <p>Use {@link #builder()} to construct a new instance or {@link #mutate()} to derive a modified copy.
 *
 * @see AtlasGenerator#generate(AtlasOptions)
 * @see Builder
 */
@Getter
@RequiredArgsConstructor
public final class AtlasOptions {

    /** Default options using the current working directory for output and all default values. */
    public static final AtlasOptions DEFAULT = builder().build();

    private final @NotNull String assetsDirectory;
    private final @NotNull String outputDirectory;
    private final int tileSize;
    private final int columns;
    private final int rows;
    private final @Nullable ConcurrentList<String> blockFilter;
    private final @Nullable ConcurrentList<String> itemFilter;
    private final boolean includeBlocks;
    private final boolean includeItems;
    private final @Nullable ConcurrentList<String> viewNames;
    private final @Nullable ConcurrentList<String> texturePackDirectories;
    private final @Nullable ConcurrentList<String> texturePackIds;
    private final @Nullable String snbtItemDirectory;
    private final @Nullable String hypixelInventoryFile;
    private final boolean generateDebugBlock;

    /**
     * Creates a new empty {@link Builder}.
     *
     * @return a new builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Creates a {@link Builder} pre-populated with the values from the given options.
     *
     * @param options the options to copy from
     * @return a pre-populated builder
     */
    public static @NotNull Builder from(@NotNull AtlasOptions options) {
        return builder()
            .withAssetsDirectory(options.getAssetsDirectory())
            .withOutputDirectory(options.getOutputDirectory())
            .withTileSize(options.getTileSize())
            .withColumns(options.getColumns())
            .withRows(options.getRows())
            .withBlockFilter(options.getBlockFilter())
            .withItemFilter(options.getItemFilter())
            .isIncludeBlocks(options.isIncludeBlocks())
            .isIncludeItems(options.isIncludeItems())
            .withViewNames(options.getViewNames())
            .withTexturePackDirectories(options.getTexturePackDirectories())
            .withTexturePackIds(options.getTexturePackIds())
            .withSnbtItemDirectory(options.getSnbtItemDirectory())
            .withHypixelInventoryFile(options.getHypixelInventoryFile())
            .isGenerateDebugBlock(options.isGenerateDebugBlock());
    }

    /**
     * Returns a {@link Builder} pre-populated with this instance's values for modification.
     *
     * @return a pre-populated builder
     */
    public @NotNull Builder mutate() {
        return from(this);
    }

    /**
     * Fluent builder for constructing {@link AtlasOptions} instances.
     */
    public static class Builder {

        private String assetsDirectory = "";
        private String outputDirectory = Path.of(System.getProperty("user.dir"), "atlases").toString();
        private int tileSize = 160;
        private int columns = 12;
        private int rows = 12;
        private ConcurrentList<String> blockFilter;
        private ConcurrentList<String> itemFilter;
        private boolean includeBlocks = true;
        private boolean includeItems = true;
        private ConcurrentList<String> viewNames;
        private ConcurrentList<String> texturePackDirectories;
        private ConcurrentList<String> texturePackIds;
        private String snbtItemDirectory;
        private String hypixelInventoryFile;
        private boolean generateDebugBlock;

        /**
         * Sets the Minecraft assets root directory.
         *
         * @param assetsDirectory the assets directory path
         */
        public @NotNull Builder withAssetsDirectory(@NotNull String assetsDirectory) {
            this.assetsDirectory = assetsDirectory;
            return this;
        }

        /**
         * Sets the output directory for generated atlas images.
         *
         * @param outputDirectory the output directory path
         */
        public @NotNull Builder withOutputDirectory(@NotNull String outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        /**
         * Sets the pixel size of each rendered tile.
         *
         * @param tileSize the tile size in pixels
         */
        public @NotNull Builder withTileSize(int tileSize) {
            this.tileSize = tileSize;
            return this;
        }

        /**
         * Sets the number of columns per atlas page.
         *
         * @param columns the column count
         */
        public @NotNull Builder withColumns(int columns) {
            this.columns = columns;
            return this;
        }

        /**
         * Sets the number of rows per atlas page.
         *
         * @param rows the row count
         */
        public @NotNull Builder withRows(int rows) {
            this.rows = rows;
            return this;
        }

        /**
         * Sets the block name filter. Only blocks matching these names are included.
         *
         * @param blockFilter the block name filter, or null for all blocks
         */
        public @NotNull Builder withBlockFilter(@Nullable ConcurrentList<String> blockFilter) {
            this.blockFilter = blockFilter;
            return this;
        }

        /**
         * Sets the item name filter. Only items matching these names are included.
         *
         * @param itemFilter the item name filter, or null for all items
         */
        public @NotNull Builder withItemFilter(@Nullable ConcurrentList<String> itemFilter) {
            this.itemFilter = itemFilter;
            return this;
        }

        /**
         * Sets whether blocks are included in the atlas.
         *
         * @param includeBlocks {@code true} to include blocks
         */
        public @NotNull Builder isIncludeBlocks(boolean includeBlocks) {
            this.includeBlocks = includeBlocks;
            return this;
        }

        /**
         * Sets whether items are included in the atlas.
         *
         * @param includeItems {@code true} to include items
         */
        public @NotNull Builder isIncludeItems(boolean includeItems) {
            this.includeItems = includeItems;
            return this;
        }

        /**
         * Sets the view names to generate. If null, all default views are used.
         *
         * @param viewNames the view name filter, or null for all views
         */
        public @NotNull Builder withViewNames(@Nullable ConcurrentList<String> viewNames) {
            this.viewNames = viewNames;
            return this;
        }

        /**
         * Sets the directories to scan for texture packs.
         *
         * @param texturePackDirectories the texture pack directories, or null for none
         */
        public @NotNull Builder withTexturePackDirectories(@Nullable ConcurrentList<String> texturePackDirectories) {
            this.texturePackDirectories = texturePackDirectories;
            return this;
        }

        /**
         * Sets the texture pack IDs to apply.
         *
         * @param texturePackIds the pack IDs to apply, or null for none
         */
        public @NotNull Builder withTexturePackIds(@Nullable ConcurrentList<String> texturePackIds) {
            this.texturePackIds = texturePackIds;
            return this;
        }

        /**
         * Sets the SNBT item directory for rendering SNBT item stacks.
         *
         * @param snbtItemDirectory the SNBT directory path, or null to skip
         */
        public @NotNull Builder withSnbtItemDirectory(@Nullable String snbtItemDirectory) {
            this.snbtItemDirectory = snbtItemDirectory;
            return this;
        }

        /**
         * Sets the Hypixel inventory file for rendering inventory data.
         *
         * @param hypixelInventoryFile the inventory file path, or null to skip
         */
        public @NotNull Builder withHypixelInventoryFile(@Nullable String hypixelInventoryFile) {
            this.hypixelInventoryFile = hypixelInventoryFile;
            return this;
        }

        /**
         * Sets whether to generate the debug block atlas.
         *
         * @param generateDebugBlock {@code true} to generate a debug block
         */
        public @NotNull Builder isGenerateDebugBlock(boolean generateDebugBlock) {
            this.generateDebugBlock = generateDebugBlock;
            return this;
        }

        public @NotNull AtlasOptions build() {
            return new AtlasOptions(
                assetsDirectory, outputDirectory, tileSize, columns, rows,
                blockFilter, itemFilter, includeBlocks, includeItems, viewNames,
                texturePackDirectories, texturePackIds, snbtItemDirectory,
                hypixelInventoryFile, generateDebugBlock
            );
        }
    }

}
