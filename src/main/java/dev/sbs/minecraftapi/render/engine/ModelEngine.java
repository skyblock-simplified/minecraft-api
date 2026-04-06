package dev.sbs.minecraftapi.render.engine;

import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.BlockModel.Element;
import dev.sbs.minecraftapi.asset.model.BlockModel.Face;
import dev.sbs.minecraftapi.asset.model.BlockModel.FaceData;
import dev.sbs.minecraftapi.asset.model.BlockModel.Transform;
import dev.sbs.minecraftapi.math.Matrix4f;
import dev.sbs.minecraftapi.math.Vector2f;
import dev.sbs.minecraftapi.math.Vector3f;
import dev.sbs.minecraftapi.math.Vector4f;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.RenderContext;
import dev.sbs.minecraftapi.render.data.BiomeTint;
import dev.sbs.minecraftapi.render.data.BlockColor;
import dev.sbs.minecraftapi.render.data.ColorUtil;
import dev.simplified.image.PixelBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 3D rendering engine for projecting Minecraft block models onto 2D images.
 * <p>
 * Handles triangle rasterization with depth buffering, backface culling,
 * orthographic and perspective projection, inventory lighting, and optional
 * FXAA antialiasing.
 */
public final class ModelEngine implements RenderEngine {

    private static final @NotNull Transform DEFAULT_GUI_TRANSFORM = createDefaultGuiTransform();
    private static final float DEFAULT_GUI_SCALE_MAGNITUDE = computeTransformScaleMagnitude(DEFAULT_GUI_TRANSFORM);
    private static final float DEFAULT_GUI_SCALE_NORMALIZATION =
        DEFAULT_GUI_SCALE_MAGNITUDE > 1e-6f ? 1f / DEFAULT_GUI_SCALE_MAGNITUDE : 1f;

    private static final @NotNull Vector3f LIGHT_DIRECTION =
        Vector3f.normalize(new Vector3f(-0.55f, -1f, 1.8f));

    private static final float DEGREES_TO_RADIANS = (float) Math.PI / 180f;

    private final @NotNull RenderContext context;
    private final @NotNull BlockModel model;
    private final @NotNull BlockRenderOptions options;
    private final @Nullable String blockName;

    public ModelEngine(@NotNull RenderContext context, @NotNull BlockModel model,
                       @NotNull BlockRenderOptions options, @Nullable String blockName) {
        this.context = context;
        this.model = model;
        this.options = options;
        this.blockName = blockName;
    }

    /** Axis-aligned 2D bounding box. */
    private record Bounds(float minX, float maxX, float minY, float maxY) {}

    // ----------------------------------------------------------------
    // Main render entry point
    // ----------------------------------------------------------------

