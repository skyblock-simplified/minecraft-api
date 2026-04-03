package dev.sbs.minecraftapi.render.engine;

import dev.sbs.api.math.Vector2f;
import dev.sbs.api.math.Vector3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Contract for 3D triangle-rasterization engines that project geometry onto a 2D image.
 *
 * <p>Shared pure-function utilities (projection, barycentric coordinates, shading, lighting,
 * antialiasing) are provided as static methods. Engine-specific rasterization and pipeline
 * logic live in the implementing classes.
 */
public interface RenderEngine {

    /**
     * Produces the rendered image.
     *
     * @return a rendered image (TYPE_INT_ARGB)
     */
    @NotNull BufferedImage render();

    // ----------------------------------------------------------------
    // Shared records
    // ----------------------------------------------------------------

    /**
     * Precomputed barycentric coordinate data for a triangle.
     */
    record BarycentricData(@NotNull Vector2f v0, @NotNull Vector2f v1,
                           float d00, float d01, float d11, float denom) {}

    /**
     * Perspective projection parameters.
     */
    record PerspectiveParams(float amount, float cameraDistance, float focalLength) {}

    // ----------------------------------------------------------------
    // Shared static methods
    // ----------------------------------------------------------------

    /**
     * Projects a 3D point onto 2D screen coordinates using orthographic projection with an
     * optional perspective blend.
     *
     * @param point the 3D point to project
     * @param scale the uniform scale factor
     * @param offsetX the horizontal screen offset
     * @param offsetY the vertical screen offset
     * @param perspective optional perspective parameters for blending with orthographic
     * @return the projected 2D screen coordinates
     */
    static @NotNull Vector2f projectToScreen(@NotNull Vector3f point, float scale,
                                             float offsetX, float offsetY,
                                             @Nullable PerspectiveParams perspective) {
        if (perspective == null)
            return new Vector2f(point.getX() * scale + offsetX, -point.getY() * scale + offsetY);

        float perspectiveFactor = perspective.focalLength()
            / (perspective.cameraDistance() - point.getZ());
        float perspX = point.getX() * perspectiveFactor;
        float perspY = point.getY() * perspectiveFactor;

        float finalX = point.getX() + (perspX - point.getX()) * perspective.amount();
        float finalY = point.getY() + (perspY - point.getY()) * perspective.amount();

        return new Vector2f(finalX * scale + offsetX, -finalY * scale + offsetY);
    }

    /**
     * Computes barycentric coordinates for a point relative to a triangle origin using
     * precomputed triangle data.
     *
     * @param origin the first vertex of the triangle (screen space)
     * @param point the point to test
     * @param data precomputed barycentric data for the triangle
     * @return barycentric coordinates (u, v, w) where u = 1 - v - w
     */
    static @NotNull Vector3f computeBarycentric(@NotNull Vector2f origin, @NotNull Vector2f point,
                                                @NotNull BarycentricData data) {
        Vector2f v2 = point.subtract(origin);
        float d20 = Vector2f.dot(v2, data.v0());
        float d21 = Vector2f.dot(v2, data.v1());

        float v = (data.d11() * d20 - data.d01() * d21) / data.denom();
        float w = (data.d00() * d21 - data.d01() * d20) / data.denom();

        return new Vector3f(1.0f - v - w, v, w);
    }

    /**
     * Applies a shading factor to an ARGB color, preserving the alpha channel.
     *
     * <p>Returns the color unmodified when the factor is approximately 1.0, and returns
     * alpha-only (black with original alpha) when the factor is zero or negative.
     *
     * @param argb the source color in ARGB format
     * @param factor the shading multiplier for RGB channels
     * @return the shaded ARGB color
     */
    static int applyShading(int argb, float factor) {
        if (factor <= 0f)
            return argb & 0xFF000000;

        if (Math.abs(factor - 1f) <= 1e-4f)
            return argb;

        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, Math.max(0, Math.round(((argb >> 16) & 0xFF) * factor)));
        int g = Math.min(255, Math.max(0, Math.round(((argb >> 8) & 0xFF) * factor)));
        int b = Math.min(255, Math.max(0, Math.round((argb & 0xFF) * factor)));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Computes inventory lighting intensity for a surface normal against a given light direction.
     *
     * @param normal the surface normal (need not be unit length)
     * @param lightDirection the normalized light direction vector
     * @return a lighting intensity in the range [0.2, 1.0]
     */
    static float computeInventoryLighting(@NotNull Vector3f normal, @NotNull Vector3f lightDirection) {
        final float normalEpsilon = 1e-6f;
        final float ambientStrength = 0.2f;
        final float diffuseStrength = 0.8f;

        float lengthSquared = normal.lengthSquared();
        if (lengthSquared <= normalEpsilon)
            return 1f;

        Vector3f normalized = normal.divide((float) Math.sqrt(lengthSquared));
        if (normalized.getY() >= 0.6f)
            return 1f;

        float diffuse = Math.max(0f, Vector3f.dot(normalized, lightDirection));
        float intensity = ambientStrength + diffuseStrength * Math.min(1f, diffuse);
        return Math.max(0.2f, Math.min(intensity, 1f));
    }

}
