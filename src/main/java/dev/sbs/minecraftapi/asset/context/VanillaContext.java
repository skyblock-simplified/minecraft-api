package dev.sbs.minecraftapi.asset.context;

import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.asset.MinecraftAssetFactory;
import dev.sbs.minecraftapi.asset.model.BlockInfo;
import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.ItemInfo;
import dev.sbs.minecraftapi.asset.namespace.AssetNamespaceRegistry;
import dev.sbs.minecraftapi.asset.texture.OverlayRoot;
import dev.sbs.minecraftapi.asset.texture.TextureContext;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Vanilla asset context that resolves block and item data from the H2 database.
 */
public final class VanillaContext extends AssetContext {

    private VanillaContext(@NotNull String assetsDirectory,
                           @NotNull ConcurrentList<OverlayRoot> baseOverlayRoots,
                           @NotNull ConcurrentMap<String, BlockModel> resolvedModels,
                           @NotNull TextureContext textureContext,
                           @NotNull ConcurrentList<OverlayRoot> overlayRoots,
                           @NotNull AssetNamespaceRegistry assetNamespaces) {
        super(assetsDirectory, baseOverlayRoots, resolvedModels, textureContext, overlayRoots, assetNamespaces);
    }

    @Override
    public @NotNull ConcurrentList<String> getPackIds() {
        return Concurrent.newUnmodifiableList();
    }

    @Override
    public @NotNull String getPackStackHash() {
        return VANILLA_PACK_ID;
    }

    @Override
    public @NotNull ConcurrentList<BlockInfo> getBlockInfos() {
        return MinecraftApi.getRepository(BlockInfo.class).findAll();
    }

    @Override
    public @NotNull ConcurrentList<ItemInfo> getItemInfos() {
        return MinecraftApi.getRepository(ItemInfo.class).findAll();
    }

    /**
     * Creates a vanilla asset context from a fully loaded asset factory.
     *
     * @param factory the loaded factory containing resolved models and overlays
     * @return a new vanilla asset context
     */
    public static @NotNull VanillaContext fromFactory(@NotNull MinecraftAssetFactory factory) {
        ConcurrentList<OverlayRoot> overlayRoots = factory.getOverlayRoots();
        String assetsDir = factory.getAssetsDirectory();
        String assetsRoot = Path.of(assetsDir).toAbsolutePath().toString();
        AssetNamespaceRegistry namespaces = AssetNamespaceRegistry.buildFromRoots(assetsRoot, overlayRoots);
        TextureContext textureContext = new TextureContext(assetsDir, overlayRoots, namespaces);

        return new VanillaContext(
            assetsDir, overlayRoots,
            factory.getResolvedModels(), textureContext,
            overlayRoots, namespaces
        );
    }

}
