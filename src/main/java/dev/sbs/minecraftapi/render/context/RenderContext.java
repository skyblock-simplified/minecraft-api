package dev.sbs.minecraftapi.render.context;

import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.asset.context.AssetContext;
import dev.sbs.minecraftapi.asset.model.BlockInfo;
import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.BlockModel.Element;
import dev.sbs.minecraftapi.asset.model.BlockModel.Face;
import dev.sbs.minecraftapi.asset.model.BlockModel.FaceData;
import dev.sbs.minecraftapi.asset.model.BlockModel.Rotation;
import dev.sbs.minecraftapi.asset.model.BlockModel.Transform;
import dev.sbs.minecraftapi.asset.model.ItemInfo;
import dev.sbs.minecraftapi.asset.model.ItemInfo.TintInfo;
import dev.sbs.minecraftapi.asset.model.ResourcePack;
import dev.sbs.minecraftapi.asset.namespace.Namespace;
import dev.sbs.minecraftapi.asset.texture.OverlayRoot;
import dev.sbs.minecraftapi.asset.texture.TextureContext;
import dev.sbs.minecraftapi.asset.texture.TexturePackStack;
import dev.sbs.minecraftapi.asset.texture.TextureReference;
import dev.sbs.minecraftapi.math.Vector3f;
import dev.sbs.minecraftapi.math.Vector4f;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import dev.sbs.minecraftapi.render.FaceRenderer;
import dev.sbs.minecraftapi.render.FlatItemRenderer;
import dev.sbs.minecraftapi.render.Renderer;
import dev.sbs.minecraftapi.render.data.BiomeTint;
import dev.sbs.minecraftapi.render.data.BlockColor;
import dev.sbs.minecraftapi.render.data.ColorUtil;
import dev.sbs.minecraftapi.render.engine.ModelEngine;
import dev.sbs.minecraftapi.render.hypixel.HypixelPrefixes;
import dev.sbs.minecraftapi.render.hypixel.LegacyItemMappings;
import dev.sbs.minecraftapi.render.hypixel.TextureResolver;
import dev.sbs.minecraftapi.render.resolver.ItemModelResolution;
import dev.sbs.minecraftapi.render.resolver.ItemModelResolver;
import dev.sbs.minecraftapi.render.resolver.PackContextManager;
import dev.sbs.minecraftapi.render.resolver.RenderedResource;
import dev.sbs.minecraftapi.render.resolver.ResourceIdResult;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentSet;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.util.StringUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Shared infrastructure for Minecraft block and item rendering.
 *
 * <p>Holds registries, texture repositories, caches, and lifecycle management. Concrete
 * {@link Renderer} implementations delegate to a {@code RenderContext} for all resource access.
 */
@Getter
public final class RenderContext implements AutoCloseable {

    // ----------------------------------------------------------------
    // Static fields
    // ----------------------------------------------------------------

    public static boolean debugDisableCulling = false;

    // ----------------------------------------------------------------
    // Item rendering constants
    // ----------------------------------------------------------------

    private static final String[] BOTTOM_ALIGNED_ITEM_SUFFIXES = {
        "_carpet", "_trapdoor", "_pressure_plate", "_weighted_pressure_plate"
    };

    private static final String[] BANNER_SUFFIXES = {"_banner"};

    private static final Set<String> ANIMATED_DIAL_ITEMS;
    static {
        Set<String> s = new HashSet<>();
        s.add("compass");
        s.add("recovery_compass");
        s.add("clock");
        ANIMATED_DIAL_ITEMS = Collections.unmodifiableSet(s);
    }

    private static final long MAX_ANIMATION_DURATION_MS = 120_000L;

    private static final int[] DEFAULT_LEATHER_ARMOR_COLOR = {0xA0, 0x65, 0x40};

    private static final Map<String, int[]> LEGACY_DEFAULT_TINT_OVERRIDES;
    static {
        Map<String, int[]> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        map.put("leather_helmet", DEFAULT_LEATHER_ARMOR_COLOR);
        map.put("leather_chestplate", DEFAULT_LEATHER_ARMOR_COLOR);
        map.put("leather_leggings", DEFAULT_LEATHER_ARMOR_COLOR);
        map.put("leather_boots", DEFAULT_LEATHER_ARMOR_COLOR);
        map.put("leather_horse_armor", DEFAULT_LEATHER_ARMOR_COLOR);
        map.put("wolf_armor_dyed", DEFAULT_LEATHER_ARMOR_COLOR);
        LEGACY_DEFAULT_TINT_OVERRIDES = Collections.unmodifiableMap(map);
    }

    private static final Map<String, int[]> LEGACY_DEFAULT_TINT_LAYER_OVERRIDES;
    static {
        Map<String, int[]> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        map.put("wolf_armor_dyed", new int[]{1});
        LEGACY_DEFAULT_TINT_LAYER_OVERRIDES = Collections.unmodifiableMap(map);
    }

    private static final HttpClient PLAYER_SKIN_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final Set<String> HEAD_ITEM_NAMES = Set.of(
        "player_head", "zombie_head", "skeleton_skull", "wither_skeleton_skull",
        "creeper_head", "dragon_head", "piglin_head");

    // ----------------------------------------------------------------
    // Instance fields
    // ----------------------------------------------------------------

    private final @NotNull Map<String, BlockModel> resolvedModels;
    private final @NotNull TextureContext textureContext;
    @Getter(AccessLevel.NONE)
    private final @NotNull Supplier<ConcurrentList<BlockInfo>> blockInfoSupplier;
    @Getter(AccessLevel.NONE)
    private final @NotNull Supplier<ConcurrentList<ItemInfo>> itemInfoSupplier;
    private final @NotNull AssetContext packAssetContext;
    private final @Nullable String assetsDirectory;
    private final @NotNull String playerSkinCacheDirectory;
    private final @NotNull List<OverlayRoot> baseOverlayRoots;
    private final @NotNull ConcurrentHashMap<String, RenderContext> packContextCache = new ConcurrentHashMap<>();
    private final @NotNull ConcurrentHashMap<String, BufferedImage> playerSkinCache = new ConcurrentHashMap<>();
    @Getter(AccessLevel.NONE)
    private final @NotNull Map<String, BufferedImage> biomeTintedTextureCache = Collections.synchronizedMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    @Getter(AccessLevel.NONE)
    private volatile boolean disposed;

    // ----------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------

    /**
     * Creates a render context from an asset context.
     * <p>
     * For vanilla contexts, block and item info lookups are served from the JPA repository.
     * For pack-specific contexts, they are served from the context's own data.
     *
     * @param assetContext the loaded asset context (vanilla or pack-specific)
     */
    public RenderContext(@NotNull AssetContext assetContext) {
        this.resolvedModels = assetContext.getResolvedModels();
        this.textureContext = assetContext.getTextureContext();
        this.blockInfoSupplier = assetContext::getBlockInfos;
        this.itemInfoSupplier = assetContext::getItemInfos;
        this.assetsDirectory = assetContext.getAssetsDirectory();
        this.playerSkinCacheDirectory = initializePlayerSkinCacheDirectory(assetContext.getAssetsDirectory());
        this.baseOverlayRoots = assetContext.getBaseOverlayRoots();
        this.packAssetContext = assetContext;
    }

    // ----------------------------------------------------------------
    // Model resolution
    // ----------------------------------------------------------------

    /**
     * Resolves a model by name from the pre-resolved model map.
     *
     * @param name the model name (may include "minecraft:", "block/", or "blocks/" prefixes)
     * @return the resolved model
     * @throws java.util.NoSuchElementException if the model is not found
     */
    public @NotNull BlockModel resolveModel(@NotNull String name) {
        String normalized = BlockModel.normalizeName(name);
        BlockModel direct = resolvedModels.get(normalized);
        if (direct != null)
            return direct;

        for (Map.Entry<String, BlockModel> entry : resolvedModels.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(normalized))
                return entry.getValue();
        }

