package dev.sbs.minecraftapi.asset.context;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.asset.model.BlockInfo;
import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.ItemInfo;
import dev.sbs.minecraftapi.asset.namespace.AssetNamespaceRegistry;
import dev.sbs.minecraftapi.asset.texture.OverlayRoot;
import dev.sbs.minecraftapi.asset.texture.TextureContext;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

/**
 * Bundles Minecraft asset state into a single object consumed by the render layer.
 * <p>
 * Contains overlay roots, namespace registry, resolved models, and a texture context.
 * Subclasses determine how block and item data is sourced - {@link VanillaContext}
 * reads from the H2 database, while {@link PackContext} holds pack-specific data directly.
 *
 * @see VanillaContext
 * @see PackContext
 */
@Getter
public abstract class AssetContext {

    public static final String VANILLA_PACK_ID = "vanilla";

    private final @NotNull String assetsDirectory;
    private final @NotNull ConcurrentList<OverlayRoot> baseOverlayRoots;
    private final @NotNull ConcurrentMap<String, BlockModel> resolvedModels;
    private final @NotNull TextureContext textureContext;
    private final @NotNull ConcurrentList<OverlayRoot> overlayRoots;
    private final @NotNull ConcurrentList<OverlayRoot> searchRoots;
    private final @NotNull AssetNamespaceRegistry assetNamespaces;

    protected AssetContext(@NotNull String assetsDirectory,
                           @NotNull ConcurrentList<OverlayRoot> baseOverlayRoots,
                           @NotNull ConcurrentMap<String, BlockModel> resolvedModels,
                           @NotNull TextureContext textureContext,
                           @NotNull ConcurrentList<OverlayRoot> overlayRoots,
                           @NotNull AssetNamespaceRegistry assetNamespaces) {
        this.assetsDirectory = assetsDirectory;
        this.baseOverlayRoots = baseOverlayRoots;
        this.resolvedModels = resolvedModels;
        this.textureContext = textureContext;
        this.overlayRoots = overlayRoots;
        this.assetNamespaces = assetNamespaces;
        this.searchRoots = buildSearchRoots();
    }

    private ConcurrentList<OverlayRoot> buildSearchRoots() {
        ConcurrentList<OverlayRoot> roots = Concurrent.newList();
        for (OverlayRoot overlay : overlayRoots)
            roots.add(new OverlayRoot(overlay.path(), overlay.sourceId(), overlay.kind()));

        String assetsRoot = Path.of(assetsDirectory).toAbsolutePath().toString();
        if (!assetsRoot.isBlank())
            roots.add(new OverlayRoot(assetsRoot, VANILLA_PACK_ID, OverlayRoot.Kind.VANILLA));

        return roots;
    }

    /** The ordered pack identifiers, or an empty list for vanilla contexts. */
    public abstract @NotNull ConcurrentList<String> getPackIds();

    /** The pack stack fingerprint, or {@code "vanilla"} for vanilla contexts. */
    public abstract @NotNull String getPackStackHash();

    /**
     * Returns the block info list for this context.
     *
     * @return the block info list
     */
    public abstract @NotNull ConcurrentList<BlockInfo> getBlockInfos();

    /**
     * Returns the item info list for this context.
     *
     * @return the item info list
     */
    public abstract @NotNull ConcurrentList<ItemInfo> getItemInfos();

    /**
     * Resolves a model by name from the pre-resolved model map.
     *
     * @param name the model name (may include "minecraft:", "block/", or "blocks/" prefixes)
     * @return the resolved model, or null if not found
     */
    public @Nullable BlockModel resolveModel(@NotNull String name) {
        String normalized = BlockModel.normalizeName(name);
        BlockModel direct = resolvedModels.get(normalized);
        if (direct != null)
            return direct;

        for (Map.Entry<String, BlockModel> entry : resolvedModels.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(normalized))
                return entry.getValue();
        }

        return null;
    }

}
