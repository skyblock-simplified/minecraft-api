package dev.sbs.minecraftapi.render.resolver;

import dev.sbs.api.io.image.AnimatedImageData;
import dev.sbs.api.io.image.ImageFactory;
import dev.sbs.api.io.image.ImageFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * An animated rendered resource containing multiple frames with timing information.
 */
@Getter
@RequiredArgsConstructor
public final class AnimatedRenderedResource implements AutoCloseable {

    private final @NotNull ResourceIdResult resourceId;
    private final @NotNull AnimatedImageData animatedData;

    /**
     * Encodes this animation to the given format.
     *
     * @param format the target image format
     * @return the encoded bytes
     */
    public byte @NotNull [] toByteArray(@NotNull ImageFormat format) {
        return new ImageFactory().toByteArray(animatedData, format);
    }

    /**
     * Saves the animation as a GIF file.
     *
     * @param path the output file path
     */
    @SneakyThrows
    public void saveAsGif(@NotNull String path) {
        new ImageFactory().toFile(animatedData, ImageFormat.GIF, new File(path));
    }

    /**
     * Saves the animation as a WebP file.
     *
     * @param path the output file path
     */
    @SneakyThrows
    public void saveAsWebP(@NotNull String path) {
        new ImageFactory().toFile(animatedData, ImageFormat.WEBP, new File(path));
    }

    @Override
    public void close() {
        // No native resources to release
    }
}
