package dev.sbs.minecraftapi.asset.context;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.asset.model.BlockInfo;
import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.ItemInfo;
import dev.sbs.minecraftapi.asset.model.ResourcePack;
import dev.sbs.minecraftapi.asset.namespace.AssetNamespaceRegistry;
import dev.sbs.minecraftapi.asset.texture.OverlayRoot;
import dev.sbs.minecraftapi.asset.texture.TextureContext;
import dev.sbs.minecraftapi.asset.texture.TexturePackStack;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Pack-specific asset context that holds block and item data loaded from texture pack overlays.
 */
@Getter
public final class PackContext extends AssetContext {

    private final @NotNull ConcurrentList<BlockInfo> blockInfos;
    private final @NotNull ConcurrentList<ItemInfo> itemInfos;
    private final @NotNull ConcurrentList<String> packIds;
    private final @NotNull String packStackHash;

    private PackContext(@NotNull String assetsDirectory,
                        @NotNull ConcurrentList<OverlayRoot> baseOverlayRoots,
                        @NotNull ConcurrentMap<String, BlockModel> resolvedModels,
                        @NotNull TextureContext textureContext,
                        @NotNull ConcurrentList<OverlayRoot> overlayRoots,
                        @NotNull AssetNamespaceRegistry assetNamespaces,
                        @NotNull ConcurrentList<BlockInfo> blockInfos,
                        @NotNull ConcurrentList<ItemInfo> itemInfos,
                        @NotNull ConcurrentList<String> packIds,
                        @NotNull String packStackHash) {
        super(assetsDirectory, baseOverlayRoots, resolvedModels, textureContext, overlayRoots, assetNamespaces);
        this.blockInfos = blockInfos;
        this.itemInfos = itemInfos;
        this.packIds = packIds;
        this.packStackHash = packStackHash;
    }

    /**
     * Creates a pack-specific asset context with block/item info data.
     *
     * @param assetsDirectory the assets root directory
     * @param baseOverlayRoots the base overlay roots
     * @param resolvedModels the pre-resolved models for this pack stack
     * @param packStack the texture pack stack
     * @param blockInfos the pack-specific block infos
     * @param itemInfos the pack-specific item infos
     * @return a new pack-specific asset context
     */
    public static @NotNull PackContext create(@NotNull String assetsDirectory,
                                              @NotNull ConcurrentList<OverlayRoot> baseOverlayRoots,
                                              @NotNull ConcurrentMap<String, BlockModel> resolvedModels,
                                              @NotNull TexturePackStack packStack,
                                              @NotNull ConcurrentList<BlockInfo> blockInfos,
                                              @NotNull ConcurrentList<ItemInfo> itemInfos) {
        ConcurrentList<OverlayRoot> overlays = Concurrent.newList(baseOverlayRoots);
        overlays.addAll(packStack.getOverlayRoots());

        String assetsRoot = Path.of(assetsDirectory).toAbsolutePath().toString();
        ConcurrentList<String> packIds = packStack.getPacks()
            .stream()
            .map(ResourcePack::getId)
            .collect(Concurrent.toList());
        String packStackHash = packStack.getFingerprint();
        AssetNamespaceRegistry namespaces = AssetNamespaceRegistry.buildFromRoots(assetsRoot, overlays);
        TextureContext textureContext = new TextureContext(assetsDirectory, overlays, namespaces);

        return new PackContext(
            assetsDirectory, baseOverlayRoots,
            resolvedModels, textureContext,
            overlays, namespaces,
            blockInfos, itemInfos, packIds, packStackHash
        );
    }

}
