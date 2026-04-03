package dev.sbs.minecraftapi.asset.texture;

import dev.sbs.minecraftapi.asset.AssetNamespaceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for namespace priority ordering and texture overlay resolution.
 */
class NamespacePriorityTests {

    private Path tempRoot;

    @BeforeEach
    void setUp() throws IOException {
        tempRoot = Files.createTempDirectory("MinecraftApi_NamespacePriorityTests");
    }

    @AfterEach
    void tearDown() {
        deleteRecursive(tempRoot);
    }

    @Test
    void higherPriorityPackWithDifferentFolderShouldWin() throws IOException {
        // Pack A: red, uses "block" folder
        Path packARoot = tempRoot.resolve("packA");
        Files.createDirectories(packARoot);
        Path packABlockDir = packARoot.resolve("assets").resolve("minecraft").resolve("textures").resolve("block");
        Files.createDirectories(packABlockDir);
        writeSolidColorPng(packABlockDir.resolve("stone.png"), 16, 16, new Color(255, 0, 0));

        // Pack B: blue, uses "blocks" folder (legacy)
        Path packBRoot = tempRoot.resolve("packB");
        Files.createDirectories(packBRoot);
        Path packBBlocksDir = packBRoot.resolve("assets").resolve("minecraft").resolve("textures").resolve("blocks");
        Files.createDirectories(packBBlocksDir);
        writeSolidColorPng(packBBlocksDir.resolve("stone.png"), 16, 16, new Color(0, 0, 255));

        String packATextures = packARoot.resolve("assets").resolve("minecraft").resolve("textures").toString();
        String packBTextures = packBRoot.resolve("assets").resolve("minecraft").resolve("textures").toString();

        AssetNamespaceRegistry registry = new AssetNamespaceRegistry();
        registry.addNamespace("minecraft", packATextures, "packA", true);
        registry.addNamespace("minecraft", packBTextures, "packB", false);

        TextureRepository repository = new TextureRepository(packATextures, null, List.of(packBTextures), registry);

        BufferedImage texture = repository.getTexture("minecraft:block/stone");

        // The overlay (Pack B) should win because it has higher priority
        int argb = texture.getRGB(0, 0);
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        assertEquals(0, r, "Expected blue (overlay Pack B should win), but got red channel=" + r);
        assertEquals(0, g, "Expected blue, but got green channel=" + g);
        assertEquals(255, b, "Expected blue (overlay Pack B should win), but got blue channel=" + b);

        repository.close();
    }

    private static void writeSolidColorPng(Path path, int width, int height, Color color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private static void deleteRecursive(Path dir) {
        try {
            if (dir != null && Files.exists(dir)) {
                try (var walk = Files.walk(dir)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
                }
            }
        } catch (IOException ignored) {}
    }
}