    @Override
    public @NotNull BufferedImage render() {
        Transform guiTransform = options.getOverrideGuiTransform();
        if (guiTransform == null && options.isUseGuiTransform())
            guiTransform = model.getDisplayTransform("gui");
        if (guiTransform == null)
            guiTransform = DEFAULT_GUI_TRANSFORM;

        Matrix4f displayTransform = buildDisplayTransform(guiTransform, true);
        Matrix4f displayTransformWithoutScale = buildDisplayTransform(guiTransform, false);

        Matrix4f additionalRotation = Matrix4f.createRotationX(options.getRollInDegrees() * DEGREES_TO_RADIANS)
            .multiply(Matrix4f.createRotationY(options.getYawInDegrees() * DEGREES_TO_RADIANS))
            .multiply(Matrix4f.createRotationZ(options.getPitchInDegrees() * DEGREES_TO_RADIANS));

        Matrix4f scaleMatrix = Matrix4f.createScale(options.getAdditionalScale());
        Vector3f translationVector = new Vector3f(
            options.getAdditionalTranslation().getX() / 16f,
            options.getAdditionalTranslation().getY() / 16f,
            options.getAdditionalTranslation().getZ() / 16f);
        Matrix4f translationMatrix = Matrix4f.createTranslation(translationVector);

        Matrix4f totalTransform = displayTransform.multiply(additionalRotation).multiply(scaleMatrix)
            .multiply(translationMatrix);
        Matrix4f referenceTransform = displayTransformWithoutScale.multiply(additionalRotation)
            .multiply(translationMatrix);

        boolean applyInventoryLighting = options.isUseGuiTransform() || options.getOverrideGuiTransform() != null;
        List<VisibleTriangle> triangles =
            buildTriangles(context, model, totalTransform,
                applyInventoryLighting ? LIGHT_DIRECTION : null, blockName);

        cullBackfaces(triangles);

        if (triangles.isEmpty())
            return PixelBuffer.of(new int[options.getSize() * options.getSize()], options.getSize(), options.getSize()).toBufferedImage();

        triangles.sort((a, b) -> {
            int depthComparison = Float.compare(b.getDepth(), a.getDepth());
            return depthComparison != 0 ? depthComparison : Integer.compare(a.getRenderPriority(), b.getRenderPriority());
        });

        Bounds bounds = computeBounds(triangles);
        Bounds referenceBounds = computeReferenceBounds(referenceTransform);
        float padding = Math.max(0f, Math.min(options.getPadding(), 0.4f));
        float dimensionX = bounds.maxX() - bounds.minX();
        float dimensionY = bounds.maxY() - bounds.minY();
        float dimension = Math.max(dimensionX, dimensionY);
        if (dimension < 1e-5f)
            dimension = 1f;

        float referenceDimensionX = referenceBounds.maxX() - referenceBounds.minX();
        float referenceDimensionY = referenceBounds.maxY() - referenceBounds.minY();
        float referenceDimension = Math.max(referenceDimensionX, referenceDimensionY);
        if (referenceDimension < 1e-5f)
            referenceDimension = dimension;

        float availableSize = options.getSize() * (1f - padding * 2f);
        float scale = availableSize / referenceDimension * DEFAULT_GUI_SCALE_NORMALIZATION;

        float[] translation = guiTransform.getTranslation();
        if (translation == null) translation = new float[]{0f, 0f, 0f};
        boolean hasExplicitTranslation = Math.abs(safeGet(translation, 0)) > 0.1f
            || Math.abs(safeGet(translation, 1)) > 0.1f
            || Math.abs(safeGet(translation, 2)) > 0.1f;

        float centerX = hasExplicitTranslation ? 0f : (bounds.minX() + bounds.maxX()) * 0.5f;
        float centerY = hasExplicitTranslation ? 0f : (bounds.minY() + bounds.maxY()) * 0.5f;
        float offsetX = options.getSize() / 2f;
        float offsetY = options.getSize() / 2f;

        RenderEngine.PerspectiveParams perspective = options.getPerspectiveAmount() > 0.01f
            ? new RenderEngine.PerspectiveParams(options.getPerspectiveAmount(), 10f, 10f)
            : null;

        int size = options.getSize();
        PixelBuffer canvas = PixelBuffer.of(new int[size * size], size, size);
        float[] depthBuffer = new float[size * size];
        java.util.Arrays.fill(depthBuffer, Float.NEGATIVE_INFINITY);
        int triangleOrder = 0;
        final float depthBiasPerTriangle = 1e-4f;

        for (VisibleTriangle tri : triangles) {
            VisibleTriangle.Projection proj = tri.project(centerX, centerY, scale, offsetX, offsetY, perspective);

            float depthBias = triangleOrder * depthBiasPerTriangle;
            triangleOrder++;

            rasterizeTriangle(
                canvas.getPixels(), size, size,
                depthBuffer, depthBias,
                proj.z1(), proj.z2(), proj.z3(),
                proj.p1(), proj.p2(), proj.p3(),
                tri.getT1(), tri.getT2(), tri.getT3(),
                tri.getTexture(), tri.getTextureRect(), tri.getShading());
        }

        if (options.isEnableAntiAliasing())
            canvas.applyFxaa();

        return canvas.toBufferedImage();
    }

    // ----------------------------------------------------------------
    // Backface culling
    // ----------------------------------------------------------------

