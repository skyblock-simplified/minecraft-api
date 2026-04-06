package dev.sbs.minecraftapi.asset.texture;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentSet;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * An overlay root path paired with its source identifier and kind.
 *
 * @param path the absolute filesystem path to the overlay root
 * @param sourceId a human-readable identifier for the source
 * @param kind the overlay root kind
 */
public record OverlayRoot(String path, String sourceId, Kind kind) {

    /**
     * The kind of overlay root used for asset resolution priority ordering.
     */
    public enum Kind {

        CUSTOM_DATA,
        RESOURCE_PACK,
        VANILLA

    }

    /**
     * Discovers overlay roots from the given assets directory by scanning for
     * {@code customdata/} directories in the parent and within the assets root itself.
     *
     * @param assetsDirectory the Minecraft assets root directory
     * @return a list of discovered overlay roots
     */
    public static @NotNull ConcurrentList<OverlayRoot> discoverFromAssetsDirectory(@NotNull String assetsDirectory) {
        ConcurrentList<OverlayRoot> overlays = Concurrent.newList();
        Path assetRoot = Path.of(assetsDirectory).toAbsolutePath();
        Path parent = assetRoot.getParent();

        if (parent != null)
            tryAddOverlay(overlays, Path.of(parent.toString(), "customdata").toString());

        tryAddOverlay(overlays, Path.of(assetRoot.toString(), "customdata").toString());
        return overlays;
    }

    private static void tryAddOverlay(@NotNull ConcurrentList<OverlayRoot> overlays, @NotNull String candidate) {
        Path fullPath = Path.of(candidate).toAbsolutePath();
        if (!Files.isDirectory(fullPath))
            return;

        String fullPathStr = fullPath.toString();
        for (OverlayRoot existing : overlays) {
            if (existing.path().equalsIgnoreCase(fullPathStr))
                return;
        }

        overlays.add(new OverlayRoot(fullPathStr, "customdata_" + overlays.size(), Kind.CUSTOM_DATA));
    }

    /**
     * Extracts a deduplicated list of unique paths from the given overlay roots,
     * preserving insertion order.
     *
     * @param roots the overlay roots to extract paths from
     * @return a list of unique overlay paths
     */
    public static @NotNull ConcurrentList<String> extractUniquePaths(@NotNull ConcurrentList<OverlayRoot> roots) {
        ConcurrentList<String> paths = Concurrent.newList();
        ConcurrentSet<String> seen = Concurrent.newSet();

        for (OverlayRoot root : roots) {
            if (seen.add(root.path().toLowerCase(Locale.ROOT)))
                paths.add(root.path());
        }

        return paths;
    }
}
