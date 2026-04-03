package dev.sbs.minecraftapi.asset;

import dev.sbs.minecraftapi.asset.texture.OverlayRoot;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An ordered list of namespace roots that should be consulted when resolving Minecraft assets.
 * <p>
 * The order preserves vanilla roots first followed by progressively higher-priority overlays such as
 * custom data and texture packs. Consumers can enumerate roots in either direction depending on whether
 * they need override-first or fallback-first semantics.
 */
public final class AssetNamespaceRegistry {

    private final List<AssetNamespace> roots = new ArrayList<>();
    private final Map<String, List<AssetNamespace>> rootsByNamespace = new HashMap<>();
    private final Set<String> deduplicationSet = new HashSet<>();

    /**
     * Adds a namespace root to the registry using the provided insertion order. Duplicate
     * namespace/path pairs are ignored to keep iteration stable.
     *
     * @param namespaceName the namespace name (defaults to {@code "minecraft"} when blank)
     * @param path the filesystem path to the namespace root
     * @param sourceId a human-readable identifier for the source of this root
     * @param isVanilla whether this root represents vanilla Minecraft assets
     */
    public void addNamespace(@NotNull String namespaceName, @NotNull String path,
                             @NotNull String sourceId, boolean isVanilla) {
        if (namespaceName.isBlank()) {
            namespaceName = "minecraft";
        }

        if (path.isBlank()) {
            return;
        }

        Path fullPath = Path.of(path).toAbsolutePath().normalize();
        if (!Files.isDirectory(fullPath)) {
            return;
        }

        String identity = namespaceName.toLowerCase(Locale.ROOT) + "|" +
            fullPath.toString().toLowerCase(Locale.ROOT);
        if (!deduplicationSet.add(identity)) {
            return;
        }

        AssetNamespace root = new AssetNamespace(namespaceName, fullPath.toString(), sourceId, isVanilla);
        roots.add(root);

        rootsByNamespace
            .computeIfAbsent(namespaceName.toLowerCase(Locale.ROOT), k -> new ArrayList<>())
            .add(root);
    }

    /**
     * All roots tracked by the registry in insertion order (vanilla first, overlays last).
     */
    public @NotNull List<AssetNamespace> getRoots() {
        return Collections.unmodifiableList(roots);
    }

    /**
     * Retrieves the ordered roots for a specific namespace. If no entries are present for the
     * requested namespace, an empty list is returned.
     *
     * @param namespaceName the namespace to look up
     * @return an unmodifiable list of roots for the given namespace
     */
    public @NotNull List<AssetNamespace> getRoots(@NotNull String namespaceName) {
        if (namespaceName.isBlank()) {
            namespaceName = "minecraft";
        }

        List<AssetNamespace> bucket = rootsByNamespace.get(namespaceName.toLowerCase(Locale.ROOT));
        return bucket != null ? Collections.unmodifiableList(bucket) : List.of();
    }

    /**
     * Resolves the ordered set of namespace roots that should be consulted for the supplied namespace.
     * When {@code fallBackToMinecraft} is {@code true} and the namespace is unknown, the registry
     * returns the roots for the vanilla {@code "minecraft"} namespace instead.
     *
     * @param namespaceName the namespace to resolve
     * @param fallBackToMinecraft whether to fall back to the {@code "minecraft"} namespace
     * @return an unmodifiable list of resolved roots
     */
    public @NotNull List<AssetNamespace> resolveRoots(@NotNull String namespaceName,
                                                          boolean fallBackToMinecraft) {
        List<AssetNamespace> result = getRoots(namespaceName);
        if (result.isEmpty() && fallBackToMinecraft &&
            !namespaceName.equalsIgnoreCase("minecraft")) {
            return getRoots("minecraft");
        }
        return result;
    }

    /**
     * Resolves the ordered set of namespace roots that should be consulted for the supplied namespace,
     * falling back to the {@code "minecraft"} namespace when the requested namespace has no roots.
     *
     * @param namespaceName the namespace to resolve
     * @return an unmodifiable list of resolved roots
     */
    public @NotNull List<AssetNamespace> resolveRoots(@NotNull String namespaceName) {
        return resolveRoots(namespaceName, true);
    }

