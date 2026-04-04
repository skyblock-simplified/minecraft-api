package dev.sbs.minecraftapi.render;

import com.google.gson.Gson;
import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.RendererApi;
import dev.sbs.minecraftapi.asset.MinecraftAssetOptions;
import dev.sbs.minecraftapi.asset.ResourcePackDiscovery;
import dev.sbs.minecraftapi.asset.context.AssetContext;
import dev.sbs.minecraftapi.nbt.NbtFactory;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.ItemRenderData;
import dev.sbs.minecraftapi.render.context.RenderContext;
import dev.sbs.minecraftapi.render.hypixel.HypixelItemData;
import dev.sbs.minecraftapi.render.hypixel.InventoryParser;
import dev.sbs.minecraftapi.render.hypixel.LegacyItemMappings;
import dev.sbs.minecraftapi.render.hypixel.TextureResolver;
import dev.sbs.minecraftapi.render.resolver.ResourceIdResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Batch generation of paginated atlas sets from blocks, items, SNBT files, and Hypixel
 * inventory data.
 */
public final class AtlasGenerator {

    private static final Gson GSON = SimplifiedApi.getGson();

    private AtlasGenerator() {
    }

    // ----------------------------------------------------------------
    // Public inner types
    // ----------------------------------------------------------------

    /**
     * A named view configuration pairing a label with block render options.
     */
    @Getter
    @RequiredArgsConstructor
    public static final class AtlasView {

        private final @NotNull String name;
        private final @NotNull BlockRenderOptions options;
    }

    /**
     * Metadata about a single generated atlas page.
     */
    @Getter
    @RequiredArgsConstructor
    public static final class AtlasResult {

        private final @NotNull String category;
        private final @NotNull String viewName;
        private final int pageNumber;
        private final @NotNull String imagePath;
        private final @NotNull String manifestPath;
    }

    /**
     * A manifest entry describing a single tile in an atlas page.
     */
    @Getter
    @AllArgsConstructor
    public static final class AtlasManifestEntry {

        private final int sequentialIndex;
        private final @NotNull String name;
        private final int column;
        private final int row;
        private final @Nullable String error;
        private final @Nullable String model;
        private final @Nullable List<String> textures;
        private final @Nullable String texturePack;

        public AtlasManifestEntry(int sequentialIndex, @NotNull String name, int column, int row,
                                  @Nullable String error) {
            this(sequentialIndex, name, column, row, error, null, null, null);
        }
    }

    /**
     * A loaded SNBT item entry representing a single file from a directory scan.
     */
    @Getter
    @RequiredArgsConstructor
    public static final class SnbtItemEntry {

        private final @NotNull String name;
        private final @NotNull String sourcePath;
        private final @Nullable CompoundTag rootCompound;
        private final @Nullable String error;
    }

    // ----------------------------------------------------------------
    // Default views
    // ----------------------------------------------------------------

    /**
     * The default set of atlas views covering isometric right, isometric left, and front perspectives.
     */
    public static final List<AtlasView> DEFAULT_VIEWS = List.of(
        new AtlasView("isometric_right", BlockRenderOptions.DEFAULT),
        new AtlasView("isometric_left", BlockRenderOptions.builder().withYawInDegrees(45f).build()),
        new AtlasView("front", BlockRenderOptions.builder().build())
    );

    // ----------------------------------------------------------------
    // High-level generation from AtlasOptions
    // ----------------------------------------------------------------

