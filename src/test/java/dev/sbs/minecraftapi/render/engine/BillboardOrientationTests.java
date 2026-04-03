package dev.sbs.minecraftapi.render.engine;

import dev.sbs.api.math.Vector2f;
import dev.sbs.api.math.Vector3f;
import dev.sbs.api.math.Vector4f;
import dev.sbs.minecraftapi.asset.MinecraftAssetFactory;
import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.BlockModel.Element;
import dev.sbs.minecraftapi.asset.model.BlockModel.Face;
import dev.sbs.minecraftapi.asset.model.BlockModel.FaceData;
import dev.sbs.minecraftapi.render.BlockRenderer;
import dev.sbs.minecraftapi.render.IntegrationTestBase;
import dev.sbs.minecraftapi.render.context.RenderContext;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class BillboardOrientationTests extends IntegrationTestBase {

    @Test
    void deadBrainCoralFan_rendersWithoutException() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BufferedImage image = new BlockRenderer(context, "dead_brain_coral_fan", null).render();
        assertNotNull(image);
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);
        context.close();
    }

    @Test
    void deadBrainCoralFan_upFaceUvsMatchExpectedOrientation() throws Exception {
        String assetsDir = getAssetsDirectory();
        java.util.Map<String, BlockModel> defs = dev.sbs.minecraftapi.asset.MinecraftAssetFactory.loadModelDefinitions(assetsDir, null, null);
                BlockModel model = MinecraftAssetFactory.resolveAllModels(defs).get(BlockModel.normalizeName("dead_brain_coral_fan"));

        Vector2f[] expectedBaseUp = {
            new Vector2f(0f, 0f),
            new Vector2f(0f, 1f),
            new Vector2f(1f, 1f),
            new Vector2f(1f, 0f)
        };

        for (int elementIndex = 0; elementIndex < model.getElements().size(); elementIndex++) {
            Element element = model.getElements().get(elementIndex);
            FaceData face = element.getFaces().get(Face.UP);
            if (face == null || face.getUv() == null) {
                continue;
            }

            Vector4f faceUv = face.getUv();
            int rotation = face.getRotation() != null ? face.getRotation() : 0;
            Vector2f[] actualUv = faceUv.createUvMap(rotation);

            assertNotNull(actualUv);
            assertEquals(4, actualUv.length);

            Vector2f[] expected = buildExpectedUv(faceUv, rotation, expectedBaseUp);

            for (int i = 0; i < actualUv.length; i++) {
                assertTrue(areClose(expected[i], actualUv[i]),
                    "Element %d, corner %d expected %s but got %s"
                        .formatted(elementIndex, i, expected[i], actualUv[i]));
            }
        }
    }

    @Test
    void deadBrainCoralFan_northSouthFacesPointOutward() throws Exception {
        String assetsDir = getAssetsDirectory();
        java.util.Map<String, BlockModel> defs = dev.sbs.minecraftapi.asset.MinecraftAssetFactory.loadModelDefinitions(assetsDir, null, null);
                BlockModel model = MinecraftAssetFactory.resolveAllModels(defs).get(BlockModel.normalizeName("dead_brain_coral_fan"));

        int[] upIndices = Face.UP.getVertexIndices();

        for (int elementIndex = 0; elementIndex < model.getElements().size(); elementIndex++) {
            Element element = model.getElements().get(elementIndex);
            if (element.getRotation() == null
                || !element.getRotation().getAxis().equalsIgnoreCase("x"))
                continue;

            if (!element.getFaces().containsKey(Face.UP))
                continue;

            Vector3f[] vertices = ModelEngine.buildElementVertices(element);
            ModelEngine.applyElementRotation(element, vertices);

            Vector3f v0 = vertices[upIndices[0]];
            Vector3f v1 = vertices[upIndices[1]];
            Vector3f v2 = vertices[upIndices[2]];

            Vector3f edge1 = v1.subtract(v0);
            Vector3f edge2 = v2.subtract(v0);
            Vector3f normal = Vector3f.cross(edge1, edge2);

            if (normal.lengthSquared() < 1e-8f) {
                continue;
            }

            normal = Vector3f.normalize(normal);
            Vector3f outward = element.getRotation().getAngle() < 0
                ? new Vector3f(0, 0, 1) : new Vector3f(0, 0, -1);

            float alignment = Vector3f.dot(normal, outward);
            if (alignment < 0) {
                normal = new Vector3f(-normal.getX(), -normal.getY(), -normal.getZ());
                alignment = -alignment;
            }

            assertTrue(alignment > 0.2f,
                "Element %d expected to face %s but normal was %s (alignment %.3f)"
                    .formatted(elementIndex, outward, normal, alignment));
        }
    }

    @Test
    void tntSideFacesAreNotMirrored() throws Exception {
        String assetsDir = getAssetsDirectory();
        java.util.Map<String, BlockModel> defs = dev.sbs.minecraftapi.asset.MinecraftAssetFactory.loadModelDefinitions(assetsDir, null, null);
                BlockModel model = MinecraftAssetFactory.resolveAllModels(defs).get(BlockModel.normalizeName("tnt"));

        Vector2f[] expectedBase = {
            new Vector2f(0f, 0f),
            new Vector2f(0f, 1f),
            new Vector2f(1f, 1f),
            new Vector2f(1f, 0f)
        };

        Face[] targets = { Face.EAST, Face.WEST };

        for (int elementIndex = 0; elementIndex < model.getElements().size(); elementIndex++) {
            Element element = model.getElements().get(elementIndex);
            for (Face direction : targets) {
                FaceData face = element.getFaces().get(direction);
                if (face == null)
                    continue;

                Vector4f faceUv = face.getUv() != null
                    ? face.getUv()
                    : direction.defaultUv(element.getFrom(), element.getTo());
                assertNotNull(faceUv, "GetFaceUv returned null for element %d, direction %s."
                    .formatted(elementIndex, direction));

                int rotation = face.getRotation() != null ? face.getRotation() : 0;
                Vector2f[] actualUv = faceUv.createUvMap(rotation);

                assertNotNull(actualUv);
                assertEquals(4, actualUv.length);

                Vector2f[] expected = buildExpectedUv(faceUv, rotation, expectedBase);

                for (int i = 0; i < actualUv.length; i++) {
                    assertTrue(areClose(expected[i], actualUv[i]),
                        "Element %d %s, corner %d expected %s but got %s"
                            .formatted(elementIndex, direction, i, expected[i], actualUv[i]));
                }
            }
        }
    }

    // ---- Helpers ----

    private static Vector2f[] buildExpectedUv(Vector4f faceUv, int rotationDegrees, Vector2f[] baseCoords) {
        float width = faceUv.getZ() - faceUv.getX();
        float height = faceUv.getW() - faceUv.getY();
        Vector2f[] absolute = new Vector2f[baseCoords.length];
        for (int i = 0; i < baseCoords.length; i++) {
            absolute[i] = new Vector2f(
                faceUv.getX() + baseCoords[i].getX() * width,
                faceUv.getY() + baseCoords[i].getY() * height);
        }

        applyFaceRotation(absolute, faceUv, rotationDegrees);
        return normalizeFaceCoordinates(absolute, faceUv);
    }

    private static void applyFaceRotation(Vector2f[] uv, Vector4f faceUv, int rotationDegrees) {
        int normalized = ((rotationDegrees % 360) + 360) % 360;
        if (normalized == 0) {
            return;
        }

        int steps = normalized / 90;
        float centerX = (faceUv.getX() + faceUv.getZ()) * 0.5f;
        float centerY = (faceUv.getY() + faceUv.getW()) * 0.5f;

        for (int s = 0; s < steps; s++) {
            for (int i = 0; i < uv.length; i++) {
                float relX = uv[i].getX() - centerX;
                float relY = uv[i].getY() - centerY;
                uv[i] = new Vector2f(relY + centerX, -relX + centerY);
            }
        }
    }

    private static Vector2f[] normalizeFaceCoordinates(Vector2f[] absoluteUv, Vector4f faceUv) {
        float uMin = Math.min(faceUv.getX(), faceUv.getZ());
        float uMax = Math.max(faceUv.getX(), faceUv.getZ());
        float vMin = Math.min(faceUv.getY(), faceUv.getW());
        float vMax = Math.max(faceUv.getY(), faceUv.getW());

        float w = uMax - uMin;
        float h = vMax - vMin;

        float invWidth = Math.abs(w) < 1e-5f ? 0f : 1f / w;
        float invHeight = Math.abs(h) < 1e-5f ? 0f : 1f / h;

        Vector2f[] result = new Vector2f[absoluteUv.length];
        for (int i = 0; i < absoluteUv.length; i++) {
            float u = (absoluteUv[i].getX() - uMin) * invWidth;
            float v = (absoluteUv[i].getY() - vMin) * invHeight;
            result[i] = new Vector2f(clamp01(u), clamp01(v));
        }

        return result;
    }

    private static float clamp01(float value) {
        return value <= 0f ? 0f : value >= 1f ? 1f : value;
    }

    private static boolean areClose(Vector2f expected, Vector2f actual) {
        float tolerance = 1e-4f;
        return Math.abs(expected.getX() - actual.getX()) <= tolerance
            && Math.abs(expected.getY() - actual.getY()) <= tolerance;
    }
}
