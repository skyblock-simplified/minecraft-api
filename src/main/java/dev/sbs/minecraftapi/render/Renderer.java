package dev.sbs.minecraftapi.render;

import dev.sbs.api.io.image.PixelBuffer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A renderer that produces a {@link BufferedImage} from its configured state.
 *
 * <p>All concrete renderers - whether they project 3D models, composite flat texture layers,
 * arrange tiles in a grid, or rasterize styled text - implement this single interface.
 * Shared pixel-level utilities (copying, scaling, rotating, tinting) are provided as static
 * methods.
 */
public interface Renderer {

    /**
     * Produces the rendered image.
     *
     * @return a rendered image (TYPE_INT_ARGB)
     */
    @NotNull BufferedImage render();

    /**
     * Produces a list of animation frames. The default implementation returns a single frame
     * from {@link #render()}.
     *
     * @return an unmodifiable list of frame images
     */
    default @NotNull List<BufferedImage> renderFrames() {
        return List.of(render());
    }

    // ----------------------------------------------------------------
    // Shared static image utilities
    // ----------------------------------------------------------------

    /**
     * Creates a deep copy of the given image.
     *
     * @param source the source image
     * @return a new image with the same contents
     */
    static @NotNull BufferedImage copyImage(@NotNull BufferedImage source) {
        return PixelBuffer.wrap(source).toBufferedImage();
    }

    /**
     * Scales an image using nearest-neighbor interpolation.
     *
     * @param source the source image
     * @param width the target width
     * @param height the target height
     * @return a new scaled image
     */
    static @NotNull BufferedImage resizeNearestNeighbor(@NotNull BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    /**
     * Scales an image using bilinear interpolation.
     *
     * @param source the source image
     * @param width the target width
     * @param height the target height
     * @return a new scaled image
     */
    static @NotNull BufferedImage resizeBilinear(@NotNull BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    /**
     * Rotates an image by the specified number of degrees.
     *
     * @param source the source image
     * @param degrees the rotation angle in degrees
     * @return a new rotated image
     */
    static @NotNull BufferedImage rotateImage(@NotNull BufferedImage source, int degrees) {
        int w = source.getWidth();
        int h = source.getHeight();
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newW = (int) Math.round(w * cos + h * sin);
        int newH = (int) Math.round(h * cos + w * sin);
        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rotated.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.translate((newW - w) / 2.0, (newH - h) / 2.0);
        g.rotate(radians, w / 2.0, h / 2.0);
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return rotated;
    }

    /**
     * Applies a multiplicative RGB tint to an image, preserving the alpha channel.
     *
     * <p>Fully transparent pixels (alpha = 0) are left unmodified.
     *
     * @param source the source image
     * @param tintRgb an RGB array of length 3 with values in the range [0, 255]
     * @return a new tinted image
     */
    static @NotNull BufferedImage applyTint(@NotNull BufferedImage source, int @NotNull [] tintRgb) {
        PixelBuffer buffer = PixelBuffer.wrap(copyImage(source));
        int[] pixels = buffer.getPixels();
        float tR = tintRgb[0] / 255f;
        float tG = tintRgb[1] / 255f;
        float tB = tintRgb[2] / 255f;

        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = (argb >> 24) & 0xFF;
            if (a == 0) continue;

            int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) / 255f * tR * 255f));
            int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) / 255f * tG * 255f));
            int b = Math.min(255, Math.round((argb & 0xFF) / 255f * tB * 255f));

            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        return buffer.toBufferedImage();
    }
}
