package dev.sbs.minecraftapi.render.data;

import dev.sbs.api.math.Vector2f;
import dev.sbs.api.math.Vector3f;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Cube face definitions for Minecraft player head rendering.
 *
 * <p>Each constant carries the skin texture mapping coordinates for both the base and overlay
 * layers, the four vertex positions on the unit cube, and the UV layout for texture sampling.
 */
public enum HeadFace {

    FRONT(8, 8, 40, 8, new int[]{7, 6, 5, 4}, UvLayout.STANDARD),
    BACK(24, 8, 56, 8, new int[]{0, 1, 2, 3}, UvLayout.BACK),
    RIGHT(0, 8, 32, 8, new int[]{6, 2, 1, 5}, UvLayout.STANDARD),
    LEFT(16, 8, 48, 8, new int[]{3, 7, 4, 0}, UvLayout.STANDARD),
    TOP(8, 0, 40, 0, new int[]{3, 2, 6, 7}, UvLayout.STANDARD),
    BOTTOM(16, 0, 48, 0, new int[]{4, 5, 1, 0}, UvLayout.BOTTOM);

    private final @NotNull Rectangle baseMapping;
    private final @NotNull Rectangle overlayMapping;
    private final int @NotNull [] vertexIndices;
    private final @NotNull UvLayout uvLayout;
    private Vector3f @NotNull [] vertices;

    HeadFace(int baseX, int baseY, int overlayX, int overlayY,
             int @NotNull [] vertexIndices, @NotNull UvLayout uvLayout) {
        this.baseMapping = new Rectangle(baseX, baseY, 8, 8);
        this.overlayMapping = new Rectangle(overlayX, overlayY, 8, 8);
        this.vertexIndices = vertexIndices;
        this.uvLayout = uvLayout;
    }

    // Unit cube vertices centered at origin
    private static final Vector3f[] CUBE_VERTICES = {
        new Vector3f(-0.5f, -0.5f, -0.5f), // 0: bottom-left-back
        new Vector3f(0.5f, -0.5f, -0.5f),  // 1: bottom-right-back
        new Vector3f(0.5f, 0.5f, -0.5f),   // 2: top-right-back
        new Vector3f(-0.5f, 0.5f, -0.5f),  // 3: top-left-back
        new Vector3f(-0.5f, -0.5f, 0.5f),  // 4: bottom-left-front
        new Vector3f(0.5f, -0.5f, 0.5f),   // 5: bottom-right-front
        new Vector3f(0.5f, 0.5f, 0.5f),    // 6: top-right-front
        new Vector3f(-0.5f, 0.5f, 0.5f)    // 7: top-left-front
    };

    // Resolve vertex arrays after CUBE_VERTICES is initialized
    static {
        for (HeadFace face : values()) {
            int[] idx = face.vertexIndices;
            face.vertices = new Vector3f[]{
                CUBE_VERTICES[idx[0]], CUBE_VERTICES[idx[1]],
                CUBE_VERTICES[idx[2]], CUBE_VERTICES[idx[3]]
            };
        }
    }

    /**
     * Returns the skin texture mapping for the base or overlay layer.
     *
     * @param overlay true for the overlay (hat) layer, false for the base layer
     * @return the texture rectangle within the skin image
     */
    public @NotNull Rectangle getMapping(boolean overlay) {
        return overlay ? overlayMapping : baseMapping;
    }

    /**
     * The four vertex positions for this face on the unit cube.
     */
    public Vector3f @NotNull [] getVertices() {
        return vertices;
    }

    /**
     * The UV texture coordinates for this face's four vertices.
     */
    public Vector2f @NotNull [] getUvMap() {
        return uvLayout.uvMap;
    }

    // ---- UV layouts ----

    private enum UvLayout {

        STANDARD(new Vector2f[]{
            new Vector2f(1, 0), new Vector2f(0, 0), new Vector2f(0, 1), new Vector2f(1, 1)
        }),
        BACK(new Vector2f[]{
            new Vector2f(0, 1), new Vector2f(1, 1), new Vector2f(1, 0), new Vector2f(0, 0)
        }),
        BOTTOM(new Vector2f[]{
            new Vector2f(1, 1), new Vector2f(0, 1), new Vector2f(0, 0), new Vector2f(1, 0)
        });

        final Vector2f[] uvMap;

        UvLayout(Vector2f[] uvMap) {
            this.uvMap = uvMap;
        }
    }
}
