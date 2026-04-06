package dev.sbs.minecraftapi.render.engine;

import dev.sbs.minecraftapi.math.Vector2f;
import dev.sbs.minecraftapi.math.Vector3f;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A projected triangle ready for rasterization, carrying vertex positions, texture coordinates,
 * depth, lighting, and engine-specific metadata.
 */
@Getter
@RequiredArgsConstructor
public final class VisibleTriangle {

    private final @NotNull Vector3f v1;

    private final @NotNull Vector3f v2;

    private final @NotNull Vector3f v3;

    private final @NotNull Vector2f t1;

    private final @NotNull Vector2f t2;

    private final @NotNull Vector2f t3;

    private final @NotNull Rectangle textureRect;

    private final float depth;

    private final float shading;

    /** The texture image to sample from. */
    private final @NotNull BufferedImage texture;

    /** The surface normal for backface culling, or null when culling is handled inline. */
    private final @Nullable Vector3f normal;

    /** Sort tiebreaker priority (higher values render later). */
    private final int renderPriority;

    /** Whether this triangle belongs to an overlay layer. */
    private final boolean overlay;

    /**
     * Screen-space projection of a triangle's vertices with their corresponding depth values.
     */
    public record Projection(@NotNull Vector2f p1, @NotNull Vector2f p2, @NotNull Vector2f p3,
                              float z1, float z2, float z3) {}

    /**
     * Projects this triangle's vertices to screen coordinates, optionally centering them first.
     *
     * @param centerX the x offset to subtract before projection (0 for no centering)
     * @param centerY the y offset to subtract before projection (0 for no centering)
     * @param scale the uniform scale factor
     * @param offsetX the horizontal screen offset
     * @param offsetY the vertical screen offset
     * @param perspective optional perspective parameters
     * @return the projected screen coordinates and depth values
     */
    public @NotNull Projection project(float centerX, float centerY, float scale,
                                       float offsetX, float offsetY,
                                       @Nullable RenderEngine.PerspectiveParams perspective) {
        Vector3f cv1 = new Vector3f(v1.getX() - centerX, v1.getY() - centerY, v1.getZ());
        Vector3f cv2 = new Vector3f(v2.getX() - centerX, v2.getY() - centerY, v2.getZ());
        Vector3f cv3 = new Vector3f(v3.getX() - centerX, v3.getY() - centerY, v3.getZ());

        return new Projection(
            RenderEngine.projectToScreen(cv1, scale, offsetX, offsetY, perspective),
            RenderEngine.projectToScreen(cv2, scale, offsetX, offsetY, perspective),
            RenderEngine.projectToScreen(cv3, scale, offsetX, offsetY, perspective),
            cv1.getZ(), cv2.getZ(), cv3.getZ()
        );
    }
}
