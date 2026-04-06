package dev.sbs.minecraftapi.render.resolver;

import dev.simplified.image.ImageData;
import dev.simplified.image.ImageFactory;
import dev.simplified.image.ImageFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A rendered resource with its image data and resource ID.
 */
@Getter
@RequiredArgsConstructor
public final class RenderedResource implements AutoCloseable {

    private final @NotNull ImageData imageData;
    private final @NotNull ResourceIdResult resourceId;

    /**
     * Encodes this resource's image data to the given format.
     *
     * @param format the target image format
     * @return the encoded bytes
     */
    public byte @NotNull [] toByteArray(@NotNull ImageFormat format) {
        return new ImageFactory().toByteArray(imageData, format);
    }

    /**
     * Encodes this resource's image data as PNG.
     *
     * @return the PNG-encoded bytes
     */
    public byte @NotNull [] toPng() {
        return toByteArray(ImageFormat.PNG);
    }

    /**
     * Encodes this resource's image data as WebP.
     *
     * @return the WebP-encoded bytes
     */
    public byte @NotNull [] toWebP() {
        return toByteArray(ImageFormat.WEBP);
    }

    @Override
    public void close() {
        // No native resources to release
    }
}
