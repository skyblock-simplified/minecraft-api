package dev.sbs.minecraftapi.render.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves skull textures for player heads and custom skulls.
 */
@FunctionalInterface
public interface SkullTextureResolver {

    /**
     * Resolves a skull texture for the given context.
     *
     * @param context complete context about the item being rendered
     * @return a texture value string (base64 or URL), or null if no texture is available
     */
    @Nullable String resolve(@NotNull SkullResolverContext context);
}