    /**
     * Generates atlas pages from the given options, handling RenderContext creation,
     * texture pack initialization, view selection, and dispatching to the appropriate
     * generation method (block/item, SNBT, or Hypixel inventory).
     *
     * @param options the atlas generation options
     * @return the list of generated atlas result metadata
     * @throws IOException if context creation or asset loading fails
     */
    public static @NotNull List<AtlasResult> generate(@NotNull AtlasOptions options) throws IOException {
        if (StringUtil.isEmpty(options.getAssetsDirectory()))
            throw new IllegalArgumentException("assetsDirectory must not be null or blank");

        Files.createDirectories(Path.of(options.getOutputDirectory()));

        List<AtlasView> views = resolveViews(options.getViewNames());
        if (options.getTexturePackIds() != null && !options.getTexturePackIds().isEmpty()) {
            List<String> packList = List.copyOf(options.getTexturePackIds());
            List<AtlasView> updated = new ArrayList<>(views.size());
            for (AtlasView view : views)
                updated.add(new AtlasView(view.getName(), view.getOptions().mutate().withPackIds(packList).build()));
            views = updated;
        }

        initializeResourcePacks(options.getTexturePackDirectories(), options.getTexturePackIds(), options.getAssetsDirectory());

        MinecraftApi.loadAssets(MinecraftAssetOptions.builder().withAssetsDirectory(options.getAssetsDirectory()).build());
        List<String> packIds = options.getTexturePackIds();
        AssetContext renderAssetContext = (packIds != null && !packIds.isEmpty())
            ? MinecraftApi.getAssetFactory().loadPackContext(packIds)
            : MinecraftApi.getServiceManager().get(AssetContext.class);
        try (RenderContext context = new RenderContext(renderAssetContext)) {

            List<AtlasResult> results = new ArrayList<>();

            if (options.getSnbtItemDirectory() != null && !options.getSnbtItemDirectory().isBlank()) {
                Path snbtDir = Path.of(options.getSnbtItemDirectory()).toAbsolutePath();
                if (!Files.isDirectory(snbtDir))
                    throw new IllegalArgumentException("SNBT directory '%s' does not exist".formatted(snbtDir));

                List<SnbtItemEntry> snbtEntries = loadSnbtDirectory(snbtDir.toString());
                if (!snbtEntries.isEmpty()) {
                    String snbtOutput = Path.of(options.getOutputDirectory(), "snbt").toString();
                    results.addAll(generateSnbtAtlases(context, snbtOutput, views,
                        options.getTileSize(), options.getColumns(), options.getRows(), snbtEntries));
                }
            } else if (options.getHypixelInventoryFile() != null && !options.getHypixelInventoryFile().isBlank()) {
                Path inventoryFile = Path.of(options.getHypixelInventoryFile()).toAbsolutePath();
                if (!Files.exists(inventoryFile))
                    throw new IllegalArgumentException("Hypixel inventory file '%s' does not exist".formatted(inventoryFile));

                String hypixelOutput = Path.of(options.getOutputDirectory(), "hypixel_inventory").toString();
                String lastPackId = options.getTexturePackIds() != null && !options.getTexturePackIds().isEmpty()
                    ? options.getTexturePackIds().get(options.getTexturePackIds().size() - 1) : null;
                generateInventoryAtlas(context, inventoryFile.toString(), hypixelOutput,
                    lastPackId, options.getTileSize(), options.getColumns());
            } else {
                boolean includeBlocks = options.isIncludeBlocks()
                    && (options.getBlockFilter() == null || !options.getBlockFilter().isEmpty());
                boolean includeItems = options.isIncludeItems()
                    && (options.getItemFilter() == null || !options.getItemFilter().isEmpty());

                results.addAll(generateBlockItemAtlases(context, options.getOutputDirectory(), views,
                    options.getTileSize(), options.getColumns(), options.getRows(),
                    options.getBlockFilter(), options.getItemFilter(), includeBlocks, includeItems));
            }

            return results;
        }
    }

    /**
     * Resolves view names to {@link AtlasView} instances. Returns all default views
     * if names is null or empty.
     *
     * @param viewNames the requested view names, or null for all defaults
     * @return the resolved views
     */
    public static @NotNull List<AtlasView> resolveViews(@Nullable List<String> viewNames) {
        if (viewNames == null || viewNames.isEmpty())
            return new ArrayList<>(DEFAULT_VIEWS);

        LinkedHashMap<String, AtlasView> allViews = new LinkedHashMap<>();
        for (AtlasView view : DEFAULT_VIEWS)
            allViews.put(view.getName().toLowerCase(Locale.ROOT), view);

        List<AtlasView> selected = new ArrayList<>(viewNames.size());
        for (String name : viewNames) {
            AtlasView view = allViews.get(name.toLowerCase(Locale.ROOT));
            if (view != null)
                selected.add(view);
        }
        return selected;
    }

