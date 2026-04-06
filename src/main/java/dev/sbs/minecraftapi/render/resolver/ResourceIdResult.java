package dev.sbs.minecraftapi.render.resolver;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A result containing the computed resource ID and its source pack.
 */
@Getter
@RequiredArgsConstructor
public final class ResourceIdResult {

    private final @NotNull String resourceId;
    private final @NotNull String sourcePackId;
    private final @NotNull String packStackHash;
    @Setter(lombok.AccessLevel.PACKAGE) private @Nullable String model;
    @Setter(lombok.AccessLevel.PACKAGE) private @NotNull ConcurrentList<String> textures = Concurrent.newList();

}
