package dev.sbs.minecraftapi.render;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Composites multiple texture layers into a single flat item image, applying per-layer
 * tinting and nearest-neighbor scaling.
 */
@RequiredArgsConstructor
public final class FlatItemRenderer implements Renderer {

    private final @NotNull List<BufferedImage> layers;
    private final @NotNull List<int @Nullable []> layerTints;
    private final int size;

    @Override
    public @NotNull BufferedImage render() {
        BufferedImage canvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        for (int i = 0; i < layers.size(); i++) {
            BufferedImage texture = layers.get(i);

            float scale = Math.min(size / (float) texture.getWidth(), size / (float) texture.getHeight());
            int targetWidth = Math.max(1, Math.round(texture.getWidth() * scale));
            int targetHeight = Math.max(1, Math.round(texture.getHeight() * scale));

            BufferedImage resized = Renderer.resizeNearestNeighbor(texture, targetWidth, targetHeight);
            if (i < layerTints.size() && layerTints.get(i) != null)
                resized = Renderer.applyTint(resized, layerTints.get(i));

            int offsetX = (canvas.getWidth() - targetWidth) / 2;
            int offsetY = (canvas.getHeight() - targetHeight) / 2;
            Graphics2D g = canvas.createGraphics();
            g.drawImage(resized, offsetX, offsetY, null);
            g.dispose();
        }

        return canvas;
    }
}
