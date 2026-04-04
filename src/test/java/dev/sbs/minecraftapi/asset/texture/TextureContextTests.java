package dev.sbs.minecraftapi.asset.texture;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.minecraftapi.asset.MinecraftAssetFactory;
import dev.sbs.minecraftapi.asset.namespace.AssetNamespaceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for the texture loading, caching, and armor trim palette subsystem.
 * <p>
 * Requires the Minecraft assets directory to be available. If not found locally,
 * the tests attempt to download assets via the Piston Meta API.
 */
class TextureContextTests {

    private static final String ASSETS_DIR;

    static {
        String found = findMinecraftDirectory();
        if (found == null) {
            try {
                Path projectRoot = Path.of(System.getProperty("user.dir"));
                System.out.println("Minecraft assets not found - downloading latest release via Piston API...");
                Path minecraftDir = MinecraftAssetFactory.downloadAndExtractAssets(null, projectRoot);
                found = minecraftDir.toAbsolutePath().toString();
                System.out.println("Minecraft assets extracted to: " + found);
            } catch (Exception e) {
                System.err.println("Failed to download Minecraft assets: " + e.getMessage());
            }
        }
        ASSETS_DIR = found;
    }

    private TextureContext context;

    @BeforeEach
    void setUp() {
        assumeTrue(ASSETS_DIR != null && Files.isDirectory(Path.of(ASSETS_DIR)),
            "Minecraft assets directory not found - skipping integration test");
        AssetNamespaceRegistry namespaces = AssetNamespaceRegistry.buildFromRoots(ASSETS_DIR, Concurrent.newList());
        context = new TextureContext(ASSETS_DIR, List.of(), namespaces);
    }

    @AfterEach
    void tearDown() {
        if (context != null)
            context.close();
    }

    @Test
    void animatedTextureUsesFirstFrameDimensions() {
        BufferedImage texture = context.getTexture("minecraft:block/campfire_fire");
        assertEquals(16, texture.getWidth());
        assertEquals(16, texture.getHeight());
    }

    @Test
    void armorTrimBootsPaletteIsApplied() {
        BufferedImage baseOverlay = context.getTexture("minecraft:trims/items/boots_trim");
        BufferedImage tintedOverlay = context.getTexture("minecraft:trims/items/boots_trim_amethyst");
        BufferedImage genericPalette = context.getTexture("minecraft:trims/color_palettes/trim_palette");
        BufferedImage amethystPalette = context.getTexture("minecraft:trims/color_palettes/amethyst");

        assertNotSame(context.getTexture("minecraft:missingno"), tintedOverlay);

        int mappedPixels = 0;
        for (int y = 0; y < baseOverlay.getHeight(); y++) {
            for (int x = 0; x < baseOverlay.getWidth(); x++) {
                int sourceArgb = baseOverlay.getRGB(x, y);
                int sourceAlpha = (sourceArgb >> 24) & 0xFF;
                if (sourceAlpha == 0) continue;

                int paletteIndex = findPaletteIndex(genericPalette, sourceArgb);
                if (paletteIndex < 0) continue;

                int expectedArgb = amethystPalette.getRGB(
                    Math.min(paletteIndex, amethystPalette.getWidth() - 1), 0);
                int actualArgb = tintedOverlay.getRGB(x, y);

                assertEquals(sourceAlpha, (actualArgb >> 24) & 0xFF, "Alpha mismatch at (" + x + "," + y + ")");
                assertEquals((expectedArgb >> 16) & 0xFF, (actualArgb >> 16) & 0xFF, "Red mismatch at (" + x + "," + y + ")");
                assertEquals((expectedArgb >> 8) & 0xFF, (actualArgb >> 8) & 0xFF, "Green mismatch at (" + x + "," + y + ")");
                assertEquals(expectedArgb & 0xFF, actualArgb & 0xFF, "Blue mismatch at (" + x + "," + y + ")");
                mappedPixels++;
            }
        }

        assertTrue(mappedPixels > 0, "Expected at least one palette-mapped trim pixel.");
    }

