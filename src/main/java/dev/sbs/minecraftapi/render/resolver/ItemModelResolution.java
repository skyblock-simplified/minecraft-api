package dev.sbs.minecraftapi.render.resolver;

import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.ItemInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pre-resolved item model info for resource ID computation.
 */
@Getter
@RequiredArgsConstructor
public final class ItemModelResolution {

    private final @NotNull String lookupTarget;
    private final @Nullable ItemInfo itemInfo;
    private final @Nullable BlockModel model;
    private final @Nullable List<String> modelCandidates;
    private final @Nullable String resolvedModelName;
}