    /**
     * Initializes resource packs from the given directories and discovered defaults,
     * caching them into the JPA repository via {@link RendererApi#cacheResourcePacks}.
     *
     * @param texturePackDirectories directories to register, or null
     * @param texturePackIds pack IDs to validate, or null
     * @param assetsPath the assets directory for default pack discovery
     */
    static void initializeResourcePacks(
        @Nullable List<String> texturePackDirectories,
        @Nullable List<String> texturePackIds,
        @NotNull String assetsPath) throws IOException {

        boolean needsPacks = (texturePackDirectories != null && !texturePackDirectories.isEmpty())
            || (texturePackIds != null && !texturePackIds.isEmpty());
        List<String> discovered = discoverDefaultTexturePacks(assetsPath);

        if (!needsPacks && discovered.isEmpty())
            return;

        List<String> allDirectories = new ArrayList<>(discovered);
        if (texturePackDirectories != null)
            allDirectories.addAll(texturePackDirectories);

        ResourcePackDiscovery.discoverPacks(allDirectories);
    }

    private static @NotNull List<String> discoverDefaultTexturePacks(@NotNull String assetsPath) {
        List<String> results = new ArrayList<>();
        Set<String> searchRoots = new HashSet<>();
        searchRoots.add(Path.of(System.getProperty("user.dir"), "texturepacks").toString());

        Path assetsParent = Path.of(assetsPath).toAbsolutePath().getParent();
        if (assetsParent != null)
            searchRoots.add(assetsParent.resolve("texturepacks").toString());

        for (String root : searchRoots) {
            if (!Files.isDirectory(Path.of(root)))
                continue;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(root), Files::isDirectory)) {
                for (Path directory : stream) {
                    if (Files.exists(directory.resolve("meta.json")))
                        results.add(directory.toString());
                }
            } catch (IOException ignored) {
                // Ignore enumeration failures during discovery
            }
        }

        return results;
    }

    // ----------------------------------------------------------------
    // Block/item atlas generation
    // ----------------------------------------------------------------

    /**
     * Generates atlas pages for blocks and/or items using the given renderer and view configurations.
     *
     * @param context the render context
     * @param outputDirectory the directory to write atlas images and manifests
     * @param views the list of view configurations
     * @param tileSize the pixel size of each tile
     * @param columns the number of columns per page
     * @param rows the number of rows per page
     * @param blockFilter optional filter of block names to include (null for all known blocks)
     * @param itemFilter optional filter of item names to include (null for all known items)
     * @param includeBlocks whether to include blocks in the atlas
     * @param includeItems whether to include items in the atlas
     * @return the list of generated atlas result metadata
     */
    public static @NotNull List<AtlasResult> generateBlockItemAtlases(
        @NotNull RenderContext context,
        @NotNull String outputDirectory,
        @NotNull List<AtlasView> views,
        int tileSize,
        int columns,
        int rows,
        @Nullable List<String> blockFilter,
        @Nullable List<String> itemFilter,
        boolean includeBlocks,
        boolean includeItems
    ) {
        validateGridParameters(tileSize, columns, rows, views);

        Path outDir = ensureOutputDirectory(outputDirectory);

        List<String> blockNames = getCandidateBlockNames(context, blockFilter, includeBlocks);
        List<String> itemNames = getCandidateItemNames(context, itemFilter, includeItems);

        List<AtlasResult> results = new ArrayList<>();
        int perPage = columns * rows;

        if (!blockNames.isEmpty()) {
            for (AtlasView view : views) {
                generatePages(blockNames, view, "blocks", perPage, columns, tileSize, true,
                    (name, options) -> new BlockRenderer(context, name, options).render(),
                    view.options, outDir, results);
            }
        }

        if (!itemNames.isEmpty()) {
            for (AtlasView view : views) {
                generatePages(itemNames, view, "items", perPage, columns, tileSize, false,
                    (name, options) -> new ItemRenderer(context, name, options).render(),
                    normalizeItemRenderOptions(view.options), outDir, results);
            }
        }

        return results;
    }

    /**
     * Generates atlas pages using default tile size (128), grid (16x16), and all blocks/items.
     *
     * @param context the render context
     * @param outputDirectory the directory to write atlas images and manifests
     * @param views the list of view configurations
     * @return the list of generated atlas result metadata
     */
    public static @NotNull List<AtlasResult> generateBlockItemAtlases(
        @NotNull RenderContext context,
        @NotNull String outputDirectory,
        @NotNull List<AtlasView> views
    ) {
        return generateBlockItemAtlases(context, outputDirectory, views, 128, 16, 16, null, null, true, true);
    }

    // ----------------------------------------------------------------
    // SNBT atlas generation
    // ----------------------------------------------------------------

    /**
     * Loads all {@code .snbt} files from the specified directory, parsing each into an NBT document.
     *
     * @param directory the directory containing SNBT files
     * @return the list of loaded entries, sorted by file name
     */
    public static @NotNull List<SnbtItemEntry> loadSnbtDirectory(@NotNull String directory) {
        if (directory == null || directory.isBlank())
            throw new IllegalArgumentException("directory must not be null or blank");

        Path dir = Path.of(directory);
        if (!Files.isDirectory(dir))
            throw new IllegalArgumentException("SNBT item directory '" + directory + "' does not exist.");

        List<Path> snbtFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.snbt")) {
            for (Path path : stream)
                if (Files.isRegularFile(path))
                    snbtFiles.add(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        snbtFiles.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString()));

        List<SnbtItemEntry> results = new ArrayList<>();
        for (Path path : snbtFiles) {
            String name = getFileNameWithoutExtension(path);
            try {
                String content = Files.readString(path);
                CompoundTag rootCompound = new NbtFactory().fromSnbt(content);
                results.add(new SnbtItemEntry(name, path.toString(), rootCompound, null));
            } catch (Exception ex) {
                results.add(new SnbtItemEntry(name, path.toString(), null, ex.getMessage()));
            }
        }

        return results;
    }

    /**
     * Generates atlas pages from pre-loaded SNBT item entries.
     *
     * @param context the render context
     * @param outputDirectory the directory to write atlas images and manifests
     * @param views the list of view configurations
     * @param tileSize the pixel size of each tile
     * @param columns the number of columns per page
     * @param rows the number of rows per page
     * @param items the list of SNBT item entries to render
     * @return the list of generated atlas result metadata
     */
    public static @NotNull List<AtlasResult> generateSnbtAtlases(
        @NotNull RenderContext context,
        @NotNull String outputDirectory,
        @NotNull List<AtlasView> views,
        int tileSize,
        int columns,
        int rows,
        @NotNull List<SnbtItemEntry> items
    ) {
        if (items.isEmpty())
            return Collections.emptyList();

        validateGridParameters(tileSize, columns, rows, views);
        Path outDir = ensureOutputDirectory(outputDirectory);

        List<AtlasResult> results = new ArrayList<>();
        int perPage = columns * rows;
        String category = "snbt-items";

        for (AtlasView view : views) {
            int totalPages = (int) Math.ceil(items.size() / (double) perPage);

            for (int page = 0; page < totalPages; page++) {
                int startIndex = page * perPage;
                int count = Math.min(perPage, items.size() - startIndex);
                if (count <= 0)
                    continue;

                List<AtlasManifestEntry> manifestEntries = new ArrayList<>(count);
                List<BufferedImage> pageTiles = new ArrayList<>(count);

                for (int localIndex = 0; localIndex < count; localIndex++) {
                    SnbtItemEntry entry = items.get(startIndex + localIndex);
                    int globalIndex = startIndex + localIndex;
                    int col = localIndex % columns;
                    int row = localIndex / columns;
                    String label = entry.name;
                    String error = entry.error;
                    String model = null;
                    List<String> textures = null;
                    String texturePack = null;

                    if (entry.rootCompound != null) {
                        CompoundTag compound = entry.rootCompound;
                        String itemId = tryGetItemId(compound);
                        if (itemId != null && !itemId.isBlank())
                            label = label + " (" + itemId + ")";

                        BlockRenderOptions itemOptions = normalizeSnbtRenderOptions(view.getOptions());

                        try {
                            ResourceIdResult resourceInfo =
                                context.computeResourceId(itemId != null ? itemId : entry.name, itemOptions);
                            model = resourceInfo.getModel();
                            textures = resourceInfo.getTextures().isEmpty() ? null : resourceInfo.getTextures();
                            texturePack = resourceInfo.getSourcePackId();
                        } catch (Exception ignored) {}

                        if (error == null) {
                            try {
                                pageTiles.add(ItemRenderer.fromNbt(context, compound, itemOptions).render());
                            } catch (Exception ex) {
                                error = ex.getMessage();
                                pageTiles.add(createPlaceholderTile());
                            }
                        } else {
                            pageTiles.add(createPlaceholderTile());
                        }
                    } else {
                        pageTiles.add(createPlaceholderTile());
                    }

                    if (model != null || textures != null || (texturePack != null && !texturePack.isBlank()))
                        manifestEntries.add(new AtlasManifestEntry(globalIndex, label, col, row, error, model, textures, texturePack));
                    else
                        manifestEntries.add(new AtlasManifestEntry(globalIndex, label, col, row, error));
                }

                BufferedImage canvas = new GridRenderer(pageTiles, columns, tileSize, false).render();
                writeAtlasPage(canvas, manifestEntries, category, view.getName(), page, outDir, results);
            }
        }

        return results;
    }

    // ----------------------------------------------------------------
    // Hypixel inventory atlas generation
    // ----------------------------------------------------------------

    /**
     * Generates a Hypixel inventory atlas from the given base64 inventory data file.
     *
     * @param context the render context
     * @param inventoryDataPath the file containing base64-encoded inventory data
     * @param outputDirectory the directory to write atlas images and manifests
     * @param texturePackId optional texture pack id to apply
     * @param tileSize the pixel size of each tile
     * @param columns the number of columns per atlas row
     */
    public static void generateInventoryAtlas(
        @NotNull RenderContext context,
        @NotNull String inventoryDataPath,
        @NotNull String outputDirectory,
        @Nullable String texturePackId,
        int tileSize,
        int columns
    ) {
        System.out.println("[HypixelInventoryAtlas] Reading inventory data from: " + inventoryDataPath);

        Path dataPath = Path.of(inventoryDataPath);
        if (!Files.exists(dataPath))
            throw new IllegalArgumentException("Inventory data file not found: " + inventoryDataPath);

        String base64Data;
        try {
            base64Data = Files.readString(dataPath).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read inventory data file", e);
        }

        List<HypixelItemData> items = InventoryParser.parseInventory(base64Data);
        System.out.println("[HypixelInventoryAtlas] Parsed " + items.size() + " items from inventory");

        if (items.isEmpty()) {
            System.out.println("[HypixelInventoryAtlas] No items to render");
            return;
        }

        int rows = (items.size() + columns - 1) / columns;
        List<Map<String, Object>> manifest = new ArrayList<>();
        List<BufferedImage> tiles = new ArrayList<>(items.size());
        int errorCount = 0;

        for (int i = 0; i < items.size(); i++) {
            HypixelItemData item = items.get(i);
            int col = i % columns;
            int row = i / columns;

            String textureId = TextureResolver.getTextureId(item);
            System.out.println("[" + (i + 1) + "/" + items.size() + "] Rendering "
                + (item.getSkyblockId() != null ? item.getSkyblockId() : item.getItemId())
                + " (texture: " + textureId + ")");

            try {
                CompoundTag customData = null;
                CompoundTag tag = item.getTag();
                if (tag != null && tag.containsPath("ExtraAttributes.id")) {
                    StringTag skyblockIdTag = tag.getPath("ExtraAttributes.id");
                    if (skyblockIdTag.notEmpty()) {
                        CompoundTag customDataCompound = new CompoundTag();
                        customDataCompound.put("id", skyblockIdTag.getValue());
                        customData = customDataCompound;
                        System.out.println("  Created custom_data with id='" + skyblockIdTag.getValue() + "'");
                    }
                }

                ItemRenderData itemRenderData = new ItemRenderData(null, null, false, customData, null);

                String itemKey;
                if (item.getNumericId() != null) {
                    String mapped = LegacyItemMappings.tryMapNumericId(item.getNumericId(), item.getDamage());
                    itemKey = mapped != null ? mapped : "minecraft:diamond_sword";
                    System.out.println("  Using base item " + itemKey + " (numeric ID: " + item.getNumericId() + ")");
                } else if (item.getItemId().startsWith("minecraft:")) {
                    itemKey = item.getItemId();
                } else {
                    itemKey = "minecraft:diamond_sword";
                    System.out.println("  WARNING: Unknown item format, using fallback");
                }

                List<String> packIds = texturePackId != null ? List.of(texturePackId) : null;
                BlockRenderOptions renderOptions = BlockRenderOptions.builder()
                    .withSize(tileSize)
                    .withPackIds(packIds)
                    .withItemData(itemRenderData)
                    .build();

                System.out.println("  RenderOptions: UseGuiTransform=" + renderOptions.isUseGuiTransform()
                    + ", PackIds=" + (renderOptions.getPackIds() != null
                        ? String.join(",", renderOptions.getPackIds()) : "[]"));

                try {
                    ResourceIdResult resourceInfo = context.computeResourceId(itemKey, renderOptions);
                    System.out.println("  Resolved model: " + (resourceInfo.getModel() != null ? resourceInfo.getModel() : "(null)")
                        + " from pack: " + resourceInfo.getSourcePackId());
                    if (!resourceInfo.getTextures().isEmpty()) {
                        List<String> texPreview = resourceInfo.getTextures().subList(
                            0, Math.min(3, resourceInfo.getTextures().size()));
                        System.out.println("    Textures: " + String.join(", ", texPreview));
                    }
                } catch (Exception ex) {
                    System.out.println("  Failed to compute resource ID: " + ex.getMessage());
                }

                tiles.add(new ItemRenderer(context, itemKey, itemRenderData, renderOptions).render());

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("index", i);
                entry.put("skyblock_id", item.getSkyblockId());
                entry.put("item_id", item.getItemId());
                entry.put("texture_id", textureId);
                entry.put("display_name", item.getDisplayName());
                entry.put("count", item.getCount());
                entry.put("damage", item.getDamage());
                entry.put("has_enchantments", item.getEnchantments() != null);
                entry.put("has_gems", item.getGems() != null);
                entry.put("has_attributes", item.getAttributes() != null);

                Map<String, Object> position = new LinkedHashMap<>();
                position.put("x", col);
                position.put("y", row);
                entry.put("position", position);

                manifest.add(entry);
            } catch (Exception ex) {
                System.out.println("  ERROR: " + ex.getMessage());
                errorCount++;
                tiles.add(createPlaceholderTile());

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("index", i);
                entry.put("skyblock_id", item.getSkyblockId());
                entry.put("item_id", item.getItemId());
                entry.put("texture_id", textureId);
                entry.put("display_name", item.getDisplayName());
                entry.put("error", ex.getMessage());

                Map<String, Object> position = new LinkedHashMap<>();
                position.put("x", col);
                position.put("y", row);
                entry.put("position", position);

                manifest.add(entry);
            }
        }

        BufferedImage atlas = new GridRenderer(tiles, columns, tileSize, false).render();

        Path outDir = ensureOutputDirectory(outputDirectory);

        String baseName = Path.of(inventoryDataPath).getFileName().toString();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0)
            baseName = baseName.substring(0, dotIndex);

        Path atlasPath = outDir.resolve("hypixel_inventory_" + baseName + ".png");
        Path manifestPath = outDir.resolve("hypixel_inventory_" + baseName + ".json");

        Map<String, Object> manifestRoot = new LinkedHashMap<>();
        manifestRoot.put("source", inventoryDataPath);
        manifestRoot.put("texture_pack", texturePackId);
        manifestRoot.put("tile_size", tileSize);
        manifestRoot.put("columns", columns);
        manifestRoot.put("rows", rows);
        manifestRoot.put("total_items", items.size());
        manifestRoot.put("errors", errorCount);
        manifestRoot.put("items", manifest);

        try {
            ImageIO.write(atlas, "PNG", atlasPath.toFile());
            Files.writeString(manifestPath, GSON.toJson(manifestRoot));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        System.out.println("[HypixelInventoryAtlas] Saved atlas: " + atlasPath);
        System.out.println("[HypixelInventoryAtlas] Saved manifest: " + manifestPath);
        System.out.println("[HypixelInventoryAtlas] Rendered " + (items.size() - errorCount)
            + "/" + items.size() + " items successfully");
    }

    // ----------------------------------------------------------------
    // Candidate name resolution
    // ----------------------------------------------------------------

    /**
     * Returns a sorted list of block names, either from the filter or from the renderer's known blocks.
     *
     * @param context the render context
     * @param blockFilter optional explicit list of block names
     * @param includeBlocks whether blocks should be included at all
     * @return the sorted candidate block name list
     */
    public static @NotNull List<String> getCandidateBlockNames(
        @NotNull RenderContext context, @Nullable List<String> blockFilter, boolean includeBlocks) {
        if (!includeBlocks)
            return Collections.emptyList();
        List<String> names = blockFilter != null
            ? new ArrayList<>(blockFilter)
            : new ArrayList<>(context.getKnownBlockNames());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /**
     * Returns a sorted list of item names, either from the filter or from the renderer's known items.
     *
     * @param context the render context
     * @param itemFilter optional explicit list of item names
     * @param includeItems whether items should be included at all
     * @return the sorted candidate item name list
     */
    public static @NotNull List<String> getCandidateItemNames(
        @NotNull RenderContext context, @Nullable List<String> itemFilter, boolean includeItems) {
        if (!includeItems)
            return Collections.emptyList();
        List<String> names = itemFilter != null
            ? new ArrayList<>(itemFilter)
            : new ArrayList<>(context.getKnownItemNames());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    // ----------------------------------------------------------------
    // Shared utilities
    // ----------------------------------------------------------------

    /**
     * Sanitizes a string for use as part of a file name.
     *
     * @param input the raw input string
     * @return the sanitized, lowercase, underscore-separated file name fragment
     */
    public static @NotNull String sanitizeFileName(@NotNull String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == ' ' || isInvalidFileNameChar(ch))
                sb.append('_');
            else
                sb.append(ch);
        }
        return sb.toString().toLowerCase();
    }

    /**
     * Normalizes item render options by zeroing out rotation angles for flat item rendering.
     *
     * @param options the original render options
     * @return the normalized options with zeroed rotation if rotation was non-zero
     */
    public static @NotNull BlockRenderOptions normalizeItemRenderOptions(@NotNull BlockRenderOptions options) {
        if (Math.abs(options.getYawInDegrees()) < 0.01f
            && Math.abs(options.getPitchInDegrees()) < 0.01f
            && Math.abs(options.getRollInDegrees()) < 0.01f)
            return options;
        return options.mutate()
            .withYawInDegrees(0f)
            .withPitchInDegrees(0f)
            .withRollInDegrees(0f)
            .build();
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private static void generatePages(List<String> names, AtlasView view, String category,
                                       int perPage, int columns, int tileSize, boolean bilinear,
                                       BiFunction<String, BlockRenderOptions, BufferedImage> renderer,
                                       BlockRenderOptions effectiveOptions,
                                       Path outDir, List<AtlasResult> results) {
        int totalPages = (int) Math.ceil(names.size() / (double) perPage);

        for (int page = 0; page < totalPages; page++) {
            int startIndex = page * perPage;
            int count = Math.min(perPage, names.size() - startIndex);
            if (count <= 0)
                continue;

            List<AtlasManifestEntry> manifestEntries = new ArrayList<>(count);
            List<BufferedImage> pageTiles = new ArrayList<>(count);

            for (int localIndex = 0; localIndex < count; localIndex++) {
                String name = names.get(startIndex + localIndex);
                int globalIndex = startIndex + localIndex;
                int col = localIndex % columns;
                int row = localIndex / columns;
                String error = null;

                try {
                    pageTiles.add(renderer.apply(name, effectiveOptions));
                } catch (Exception ex) {
                    error = ex.getMessage();
                    pageTiles.add(createPlaceholderTile());
                }

                manifestEntries.add(new AtlasManifestEntry(globalIndex, name, col, row, error));
            }

            BufferedImage canvas = new GridRenderer(pageTiles, columns, tileSize, bilinear).render();
            writeAtlasPage(canvas, manifestEntries, category, view.getName(), page, outDir, results);
        }
    }

    private static void writeAtlasPage(BufferedImage canvas, Object manifestData,
                                        String category, String viewName, int page,
                                        Path outDir, List<AtlasResult> results) {
        String baseFileName = String.join("_",
            sanitizeFileName(category),
            sanitizeFileName(viewName),
            String.format("page%02d", page + 1)
        );

        Path imagePath = outDir.resolve(baseFileName + ".png");
        Path manifestPath = outDir.resolve(baseFileName + ".json");

        try {
            ImageIO.write(canvas, "PNG", imagePath.toFile());
            Files.writeString(manifestPath, GSON.toJson(manifestData));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        results.add(new AtlasResult(category, viewName, page + 1,
            imagePath.toString(), manifestPath.toString()));
    }

    private static @NotNull BlockRenderOptions normalizeSnbtRenderOptions(@NotNull BlockRenderOptions options) {
        BlockRenderOptions normalized = normalizeItemRenderOptions(options);

        if (!normalized.isUseGuiTransform())
            normalized = normalized.mutate().isUseGuiTransform(true).build();

        return normalized;
    }

    private static void validateGridParameters(int tileSize, int columns, int rows, List<AtlasView> views) {
        if (tileSize <= 0) throw new IllegalArgumentException("tileSize must be positive");
        if (columns <= 0) throw new IllegalArgumentException("columns must be positive");
        if (rows <= 0) throw new IllegalArgumentException("rows must be positive");
        if (views.isEmpty()) throw new IllegalArgumentException("At least one view must be provided");
    }

    private static @NotNull Path ensureOutputDirectory(@NotNull String outputDirectory) {
        Path outDir = Path.of(outputDirectory);
        try {
            Files.createDirectories(outDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return outDir;
    }

    private static @NotNull BufferedImage createPlaceholderTile() {
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }

    private static boolean isInvalidFileNameChar(char ch) {
        return ch == '<' || ch == '>' || ch == ':' || ch == '"'
            || ch == '/' || ch == '\\' || ch == '|' || ch == '?' || ch == '*'
            || ch < 32;
    }

    private static @NotNull String getFileNameWithoutExtension(@NotNull Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static final String[] NESTED_KEYS = {"item", "Item", "stack", "Stack"};

    private static @Nullable String tryGetItemId(@NotNull CompoundTag compound) {
        if (compound.containsPath("id")) {
            StringTag tag = compound.getPath("id");
            if (tag.notEmpty())
                return tag.getValue();
        }

        for (String key : NESTED_KEYS) {
            String path = key + ".id";
            if (compound.containsPath(path)) {
                StringTag tag = compound.getPath(path);
                if (tag.notEmpty())
                    return tag.getValue();
            }
        }

        return null;
    }
}
