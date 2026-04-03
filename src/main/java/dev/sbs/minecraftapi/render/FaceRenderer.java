package dev.sbs.minecraftapi.render;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

/**
 * Transforms a single texture into a face image by copying, rotating, and resizing.
 */
@RequiredArgsConstructor
public final class FaceRenderer implements Renderer {

    private final @NotNull BufferedImage texture;
    private final int size;
    private final int rotation;

    @Override
    public @NotNull BufferedImage render() {
        BufferedImage result = Renderer.copyImage(texture);

        int normalizedRotation = ((rotation % 360) + 360) % 360;
        if (normalizedRotation != 0)
            result = Renderer.rotateImage(result, normalizedRotation);

        if (result.getWidth() != size || result.getHeight() != size)
            result = Renderer.resizeNearestNeighbor(result, size, size);

        return result;
    }
}
