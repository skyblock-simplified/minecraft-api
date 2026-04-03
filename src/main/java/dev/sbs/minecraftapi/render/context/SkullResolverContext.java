package dev.sbs.minecraftapi.render.context;

import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context information provided to the skull texture resolver.
 */
@Getter
@RequiredArgsConstructor
public final class SkullResolverContext {

    private final @NotNull String itemId;
    private final @Nullable ItemRenderData itemData;
    private final @Nullable String customDataId;
    private final @Nullable CompoundTag profile;
    private final @Nullable CompoundTag customData;
}