    /**
     * Enumerates candidate absolute paths for a relative asset path within the specified namespace.
     *
     * @param namespaceName the namespace to search within
     * @param relativePath the relative asset path
     * @param preferOverrides when {@code true}, higher-priority overlays are returned first
     * @return a list of candidate absolute paths
     */
    public @NotNull List<String> enumerateCandidatePaths(@NotNull String namespaceName,
                                                         @NotNull String relativePath,
                                                         boolean preferOverrides) {
        List<AssetNamespace> resolved = resolveRoots(namespaceName);
        if (resolved.isEmpty()) {
            return List.of();
        }

        List<String> paths = new ArrayList<>(resolved.size());
        if (preferOverrides) {
            for (int i = resolved.size() - 1; i >= 0; i--) {
                paths.add(Path.of(resolved.get(i).path()).resolve(relativePath).toString());
            }
        } else {
            for (AssetNamespace root : resolved) {
                paths.add(Path.of(root.path()).resolve(relativePath).toString());
            }
        }
        return paths;
    }

    /**
     * Returns a list of unique source IDs in the order they were first encountered.
     *
     * @return an unmodifiable list of source identifiers
     */
    public @NotNull List<String> getSources() {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (AssetNamespace root : roots) {
            seen.add(root.sourceId());
        }
        return List.copyOf(seen);
    }

    /**
     * Retrieves the ordered roots for a specific namespace and source ID.
     *
     * @param namespaceName the namespace to look up
     * @param sourceId the source identifier to filter by
     * @return an unmodifiable list of matching roots
     */
    public @NotNull List<AssetNamespace> getRoots(@NotNull String namespaceName,
                                                      @NotNull String sourceId) {
        if (namespaceName.isBlank()) {
            namespaceName = "minecraft";
        }

        List<AssetNamespace> bucket = rootsByNamespace.get(namespaceName.toLowerCase(Locale.ROOT));
        if (bucket == null) {
            return List.of();
        }

        return bucket.stream()
            .filter(root -> root.sourceId().equalsIgnoreCase(sourceId))
            .collect(Collectors.toUnmodifiableList());
    }

    // ================================================================
    // Static factory
    // ================================================================

    /**
     * Builds an asset namespace registry from an assets directory and overlay roots.
     * <p>
     * Registers the vanilla assets directory first, then adds overlay namespace roots in order.
     *
     * @param assetsDirectory the Minecraft assets root directory
     * @param overlayRoots the overlay roots to register after vanilla
     * @return a populated registry
     */
    public static @NotNull AssetNamespaceRegistry buildFromRoots(
        @NotNull String assetsDirectory,
        @NotNull List<OverlayRoot> overlayRoots
    ) {
        AssetNamespaceRegistry registry = new AssetNamespaceRegistry();

        if (!assetsDirectory.isBlank() && Files.isDirectory(Path.of(assetsDirectory)))
            registerNamespaceRoot(registry, "minecraft", assetsDirectory, "vanilla", true);

        for (OverlayRoot overlay : overlayRoots)
            addOverlayNamespaces(registry, overlay);

        return registry;
    }

    private static void addOverlayNamespaces(@NotNull AssetNamespaceRegistry registry, @NotNull OverlayRoot overlay) {
        if (overlay.path() == null || overlay.path().isBlank() || !Files.isDirectory(Path.of(overlay.path())))
            return;

        Path normalized = Path.of(overlay.path()).toAbsolutePath();
        Path parent = normalized.getParent();
        if (parent != null && parent.getFileName() != null
            && parent.getFileName().toString().equalsIgnoreCase("assets")) {
            registerNamespaceRoot(registry, normalized.getFileName().toString(),
                normalized.toString(), overlay.sourceId(), overlay.kind() == OverlayRoot.Kind.VANILLA);
            return;
        }

        Path assetsDir = normalized.resolve("assets");
        if (Files.isDirectory(assetsDir)) {
            try {
                Files.list(assetsDir)
                    .filter(Files::isDirectory)
                    .forEach(nsDir -> registerNamespaceRoot(registry, nsDir.getFileName().toString(),
                        nsDir.toAbsolutePath().toString(), overlay.sourceId(),
                        overlay.kind() == OverlayRoot.Kind.VANILLA));
            } catch (Exception ignored) {
            }
            return;
        }

        registerNamespaceRoot(registry, "minecraft", normalized.toString(), overlay.sourceId(),
            overlay.kind() == OverlayRoot.Kind.VANILLA);
    }

    private static void registerNamespaceRoot(@NotNull AssetNamespaceRegistry registry,
                                               @NotNull String namespaceName, @NotNull String path,
                                               @NotNull String sourceId, boolean isVanilla) {
        registry.addNamespace(namespaceName, path, sourceId, isVanilla);
        Path texturesPath = Path.of(path, "textures");
        if (Files.isDirectory(texturesPath))
            registry.addNamespace(namespaceName, texturesPath.toString(), sourceId, isVanilla);
    }

}
