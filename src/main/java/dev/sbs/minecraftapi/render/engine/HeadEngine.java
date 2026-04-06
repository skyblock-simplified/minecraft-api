package dev.sbs.minecraftapi.render.engine;

import dev.sbs.minecraftapi.math.Matrix4f;
import dev.sbs.minecraftapi.math.Vector2f;
import dev.sbs.minecraftapi.math.Vector3f;
import dev.sbs.minecraftapi.render.context.HeadRenderOptions;
import dev.sbs.minecraftapi.render.data.HeadFace;
import dev.simplified.image.PixelBuffer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 3D rendering engine for projecting a Minecraft player head onto a 2D image.
 * <p>
 * Handles face processing, triangle rasterization with depth buffering, backface culling,
 * perspective projection, inventory lighting, and optional FXAA antialiasing.
 */
public final class HeadEngine implements RenderEngine {

    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final @NotNull Vector3f LIGHT_DIRECTION =
        Vector3f.normalize(new Vector3f(0.55f, -1f, 1.8f));

    private final @NotNull HeadRenderOptions options;
    private final @NotNull BufferedImage skin;

    public HeadEngine(@NotNull HeadRenderOptions options, @NotNull BufferedImage skin) {
        this.options = options;
        this.skin = skin;
    }

    // ----------------------------------------------------------------
    // Main render entry point
    // ----------------------------------------------------------------

    @Override
    public @NotNull BufferedImage render() {
        Matrix4f transform = createRotationMatrix(
            options.getYawInDegrees() * DEGREES_TO_RADIANS,
            options.getPitchInDegrees() * DEGREES_TO_RADIANS,
            options.getRollInDegrees() * DEGREES_TO_RADIANS
        );

        HeadFace[] faces = HeadFace.values();
        int initialCapacity = faces.length * (options.isShowOverlay() ? 4 : 2);
        List<VisibleTriangle> visibleTriangles = new ArrayList<>(initialCapacity);

        processFaces(faces, transform, false, skin, visibleTriangles);

        if (options.isShowOverlay()) {
            Matrix4f overlayTransform = Matrix4f.createScale(1.125f).multiply(transform);
            processFaces(faces, overlayTransform, true, skin, visibleTriangles);
        }

        visibleTriangles.sort((a, b) -> Float.compare(b.getDepth(), a.getDepth()));

        int size = options.getSize();
        PixelBuffer canvas = PixelBuffer.of(new int[size * size], size, size);
        float scale = size / 1.75f;
        float offsetX = size / 2f;
        float offsetY = size / 2f;
        float[] depthBuffer = new float[size * size];
        Arrays.fill(depthBuffer, Float.POSITIVE_INFINITY);

        RenderEngine.PerspectiveParams perspective = options.getPerspectiveAmount() > 0.01f
            ? new RenderEngine.PerspectiveParams(options.getPerspectiveAmount(), 10.0f, 10.0f)
            : null;

        int[] canvasPixels = canvas.getPixels();

        float depthBiasPerTriangle = 1e-4f;
        int triangleOrder = 0;
        for (VisibleTriangle tri : visibleTriangles) {
            VisibleTriangle.Projection proj = tri.project(0, 0, scale, offsetX, offsetY, perspective);

            rasterizeTriangle(
                canvasPixels, size, size,
                depthBuffer,
                triangleOrder * depthBiasPerTriangle,
                proj.z1(), proj.z2(), proj.z3(),
                proj.p1(), proj.p2(), proj.p3(),
                tri.getT1(), tri.getT2(), tri.getT3(),
                tri.getTexture(), tri.getTextureRect(),
                tri.getShading(),
                tri.isOverlay()
            );
            triangleOrder++;
        }

        if (options.isEnableAntiAliasing())
            canvas.applyFxaa();

        return canvas.toBufferedImage();
    }

    // ----------------------------------------------------------------
    // Face processing
    // ----------------------------------------------------------------

    private static void processFaces(HeadFace[] faces, Matrix4f transform, boolean isOverlay,
                                     BufferedImage skin, List<VisibleTriangle> triangles) {
        Vector3f[] transformed = new Vector3f[4];

        for (HeadFace face : faces) {
            Rectangle texRect = face.getMapping(isOverlay);
            Vector3f[] vertices = face.getVertices();
            Vector2f[] uvMap = face.getUvMap();

            for (int i = 0; i < 4; i++)
                transformed[i] = Vector3f.transform(vertices[i], transform);

            Vector3f edge1 = transformed[1].subtract(transformed[0]);
            Vector3f edge2 = transformed[2].subtract(transformed[0]);
            Vector3f normal = Vector3f.cross(edge1, edge2);

            if (!isOverlay && normal.getZ() < 0)
                continue;

            float shading = RenderEngine.computeInventoryLighting(normal, LIGHT_DIRECTION);
            float depth = (transformed[0].getZ() + transformed[1].getZ()
                + transformed[2].getZ() + transformed[3].getZ()) * 0.25f;

            triangles.add(new VisibleTriangle(
                transformed[0], transformed[1], transformed[2],
                uvMap[0], uvMap[1], uvMap[2],
                texRect, depth, shading,
                skin, null, 0, isOverlay
            ));

            triangles.add(new VisibleTriangle(
                transformed[0], transformed[2], transformed[3],
                uvMap[0], uvMap[2], uvMap[3],
                texRect, depth, shading,
                skin, null, 0, isOverlay
            ));
        }
    }

