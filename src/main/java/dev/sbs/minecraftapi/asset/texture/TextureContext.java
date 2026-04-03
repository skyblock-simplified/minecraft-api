package dev.sbs.minecraftapi.asset.texture;

import dev.sbs.api.io.image.AnimatedImageData;
import dev.sbs.api.io.image.ImageFrame;
import dev.sbs.minecraftapi.asset.AssetNamespaceRegistry;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Public facade for the texture loading, caching, and tinting subsystem.
 * <p>
 * Wraps the internal {@link TextureRepository} and exposes only the consumer-facing API.
 * Resolves textures from multiple prioritized filesystem sources, handles tint colors,
 * and caches results for reuse.
 */
public final class TextureContext implements AutoCloseable {

    @Getter(onMethod_ = @NotNull)
    private final TextureRepository textureRepository;

    /**
     * Constructs a new texture context from an assets directory, overlay roots, and namespace registry.
     *
     * @param assetsDirectory the Minecraft assets root directory
     * @param overlayRoots the overlay roots providing additional search paths
     * @param assetNamespaces the asset namespace registry for multi-source resolution
     */
    public TextureContext(@NotNull String assetsDirectory,
                          @NotNull List<OverlayRoot> overlayRoots,
                          @NotNull AssetNamespaceRegistry assetNamespaces) {
        this.textureRepository = new TextureRepository(assetsDirectory, overlayRoots, assetNamespaces);
    }

    /**
     * Constructs a new texture context wrapping an existing texture repository.
     *
     * @param textureRepository the texture repository to wrap
     */
    public TextureContext(@NotNull TextureRepository textureRepository) {
        this.textureRepository = textureRepository;
    }

    /**
     * Retrieves a texture by its identifier, returning the missing texture placeholder if not found.
     *
     * @param textureId the texture resource identifier
     * @return the resolved texture, or the missing texture placeholder
     */
    public @NotNull BufferedImage getTexture(@Nullable String textureId) {
        return textureRepository.getTexture(textureId);
    }

    /**
     * Attempts to retrieve a texture by its identifier.
     *
     * @param textureId the texture resource identifier
     * @return the resolved texture, or null if not found
     */
    public @Nullable BufferedImage tryGetTexture(@Nullable String textureId) {
        return textureRepository.tryGetTexture(textureId);
    }

    /**
     * Retrieves a texture with an applied tint color.
     *
     * @param textureId the texture resource identifier
     * @param tintArgb the tint color as a packed ARGB integer
     * @param strengthMultiplier the tint strength multiplier
     * @param blend the blend factor between 0.0 (original) and 1.0 (fully tinted)
     * @return the tinted texture
     */
    public @NotNull BufferedImage getTintedTexture(@NotNull String textureId, int tintArgb,
                                                    float strengthMultiplier, float blend) {
        return textureRepository.getTintedTexture(textureId, tintArgb, strengthMultiplier, blend);
    }

    /**
     * Retrieves a texture with an applied tint color using default strength and full blend.
     *
     * @param textureId the texture resource identifier
     * @param tintArgb the tint color as a packed ARGB integer
     * @return the tinted texture
     */
    public @NotNull BufferedImage getTintedTexture(@NotNull String textureId, int tintArgb) {
        return textureRepository.getTintedTexture(textureId, tintArgb);
    }

    /**
     * Registers a texture in the cache under the given identifier.
     *
     * @param textureId the texture resource identifier
     * @param image the image to register
     * @param overwrite whether to overwrite an existing cached entry
     */
    public void registerTexture(@NotNull String textureId, @NotNull BufferedImage image, boolean overwrite) {
        textureRepository.registerTexture(textureId, image, overwrite);
    }

    /**
     * Registers a texture in the cache, overwriting any existing entry.
     *
     * @param textureId the texture resource identifier
     * @param image the image to register
     */
    public void registerTexture(@NotNull String textureId, @NotNull BufferedImage image) {
        textureRepository.registerTexture(textureId, image);
    }

    /** The grass biome colormap. */
    public @NotNull BufferedImage getGrassColorMap() {
        return textureRepository.getGrassColorMap();
    }

    /** The foliage biome colormap. */
    public @NotNull BufferedImage getFoliageColorMap() {
        return textureRepository.getFoliageColorMap();
    }

    /** The dry foliage biome colormap, if present. */
    public @NotNull Optional<BufferedImage> getDryFoliageColorMap() {
        return textureRepository.getDryFoliageColorMap();
    }

    /**
     * Attempts to retrieve the animation data for the given texture identifier.
     *
     * @param textureId the texture resource identifier
     * @return the animation data, or null if the texture is not animated
     */
    public @Nullable AnimatedImageData tryGetAnimation(@NotNull String textureId) {
        return textureRepository.tryGetAnimation(textureId);
    }

    /**
     * Returns whether interpolation is enabled for the given texture animation.
     *
     * @param textureId the texture resource identifier
     * @return true if interpolation is enabled
     */
    public boolean isInterpolationEnabled(@NotNull String textureId) {
        return textureRepository.isInterpolationEnabled(textureId);
    }

    /**
     * Begins an animation override scope that temporarily overrides specific texture frames.
     *
     * @param frames a map of normalized texture identifiers to their override frames
     * @return a scope that restores the previous state when closed
     */
    public @NotNull AutoCloseable beginAnimationOverride(@Nullable Map<String, ImageFrame> frames) {
        return textureRepository.beginAnimationOverride(frames);
    }

    @Override
    public void close() {
        textureRepository.close();
    }
}
