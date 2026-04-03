package dev.sbs.minecraftapi.render;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Composes a list of pre-rendered tile images into a single grid image.
 */
@RequiredArgsConstructor
public final class GridRenderer implements Renderer {

    private final @NotNull List<BufferedImage> tiles;
    private final int columns;
    private final int tileSize;
    private final boolean bilinear;

    @Override
    public @NotNull BufferedImage render() {
        int rows = (tiles.size() + columns - 1) / columns;
        BufferedImage canvas = new BufferedImage(columns * tileSize, rows * tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        for (int i = 0; i < tiles.size(); i++) {
            BufferedImage scaled = bilinear
                ? Renderer.resizeBilinear(tiles.get(i), tileSize, tileSize)
                : Renderer.resizeNearestNeighbor(tiles.get(i), tileSize, tileSize);
            g.drawImage(scaled, (i % columns) * tileSize, (i / columns) * tileSize, null);
        }

        g.dispose();
        return canvas;
    }
}