    private static void cullBackfaces(List<VisibleTriangle> triangles) {
        if (RenderContext.debugDisableCulling) return;

        final float normalLengthThreshold = 1e-6f;
        final float dotCullThreshold = 5e-3f;
        Vector3f cameraForward = new Vector3f(0f, 0f, -1f);

        for (int i = triangles.size() - 1; i >= 0; i--) {
            VisibleTriangle triangle = triangles.get(i);
            Vector3f normal = triangle.getNormal();
            if (normal.lengthSquared() < normalLengthThreshold)
                continue;
            float dot = Vector3f.dot(normal, cameraForward);
            if (dot > dotCullThreshold)
                triangles.remove(i);
        }
    }

    // ----------------------------------------------------------------
    // Bounds computation
    // ----------------------------------------------------------------

    private static Bounds computeBounds(List<VisibleTriangle> triangles) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (VisibleTriangle tri : triangles) {
            for (Vector3f v : new Vector3f[]{tri.getV1(), tri.getV2(), tri.getV3()}) {
                minX = Math.min(minX, v.getX());
                maxX = Math.max(maxX, v.getX());
                minY = Math.min(minY, v.getY());
                maxY = Math.max(maxY, v.getY());
            }
        }

        return new Bounds(minX, maxX, minY, maxY);
    }

    private static Bounds computeReferenceBounds(Matrix4f transform) {
        Vector3f[] corners = {
            new Vector3f(-0.5f, -0.5f, -0.5f),
            new Vector3f(0.5f, -0.5f, -0.5f),
            new Vector3f(0.5f, 0.5f, -0.5f),
            new Vector3f(-0.5f, 0.5f, -0.5f),
            new Vector3f(-0.5f, -0.5f, 0.5f),
            new Vector3f(0.5f, -0.5f, 0.5f),
            new Vector3f(0.5f, 0.5f, 0.5f),
            new Vector3f(-0.5f, 0.5f, 0.5f)
        };

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (Vector3f corner : corners) {
            Vector3f transformed = Vector3f.transform(corner, transform);
            minX = Math.min(minX, transformed.getX());
            maxX = Math.max(maxX, transformed.getX());
            minY = Math.min(minY, transformed.getY());
            maxY = Math.max(maxY, transformed.getY());
        }

        if (Float.isInfinite(minX) || Float.isInfinite(minY) || Float.isInfinite(maxX) || Float.isInfinite(maxY))
            return new Bounds(-0.5f, 0.5f, -0.5f, 0.5f);

        return new Bounds(minX, maxX, minY, maxY);
    }

    // ----------------------------------------------------------------
    // Display transform
    // ----------------------------------------------------------------

    private static @NotNull Matrix4f buildDisplayTransform(@Nullable Transform transform, boolean includeScale) {
        if (transform == null)
            return Matrix4f.IDENTITY;

        float[] rotation = transform.getRotation();
        if (rotation == null) rotation = new float[]{0f, 0f, 0f};
        float[] translation = transform.getTranslation();
        if (translation == null) translation = new float[]{0f, 0f, 0f};
        float[] scaleComponents = transform.getScale();
        if (scaleComponents == null) scaleComponents = new float[]{1f, 1f, 1f};

        float scaleX = scaleComponents.length > 0 ? scaleComponents[0] : 1f;
        float scaleY = scaleComponents.length > 1 ? scaleComponents[1] : scaleX;
        float scaleZ = scaleComponents.length > 2 ? scaleComponents[2] : scaleX;

        if (!includeScale) {
            scaleX = 1f;
            scaleY = 1f;
            scaleZ = 1f;
        }

        ItemTransform itemTransform = new ItemTransform(
            new Vector3f(safeGet(rotation, 0), safeGet(rotation, 1), safeGet(rotation, 2)),
            new Vector3f(safeGet(translation, 0), safeGet(translation, 1), safeGet(translation, 2)),
            new Vector3f(scaleX, scaleY, scaleZ)
        );

        return itemTransform.buildMatrix();
    }

    private static float computeTransformScaleMagnitude(@Nullable Transform transform) {
        if (transform == null || transform.getScale() == null || transform.getScale().length == 0)
            return 1f;

        float[] components = transform.getScale();
        float sx = Math.abs(components[0]);
        float sy = components.length > 1 ? Math.abs(components[1]) : sx;
        float sz = components.length > 2 ? Math.abs(components[2]) : sx;
        float max = Math.max(Math.max(sx, sy), sz);
        return max > 1e-6f ? max : 1f;
    }

    // ----------------------------------------------------------------
    // Rasterization
    // ----------------------------------------------------------------

    private static void rasterizeTriangle(
        int @NotNull [] canvasPixels, int canvasWidth, int canvasHeight,
        float[] depthBuffer,
        float depthBias,
        float z1, float z2, float z3,
        Vector2f p1, Vector2f p2, Vector2f p3,
        Vector2f t1, Vector2f t2, Vector2f t3,
        BufferedImage texture,
        Rectangle textureRect,
        float shadingFactor
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

        final float depthTestEpsilon = 1e-6f;
        final float alphaThreshold = 10f;

        int[] texturePixels = texture.getRGB(textureRect.x, textureRect.y,
            textureRect.width, textureRect.height, null, 0, textureRect.width);

        IntStream.rangeClosed(minY, maxY).parallel().forEach(y -> {
            int rowOffset = y * canvasWidth;
            for (int x = minX; x <= maxX; x++) {
                Vector2f point = new Vector2f(x + 0.5f, y + 0.5f);
                Vector3f bary = RenderEngine.computeBarycentric(p1, point, baryData);

                final float epsilon = 1e-4f;
                if (bary.getX() < -epsilon || bary.getY() < -epsilon || bary.getZ() < -epsilon)
                    continue;

                float depth = z1 * bary.getX() + z2 * bary.getY() + z3 * bary.getZ() - depthBias;

                float texCoordX = t1.getX() * bary.getX() + t2.getX() * bary.getY() + t3.getX() * bary.getZ();
                float texCoordY = t1.getY() * bary.getX() + t2.getY() * bary.getY() + t3.getY() * bary.getZ();

                int texX = (int) Math.max(0, Math.min(texCoordX * textureRect.width, texWidth));
                int texY = (int) Math.max(0, Math.min(texCoordY * textureRect.height, texHeight));

                int color = texturePixels[texY * textureRect.width + texX];
                int alpha = (color >> 24) & 0xFF;
                if (alpha <= alphaThreshold)
                    continue;

                int bufferIndex = rowOffset + x;
                if (depth <= depthBuffer[bufferIndex] + depthTestEpsilon)
                    continue;

                depthBuffer[bufferIndex] = depth;
                int finalColor = (shadingFactor >= 0.999f && shadingFactor <= 1.001f)
                    ? color
                    : RenderEngine.applyShading(color, shadingFactor);
                canvasPixels[bufferIndex] = finalColor;
            }
        });
    }

    // ----------------------------------------------------------------
    // Geometry building
    // ----------------------------------------------------------------

    private static @NotNull List<VisibleTriangle> buildTriangles(@NotNull RenderContext context,
                                                                  @NotNull BlockModel model,
                                                                  @NotNull Matrix4f transform,
                                                                  @Nullable Vector3f lightDirection,
                                                                  @Nullable String blockName) {
        List<VisibleTriangle> triangles = new ArrayList<>(model.getElements().size() * 12);

        for (int elementIndex = 0; elementIndex < model.getElements().size(); elementIndex++) {
            Element element = model.getElements().get(elementIndex);
            buildTrianglesForElement(context, model, element, transform, lightDirection, blockName, triangles);
        }

        return triangles;
    }

    private static void buildTrianglesForElement(
        RenderContext context, BlockModel model, Element element,
        Matrix4f transform, @Nullable Vector3f lightDirection, String blockName,
        List<VisibleTriangle> results
    ) {
        Vector3f[] vertices = buildElementVertices(element);
        applyElementRotation(element, vertices);

        for (Map.Entry<Face, FaceData> entry : element.getFaces().entrySet()) {
            Face direction = entry.getKey();
            FaceData face = entry.getValue();

            String textureId = model.resolveTexture(face.getTexture());
            BufferedImage texture;

            int renderPriority = face.getTintIndex() != null ? 1 : 0;

            if (face.getTintIndex() != null) {
                int[] constantTint = ColorUtil.tryGetConstantTint(textureId, blockName);
                if (constantTint != null) {
                    texture = context.getTextureContext().getTintedTexture(textureId,
                        BlockColor.packArgb(constantTint),
                        ColorUtil.CONSTANT_TINT_STRENGTH, 1f);
                } else {
                    BiomeTint biomeKind = ColorUtil.tryGetBiomeTint(textureId, blockName);
                    if (biomeKind != null) {
                        texture = context.getBiomeTintedTexture(textureId, biomeKind);
                    } else {
                        int[] fallbackTint = ColorUtil.getColorFromBlockName(blockName);
                        if (fallbackTint == null)
                            fallbackTint = ColorUtil.getColorFromBlockName(textureId);
                        if (fallbackTint != null) {
                            texture = context.getTextureContext().getTintedTexture(textureId,
                                BlockColor.packArgb(fallbackTint),
                                1f, BlockColor.TINT_BLEND);
                        } else {
                            texture = context.getTextureContext().getTexture(textureId);
                        }
                    }
                }
            } else {
                texture = context.getTextureContext().getTexture(textureId);
            }

            Vector4f faceUv = face.getUv() != null
                ? face.getUv()
                : direction.defaultUv(element.getFrom(), element.getTo());

            Vector2f[] uvMap = faceUv.createUvMap(face.getRotation() != null ? face.getRotation() : 0);
            Rectangle textureRect = computeTextureRectangle(uvMap, texture);

            float rectMinU = (float) textureRect.x / texture.getWidth();
            float rectRangeU = (float) textureRect.width / texture.getWidth();
            float rectMinV = (float) textureRect.y / texture.getHeight();
            float rectRangeV = (float) textureRect.height / texture.getHeight();
            for (int i = 0; i < 4; i++) {
                uvMap[i] = new Vector2f(
                    rectRangeU > 1e-6f ? (uvMap[i].getX() - rectMinU) / rectRangeU : 0f,
                    rectRangeV > 1e-6f ? (uvMap[i].getY() - rectMinV) / rectRangeV : 0f
                );
            }

            int[] indices = direction.getVertexIndices();
            Vector3f[] localFace = new Vector3f[4];
            for (int i = 0; i < 4; i++)
                localFace[i] = vertices[indices[i]];

            Vector3f[] transformed = new Vector3f[4];
            for (int i = 0; i < 4; i++)
                transformed[i] = Vector3f.transform(localFace[i], transform);

            float depth = (transformed[0].getZ() + transformed[1].getZ()
                + transformed[2].getZ() + transformed[3].getZ()) * 0.25f;
            Vector3f triangle1Normal = Vector3f.cross(
                transformed[1].subtract(transformed[0]),
                transformed[2].subtract(transformed[0]));
            Vector3f triangle2Normal = Vector3f.cross(
                transformed[2].subtract(transformed[0]),
                transformed[3].subtract(transformed[0]));
            boolean shadingEnabled = lightDirection != null && element.isShade();
            float triangle1Shading = shadingEnabled
                ? RenderEngine.computeInventoryLighting(triangle1Normal, lightDirection) : 1f;
            float triangle2Shading = shadingEnabled
                ? RenderEngine.computeInventoryLighting(triangle2Normal, lightDirection) : 1f;

            results.add(new VisibleTriangle(
                transformed[0], transformed[1], transformed[2],
                uvMap[0], uvMap[1], uvMap[2],
                textureRect, depth, triangle1Shading,
                texture, triangle1Normal, renderPriority, false));

            results.add(new VisibleTriangle(
                transformed[0], transformed[2], transformed[3],
                uvMap[0], uvMap[2], uvMap[3],
                textureRect, depth, triangle2Shading,
                texture, triangle2Normal, renderPriority, false));
        }
    }

    static Vector3f[] buildElementVertices(Element element) {
        Vector3f min = element.getFrom();
        Vector3f max = element.getTo();

        float fx = normalizeComponent(min.getX());
        float fy = normalizeComponent(min.getY());
        float fz = normalizeComponent(min.getZ());
        float tx = normalizeComponent(max.getX());
        float ty = normalizeComponent(max.getY());
        float tz = normalizeComponent(max.getZ());

        return new Vector3f[]{
            new Vector3f(fx, fy, fz),
            new Vector3f(tx, fy, fz),
            new Vector3f(tx, ty, fz),
            new Vector3f(fx, ty, fz),
            new Vector3f(fx, fy, tz),
            new Vector3f(tx, fy, tz),
            new Vector3f(tx, ty, tz),
            new Vector3f(fx, ty, tz)
        };
    }

    private static float normalizeComponent(float value) {
        return value / 16f - 0.5f;
    }

    static void applyElementRotation(Element element, Vector3f[] vertices) {
        if (element.getRotation() == null)
            return;

        Vector3f axis = switch (element.getRotation().getAxis()) {
            case "x" -> new Vector3f(1, 0, 0);
            case "z" -> new Vector3f(0, 0, 1);
            default -> new Vector3f(0, 1, 0);
        };

        float angle = element.getRotation().getAngle() * DEGREES_TO_RADIANS;
        Vector3f pivot = new Vector3f(
            normalizeComponent(element.getRotation().getOrigin().getX()),
            normalizeComponent(element.getRotation().getOrigin().getY()),
            normalizeComponent(element.getRotation().getOrigin().getZ()));

        Matrix4f rotationMatrix = Matrix4f.createFromAxisAngle(axis, angle);

        for (int i = 0; i < vertices.length; i++) {
            Vector3f relative = vertices[i].subtract(pivot);
            relative = Vector3f.transform(relative, rotationMatrix);
            vertices[i] = relative.add(pivot);
        }
    }

    private static Rectangle computeTextureRectangle(Vector2f[] uvMap, BufferedImage texture) {
        int widthFactor = texture.getWidth();
        int heightFactor = texture.getHeight();

        float minU = Math.min(Math.min(uvMap[0].getX(), uvMap[1].getX()),
            Math.min(uvMap[2].getX(), uvMap[3].getX()));
        float maxU = Math.max(Math.max(uvMap[0].getX(), uvMap[1].getX()),
            Math.max(uvMap[2].getX(), uvMap[3].getX()));
        float minV = Math.min(Math.min(uvMap[0].getY(), uvMap[1].getY()),
            Math.min(uvMap[2].getY(), uvMap[3].getY()));
        float maxV = Math.max(Math.max(uvMap[0].getY(), uvMap[1].getY()),
            Math.max(uvMap[2].getY(), uvMap[3].getY()));

        int minPixelX = Math.round(minU * widthFactor);
        int maxPixelX = Math.round(maxU * widthFactor);
        int minPixelY = Math.round(minV * heightFactor);
        int maxPixelY = Math.round(maxV * heightFactor);

        minPixelX = Math.max(0, Math.min(minPixelX, texture.getWidth() - 1));
        minPixelY = Math.max(0, Math.min(minPixelY, texture.getHeight() - 1));
        maxPixelX = Math.max(minPixelX + 1, Math.min(Math.max(maxPixelX, minPixelX + 1), texture.getWidth()));
        maxPixelY = Math.max(minPixelY + 1, Math.min(Math.max(maxPixelY, minPixelY + 1), texture.getHeight()));

        return new Rectangle(minPixelX, minPixelY, maxPixelX - minPixelX, maxPixelY - minPixelY);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static Transform createDefaultGuiTransform() {
        return Transform.create(
            new float[]{30f, 225f, 0f},
            new float[]{0f, 0f, 0f},
            new float[]{0.625f, 0.625f, 0.625f}
        );
    }

    private static float safeGet(float @Nullable [] array, int index) {
        if (array == null || index >= array.length) return 0f;
        return array[index];
    }
}