    @Test
    void armorTrimLeggingsUseBasePaletteWhenMaterialIsNotDarker() {
        BufferedImage baseOverlay = context.getTexture("minecraft:trims/items/leggings_trim");
        BufferedImage tintedOverlay = context.getTexture("minecraft:trims/items/leggings_trim_netherite");
        BufferedImage genericPalette = context.getTexture("minecraft:trims/color_palettes/trim_palette");
        BufferedImage netheritePalette = context.getTexture("minecraft:trims/color_palettes/netherite");
        BufferedImage netheriteDarkerPalette = context.getTexture("minecraft:trims/color_palettes/netherite_darker");

        assertNotSame(context.getTexture("minecraft:missingno"), tintedOverlay);

        int mappedPixels = 0;
        boolean observedMatchBase = false;
        boolean observedNotUsingDarker = false;

        for (int y = 0; y < baseOverlay.getHeight(); y++) {
            for (int x = 0; x < baseOverlay.getWidth(); x++) {
                int sourceArgb = baseOverlay.getRGB(x, y);
                int sourceAlpha = (sourceArgb >> 24) & 0xFF;
                if (sourceAlpha == 0) continue;

                int paletteIndex = findPaletteIndex(genericPalette, sourceArgb);
                if (paletteIndex < 0) continue;

                int clampedIndex = Math.min(paletteIndex, netheritePalette.getWidth() - 1);
                int expectedBase = netheritePalette.getRGB(clampedIndex, 0);
                int actual = tintedOverlay.getRGB(x, y);

                assertEquals(sourceAlpha, (actual >> 24) & 0xFF);
                assertEquals((expectedBase >> 16) & 0xFF, (actual >> 16) & 0xFF);
                assertEquals((expectedBase >> 8) & 0xFF, (actual >> 8) & 0xFF);
                assertEquals(expectedBase & 0xFF, actual & 0xFF);
                mappedPixels++;

                observedMatchBase = true;
                int darkerClampedIndex = Math.min(paletteIndex, netheriteDarkerPalette.getWidth() - 1);
                int expectedDarker = netheriteDarkerPalette.getRGB(darkerClampedIndex, 0);
                if (expectedBase != expectedDarker) {
                    if ((actual >> 16 & 0xFF) != (expectedDarker >> 16 & 0xFF)
                        || (actual >> 8 & 0xFF) != (expectedDarker >> 8 & 0xFF)
                        || (actual & 0xFF) != (expectedDarker & 0xFF))
                        observedNotUsingDarker = true;
                }
            }
        }

        assertTrue(mappedPixels > 0, "Expected leggings trim pixels to map through the palette.");
        assertTrue(observedMatchBase, "Expected leggings trim to match the base material palette.");
        assertTrue(observedNotUsingDarker, "Expected leggings trim to avoid using the darker palette.");
    }

    @Test
    void armorTrimUsesExplicitDarkerPaletteWhenMaterialIncludesSuffix() {
        BufferedImage baseOverlay = context.getTexture("minecraft:trims/items/boots_trim");
        BufferedImage tintedOverlay = context.getTexture("minecraft:trims/items/boots_trim_netherite_darker");
        BufferedImage genericPalette = context.getTexture("minecraft:trims/color_palettes/trim_palette");
        BufferedImage netheriteDarkerPalette = context.getTexture("minecraft:trims/color_palettes/netherite_darker");

        assertNotSame(context.getTexture("minecraft:missingno"), tintedOverlay);

        int mappedPixels = 0;
        for (int y = 0; y < baseOverlay.getHeight(); y++) {
            for (int x = 0; x < baseOverlay.getWidth(); x++) {
                int sourceArgb = baseOverlay.getRGB(x, y);
                int sourceAlpha = (sourceArgb >> 24) & 0xFF;
                if (sourceAlpha == 0) continue;

                int paletteIndex = findPaletteIndex(genericPalette, sourceArgb);
                if (paletteIndex < 0) continue;

                int clampedIndex = Math.min(paletteIndex, netheriteDarkerPalette.getWidth() - 1);
                int expected = netheriteDarkerPalette.getRGB(clampedIndex, 0);
                int actual = tintedOverlay.getRGB(x, y);

                assertEquals(sourceAlpha, (actual >> 24) & 0xFF);
                assertEquals((expected >> 16) & 0xFF, (actual >> 16) & 0xFF);
                assertEquals((expected >> 8) & 0xFF, (actual >> 8) & 0xFF);
                assertEquals(expected & 0xFF, actual & 0xFF);
                mappedPixels++;
            }
        }

        assertTrue(mappedPixels > 0, "Expected explicit darker material trim pixels to map through the darker palette.");
    }

    private static int findPaletteIndex(BufferedImage palette, int targetArgb) {
        for (int i = 0; i < palette.getWidth(); i++) {
            if (palette.getRGB(i, 0) == targetArgb)
                return i;
        }
        return -1;
    }

    private static String findMinecraftDirectory() {
        java.io.File current = new java.io.File(System.getProperty("user.dir"));
        while (current != null) {
            java.io.File candidate = new java.io.File(current, "minecraft");
            if (candidate.isDirectory())
                return candidate.getAbsolutePath();
            current = current.getParentFile();
        }
        return null;
    }
}