        throw new java.util.NoSuchElementException("Model '%s' was not found in the resolved models".formatted(name));
    }

    // ----------------------------------------------------------------
    // Block/Item lookup helpers
    // ----------------------------------------------------------------

    /**
     * Looks up a block info entry by name (case-insensitive).
     *
     * @param blockName the block name
     * @return the block info, or null if not found
     */
    public @Nullable BlockInfo getBlockInfo(@NotNull String blockName) {
        return blockInfoSupplier.get().matchFirstOrNull(info -> info.getName().equalsIgnoreCase(blockName));
    }

    /**
     * Looks up the model path for the given block name (case-insensitive).
     *
     * @param blockName the block name
     * @return the model path, or null if not found or blank
     */
    public @Nullable String getBlockModel(@NotNull String blockName) {
        BlockInfo info = getBlockInfo(blockName);
        if (info != null && info.getModel() != null && !info.getModel().isBlank())
            return info.getModel();
        return null;
    }

    /**
     * Looks up an item info entry by name (case-insensitive).
     *
     * @param itemName the item name
     * @return the item info, or null if not found
     */
    public @Nullable ItemInfo getItemInfo(@NotNull String itemName) {
        return itemInfoSupplier.get().matchFirstOrNull(info -> info.getName().equalsIgnoreCase(itemName));
    }

    /**
     * Looks up the model path for the given item name (case-insensitive).
     *
     * @param itemName the item name
     * @return the model path, or null if not found or blank
     */
    public @Nullable String getItemModel(@NotNull String itemName) {
        ItemInfo info = getItemInfo(itemName);
        if (info != null && info.getModel() != null && !info.getModel().isBlank())
            return info.getModel();
        return null;
    }

    /**
     * Returns whether item data is available (either local list or repository).
     *
     * @return true if item lookups can be performed
     */
    public boolean hasItemData() {
        try {
            return !itemInfoSupplier.get().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns all known block names.
     *
     * @return a list of block names
     */
    public @NotNull ConcurrentList<String> getKnownBlockNames() {
        return blockInfoSupplier.get()
            .stream()
            .map(BlockInfo::getName)
            .toList();
    }

    /**
     * Returns all known item names.
     *
     * @return a list of item names, or an empty list if no item data
     */
    public @NotNull ConcurrentList<String> getKnownItemNames() {
        try {
            return itemInfoSupplier.get()
                .stream()
                .map(ItemInfo::getName)
                .toList();
        } catch (Exception e) {
            return Concurrent.newList();
        }
    }

    // ----------------------------------------------------------------
    // Preloading
    // ----------------------------------------------------------------

    /**
     * Preloads render contexts for the specified texture pack stacks, ensuring their assets are
     * parsed before serving requests.
     *
     * @param packStacks sequences of pack identifiers representing each stack to preload
     */
    public void preloadTexturePackStacks(@NotNull Iterable<ConcurrentList<String>> packStacks) {
        ensureNotDisposed();
        if (packStacks == null)
            throw new IllegalArgumentException("packStacks cannot be null");

        if (!hasResourcePacks()) {
            for (List<String> stack : packStacks) {
                if (stack != null && !stack.isEmpty())
                    throw new IllegalStateException(
                        "No resource packs are loaded and cannot preload pack combinations.");
            }
            return;
        }

        ConcurrentSet<String> seenStacks = Concurrent.newSet();
        for (ConcurrentList<String> packIds : packStacks) {
            if (packIds == null || packIds.isEmpty())
                continue;

            TexturePackStack stack = TexturePackStack.buildPackStack(packIds);
            if (!seenStacks.add(stack.getFingerprint().toLowerCase()))
                continue;

            PackContextManager.resolveContext(this, packIds);
        }
    }

    /**
     * Preloads render contexts for all registered resource packs.
     *
     * @param includeDefaultPackStack when true, also preloads the context for the default pack stack
     */
    public void preloadRegisteredPacks(boolean includeDefaultPackStack) {
        ensureNotDisposed();

        ConcurrentList<ConcurrentList<String>> stacksToPreload = Concurrent.newList();
        if (includeDefaultPackStack && !packAssetContext.getPackIds().isEmpty())
            stacksToPreload.add(Concurrent.newList(packAssetContext.getPackIds()));

        if (!hasResourcePacks())
            return;

        for (ResourcePack pack : MinecraftApi.getRepository(ResourcePack.class).findAll())
            stacksToPreload.add(Concurrent.newUnmodifiableList(pack.getId()));

        preloadTexturePackStacks(stacksToPreload);
    }

    /**
     * Preloads render contexts for all registered texture packs, including the default pack stack.
     */
    public void preloadRegisteredPacks() {
        preloadRegisteredPacks(true);
    }

    // ----------------------------------------------------------------
    // Texture pack icon
    // ----------------------------------------------------------------

    /**
     * Returns the pack icon image for the specified pack ID.
     *
     * @param packId the pack identifier
     * @return the pack icon image, or null if no icon is available
     */
    public @Nullable BufferedImage getTexturePackIcon(@NotNull String packId) {
        if (StringUtil.isEmpty(packId))
            throw new IllegalArgumentException("packId cannot be null or blank");

        ensureNotDisposed();

        if (packId.equalsIgnoreCase(AssetContext.VANILLA_PACK_ID)) {
            String vanillaRoot = packAssetContext.getAssetsDirectory();

            if (StringUtil.isNotEmpty(vanillaRoot)) {
                Path vanillaIconPath = Path.of(vanillaRoot, "pack.png");

                if (Files.exists(vanillaIconPath)) {
                    try {
                        return javax.imageio.ImageIO.read(vanillaIconPath.toFile());
                    } catch (Exception ignored) {
                        return null;
                    }
                }
            }

            return null;
        }

        ResourcePack pack = tryResolveResourcePack(packId);
        if (pack == null)
            return null;

        Path iconPath = Path.of(pack.getRootPath(), "pack.png");
        if (!Files.exists(iconPath))
            return null;

        try {
            return javax.imageio.ImageIO.read(iconPath.toFile());
        } catch (Exception ignored) {
            return null;
        }
    }

    // ----------------------------------------------------------------
    // Resource ID computation
    // ----------------------------------------------------------------

    /**
     * Computes a resource ID for a given target without performing a full render.
     *
     * @param target the block or item name
     * @param options optional render options
     * @return the computed resource ID result
     */
    public @NotNull ResourceIdResult computeResourceId(@NotNull String target, @Nullable BlockRenderOptions options) {
        if (StringUtil.isEmpty(target))
            throw new IllegalArgumentException("target cannot be null or blank");

        BlockRenderOptions effectiveOptions = options != null ? options : BlockRenderOptions.DEFAULT;
        RenderContext ctx = PackContextManager.resolveContext(this, effectiveOptions.getPackIds());
        BlockRenderOptions forwardedOptions = PackContextManager.forwardOptions(this, effectiveOptions);
        return PackContextManager.computeResourceId(ctx, target, forwardedOptions, null);
    }

    // ----------------------------------------------------------------
    // Render methods
    // ----------------------------------------------------------------

    /**
     * Renders a block model to a 2D image using the block registry and model resolver.
     *
     * @param blockName the block name
     * @param options render options
     * @return the rendered image
     */
    public @NotNull BufferedImage renderBlock(@NotNull String blockName, @NotNull BlockRenderOptions options) {
        ensureNotDisposed();
        String modelName = blockName;
        String mappedModel = getBlockModel(blockName);
        if (mappedModel != null && !mappedModel.isBlank())
            modelName = mappedModel;

        BlockModel model = resolveModel(modelName);
        return new ModelEngine(this, model, options, blockName).render();
    }

    /**
     * Renders a single face of a block to a flat 2D image.
     *
     * @param blockName the block name
     * @param options face render options
     * @return the rendered face image
     */
    public @NotNull BufferedImage renderBlockFace(@NotNull String blockName, @NotNull BlockFaceRenderOptions options) {
        ensureNotDisposed();

        var direction = options.getDirection();
        int size = options.getSize();

        String modelName = blockName;
        String mappedModel = getBlockModel(blockName);
        if (mappedModel != null && !mappedModel.isBlank())
            modelName = mappedModel;

        BlockModel model = resolveModel(modelName);

        String textureId = null;
        FaceData face = null;

        for (var element : model.getElements()) {
            face = element.getFaces().get(direction);
            if (face != null) {
                textureId = model.resolveTexture(face.getTexture());
                break;
            }
        }

        if (textureId == null || textureId.isBlank() || face == null)
            return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        BufferedImage texture;
        if (face.getTintIndex() != null) {
            int[] constantTint = ColorUtil.tryGetConstantTint(textureId, blockName);
            if (constantTint != null) {
                texture = textureContext.getTintedTexture(textureId, BlockColor.packArgb(constantTint),
                    ColorUtil.CONSTANT_TINT_STRENGTH, 1f);
            } else {
                BiomeTint biomeKind = ColorUtil.tryGetBiomeTint(textureId, blockName);
                if (biomeKind != null) {
                    texture = getBiomeTintedTexture(textureId, biomeKind);
                } else {
                    int[] fallbackTint = ColorUtil.getColorFromBlockName(blockName);
                    if (fallbackTint == null) fallbackTint = ColorUtil.getColorFromBlockName(textureId);
                    if (fallbackTint != null) {
                        texture = textureContext.getTintedTexture(textureId, BlockColor.packArgb(fallbackTint),
                            1f, BlockColor.TINT_BLEND);
                    } else {
                        texture = textureContext.getTexture(textureId);
                    }
                }
            }
        } else {
            texture = textureContext.getTexture(textureId);
        }

        return new FaceRenderer(texture, size, options.getRotation()).render();
    }

    /**
     * Renders an item model to a 2D image.
     *
     * @param itemName the item name
     * @param options render options
     * @return the rendered image
     */
    public @NotNull BufferedImage renderItem(@NotNull String itemName, @NotNull BlockRenderOptions options) {
        ensureNotDisposed();
        return renderGuiItemInternal(itemName, options, null);
    }

    /**
     * Renders an item model and computes a resource ID for the result.
     *
     * @param itemName the item name
     * @param options render options
     * @return the rendered resource containing the image and resource ID
     */
    public @NotNull RenderedResource renderItemWithResourceId(@NotNull String itemName,
                                                                           @NotNull BlockRenderOptions options) {
        ensureNotDisposed();
        ItemRenderCapture capture = new ItemRenderCapture();
        BufferedImage image = renderGuiItemInternal(itemName, options, capture);
        String resourceTarget = (capture.getOriginalTarget() == null || capture.getOriginalTarget().isBlank())
            ? itemName.trim() : capture.getOriginalTarget();
        BlockRenderOptions idOptions = capture.getFinalOptions() != null ? capture.getFinalOptions() : options;
        ResourceIdResult resourceId = PackContextManager.computeResourceId(this, resourceTarget,
            idOptions, capture.toResolution());
        return new RenderedResource(dev.simplified.image.StaticImageData.of(image), resourceId);
    }

    /**
     * Renders an item from a flat texture ID sprite.
     *
     * @param textureId the texture identifier
     * @param options render options
     * @return the rendered image
     */
    public @NotNull BufferedImage renderGuiItemFromTextureId(@NotNull String textureId,
                                                              @NotNull BlockRenderOptions options) {
        ensureNotDisposed();
        String trimmed = textureId.trim();
        String descriptor = TextureResolver.tryDecodeTextureId(trimmed);
        if (descriptor == null)
            descriptor = trimmed;

        if (descriptor.isBlank())
            return renderGuiItemInternal(trimmed, options, null);

        if (descriptor.toLowerCase().startsWith("custom:")) {
            String payload = descriptor.substring(7);
            BufferedImage customRendered = tryRenderEmbeddedTexture(payload, options, payload);
            if (customRendered != null) {
                String normalized = ItemModelResolver.normalizeItemTextureKey(payload);
                Float postScale = getPostRenderScale(normalized);
                if (postScale == null) postScale = getPostRenderScale(payload);
                if (postScale != null)
                    applyCenteredScale(customRendered, postScale);
                if (shouldAlignGuiItemToBottom(normalized))
                    alignImageToBottom(customRendered);
                return customRendered;
            }
            return renderGuiItemInternal(payload, options, null);
        }

        if (descriptor.toLowerCase().startsWith(HypixelPrefixes.SKYBLOCK.toLowerCase()))
            return renderSkyblockDescriptor(descriptor.substring(HypixelPrefixes.SKYBLOCK.length()), options);
        if (descriptor.toLowerCase().startsWith(HypixelPrefixes.LEGACY_SKYBLOCK.toLowerCase()))
            return renderSkyblockDescriptor(
                descriptor.substring(HypixelPrefixes.LEGACY_SKYBLOCK.length()), options);

        if (descriptor.toLowerCase().startsWith(HypixelPrefixes.NUMERIC.toLowerCase())) {
            String numericPayload = descriptor.substring(HypixelPrefixes.NUMERIC.length());
            try {
                short numericId = Short.parseShort(numericPayload);
                String mappedId = LegacyItemMappings.tryMapNumericId(numericId);
                if (mappedId != null)
                    return renderGuiItemInternal(mappedId, options, null);
            } catch (NumberFormatException ignored) {}
        }

        if (descriptor.toLowerCase().startsWith(HypixelPrefixes.LEGACY_NUMERIC.toLowerCase())) {
            String numericPayload = descriptor.substring(HypixelPrefixes.LEGACY_NUMERIC.length());
            try {
                short numericId = Short.parseShort(numericPayload);
                String mappedId = LegacyItemMappings.tryMapNumericId(numericId);
                if (mappedId != null)
                    return renderGuiItemInternal(mappedId, options, null);
            } catch (NumberFormatException ignored) {}
        }

        return renderGuiItemInternal(descriptor, options, null);
    }

    /**
     * Renders a pre-resolved block model instance to a 2D image.
     *
     * @param model the resolved model instance
     * @param options render options
     * @param blockName optional block name for tinting
     * @return the rendered image
     */
    public @NotNull BufferedImage renderModel(@NotNull BlockModel model, @NotNull BlockRenderOptions options,
                                              @Nullable String blockName) {
        ensureNotDisposed();
        return new ModelEngine(this, model, options, blockName).render();
    }

    // ----------------------------------------------------------------
    // Biome tinting
    // ----------------------------------------------------------------

    /**
     * Returns a biome-tinted version of a texture, caching the result.
     *
     * @param textureId the texture identifier
     * @param kind the biome tint type
     * @return the tinted texture
     */
    public @NotNull BufferedImage getBiomeTintedTexture(@NotNull String textureId, @NotNull BiomeTint kind) {
        String cacheKey = ColorUtil.normalizeResourceKey(textureId) + "|" + kind.name();
        BufferedImage cached = biomeTintedTextureCache.get(cacheKey);
        if (cached != null)
            return cached;

        BufferedImage colormap = switch (kind) {
            case GRASS -> textureContext.getGrassColorMap();
            case FOLIAGE -> textureContext.getFoliageColorMap();
            case DRY_FOLIAGE -> textureContext.getDryFoliageColorMap().orElse(null);
        };

        if (colormap == null)
            return textureContext.getTexture(textureId);

        int[] tintColor = sampleBiomeTintColor(colormap, kind);
        BufferedImage tinted = Renderer.applyTint(textureContext.getTexture(textureId), tintColor);
        biomeTintedTextureCache.put(cacheKey, tinted);
        return tinted;
    }

    // ----------------------------------------------------------------
    // Dispose / close
    // ----------------------------------------------------------------

    @Override
    public void close() {
        if (disposed) return;
        disposed = true;

        for (RenderContext context : packContextCache.values())
            context.close();
        packContextCache.clear();
        textureContext.close();
        biomeTintedTextureCache.clear();
        playerSkinCache.clear();
    }

    /**
     * Throws if this context has been disposed.
     */
    public void ensureNotDisposed() {
        if (disposed)
            throw new IllegalStateException("RenderContext has been disposed");
    }

    // ----------------------------------------------------------------
    // Private helpers (original)
    // ----------------------------------------------------------------

    /**
     * Returns true if resource packs are loaded in the JPA repository.
     *
     * @return true if any resource packs are available
     */
    public boolean hasResourcePacks() {
        try {
            return !MinecraftApi.getRepository(ResourcePack.class).findAll().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private @Nullable ResourcePack tryResolveResourcePack(String packId) {
        try {
            for (ResourcePack candidate : MinecraftApi.getRepository(ResourcePack.class).findAll()) {
                if (candidate.getId().equalsIgnoreCase(packId))
                    return candidate;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static int[] sampleBiomeTintColor(BufferedImage colormap, BiomeTint kind) {
        float[] coordinates = kind.getDefaultCoordinates();

        float temperature = Math.clamp(coordinates[0], 0f, 1f);
        float downfall = Math.clamp(coordinates[1], 0f, 1f);
        float rainfall = Math.clamp(downfall * temperature, 0f, 1f);
        int x = Math.max(0, Math.min(Math.round((1f - temperature) * (colormap.getWidth() - 1)), colormap.getWidth() - 1));
        int y = Math.max(0, Math.min(Math.round((1f - rainfall) * (colormap.getHeight() - 1)), colormap.getHeight() - 1));

        int argb = colormap.getRGB(x, y);
        return new int[] { (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF };
    }

    private static String initializePlayerSkinCacheDirectory(@Nullable String assetsDirectory) {
        List<String> candidates = new ArrayList<>();

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank())
            candidates.add(Path.of(localAppData, "MinecraftRenderer", "PlayerSkins").toString());

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank())
            candidates.add(Path.of(userHome, ".cache", "MinecraftRenderer", "PlayerSkins").toString());

        if (StringUtil.isNotEmpty(assetsDirectory)) {
            try {
                Path assetRoot = Path.of(assetsDirectory).toAbsolutePath();
                candidates.add(Path.of(assetRoot.toString(), "cached_player_skins").toString());
            } catch (Exception ignored) { }
        }

        candidates.add(Path.of(System.getProperty("java.io.tmpdir"), "MinecraftRenderer", "PlayerSkins").toString());

        for (String candidate : candidates) {
            if (StringUtil.isEmpty(candidate))
                continue;

            try {
                Files.createDirectories(Path.of(candidate));
                return candidate;
            } catch (Exception ignored) { }
        }

        throw new IllegalStateException("Unable to initialize player skin cache directory.");
    }

    // ----------------------------------------------------------------
    // Item render pipeline - main entry
    // ----------------------------------------------------------------

    private BufferedImage renderGuiItemInternal(String itemName, BlockRenderOptions options, @Nullable ItemRenderCapture capture) {
        options = options.mutate().withPadding(0f).build();
        ensureNotDisposed();

        String normalizedItemKey = ItemModelResolver.normalizeItemTextureKey(itemName);
        if (capture != null) {
            capture.setOriginalTarget(itemName.trim());
            capture.setNormalizedItemKey(normalizedItemKey);
        }

        boolean alignToBottom = shouldAlignGuiItemToBottom(normalizedItemKey);
        Float[] postScaleHolder = {null};

        ItemInfo itemInfo = null;
        if (hasItemData()) {
            itemInfo = getItemInfo(normalizedItemKey);
            if (capture != null)
                capture.setItemInfo(itemInfo);
        }

        invokeSkullResolverIfApplicable(normalizedItemKey, options);

        var resolveResult = ItemModelResolver.resolveItemModel(this, normalizedItemKey, itemInfo, options);
        BlockModel model = resolveResult.model();
        List<String> modelCandidates = resolveResult.candidates();
        String resolvedModelName = resolveResult.resolvedModelName();
        if (capture != null) {
            capture.setModel(model);
            capture.setModelCandidates(modelCandidates);
            capture.setResolvedModelName(resolvedModelName);
        }

        if (isBannerItem(normalizedItemKey) || (resolvedModelName != null && isBannerItem(resolvedModelName)))
            options = options.mutate().withAdditionalScale(options.getAdditionalScale() * 0.8f).build();

        if (options.getOverrideGuiTransform() == null && options.isUseGuiTransform() && model != null) {
            Transform guiOverride = model.getDisplayTransform("gui");
            if (guiOverride != null)
                options = options.mutate().withOverrideGuiTransform(guiOverride).build();
        }

        postScaleHolder[0] = getPostRenderScale(normalizedItemKey);
        if (postScaleHolder[0] == null)
            postScaleHolder[0] = getPostRenderScale(resolvedModelName);

        BufferedImage flatRender = tryRenderGuiTextureLayers(itemName, itemInfo, model, options);
        if (flatRender != null)
            return finalizeGuiResult(flatRender, postScaleHolder[0], alignToBottom, capture, options);

        BufferedImage bedComposite = tryRenderBedItem(itemName, model, options);
        if (bedComposite != null)
            return finalizeGuiResult(bedComposite, postScaleHolder[0], alignToBottom, capture, options);

        if (model != null && ItemModelResolver.isBillboardModel(model)) {
            List<String> billboardTextures = collectBillboardTextures(model, itemInfo);
            flatRender = tryRenderFlatItemFromIdentifiers(billboardTextures, model, options, itemName);
            if (flatRender != null)
                return finalizeGuiResult(flatRender, postScaleHolder[0], alignToBottom, capture, options);
        }

        if (model != null && !model.getElements().isEmpty())
            return finalizeGuiResult(new ModelEngine(this, model, options, itemName).render(),
                postScaleHolder[0], alignToBottom, capture, options);

        BufferedImage blockRender = tryRenderBlockEntityFallback(itemName, itemInfo, model,
            modelCandidates, options);
        if (blockRender != null)
            return finalizeGuiResult(blockRender, postScaleHolder[0], alignToBottom, capture, options);

        return finalizeGuiResult(renderFallbackTexture(itemName, itemInfo, model, options),
            postScaleHolder[0], alignToBottom, capture, options);
    }

    // ----------------------------------------------------------------
    // Item render pipeline - skull resolution
    // ----------------------------------------------------------------

    private static boolean isHeadItem(String normalizedKey) {
        String lower = normalizedKey.toLowerCase();
        for (String head : HEAD_ITEM_NAMES) {
            if (lower.equals(head) || lower.endsWith(":" + head))
                return true;
        }
        return false;
    }

    private static void invokeSkullResolverIfApplicable(String normalizedItemKey,
                                                         BlockRenderOptions options) {
        if (!isHeadItem(normalizedItemKey)) return;

        SkullTextureResolver resolver = options.getSkullTextureResolver();
        if (resolver == null) return;

        ItemRenderData itemData = options.getItemData();
        CompoundTag customData = itemData != null ? itemData.getCustomData() : null;
        CompoundTag profile = itemData != null ? itemData.getProfile() : null;

        String customDataId = null;
        if (customData != null)
            customDataId = customData.getOrDefault("id", StringTag.EMPTY).getValue();

        String fullItemId = normalizedItemKey.contains(":") ? normalizedItemKey : "minecraft:" + normalizedItemKey;
        SkullResolverContext context = new SkullResolverContext(fullItemId, itemData, customDataId, profile, customData);
        resolver.resolve(context);
    }

    // ----------------------------------------------------------------
    // Item render pipeline - texture layer rendering
    // ----------------------------------------------------------------

    private @Nullable BufferedImage tryRenderGuiTextureLayers(String itemName, @Nullable ItemInfo itemInfo, @Nullable BlockModel model, BlockRenderOptions options) {
        List<String> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        boolean isBillboard = model != null && ItemModelResolver.isBillboardModel(model);
        boolean[] hasModelLayer = { false };

        if (model != null) {
            model.getTextures()
                .stream()
                .filterKey(key -> key.toLowerCase().startsWith("layer"))
                .sortedByKey(String.CASE_INSENSITIVE_ORDER)
                .forEach((key, texture) -> {
                    String value = texture.sprite();

                    if (value != null && !value.isBlank() && (isBillboard || ItemModelResolver.isGuiTexture(value))) {
                        if (seen.add(value.toLowerCase())) {
                            candidates.add(value);
                            hasModelLayer[0] = true;
                        }
                    }
                });
        }

        if (!hasModelLayer[0] && itemInfo != null && itemInfo.getTexture() != null && !itemInfo.getTexture().isBlank()) {
            String tex = itemInfo.getTexture();

            if (isBillboard || ItemModelResolver.isGuiTexture(tex)) {
                if (seen.add(tex.toLowerCase()))
                    candidates.add(tex);
            }
        }

        if (!hasModelLayer[0]) {
            String normalized = ItemModelResolver.normalizeItemTextureKey(itemName);
            tryAddGuiCandidate(candidates, seen, "minecraft:item/" + normalized);
            tryAddGuiCandidate(candidates, seen, "minecraft:item/" + normalized + "_overlay");
            tryAddGuiCandidate(candidates, seen, "item/" + normalized);
            tryAddGuiCandidate(candidates, seen, "textures/item/" + normalized);
        }

        if (model != null && !model.getElements().isEmpty())
            return null;

        if (candidates.isEmpty())
            return null;

        return tryRenderFlatItemFromIdentifiers(candidates, model, options, itemName);
    }

    private static void tryAddGuiCandidate(List<String> candidates, Set<String> seen, String candidate) {
        if (ItemModelResolver.isGuiTexture(candidate) && seen.add(candidate.toLowerCase()))
            candidates.add(candidate);
    }

    private @Nullable BufferedImage tryRenderFlatItemFromIdentifiers(Iterable<String> identifiers, @Nullable BlockModel model, BlockRenderOptions options, @Nullable String tintContext) {
        List<String> resolved = resolveTextureIdentifiers(identifiers, model);
        List<String> available = new ArrayList<>();

        for (String textureId : resolved) {
            if (textureContext.tryGetTexture(textureId) != null)
                available.add(textureId);
        }

        if (available.isEmpty())
            return null;

        return renderFlatItem(available, options, tintContext);
    }

    // ----------------------------------------------------------------
    // Item render pipeline - flat item rendering
    // ----------------------------------------------------------------

    private @NotNull BufferedImage renderFlatItem(List<String> layerTextureIds, @NotNull BlockRenderOptions options, @Nullable String tintContext) {
        int size = options.getSize();
        ItemInfo itemInfo = null;
        String normalizedItemKey = null;

        if (tintContext != null && !tintContext.isBlank()) {
            normalizedItemKey = ItemModelResolver.normalizeItemTextureKey(tintContext);
            if (hasItemData()) {
                itemInfo = getItemInfo(tintContext);
                if (itemInfo == null && !normalizedItemKey.equalsIgnoreCase(tintContext))
                    itemInfo = getItemInfo(normalizedItemKey);
            }
        }

        int primaryTintLayerIndex = determinePrimaryTintLayerIndex(normalizedItemKey, itemInfo);
        ItemRenderData explicitItemData = options.getItemData();
        boolean disablePrimaryDefault = explicitItemData != null && explicitItemData.isDisableDefaultLayer0Tint();

        List<BufferedImage> resolvedLayers = new ArrayList<>(layerTextureIds.size());
        List<int[]> resolvedTints = new ArrayList<>(layerTextureIds.size());

        for (int layerIndex = 0; layerIndex < layerTextureIds.size(); layerIndex++) {
            String textureId = layerTextureIds.get(layerIndex);
            int @Nullable [] layerTint = getExplicitLayerTint(explicitItemData, layerIndex, primaryTintLayerIndex);
            boolean defaultTintApplied = false;
            if (layerTint == null) {
                layerTint = tryResolveDefaultLayerTint(normalizedItemKey, itemInfo, layerIndex,
                    layerIndex == primaryTintLayerIndex, disablePrimaryDefault);
                defaultTintApplied = layerTint != null;
            }

            if (defaultTintApplied && shouldBypassDefaultLayerTint(textureId, layerIndex, primaryTintLayerIndex, layerTextureIds.size()))
                layerTint = null;

            boolean hasExplicitPerLayerTint = explicitItemData != null
                && explicitItemData.getAdditionalLayerTints() != null
                && explicitItemData.getAdditionalLayerTints().containsKey(layerIndex);
            boolean hasPrimaryExplicitTint =
                layerIndex == primaryTintLayerIndex && explicitItemData != null
                    && explicitItemData.getLayer0Tint() != null;
            boolean skipContextTint = layerTint != null || hasExplicitPerLayerTint || hasPrimaryExplicitTint;

            resolvedLayers.add(resolveItemLayerTexture(textureId, tintContext, skipContextTint));
            resolvedTints.add(layerTint);
        }

        return new FlatItemRenderer(resolvedLayers, resolvedTints, size).render();
    }

    // ----------------------------------------------------------------
    // Item render pipeline - tinting
    // ----------------------------------------------------------------

    private static int @Nullable [] getExplicitLayerTint(@Nullable ItemRenderData itemData, int layerIndex, int primaryTintLayerIndex) {
        if (itemData == null)
            return null;

        if (itemData.getAdditionalLayerTints() != null
            && itemData.getAdditionalLayerTints().containsKey(layerIndex))
            return itemData.getAdditionalLayerTints().get(layerIndex);

        if (layerIndex == primaryTintLayerIndex && itemData.getLayer0Tint() != null)
            return itemData.getLayer0Tint();

        return null;
    }

    private static int determinePrimaryTintLayerIndex(@Nullable String normalizedItemKey, @Nullable ItemInfo itemInfo) {
        if (itemInfo != null && !itemInfo.getLayerTints().isEmpty()) {
            int minDyeLayer = Integer.MAX_VALUE;
            int minAnyLayer = Integer.MAX_VALUE;
            for (Map.Entry<Integer, TintInfo> e : itemInfo.getLayerTints().entrySet()) {
                if (e.getValue().getKind() == TintInfo.Kind.DYE)
                    minDyeLayer = Math.min(minDyeLayer, e.getKey());

                minAnyLayer = Math.min(minAnyLayer, e.getKey());
            }

            if (minDyeLayer != Integer.MAX_VALUE)
                return minDyeLayer;

            return minAnyLayer;
        }

        if (normalizedItemKey != null && !normalizedItemKey.isBlank()) {
            int[] overrides = LEGACY_DEFAULT_TINT_LAYER_OVERRIDES.get(normalizedItemKey);

            if (overrides != null && overrides.length > 0) {
                int min = overrides[0];
                for (int v : overrides) min = Math.min(min, v);
                return min;
            }
        }

        return 0;
    }

    private static int @Nullable [] tryResolveDefaultLayerTint(@Nullable String normalizedItemKey, @Nullable ItemInfo itemInfo, int layerIndex, boolean isPrimaryDyeLayer, boolean disablePrimaryDefault) {
        if (itemInfo != null && !itemInfo.getLayerTints().isEmpty()) {
            TintInfo tintInfo = itemInfo.getLayerTints().get(layerIndex);
            if (tintInfo != null) {
                switch (tintInfo.getKind()) {
                    case DYE:
                        if (!disablePrimaryDefault || !isPrimaryDyeLayer) {
                            if (tintInfo.getDefaultColor() != null)
                                return tintInfo.getDefaultColor();
                        }
                        break;
                    case CONSTANT:
                        if (tintInfo.getDefaultColor() != null)
                            return tintInfo.getDefaultColor();
                        break;
                    default:
                        if (tintInfo.getDefaultColor() != null && !(disablePrimaryDefault && isPrimaryDyeLayer))
                            return tintInfo.getDefaultColor();
                        break;
                }
            }
        }

        if (normalizedItemKey == null || normalizedItemKey.isBlank())
            return null;

        int[] overrides = LEGACY_DEFAULT_TINT_LAYER_OVERRIDES.get(normalizedItemKey);
        if (overrides != null && containsInt(overrides, layerIndex)) {
            if (!(disablePrimaryDefault && isPrimaryDyeLayer)) {
                int[] overrideColor = LEGACY_DEFAULT_TINT_OVERRIDES.get(normalizedItemKey);
                if (overrideColor != null) return overrideColor;
            }
        }

        if (layerIndex == 0) {
            int[] legacyColor = LEGACY_DEFAULT_TINT_OVERRIDES.get(normalizedItemKey);
            int[] constrainedLayers = LEGACY_DEFAULT_TINT_LAYER_OVERRIDES.get(normalizedItemKey);

            if (legacyColor != null
                && (constrainedLayers == null || containsInt(constrainedLayers, layerIndex))
                && !(disablePrimaryDefault && isPrimaryDyeLayer))
                return legacyColor;
        }

        if (normalizedItemKey.toLowerCase().startsWith("leather_") && layerIndex == 0 && !(disablePrimaryDefault && isPrimaryDyeLayer))
            return DEFAULT_LEATHER_ARMOR_COLOR;

        return null;
    }

    private static boolean shouldBypassDefaultLayerTint(String textureId, int layerIndex, int primaryTintLayerIndex, int totalLayerCount) {
        if (totalLayerCount != 1) return false;
        if (layerIndex != primaryTintLayerIndex) return false;
        String ns = (textureId == null || textureId.isBlank()) ? Namespace.DEFAULT_NAME : Namespace.of(textureId).name();
        return !ns.equalsIgnoreCase("minecraft");
    }

    private BufferedImage resolveItemLayerTexture(String textureId, @Nullable String tintContext, boolean skipContextTint) {
        if (skipContextTint)
            return textureContext.getTexture(textureId);

        int[] constantTint = ColorUtil.tryGetConstantTint(textureId, tintContext);
        if (constantTint != null)
            return textureContext.getTintedTexture(textureId,
                BlockColor.packArgb(constantTint),
                ColorUtil.CONSTANT_TINT_STRENGTH, 1f);

        BiomeTint biomeKind = ColorUtil.tryGetBiomeTint(textureId, tintContext);
        if (biomeKind != null)
            return getBiomeTintedTexture(textureId, biomeKind);

        return textureContext.getTexture(textureId);
    }

    // ----------------------------------------------------------------
    // Item render pipeline - block entity fallback
    // ----------------------------------------------------------------

    private @Nullable BufferedImage tryRenderBlockEntityFallback(String itemName, @Nullable ItemInfo itemInfo, @Nullable BlockModel model, List<String> modelCandidates, @NotNull BlockRenderOptions options) {
        BlockRenderOptions blockOptions = options;
        if (options.getOverrideGuiTransform() != null && model != null && model.getElements().isEmpty()) {
            Transform itemGuiTransform = model.getDisplayTransform("gui");

            if (itemGuiTransform == options.getOverrideGuiTransform())
                blockOptions = options.mutate().withOverrideGuiTransform(null).build();
        }

        for (String candidate : enumerateBlockFallbackNames(itemName, itemInfo, model, modelCandidates)) {
            try {
                return renderBlock(candidate, blockOptions);
            } catch (Exception ignored) { }
        }

        return null;
    }

    private static @NotNull List<String> enumerateBlockFallbackNames(String itemName, @Nullable ItemInfo itemInfo, @Nullable BlockModel model, List<String> modelCandidates) {
        List<String> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        addBlockCandidates(results, seen, ItemModelResolver.normalizeToBlockCandidates(itemName));
        if (itemInfo != null) {
            addBlockCandidates(results, seen, ItemModelResolver.normalizeToBlockCandidates(itemInfo.getModel()));
            addBlockCandidates(results, seen, ItemModelResolver.normalizeToBlockCandidates(itemInfo.getTexture()));
        }
        for (String mc : modelCandidates)
            addBlockCandidates(results, seen, ItemModelResolver.normalizeToBlockCandidates(mc));

        if (model != null) {
            addBlockCandidates(results, seen, ItemModelResolver.normalizeToBlockCandidates(model.getName()));
            for (String parent : model.getParentChain())
                addBlockCandidates(results, seen, ItemModelResolver.normalizeToBlockCandidates(parent));
            for (TextureReference ref : model.getTextures().values())
                addBlockCandidates(results, seen, ItemModelResolver.normalizeToBlockCandidates(ref.sprite()));
        }

        return results;
    }

    private static void addBlockCandidates(List<String> results, Set<String> seen, List<String> candidates) {
        for (String c : candidates) {
            if (seen.add(c.toLowerCase()))
                results.add(c);
        }
    }

    // ----------------------------------------------------------------
    // Item render pipeline - fallback rendering
    // ----------------------------------------------------------------

    private BufferedImage renderFallbackTexture(String itemName,
                                                @Nullable ItemInfo itemInfo,
                                                @Nullable BlockModel model,
                                                BlockRenderOptions options) {
        BufferedImage rendered = tryRenderFlatItemFromIdentifiers(
            collectItemLayerTextures(model, itemInfo), model, options, itemName);
        if (rendered != null) return rendered;

        if (itemInfo != null && itemInfo.getTexture() != null && !itemInfo.getTexture().isBlank()) {
            rendered = tryRenderEmbeddedTexture(itemInfo.getTexture(), options, itemName);
            if (rendered != null) return rendered;
        }

        for (String candidate : enumerateTextureFallbackCandidates(itemName)) {
            rendered = tryRenderEmbeddedTexture(candidate, options, itemName);
            if (rendered != null) return rendered;
        }

        return renderFlatItem(List.of("minecraft:missingno"), options, itemName);
    }

    private @Nullable BufferedImage tryRenderEmbeddedTexture(String textureId,
                                                              BlockRenderOptions options,
                                                              @Nullable String tintContext) {
        if (textureContext.tryGetTexture(textureId) != null)
            return renderFlatItem(List.of(textureId), options, tintContext);
        return null;
    }

    // ----------------------------------------------------------------
    // Item render pipeline - bed rendering
    // ----------------------------------------------------------------

    private @Nullable BufferedImage tryRenderBedItem(String itemName,
                                                      @Nullable BlockModel itemModel,
                                                      BlockRenderOptions options) {
        String normalizedName = ItemModelResolver.normalizeItemTextureKey(itemName);
        if (!normalizedName.toLowerCase().endsWith("_bed")
            && !normalizedName.equalsIgnoreCase("bed"))
            return null;

        String colorName = normalizedName.equalsIgnoreCase("bed")
            ? "red"
            : normalizedName.substring(0, normalizedName.length() - 4);

        String bedTextureId = "minecraft:entity/bed/" + colorName;
        if (textureContext.tryGetTexture(bedTextureId) == null) {
            bedTextureId = "minecraft:entity/bed/red";
            if (textureContext.tryGetTexture(bedTextureId) == null)
                return null;
        }

        BlockModel headModel = ItemModelResolver.resolveModelOrNull(this, "bed/bed_head");
        BlockModel footModel = ItemModelResolver.resolveModelOrNull(this, "bed/bed_foot");
        if (headModel == null || footModel == null) return null;

        List<Element> elements = new ArrayList<>();
        elements.addAll(cloneAndTranslateElements(headModel, new Vector3f(0f, 0f, -16f), false, false));
        elements.addAll(cloneAndTranslateElements(footModel, Vector3f.ZERO, true, true));

        if (elements.isEmpty()) return null;

        Map<String, TextureReference> textures = cloneTextureDictionary(itemModel);
        textures.put("bed", TextureReference.of(bedTextureId));
        if (!textures.containsKey("particle"))
            textures.put("particle", TextureReference.of(determineBedParticleTexture(colorName, bedTextureId)));

        BlockModel displaySource = itemModel;
        if (displaySource == null || displaySource.getDisplay().isEmpty()) {
            BlockModel templateBed = ItemModelResolver.resolveModelOrNull(this, "item/template_bed");
            if (templateBed != null) displaySource = templateBed;
        }

        Map<String, Transform> display = cloneDisplayDictionary(displaySource);
        adjustBedGuiTransform(display);

        List<String> parentChain = itemModel != null
            ? new ArrayList<>(itemModel.getParentChain())
            : new ArrayList<>();

        BlockRenderOptions renderOptions = options;
        Transform adjustedGui = display.get("gui");
        if (adjustedGui != null)
            renderOptions = renderOptions.mutate().withOverrideGuiTransform(adjustedGui).build();
        else
            renderOptions = renderOptions.mutate().withOverrideGuiTransform(null).build();

        BlockModel composite = new BlockModel(
            "minecraft:generated/bed_composite", Concurrent.newUnmodifiableList(parentChain),
            Concurrent.newUnmodifiableMap(textures), Concurrent.newUnmodifiableMap(display),
            Concurrent.newUnmodifiableList(elements));

        return new ModelEngine(this, composite, renderOptions, null).render();
    }

    private static List<Element> cloneAndTranslateElements(BlockModel source, Vector3f translation,
                                                                boolean flipBottomFaces,
                                                                boolean flipNorthSouthFaces) {
        List<Element> result = new ArrayList<>(source.getElements().size());
        for (Element element : source.getElements())
            result.add(cloneAndTranslateElement(element, translation, flipBottomFaces, flipNorthSouthFaces));
        return result;
    }

    private static Element cloneAndTranslateElement(Element element, Vector3f translation,
                                                          boolean flipBottomFaces, boolean flipNorthSouthFaces) {
        Vector3f from = element.getFrom().add(translation);
        Vector3f to = element.getTo().add(translation);

        Rotation rotation = null;
        if (element.getRotation() != null) {
            rotation = new Rotation(
                element.getRotation().getAngle(),
                element.getRotation().getOrigin().add(translation),
                element.getRotation().getAxis(),
                element.getRotation().isRescale());
        }

        Map<Face, FaceData> faces = new LinkedHashMap<>(element.getFaces().size());
        float elementHeight = element.getTo().getY() - element.getFrom().getY();
        boolean shouldFlipLargeFaces = elementHeight > 3.01f;

        for (Map.Entry<Face, FaceData> entry : element.getFaces().entrySet()) {
            Face direction = entry.getKey();
            FaceData face = entry.getValue();

            if (flipBottomFaces && direction == Face.DOWN && shouldFlipLargeFaces) {
                Vector4f uv = face.getUv();
                if (uv != null)
                    uv = new Vector4f(uv.getZ(), uv.getY(), uv.getX(), uv.getW());
                Integer rotated = face.getRotation() != null ? normalizeRotation(face.getRotation() + 180) : null;
                faces.put(direction, new FaceData(face.getTexture(), uv, rotated, face.getTintIndex(),
                    face.getCullFace()));
            } else if (flipNorthSouthFaces && shouldFlipLargeFaces
                && (direction == Face.NORTH || direction == Face.SOUTH)) {
                Vector4f uv = face.getUv();
                if (uv != null)
                    uv = new Vector4f(uv.getX(), uv.getW(), uv.getZ(), uv.getY());
                int rotated = normalizeRotation((face.getRotation() != null ? face.getRotation() : 0) + 180);
                faces.put(direction, new FaceData(face.getTexture(), uv, rotated, face.getTintIndex(),
                    face.getCullFace()));
            } else {
                faces.put(direction, new FaceData(face.getTexture(), face.getUv(), face.getRotation(),
                    face.getTintIndex(), face.getCullFace()));
            }
        }

        return new Element(from, to, rotation, faces, element.isShade());
    }

    private static Map<String, TextureReference> cloneTextureDictionary(@Nullable BlockModel source) {
        Map<String, TextureReference> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (source == null || source.getTextures().isEmpty()) return result;
        result.putAll(source.getTextures());
        return result;
    }

    private static Map<String, Transform> cloneDisplayDictionary(@Nullable BlockModel source) {
        Map<String, Transform> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (source == null || source.getDisplay().isEmpty()) return result;

        for (Map.Entry<String, Transform> entry : source.getDisplay().entrySet()) {
            Transform td = entry.getValue();
            result.put(entry.getKey(), Transform.create(
                td.getRotation() != null ? td.getRotation().clone() : null,
                td.getTranslation() != null ? td.getTranslation().clone() : null,
                td.getScale() != null ? td.getScale().clone() : null
            ));
        }
        return result;
    }

    private static void adjustBedGuiTransform(Map<String, Transform> display) {
        final float rotationAdjustment = 180f;
        final float scaleMultiplier = 0.9f;
        float[] defaultScale = {0.48f, 0.48f, 0.48f};
        float[] translationAdjustment = {-2.5f, -2.75f, 0f};

        Transform gui = display.get("gui");
        if (gui == null) {
            display.put("gui", Transform.create(
                new float[]{30f, 160f + rotationAdjustment, 0f},
                translationAdjustment.clone(),
                defaultScale.clone()
            ));
            return;
        }

        float[] rotationArray = gui.getRotation() != null
            ? Arrays.copyOf(gui.getRotation(), Math.max(gui.getRotation().length, 3))
            : new float[3];
        rotationArray = ensureLength(rotationArray, 3);
        rotationArray[1] = normalizeRotationFloat(rotationArray[1] + rotationAdjustment);

        float[] translationArray;
        if (gui.getTranslation() == null || gui.getTranslation().length == 0) {
            translationArray = new float[3];
        } else {
            translationArray = Arrays.copyOf(gui.getTranslation(), Math.max(gui.getTranslation().length, 3));
        }
        translationArray = ensureLength(translationArray, 3);
        for (int i = 0; i < 3; i++)
            translationArray[i] += translationAdjustment[i];

        float[] scaleArray;
        if (gui.getScale() == null || gui.getScale().length == 0) {
            scaleArray = defaultScale.clone();
        } else {
            scaleArray = Arrays.copyOf(gui.getScale(), Math.max(gui.getScale().length, 3));
            scaleArray = ensureLength(scaleArray, 3);
            for (int i = 0; i < scaleArray.length; i++)
                scaleArray[i] *= scaleMultiplier;
        }

        display.put("gui", Transform.create(rotationArray, translationArray, scaleArray));
    }

    private String determineBedParticleTexture(String colorName, String fallbackTextureId) {
        String candidate = "minecraft:block/" + colorName + "_wool";
        if (textureContext.tryGetTexture(candidate) != null)
            return candidate;
        return fallbackTextureId;
    }

    // ----------------------------------------------------------------
    // Item render pipeline - skyblock / firmament
    // ----------------------------------------------------------------

    private BufferedImage renderSkyblockDescriptor(String descriptorPayload,
                                                    BlockRenderOptions options) {
        var parsed = parseDescriptorPayload(descriptorPayload);
        String skyblockId = parsed.getLeft();
        Map<String, String> parameters = parsed.getRight();

        if (skyblockId == null || skyblockId.isBlank())
            return renderGuiItemInternal("minecraft:player_head", options, null);

        BlockRenderOptions mergedOptions = applySkyblockOverrides(options, skyblockId, parameters);
        String firmamentModel = "firmskyblock:item/" + ItemModelResolver.encodeFirmamentId(skyblockId);
        boolean hasTexturePack = mergedOptions.getPackIds() != null && !mergedOptions.getPackIds().isEmpty();

        if (hasTexturePack) {
            try {
                return renderGuiItemInternal(firmamentModel, mergedOptions, null);
            } catch (Exception ignored) {
            }
        }

        if (parameters.containsKey("base")) {
            String baseItem = parameters.get("base");
            if (baseItem != null && !baseItem.isBlank()) {
                try {
                    return renderGuiItemInternal(baseItem, mergedOptions, null);
                } catch (Exception ignored) {}
            }
        }

        if (parameters.containsKey("numeric")) {
            String numericValue = parameters.get("numeric");
            try {
                short numericId = Short.parseShort(numericValue);
                String mappedId = LegacyItemMappings.tryMapNumericId(numericId);
                if (mappedId != null && !mappedId.isBlank())
                    return renderGuiItemInternal(mappedId, mergedOptions, null);
            } catch (Exception ignored) {}
        }

        return renderGuiItemInternal("minecraft:player_head", mergedOptions, null);
    }

    private static Pair<String, Map<String, String>> parseDescriptorPayload(String payload) {
        Map<String, String> parameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (payload == null || payload.isBlank())
            return Pair.of("", parameters);

        int questionMarkIndex = payload.indexOf('?');
        if (questionMarkIndex < 0)
            return Pair.of(payload, parameters);

        String identifier = payload.substring(0, questionMarkIndex);
        String query = payload.substring(questionMarkIndex + 1);
        for (String segment : query.split("&")) {
            segment = segment.trim();
            if (segment.isEmpty()) continue;
            int equalsIndex = segment.indexOf('=');
            if (equalsIndex < 0)
                parameters.put(segment, "");
            else
                parameters.put(segment.substring(0, equalsIndex), segment.substring(equalsIndex + 1));
        }

        return Pair.of(identifier, parameters);
    }

    private static BlockRenderOptions applySkyblockOverrides(
        BlockRenderOptions options, String skyblockId, Map<String, String> parameters) {
        CompoundTag customCompound = new CompoundTag();
        customCompound.put("id", skyblockId.toUpperCase());

        if (parameters.containsKey("attrs")) {
            String attributeList = parameters.get("attrs");
            if (attributeList != null && !attributeList.isBlank()) {
                CompoundTag attrCompound = new CompoundTag();
                for (String name : attributeList.split(",")) {
                    name = name.trim();
                    if (!name.isEmpty())
                        attrCompound.put(name, "1");
                }
                customCompound.put("attributes", attrCompound);
            }
        }

        ItemRenderData mergedItemData = mergeItemRenderData(options.getItemData(), customCompound);
        return options.mutate().withItemData(mergedItemData).build();
    }

    private static ItemRenderData mergeItemRenderData(
        @Nullable ItemRenderData existing, CompoundTag customData) {
        if (existing == null)
            return new ItemRenderData(null, null, false, customData, null);
        CompoundTag mergedCustom = mergeCustomDataCompounds(existing.getCustomData(), customData);
        return existing.withCustomData(mergedCustom);
    }

    private static CompoundTag mergeCustomDataCompounds(@Nullable CompoundTag existing, CompoundTag overrides) {
        if (existing == null) return overrides;
        CompoundTag merged = new CompoundTag();
        merged.putAll(existing);
        merged.putAll(overrides);
        return merged;
    }

    // ----------------------------------------------------------------
    // Item render pipeline - billboard texture collection
    // ----------------------------------------------------------------

    private static List<String> collectBillboardTextures(BlockModel model,
                                                         @Nullable ItemInfo itemInfo) {
        List<String> textures = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        TextureReference crossRef = model.getTextures().get("cross");
        if (crossRef != null && !crossRef.sprite().isBlank() && seen.add(crossRef.sprite().toLowerCase()))
            textures.add(crossRef.sprite());

        TextureReference genericRef = model.getTextures().get("texture");
        if (genericRef != null && !genericRef.sprite().isBlank() && seen.add(genericRef.sprite().toLowerCase()))
            textures.add(genericRef.sprite());

        if (textures.isEmpty() && itemInfo != null && itemInfo.getTexture() != null
            && !itemInfo.getTexture().isBlank())
            textures.add(itemInfo.getTexture());

        return textures;
    }

    private static List<String> collectItemLayerTextures(@Nullable BlockModel model,
                                                          @Nullable ItemInfo itemInfo) {
        List<String> layers = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        if (model != null) {
            model.getTextures().entrySet().stream()
                .filter(kvp -> kvp.getKey().toLowerCase().startsWith("layer"))
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(kvp -> {
                    String sprite = kvp.getValue().sprite();
                    if (sprite != null && !sprite.isBlank()
                        && seen.add(sprite.toLowerCase()))
                        layers.add(sprite);
                });
        }

        if (itemInfo != null && itemInfo.getTexture() != null && !itemInfo.getTexture().isBlank()
            && seen.add(itemInfo.getTexture().toLowerCase()))
            layers.add(itemInfo.getTexture());

        return layers;
    }

    // ----------------------------------------------------------------
    // Item render pipeline - texture resolution helpers
    // ----------------------------------------------------------------

    private static List<String> resolveTextureIdentifiers(Iterable<String> identifiers, @Nullable BlockModel model) {
        List<String> resolved = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String identifier : identifiers) {
            if (identifier == null || identifier.isBlank())
                continue;

            String textureId = model.resolveTexture(identifier);
            if (StringUtil.isEmpty(textureId) || textureId.equalsIgnoreCase("minecraft:missingno"))
                continue;

            String canonical = ColorUtil.normalizeResourceKey(textureId);
            if (StringUtil.isEmpty(canonical))
                canonical = textureId;

            if (seen.add(canonical.toLowerCase()))
                resolved.add(textureId);
        }

        return resolved;
    }

    // ----------------------------------------------------------------
    // Item render pipeline - texture fallback enumeration
    // ----------------------------------------------------------------

    private static List<String> enumerateTextureFallbackCandidates(String itemName) {
        String normalized = ItemModelResolver.normalizeItemTextureKey(itemName);
        List<String> result = new ArrayList<>();
        Set<String> yielded = new HashSet<>();

        for (String candidate : enumerateTextureNameVariants(normalized)) {
            if (yielded.add(candidate.toLowerCase()))
                result.add(candidate);
        }

        if (ANIMATED_DIAL_ITEMS.contains(normalized)) {
            for (String candidate : enumerateTextureNameVariants(normalized + "_00")) {
                if (yielded.add(candidate.toLowerCase()))
                    result.add(candidate);
            }
        }

        return result;
    }

    private static List<String> enumerateTextureNameVariants(String textureKey) {
        return List.of(
            textureKey,
            "minecraft:item/" + textureKey,
            "item/" + textureKey,
            "textures/item/" + textureKey,
            "minecraft:block/" + textureKey,
            "block/" + textureKey
        );
    }

    // ----------------------------------------------------------------
    // Item render pipeline - GUI item appearance helpers
    // ----------------------------------------------------------------

    private static boolean shouldAlignGuiItemToBottom(@Nullable String normalizedItemKey) {
        if (StringUtil.isEmpty(normalizedItemKey))
            return false;

        for (String suffix : BOTTOM_ALIGNED_ITEM_SUFFIXES) {
            if (normalizedItemKey.toLowerCase().endsWith(suffix.toLowerCase()))
                return true;
        }

        return normalizedItemKey.equalsIgnoreCase("carpet")
            || normalizedItemKey.equalsIgnoreCase("trapdoor")
            || normalizedItemKey.equalsIgnoreCase("pressure_plate");
    }

    private static @Nullable Float getPostRenderScale(@Nullable String normalizedItemKey) {
        if (StringUtil.isEmpty(normalizedItemKey)) return null;
        if (isBedItem(normalizedItemKey)) return 0.92f;
        return null;
    }

    private static boolean isBedItem(String normalizedItemKey) {
        return normalizedItemKey.toLowerCase().endsWith("_bed")
            || normalizedItemKey.equalsIgnoreCase("bed");
    }

    private static boolean isBannerItem(@Nullable String normalizedItemKey) {
        if (StringUtil.isEmpty(normalizedItemKey)) return false;
        if (normalizedItemKey.toLowerCase().contains("banner")) return true;
        for (String suffix : BANNER_SUFFIXES)
            if (normalizedItemKey.toLowerCase().endsWith(suffix.toLowerCase())) return true;
        return false;
    }

    // ----------------------------------------------------------------
    // Item render pipeline - image manipulation
    // ----------------------------------------------------------------

    private static void alignImageToBottom(BufferedImage image) {
        Rectangle bounds = findOpaqueBounds(image);
        if (bounds.height <= 0) return;

        int desiredTop = image.getHeight() - bounds.height;
        int deltaY = desiredTop - bounds.y;
        if (deltaY == 0) return;

        BufferedImage clone = Renderer.copyImage(image);
        clearImage(image);
        Graphics2D g = image.createGraphics();
        g.drawImage(clone, 0, deltaY, null);
        g.dispose();
    }

    private static void applyCenteredScale(BufferedImage image, float scaleFactor) {
        if (scaleFactor <= 0f || Math.abs(scaleFactor - 1f) < 1e-3f) return;

        int targetWidth = Math.max(1, Math.round(image.getWidth() * scaleFactor));
        int targetHeight = Math.max(1, Math.round(image.getHeight() * scaleFactor));

        BufferedImage resized = Renderer.resizeNearestNeighbor(
            Renderer.copyImage(image), targetWidth, targetHeight);

        clearImage(image);
        int offsetX = (image.getWidth() - targetWidth) / 2;
        int offsetY = (image.getHeight() - targetHeight) / 2;
        Graphics2D g = image.createGraphics();
        g.drawImage(resized, offsetX, offsetY, null);
        g.dispose();
    }

    private static void clearImage(@NotNull BufferedImage image) {
        int[] empty = new int[image.getWidth() * image.getHeight()];
        image.setRGB(0, 0, image.getWidth(), image.getHeight(), empty, 0, image.getWidth());
    }

    private static @NotNull Rectangle findOpaqueBounds(@NotNull BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (((pixels[y * w + x] >> 24) & 0xFF) == 0) continue;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }

        if (maxX < 0 || maxY < 0)
            return new Rectangle(0, 0, 0, 0);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    // ----------------------------------------------------------------
    // Item render pipeline - finalize
    // ----------------------------------------------------------------

    private static BufferedImage finalizeGuiResult(BufferedImage image, @Nullable Float postScale, boolean alignToBottom, @Nullable ItemRenderCapture capture, BlockRenderOptions options) {
        if (capture != null)
            capture.setFinalOptions(options);

        if (postScale != null)
            applyCenteredScale(image, postScale);

        if (alignToBottom)
            alignImageToBottom(image);

        return image;
    }

    // ----------------------------------------------------------------
    // Item render pipeline - utility
    // ----------------------------------------------------------------

    private static int normalizeRotation(int rotation) {
        int normalized = rotation % 360;
        if (normalized < 0)
            normalized += 360;

        return normalized;
    }

    private static float normalizeRotationFloat(float rotation) {
        float normalized = rotation % 360f;
        if (normalized < 0f)
            normalized += 360f;

        return normalized;
    }

    private static float[] ensureLength(float @NotNull [] array, int length) {
        if (array.length >= length)
            return array;

        return Arrays.copyOf(array, length);
    }

    private static boolean containsInt(int @NotNull [] array, int value) {
        for (int v : array) {
            if (v == value)
                return true;
        }

        return false;
    }

    // ----------------------------------------------------------------
    // Inner class: ItemRenderCapture
    // ----------------------------------------------------------------

    /**
     * Mutable diagnostic carrier that captures intermediate results during item rendering.
     */
    @Getter
    @Setter(lombok.AccessLevel.PRIVATE)
    private static final class ItemRenderCapture {

        private String originalTarget = "";
        private String normalizedItemKey = "";
        private @Nullable ItemInfo itemInfo;
        private @Nullable BlockModel model;
        private @Nullable List<String> modelCandidates;
        private @Nullable String resolvedModelName;
        private @Nullable BlockRenderOptions finalOptions;

        @Nullable ItemModelResolution toResolution() {
            if (normalizedItemKey == null || normalizedItemKey.isBlank())
                return null;
            return new ItemModelResolution(normalizedItemKey, itemInfo, model,
                modelCandidates, resolvedModelName);
        }
    }

}
