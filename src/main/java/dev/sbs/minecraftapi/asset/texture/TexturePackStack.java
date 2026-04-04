package dev.sbs.minecraftapi.asset.texture;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.asset.ResourcePackDiscovery;
import dev.sbs.minecraftapi.asset.model.ResourcePack;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.NoSuchElementException;

/**
 * An ordered stack of resource packs with precomputed overlay roots and a combined
 * content fingerprint.
 */
@Getter
public final class TexturePackStack {

    private final @NotNull ConcurrentList<ResourcePack> packs;
    private final @NotNull ConcurrentList<OverlayRoot> overlayRoots;
    private final @NotNull String fingerprint;

    /**
     * Constructs a new {@code TexturePackStack} from the specified packs, overlay roots,
     * and combined fingerprint.
     *
     * @param packs the ordered list of resource packs
     * @param overlayRoots the ordered list of overlay root entries
     * @param fingerprint the SHA-256 fingerprint of the combined stack
     */
    public TexturePackStack(
        @NotNull ConcurrentList<ResourcePack> packs,
        @NotNull ConcurrentList<OverlayRoot> overlayRoots,
        @NotNull String fingerprint
    ) {
        this.packs = packs.toUnmodifiableList();
        this.overlayRoots = overlayRoots.toUnmodifiableList();
        this.fingerprint = fingerprint;
    }

    /**
     * Returns true if any pack in this stack supports Custom Item Textures.
     *
     * @return true if CIT is supported by at least one pack
     */
    public boolean isSupportsCit() {
        return packs.stream().anyMatch(ResourcePack::isSupportsCit);
    }

    /**
     * Builds an ordered texture pack stack from the given list of pack identifiers.
     *
     * <p>
     * Queries {@link SimplifiedApi#getRepository(Class)} for each pack ID (case-insensitive).
     * Returns an empty stack with a {@code "vanilla"} fingerprint when packIds is empty.
     *
     * @param packIds the ordered list of pack identifiers to include in the stack
     * @return the assembled texture pack stack
     * @throws NoSuchElementException if any pack id is not found in the repository
     */
    public static @NotNull TexturePackStack buildPackStack(@NotNull ConcurrentList<String> packIds) {
        if (packIds.isEmpty())
            return new TexturePackStack(Concurrent.newUnmodifiableList(), Concurrent.newUnmodifiableList(), "vanilla");

        ConcurrentList<ResourcePack> allPacks = SimplifiedApi.getRepository(ResourcePack.class).findAll();
        ConcurrentList<ResourcePack> ordered = getResourcePacks(packIds, allPacks);
        ConcurrentList<OverlayRoot> overlayRoots = Concurrent.newList();

        for (ResourcePack pack : ordered) {
            for (String overlayPath : pack.enumerateOverlayRootPaths())
                overlayRoots.add(new OverlayRoot(overlayPath, pack.getId(), OverlayRoot.Kind.RESOURCE_PACK));
        }

        String fingerprintInput = String.join("|",
            ordered.stream()
                .map(pack -> pack.getId() + ":" + pack.getFingerprint())
                .toList()
        );
        String stackFingerprint = ResourcePackDiscovery.computeSha256("packstack:" + fingerprintInput);

        return new TexturePackStack(ordered, overlayRoots, stackFingerprint);
    }

    private static @NonNull ConcurrentList<ResourcePack> getResourcePacks(@NonNull ConcurrentList<String> packIds, ConcurrentList<ResourcePack> allPacks) {
        ConcurrentList<ResourcePack> ordered = Concurrent.newList();

        for (String packId : packIds) {
            ResourcePack pack = null;
            for (ResourcePack candidate : allPacks) {
                if (candidate.getId().equalsIgnoreCase(packId)) {
                    pack = candidate;
                    break;
                }
            }

            if (pack == null)
                throw new NoSuchElementException("Unknown texture pack id '%s'".formatted(packId));

            ordered.add(pack);
        }

        return ordered;
    }

}