    // ----------------------------------------------------------------
    // Rotation matrix
    // ----------------------------------------------------------------

    private static Matrix4f createRotationMatrix(float yaw, float pitch, float roll) {
        float cosY = (float) Math.cos(yaw);
        float sinY = (float) Math.sin(yaw);
        float cosP = (float) Math.cos(pitch);
        float sinP = (float) Math.sin(pitch);
        float cosR = (float) Math.cos(roll);
        float sinR = (float) Math.sin(roll);

        return new Matrix4f(
            cosY * cosR + sinY * sinP * sinR, -cosY * sinR + sinY * sinP * cosR, sinY * cosP, 0,
            cosP * sinR, cosP * cosR, -sinP, 0,
            -sinY * cosR + cosY * sinP * sinR, sinY * sinR + cosY * sinP * cosR, cosY * cosP, 0,
            0, 0, 0, 1
        );
    }

    // ----------------------------------------------------------------
    // Triangle rasterization
    // ----------------------------------------------------------------

    private static void rasterizeTriangle(
        int[] canvasPixels, int canvasWidth, int canvasHeight,
        float[] depthBuffer,
        float depthBias,
        float z1, float z2, float z3,
        Vector2f p1, Vector2f p2, Vector2f p3,
        Vector2f t1, Vector2f t2, Vector2f t3,
        BufferedImage texture, Rectangle textureRect,
        float shadingFactor,
        boolean isOverlay
    ) {
        float area = (p2.getX() - p1.getX()) * (p3.getY() - p1.getY())
            - (p3.getX() - p1.getX()) * (p2.getY() - p1.getY());
        if (Math.abs(area) < 0.01f) return;

        Vector2f v0 = p2.subtract(p1);
        Vector2f v1 = p3.subtract(p1);
        float d00 = Vector2f.dot(v0, v0);
        float d01 = Vector2f.dot(v0, v1);
        float d11 = Vector2f.dot(v1, v1);
        float denom = d00 * d11 - d01 * d01;

        if (Math.abs(denom) < 1e-6f) return;

        RenderEngine.BarycentricData baryData = new RenderEngine.BarycentricData(v0, v1, d00, d01, d11, denom);

        int minX = (int) Math.max(0, Math.min(Math.min(p1.getX(), p2.getX()), p3.getX()));
        int minY = (int) Math.max(0, Math.min(Math.min(p1.getY(), p2.getY()), p3.getY()));
        int maxX = (int) Math.min(canvasWidth - 1,
            Math.ceil(Math.max(Math.max(p1.getX(), p2.getX()), p3.getX())));
        int maxY = (int) Math.min(canvasHeight - 1,
            Math.ceil(Math.max(Math.max(p1.getY(), p2.getY()), p3.getY())));

        int texWidth = textureRect.width - 1;
        int texHeight = textureRect.height - 1;
        int width = canvasWidth;

        int[] texturePixels = texture.getRGB(textureRect.x, textureRect.y,
            textureRect.width, textureRect.height, null, 0, textureRect.width);

        IntStream.rangeClosed(minY, maxY).parallel().forEach(y -> {
            int rowOffset = y * width;

            for (int x = minX; x <= maxX; x++) {
                Vector3f bary = RenderEngine.computeBarycentric(p1, new Vector2f(x + 0.5f, y + 0.5f), baryData);

                float epsilon = 1e-5f;
                if (bary.getX() < -epsilon || bary.getY() < -epsilon || bary.getZ() < -epsilon)
                    continue;

                float depth = z1 * bary.getX() + z2 * bary.getY() + z3 * bary.getZ() - depthBias;

                float texU = t1.getX() * bary.getX() + t2.getX() * bary.getY() + t3.getX() * bary.getZ();
                float texV = t1.getY() * bary.getX() + t2.getY() * bary.getY() + t3.getY() * bary.getZ();

                int texX = (int) Math.max(0, Math.min(texU * textureRect.width, texWidth));
                int texY = (int) Math.max(0, Math.min(texV * textureRect.height, texHeight));

                int argb = texturePixels[texY * textureRect.width + texX];
                int a = (argb >> 24) & 0xFF;

                if (!isOverlay && a != 255)
                    argb = (255 << 24) | (argb & 0x00FFFFFF);
                else if (isOverlay && a <= 10)
                    continue;

                int bufferIndex = rowOffset + x;
                if (depth >= depthBuffer[bufferIndex] - 1e-6f)
                    continue;

                depthBuffer[bufferIndex] = depth;
                canvasPixels[bufferIndex] = (shadingFactor >= 0.999f && shadingFactor <= 1.001f)
                    ? argb : RenderEngine.applyShading(argb, shadingFactor);
            }
        });
    }
}
