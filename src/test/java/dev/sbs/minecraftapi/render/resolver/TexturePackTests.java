package dev.sbs.minecraftapi.render.resolver;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.minecraftapi.asset.texture.OverlayRoot;
import dev.sbs.minecraftapi.asset.texture.TexturePackStack;
import dev.sbs.minecraftapi.render.BlockRenderer;
import dev.sbs.minecraftapi.render.IntegrationTestBase;
import dev.sbs.minecraftapi.render.ItemRenderer;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.ItemRenderData;
import dev.sbs.minecraftapi.render.context.RenderContext;
import lombok.Cleanup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TexturePackTests extends IntegrationTestBase {

    private Path tempRoot;

    @BeforeEach
    void setUp() throws IOException {
        tempRoot = Files.createTempDirectory("MinecraftRenderer_TexturePackTests");
    }

    @AfterEach
    void tearDown() {
        deleteRecursive(tempRoot);
    }

    @Test
    void computeResourceIdIncludesPackOverrides() throws Exception {
        String assetsDir = getAssetsDirectory();
        Path packRoot = createTestPack("testpack", new Color(220, 20, 60));
        registerPacks(List.of(packRoot.toString()));

        RenderContext context = createRenderContext(assetsDir, null);

        BufferedImage vanillaStone = new BlockRenderer(context, "stone", BlockRenderOptions.DEFAULT).render();

        BlockRenderOptions packOptions = BlockRenderOptions.builder()
            .withSize(128)
            .withPackIds(Concurrent.newUnmodifiableList("testpack"))
            .build();

        BufferedImage packStone = new BlockRenderer(context, "stone", packOptions).render();

        assertFalseImageEqual(vanillaStone, packStone);

        ResourceIdResult vanillaId = context.computeResourceId("stone", null);
        ResourceIdResult packId = context.computeResourceId("stone", packOptions);

        assertNotEquals(vanillaId.getResourceId(), packId.getResourceId());
        assertEquals("vanilla", vanillaId.getSourcePackId());
        assertEquals("testpack", packId.getSourcePackId());

        context.close();
    }

    @Test
    void registerAllPacksRegistersPacksUnderRoot() throws Exception {
        Path packsRoot = tempRoot.resolve("packs-root");
        Files.createDirectories(packsRoot);
        Path nestedRoot = packsRoot.resolve("nested");
        Files.createDirectories(nestedRoot);

        createTestPack("top-pack", new Color(10, 200, 240), packsRoot);
        createTestPack("nested-pack", new Color(140, 50, 200), nestedRoot);
        Files.createDirectories(packsRoot.resolve("ignore-me"));

        var topLevel = dev.sbs.minecraftapi.asset.ResourcePackDiscovery.discoverAllPacks(packsRoot.toString(), false);
        assertEquals(1, topLevel.size());
        assertTrue(topLevel.stream().anyMatch(p -> p.getId().equalsIgnoreCase("top-pack")));

        var recursive = dev.sbs.minecraftapi.asset.ResourcePackDiscovery.discoverAllPacks(packsRoot.toString(), true);
        assertEquals(2, recursive.size());
        assertTrue(recursive.stream().anyMatch(p -> p.getId().equalsIgnoreCase("top-pack")));
        assertTrue(recursive.stream().anyMatch(p -> p.getId().equalsIgnoreCase("nested-pack")));
    }

    @Test
    void packOverlayRootsRespectCitNamespacePriority() throws Exception {
        Path packRoot = createTestPackWithNamespaces("prioritypack", new Color(128, 128, 128), Map.of(
            "minecraft", new Color(32, 32, 32),
            "firmskyblock", new Color(64, 224, 135),
            "cittofirmgenerated", new Color(220, 220, 20),
            "cit", new Color(220, 20, 60)
        ));

        registerPacks(Concurrent.newList(packRoot.toString()));

        TexturePackStack stack = TexturePackStack.buildPackStack(Concurrent.newList("prioritypack"));
        List<String> overlayPaths = stack.getOverlayRoots().stream()
            .filter(overlay -> overlay.sourceId().equalsIgnoreCase("prioritypack"))
            .map(OverlayRoot::path)
            .toList();

        String minecraftPath = packRoot.resolve("assets").resolve("minecraft").toAbsolutePath().normalize().toString();
        String firmskyblockPath = packRoot.resolve("assets").resolve("firmskyblock").toAbsolutePath().normalize().toString();
        String cittoFirmGeneratedPath = packRoot.resolve("assets").resolve("cittofirmgenerated").toAbsolutePath().normalize().toString();
        String citPath = packRoot.resolve("assets").resolve("cit").toAbsolutePath().normalize().toString();

        int minecraftIndex = findPathIndex(overlayPaths, minecraftPath);
        int firmskyblockIndex = findPathIndex(overlayPaths, firmskyblockPath);
        int cittoFirmGeneratedIndex = findPathIndex(overlayPaths, cittoFirmGeneratedPath);
        int citIndex = findPathIndex(overlayPaths, citPath);

        assertTrue(minecraftIndex >= 0, "Expected minecraft namespace to be present in overlay roots.");
        assertTrue(firmskyblockIndex >= 0, "Expected firmskyblock namespace to be present in overlay roots.");
        assertTrue(cittoFirmGeneratedIndex >= 0, "Expected cittofirmgenerated namespace to be present in overlay roots.");
        assertTrue(citIndex >= 0, "Expected cit namespace to be present in overlay roots.");

        assertTrue(minecraftIndex < firmskyblockIndex,
            "Expected minecraft namespace to have lower priority than firmskyblock (indexes %d vs %d)."
                .formatted(minecraftIndex, firmskyblockIndex));
        assertTrue(firmskyblockIndex < cittoFirmGeneratedIndex,
            "Expected firmskyblock namespace to have lower priority than cittofirmgenerated (indexes %d vs %d)."
                .formatted(firmskyblockIndex, cittoFirmGeneratedIndex));
        assertTrue(cittoFirmGeneratedIndex < citIndex,
            "Expected cittofirmgenerated namespace to have lower priority than cit (indexes %d vs %d)."
                .formatted(cittoFirmGeneratedIndex, citIndex));
    }

    @Test
    void preloadRegisteredPacksCachesPackContextManagers() throws Exception {
        String assetsDir = getAssetsDirectory();
        Path packRoot = createTestPack("warmup-pack", new Color(200, 120, 40));
        registerPacks(List.of(packRoot.toString()));

        RenderContext context = createRenderContext(assetsDir, null);
        context.preloadRegisteredPacks();

        BlockRenderOptions packOptions = BlockRenderOptions.builder()
            .withPackIds(Concurrent.newUnmodifiableList("warmup-pack"))
            .build();
        ResourceIdResult result = context.computeResourceId("stone", packOptions);
        assertNotNull(result);
        context.close();
    }

    @Test
    void computeResourceIdRemainsStableAcrossRepeatedCalls() throws Exception {
        String assetsDir = getAssetsDirectory();
        Path packRoot = createTestPack("stable-pack", new Color(180, 80, 200));
        registerPacks(List.of(packRoot.toString()));

        RenderContext context = createRenderContext(assetsDir, null);

        ResourceIdResult vanillaFirst = context.computeResourceId("stone", null);
        ResourceIdResult vanillaSecond = context.computeResourceId("stone", null);
        assertEquals(vanillaFirst.getResourceId(), vanillaSecond.getResourceId());
        assertEquals(vanillaFirst.getPackStackHash(), vanillaSecond.getPackStackHash());
        assertEquals(vanillaFirst.getSourcePackId(), vanillaSecond.getSourcePackId());

        BlockRenderOptions packOptions = BlockRenderOptions.builder()
            .withPackIds(Concurrent.newUnmodifiableList("stable-pack"))
            .build();

        ResourceIdResult packFirst = context.computeResourceId("stone", packOptions);
        ResourceIdResult packSecond = context.computeResourceId("stone", packOptions);
        assertEquals(packFirst.getResourceId(), packSecond.getResourceId());
        assertEquals(packFirst.getPackStackHash(), packSecond.getPackStackHash());
        assertEquals(packFirst.getSourcePackId(), packSecond.getSourcePackId());

        ResourceIdResult itemFirst = context.computeResourceId("minecraft:diamond_sword", null);
        ResourceIdResult itemSecond = context.computeResourceId("minecraft:diamond_sword", null);
        assertEquals(itemFirst.getResourceId(), itemSecond.getResourceId());

        assertNotEquals(vanillaFirst.getResourceId(), packFirst.getResourceId());
        assertNotEquals(vanillaFirst.getPackStackHash(), packFirst.getPackStackHash());
        assertNotEquals(vanillaFirst.getSourcePackId(), packFirst.getSourcePackId());

        context.close();
    }

    @Test
    void renderGuiItemWithResourceIdMatchesSeparateOperations() throws Exception {
        String assetsDir = getAssetsDirectory();
        Path packRoot = createTestPack("combined-id-pack", new Color(90, 140, 200));
        registerPacks(List.of(packRoot.toString()));

        RenderContext context = createRenderContext(assetsDir, null);

        BlockRenderOptions baselineOptions = BlockRenderOptions.builder()
            .withSize(128)
            .build();

        BlockRenderOptions packOptions = baselineOptions.mutate()
            .withPackIds(Concurrent.newUnmodifiableList("combined-id-pack"))
            .build();

        ItemRenderData tintedItemData = new ItemRenderData(
            new int[]{80, 25, 180}, null, false, null, null);
        BlockRenderOptions tintedOptions = baselineOptions.mutate().withItemData(tintedItemData).build();

        record TestCase(String target, BlockRenderOptions options) {}
        List<TestCase> testCases = List.of(
            new TestCase("minecraft:diamond_sword", baselineOptions),
            new TestCase("minecraft:diamond_sword", packOptions),
            new TestCase("minecraft:leather_boots", tintedOptions)
        );

        for (TestCase tc : testCases) {
            @Cleanup RenderedResource combined = new ItemRenderer(context, tc.target, tc.options).renderWithResourceId();
            BufferedImage separateImage = new ItemRenderer(context, tc.target, tc.options).render();
            ResourceIdResult resourceId = context.computeResourceId(tc.target, tc.options);

            assertImagesEqual(separateImage, combined.getImageData().toBufferedImage());
            assertEquals(resourceId.getResourceId(), combined.getResourceId().getResourceId());
            assertEquals(resourceId.getPackStackHash(), combined.getResourceId().getPackStackHash());
            assertEquals(resourceId.getSourcePackId(), combined.getResourceId().getSourcePackId());
            assertEquals(resourceId.getModel(), combined.getResourceId().getModel());
            assertEquals(resourceId.getTextures(), combined.getResourceId().getTextures());
        }

        context.close();
    }

    @Test
    void getTexturePackIconReturnsIconWhenAvailable() throws Exception {
        String assetsDir = getAssetsDirectory();
        Path iconPackRoot = createTestPack("icon-pack", new Color(64, 128, 200), true);
        Path missingIconPackRoot = createTestPack("no-icon-pack", new Color(180, 60, 120), false);

        registerPacks(List.of(iconPackRoot.toString(), missingIconPackRoot.toString()));

        RenderContext context = createRenderContext(assetsDir, null);

        BufferedImage icon = context.getTexturePackIcon("icon-pack");
        assertNotNull(icon);
        assertTrue(icon.getWidth() > 0);
        assertTrue(icon.getHeight() > 0);

        BufferedImage missingIcon = context.getTexturePackIcon("no-icon-pack");
        assertNull(missingIcon);

        BufferedImage unknownIcon = context.getTexturePackIcon("unknown-pack");
        assertNull(unknownIcon);

        context.close();
    }

    // ---- Helpers ----

    private Path createTestPack(String id, Color color) throws IOException {
        return createTestPack(id, color, true);
    }

    private Path createTestPack(String id, Color color, boolean includePackPng) throws IOException {
        return createTestPack(id, color, tempRoot, includePackPng);
    }

    private Path createTestPack(String id, Color color, Path rootOverride) throws IOException {
        return createTestPack(id, color, rootOverride, true);
    }

    private Path createTestPack(String id, Color color, Path rootOverride, boolean includePackPng) throws IOException {
        Files.createDirectories(rootOverride);
        Path packRoot = rootOverride.resolve(id);
        Files.createDirectories(packRoot);

        Files.writeString(packRoot.resolve("meta.json"),
            "{\n  \"id\": \"%s\",\n  \"name\": \"%s\",\n  \"version\": \"1.0.0\",\n  \"description\": \"Test pack\",\n  \"authors\": [\"tests\"]\n}\n"
                .formatted(id, id));
        Files.writeString(packRoot.resolve("pack.mcmeta"),
            "{\"pack\": {\"pack_format\": 32, \"description\": \"Test\"}}\n");

        Path texturesDir = packRoot.resolve("assets").resolve("minecraft").resolve("textures").resolve("block");
        Files.createDirectories(texturesDir);
        writeSolidColorPng(texturesDir.resolve("stone.png"), 16, 16, color);

        if (includePackPng) {
            writeSolidColorPng(packRoot.resolve("pack.png"), 32, 32, color);
        }

        return packRoot;
    }

    private Path createTestPackWithNamespaces(String id, Color defaultColor, Map<String, Color> namespaceColors)
        throws IOException {

        Path packRoot = tempRoot.resolve(id);
        Files.createDirectories(packRoot);

        Files.writeString(packRoot.resolve("meta.json"),
            "{\n  \"id\": \"%s\",\n  \"name\": \"%s\",\n  \"version\": \"1.0.0\",\n  \"description\": \"Test pack\",\n  \"authors\": [\"tests\"]\n}\n"
                .formatted(id, id));
        Files.writeString(packRoot.resolve("pack.mcmeta"),
            "{\"pack\": {\"pack_format\": 32, \"description\": \"Test\"}}\n");

        Map<String, Color> entries = new LinkedHashMap<>(namespaceColors);
        if (!entries.containsKey("minecraft")) {
            entries.put("minecraft", defaultColor);
        }

        for (Map.Entry<String, Color> entry : entries.entrySet()) {
            Path texturesDir = packRoot.resolve("assets").resolve(entry.getKey()).resolve("textures").resolve("block");
            Files.createDirectories(texturesDir);
            writeSolidColorPng(texturesDir.resolve("stone.png"), 16, 16, entry.getValue());
        }

        writeSolidColorPng(packRoot.resolve("pack.png"), 32, 32, defaultColor);
        return packRoot;
    }

    private static void writeSolidColorPng(Path path, int width, int height, Color color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private static void assertFalseImageEqual(BufferedImage baseline, BufferedImage candidate) {
        int width = Math.min(baseline.getWidth(), candidate.getWidth());
        int height = Math.min(baseline.getHeight(), candidate.getHeight());
        for (int y = 0; y < height; y += 8) {
            for (int x = 0; x < width; x += 8) {
                if (baseline.getRGB(x, y) != candidate.getRGB(x, y)) {
                    return;
                }
            }
        }
        fail("Expected images rendered with texture pack to differ from vanilla render.");
    }

    private static void assertImagesEqual(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y),
                    "Pixel mismatch at (%d, %d)".formatted(x, y));
            }
        }
    }

    private static int findPathIndex(List<String> paths, String target) {
        for (int i = 0; i < paths.size(); i++) {
            if (paths.get(i).equalsIgnoreCase(target)) {
                return i;
            }
        }
        return -1;
    }

    private static void deleteRecursive(Path dir) {
        try {
            if (dir != null && Files.exists(dir)) {
                try (var walk = Files.walk(dir)) {
                    walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
                }
            }
        } catch (IOException ignored) {}
    }
}
