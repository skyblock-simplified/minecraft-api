package dev.sbs.minecraftapi.render;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.RenderContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasGeneratorTests extends IntegrationTestBase {

    @Test
    void generateAtlasesProducesImagesAndManifests() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        Path tempDirectory = Files.createTempDirectory("MinecraftRenderer.AtlasTests");

        try {
            ConcurrentList<String> blockSubset = context.getKnownBlockNames().stream().limit(4).collect(Concurrent.toList());
            ConcurrentList<String> itemSubset = context.getKnownItemNames().stream().limit(4).collect(Concurrent.toList());

            ConcurrentList<AtlasGenerator.AtlasView> views = Concurrent.newList(
                new AtlasGenerator.AtlasView("test", BlockRenderOptions.builder()
                    .withSize(256)
                    .build())
            );

            List<AtlasGenerator.AtlasResult> results = AtlasGenerator.generateBlockItemAtlases(
                context,
                tempDirectory.toString(),
                views,
                96, 2, 2,
                blockSubset, itemSubset,
                true, true);

            assertFalse(results.isEmpty());
            for (AtlasGenerator.AtlasResult result : results) {
                assertTrue(Files.exists(Path.of(result.getImagePath())),
                    "Expected atlas image '%s' to exist.".formatted(result.getImagePath()));
                assertTrue(Files.exists(Path.of(result.getManifestPath())),
                    "Expected manifest '%s' to exist.".formatted(result.getManifestPath()));
            }
        } finally {
            deleteRecursive(tempDirectory);
            context.close();
        }
    }

    @Disabled("Manual integration example - use CreateAtlases console tool instead.")
    @Test
    void generateAtlases() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        String outputPath = Path.of(System.getProperty("user.dir"), "atlases").toString();
        Files.createDirectories(Path.of(outputPath));

        List<AtlasGenerator.AtlasResult> results = AtlasGenerator.generateBlockItemAtlases(
            context, outputPath, AtlasGenerator.DEFAULT_VIEWS);

        for (AtlasGenerator.AtlasResult result : results) {
            System.out.println("Generated " + result.getImagePath());
        }

        context.close();
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
