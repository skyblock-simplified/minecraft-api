package dev.sbs.minecraftapi.asset.texture;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.asset.ResourcePackDiscovery;
import dev.sbs.minecraftapi.asset.model.ResourcePack;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An ordered stack of resource packs with precomputed overlay roots and a combined
 * content fingerprint.
 */
@Getter
public final class TexturePackStack {

    private final @NotNull List<ResourcePack> packs;
    private final @NotNull List<OverlayRoot> overlayRoots;
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
        @NotNull List<ResourcePack> packs,
        @NotNull List<OverlayRoot> overlayRoots,
        @NotNull String fingerprint
    ) {
        this.packs = Collections.unmodifiableList(packs);
        this.overlayRoots = Collections.unmodifiableList(overlayRoots);
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
    public static @NotNull TexturePackStack buildPackStack(@NotNull List<String> packIds) {
        if (packIds.isEmpty())
            return new TexturePackStack(List.of(), List.of(), "vanilla");

        ConcurrentList<ResourcePack> allPacks = SimplifiedApi.getRepository(ResourcePack.class).findAll();
        List<ResourcePack> ordered = getResourcePacks(packIds, allPacks);
        List<OverlayRoot> overlayRoots = new ArrayList<>();

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

    private static @NonNull List<ResourcePack> getResourcePacks(@NonNull List<String> packIds, ConcurrentList<ResourcePack> allPacks) {
        List<ResourcePack> ordered = new ArrayList<>(packIds.size());

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
