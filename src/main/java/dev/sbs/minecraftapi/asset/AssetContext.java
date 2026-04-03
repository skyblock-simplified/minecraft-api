package dev.sbs.minecraftapi.asset;

import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.ResourcePack;
import dev.sbs.minecraftapi.asset.texture.OverlayRoot;
import dev.sbs.minecraftapi.asset.texture.TextureContext;
import dev.sbs.minecraftapi.asset.texture.pack.TexturePackStack;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bundles Minecraft asset state into a single object consumed by the render layer.
 * <p>
 * Serves as both the vanilla asset context and pack-specific context, replacing the
 * former separate {@code PackContext} class. Contains overlay roots, namespace registry,
 * pack IDs, resolved models, and a texture repository.
 * <p>
 * Block info and item info data are stored in the H2 database via
 * {@link MinecraftAssetFactory} and accessed through
 * {@link dev.sbs.api.SimplifiedApi#getRepository(Class)}.
 */
@Getter
public final class AssetContext {

    public static final String VANILLA_PACK_ID = "vanilla";

    // --- Asset fields ---

    private final @Nullable String assetsDirectory;
    private final @NotNull List<OverlayRoot> baseOverlayRoots;
    private final @NotNull Map<String, BlockModel> resolvedModels;
    private final @NotNull TextureContext textureContext;

    // --- Pack fields (merged from former PackContext) ---

    private final @NotNull List<OverlayRoot> overlayRoots;
    private final @NotNull List<String> packIds;
    private final @NotNull String packStackHash;
    private final @NotNull List<OverlayRoot> searchRoots;
    private final @NotNull AssetNamespaceRegistry assetNamespaces;

    private AssetContext(@Nullable String assetsDirectory,
                         @NotNull List<OverlayRoot> baseOverlayRoots,
                         @NotNull Map<String, BlockModel> resolvedModels,
                         @NotNull TextureContext textureContext,
                         @NotNull List<OverlayRoot> overlayRoots,
                         @NotNull List<String> packIds,
                         @NotNull String packStackHash,
                         @NotNull AssetNamespaceRegistry assetNamespaces) {
        this.assetsDirectory = assetsDirectory;
        this.baseOverlayRoots = baseOverlayRoots;
        this.resolvedModels = resolvedModels;
        this.textureContext = textureContext;
        this.overlayRoots = overlayRoots;
        this.packIds = packIds;
        this.packStackHash = packStackHash;
        this.assetNamespaces = assetNamespaces;
        this.searchRoots = buildSearchRoots();
    }

    private List<OverlayRoot> buildSearchRoots() {
        List<OverlayRoot> roots = new ArrayList<>();
        for (OverlayRoot overlay : overlayRoots)
            roots.add(new OverlayRoot(overlay.path(), overlay.sourceId(), overlay.kind()));

        String assetsRoot = assetsDirectory != null
            ? Path.of(assetsDirectory).toAbsolutePath().toString() : "";
        if (!assetsRoot.isBlank())
            roots.add(new OverlayRoot(assetsRoot, VANILLA_PACK_ID, OverlayRoot.Kind.VANILLA));

        return roots;
    }

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

    /**
     * Creates a vanilla asset context from a fully loaded asset factory.
     *
     * @param factory the loaded factory containing resolved models and overlays
     * @return a new asset context
     */
    public static @NotNull AssetContext fromFactory(@NotNull MinecraftAssetFactory factory) {
        List<OverlayRoot> overlayRoots = factory.getOverlayRoots();
        String assetsDir = factory.getAssetsDirectory();
        String assetsRoot = assetsDir != null ? Path.of(assetsDir).toAbsolutePath().toString() : "";
        AssetNamespaceRegistry namespaces = AssetNamespaceRegistry.buildFromRoots(assetsRoot, overlayRoots);
        TextureContext textureContext = new TextureContext(assetsDir, overlayRoots, namespaces);

        return new AssetContext(
            assetsDir, overlayRoots,
            factory.getResolvedModels(), textureContext,
            overlayRoots, Collections.emptyList(), VANILLA_PACK_ID, namespaces
        );
    }

    /**
     * Creates a pack-specific asset context from the given parameters.
     *
     * @param assetsDirectory the assets root directory, or null
     * @param baseOverlayRoots the base overlay roots
     * @param resolvedModels the pre-resolved models for this pack stack
     * @param packStack the texture pack stack, or null for vanilla
     * @return a new asset context
     */
    public static @NotNull AssetContext create(@Nullable String assetsDirectory,
                                                @NotNull List<OverlayRoot> baseOverlayRoots,
                                                @NotNull Map<String, BlockModel> resolvedModels,
                                                @Nullable TexturePackStack packStack) {
        List<OverlayRoot> overlays = new ArrayList<>(baseOverlayRoots);
        if (packStack != null)
            overlays.addAll(packStack.getOverlayRoots());

        String assetsRoot = assetsDirectory != null
            ? Path.of(assetsDirectory).toAbsolutePath().toString() : "";
        List<String> packIds = packStack != null
            ? packStack.getPacks().stream().map(ResourcePack::getId).collect(Collectors.toList())
            : Collections.emptyList();
        String packStackHash = packStack != null ? packStack.getFingerprint() : VANILLA_PACK_ID;
        AssetNamespaceRegistry namespaces = AssetNamespaceRegistry.buildFromRoots(assetsRoot, overlays);
        TextureContext textureContext = new TextureContext(assetsDirectory, overlays, namespaces);

        return new AssetContext(
            assetsDirectory, baseOverlayRoots,
            resolvedModels, textureContext,
            overlays, packIds, packStackHash, namespaces
        );
    }
}
