package dev.sbs.minecraftapi.render.resolver;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.math.Vector2f;
import dev.sbs.api.math.Vector3f;
import dev.sbs.api.math.Vector4f;
import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.BlockModel.Element;
import dev.sbs.minecraftapi.asset.model.BlockModel.Face;
import dev.sbs.minecraftapi.asset.model.BlockModel.FaceData;
import dev.sbs.minecraftapi.asset.model.ItemInfo;
import dev.sbs.minecraftapi.asset.model.ItemInfo.TintInfo;
import dev.sbs.minecraftapi.asset.texture.TextureReference;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.collection.ListTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.ByteTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.IntTag;
import dev.sbs.minecraftapi.render.BlockFaceRenderer;
import dev.sbs.minecraftapi.render.BlockRenderer;
import dev.sbs.minecraftapi.render.GuiItemRenderer;
import dev.sbs.minecraftapi.render.IntegrationTestBase;
import dev.sbs.minecraftapi.render.ItemRenderer;
import dev.sbs.minecraftapi.render.ModelRenderer;
import dev.sbs.minecraftapi.render.context.BlockFaceRenderOptions;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.ItemRenderData;
import dev.sbs.minecraftapi.render.context.RenderContext;
import dev.sbs.minecraftapi.render.data.BiomeTint;
import dev.sbs.minecraftapi.render.data.ColorUtil;
import dev.sbs.minecraftapi.render.hypixel.HypixelItemData;
import dev.sbs.minecraftapi.render.hypixel.HypixelPrefixes;
import dev.sbs.minecraftapi.render.hypixel.TextureResolver;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class BlockRendererTests extends IntegrationTestBase {

    // ----------------------------------------------------------------
    // Existing tests
    // ----------------------------------------------------------------

    @Test
    void renderStoneProducesOpaquePixels() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BufferedImage image = new BlockRenderer(context, "stone", null).render();

        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());

        boolean hasOpaquePixel = false;
        outer:
        for (int y = 0; y < image.getHeight(); y += 8) {
            for (int x = 0; x < image.getWidth(); x += 8) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 10) {
                    hasOpaquePixel = true;
                    break outer;
                }
            }
        }

        assertTrue(hasOpaquePixel, "Rendered stone block should contain opaque pixels.");
        context.close();
    }

    @Test
    void blockRenderOptionsRetainsItemDataWhenCloned() {
        ItemRenderData customTint =
            new ItemRenderData(new int[]{1, 2, 3}, null, false, null, null);
        BlockRenderOptions options = BlockRenderOptions.builder()
            .withItemData(customTint)
            .build();

        assertNotNull(options.getItemData());
        assertSame(customTint, options.getItemData());
        assertArrayEquals(new int[]{1, 2, 3}, options.getItemData().getLayer0Tint());
    }

    @Test
    void renderGuiItemWithProfileRendersSuccessfully() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        String textureValue = "ewogICJ0aW1lc3RhbXAiIDogMTYzMzQ2NzI4MiwKICAicHJvZmlsZUlkIiA6ICI0MTNkMTdkMzMyODQ0OTYwYTExNWU2ZjYzNmE0ZDcyYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaW5lY3JhZnRTa2luIiwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzE0ZjZhYjdkMWQyOGJkZTY1OTZiZjdkNGU5ZjlmMGI0ZjFlNWY5MTdkNTI1MjQ0ODJlZWM4ODFlYWM4YTZjNTEiCiAgICB9CiAgfQp9";

        CompoundTag root = new CompoundTag();
        root.put("id", "minecraft:player_head");
        root.put("count", new IntTag(1));

        CompoundTag propertyCompound = new CompoundTag();
        propertyCompound.put("name", "textures");
        propertyCompound.put("value", textureValue);

        CompoundTag profileCompound = new CompoundTag();
        profileCompound.put("properties", new ListTag<>(List.of(propertyCompound)));

        CompoundTag components = new CompoundTag();
        components.put("minecraft:profile", profileCompound);

        root.put("components", components);

        ItemRenderData itemData =
            ItemRenderData.fromComponents(root);
        assertNotNull(itemData);
        assertNotNull(itemData.getProfile());

        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(64)
            .withItemData(itemData)
            .build();

        BufferedImage image = new ItemRenderer(context, "minecraft:player_head", itemData, options).render();
        assertNotNull(image);
        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());
        context.close();
    }

    // ----------------------------------------------------------------
    // Migrated tests
    // ----------------------------------------------------------------

    @Test
    void leatherHelmetItemInfoContainsTintMetadata() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        ItemInfo info = context.getItemInfo("leather_helmet");
        assertNotNull(info);
        assertFalse(info.getLayerTints().isEmpty());
        assertTrue(info.getLayerTints().containsKey(0));
        assertEquals(TintInfo.Kind.DYE, info.getLayerTints().get(0).getKind());

        context.close();
    }

    @Test
    void renderFlatItemWithCustomTintProducesDifferentResult() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        BlockRenderOptions baselineOptions = optionsWithSize(64);
        BufferedImage apiBaseline = new ItemRenderer(context, "leather_helmet", baselineOptions).render();

        ItemRenderData customTint =
            new ItemRenderData(new int[]{10, 200, 240}, null, false, null, null);
        BufferedImage apiCustom = new ItemRenderer(context, "leather_helmet", customTint, baselineOptions).render();

        assertFalse(imagesAreIdentical(apiBaseline, apiCustom));
        context.close();
    }

    @Test
    void itemRegistryIncludesBlockInventoryItems() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        List<String> knownItems = context.getKnownItemNames();
        assertTrue(knownItems.contains("oak_fence"));
        assertTrue(knownItems.contains("white_shulker_box"));

        BufferedImage fence = new ItemRenderer(context, "oak_fence", null).render();
        assertTrue(hasOpaquePixels(fence), "Oak fence item render should contain opaque pixels.");

        BufferedImage shulker = new ItemRenderer(context, "white_shulker_box", null).render();
        assertTrue(hasOpaquePixels(shulker), "White shulker box item render should contain opaque pixels.");

        context.close();
    }

    @Test
    void renderBedItemUsesBlockModelFallback() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        BufferedImage bed = new ItemRenderer(context, "white_bed", null).render();
        assertTrue(hasOpaquePixels(bed), "White bed item render should contain opaque pixels.");

        int[] bounds = getOpaqueHorizontalBounds(bed);
        int minX = bounds[0];
        int maxX = bounds[1];
        assertTrue(minX >= 0 && maxX >= minX,
            "White bed render should contain opaque horizontal coverage.");
        int horizontalSpan = maxX - minX;
        assertTrue(horizontalSpan > bed.getWidth() / 2,
            "White bed render should span more than half the image width, but spanned "
                + horizontalSpan + " pixels out of " + bed.getWidth() + ".");

        context.close();
    }

    @Test
    void renderSkyblockTextureIdFallsBackWhenNoTexturePack() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        CompoundTag extraAttributes = new CompoundTag();
        extraAttributes.put("id", "FUNGI_CUTTER");

        CompoundTag tag = new CompoundTag();
        tag.put("ExtraAttributes", extraAttributes);

        HypixelItemData hypixelItem = new HypixelItemData(
            HypixelPrefixes.NUMERIC + "294", 1, (short) 0, tag, null);
        String textureId = TextureResolver.getTextureId(hypixelItem);

        BufferedImage expected = new ItemRenderer(context, "minecraft:golden_hoe", null).render();
        BufferedImage actual = new GuiItemRenderer(context, textureId, null).render();

        assertTrue(hasOpaquePixels(actual), "Skyblock fallback render should produce visible pixels.");
        assertTrue(imagesAreIdentical(expected, actual),
            "Skyblock fallback render should match the base item when no texture pack is applied.");

        context.close();
    }

    @Test
    void renderItemFromReconstructedNbtMatchesDirectRender() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(64);

        BufferedImage expected = new ItemRenderer(context, "minecraft:diamond_sword", options).render();

        CompoundTag nbt = new CompoundTag();
        nbt.put("id", "minecraft:diamond_sword");
        nbt.put("Count", new ByteTag((byte) 1));

        BufferedImage actual = ItemRenderer.fromNbt(context, nbt, options).render();

        assertTrue(imagesAreIdentical(expected, actual),
            "Rendering from a reconstructed NBT payload should match the direct item render.");

        context.close();
    }

    // Skipped: renderItemFromNbtWithResourceIdMatchesDirectOperations
    //   Requires renderItemFromNbtWithResourceId() and computeResourceIdFromNbt() which do not
    //   exist in the Java codebase.

    // Skipped: renderItemFromNbtWithResourceIdIgnoresUnusedData
    //   Requires renderItemFromNbtWithResourceId() and computeResourceIdFromNbt() which do not
    //   exist in the Java codebase.

    // Skipped: renderItemFromNbtWithResourceIdHonorsExtractedItemData
    //   Requires renderItemFromNbtWithResourceId() and computeResourceIdFromNbt() which do not
    //   exist in the Java codebase.

    @Test
    void biomeTintWhitelistMatchesExpectations() {
        BiomeTint grass = ColorUtil.tryGetBiomeTint("minecraft:block/grass_block_top", "grass_block");
        assertNotNull(grass);
        assertEquals("GRASS", grass.toString());

        BiomeTint foliage = ColorUtil.tryGetBiomeTint("minecraft:block/oak_leaves", "oak_leaves");
        assertNotNull(foliage);
        assertEquals("FOLIAGE", foliage.toString());

        BiomeTint cherrySapling = ColorUtil.tryGetBiomeTint("minecraft:block/cherry_sapling", "cherry_sapling");
        assertNull(cherrySapling);
    }

    @Test
    void renderChestUsesOverlayModels() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BufferedImage image = new BlockRenderer(context, "chest", null).render();

        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());

        boolean hasNonMissingPixel = false;
        outer:
        for (int y = 0; y < image.getHeight(); y += 16) {
            for (int x = 0; x < image.getWidth(); x += 16) {
                int argb = image.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                if (a > 0 && !(r == 0xFF && g == 0x00 && b == 0xFF)) {
                    hasNonMissingPixel = true;
                    break outer;
                }
            }
        }

        assertTrue(hasNonMissingPixel, "Chest rendering should include non-missing texture pixels.");
        context.close();
    }

    @Test
    void leatherHelmetUsesDefaultTintWhenNoOverride() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(128);

        BufferedImage baseline = new ItemRenderer(context, "leather_helmet", options).render();
        ItemRenderData defaultTintData =
            new ItemRenderData(new int[]{0xA0, 0x65, 0x40}, null, false, null, null);
        BufferedImage explicitDefault = new ItemRenderer(context, "leather_helmet", defaultTintData, options).render();

        assertTrue(imagesAreIdentical(baseline, explicitDefault));
        context.close();
    }

    @Test
    void leatherHelmetRespectsCustomTint() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(128);

        BufferedImage baseline = new ItemRenderer(context, "leather_helmet", options).render();
        ItemRenderData customTint =
            new ItemRenderData(new int[]{0x20, 0x60, 0xFF}, null, false, null, null);
        BufferedImage custom = new ItemRenderer(context, "leather_helmet", customTint, options).render();

        assertFalse(imagesAreIdentical(baseline, custom));
        Vector3f baselineAverage = computeAverageColor(baseline);
        Vector3f customAverage = computeAverageColor(custom);
        float difference = baselineAverage.subtract(customAverage).length();
        assertTrue(difference > 10f,
            "Expected custom tint to alter color significantly (difference "
                + String.format("%.2f", difference) + ").");

        context.close();
    }

    @Test
    void wolfArmorDyedUsesDefaultTint() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(128);

        BufferedImage baseline = new ItemRenderer(context, "wolf_armor_dyed", options).render();
        ItemRenderData defaultTintData =
            new ItemRenderData(new int[]{0xA0, 0x65, 0x40}, null, false, null, null);
        BufferedImage explicitDefault = new ItemRenderer(context, "wolf_armor_dyed", defaultTintData, options).render();

        assertTrue(imagesAreIdentical(baseline, explicitDefault));
        context.close();
    }

    @Test
    void wolfArmorDyedRespectsCustomTint() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(128);

        BufferedImage baseline = new ItemRenderer(context, "wolf_armor_dyed", options).render();
        ItemRenderData customTint =
            new ItemRenderData(new int[]{0x40, 0x90, 0x30}, null, false, null, null);
        BufferedImage custom = new ItemRenderer(context, "wolf_armor_dyed", customTint, options).render();

        assertFalse(imagesAreIdentical(baseline, custom));
        Vector3f baselineAverage = computeAverageColor(baseline);
        Vector3f customAverage = computeAverageColor(custom);
        float difference = baselineAverage.subtract(customAverage).length();
        assertTrue(difference > 8f,
            "Expected custom tint to alter wolf armor overlay significantly (difference "
                + String.format("%.2f", difference) + ").");

        context.close();
    }

    @Test
    void wolfArmorDyedAllowsExplicitLayerTintOverride() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(128);

        BufferedImage baseline = new ItemRenderer(context, "wolf_armor_dyed", options).render();
        Map<Integer, int[]> overrides = new HashMap<>();
        overrides.put(1, new int[]{0x90, 0x20, 0xF0});
        ItemRenderData customTint =
            new ItemRenderData(null, overrides, false, null, null);
        BufferedImage custom = new ItemRenderer(context, "wolf_armor_dyed", customTint, options).render();

        assertFalse(imagesAreIdentical(baseline, custom));
        Vector3f baselineAverage = computeAverageColor(baseline);
        Vector3f customAverage = computeAverageColor(custom);
        float difference = baselineAverage.subtract(customAverage).length();
        assertTrue(difference > 10f,
            "Expected explicit layer tint to alter wolf armor overlay (difference "
                + String.format("%.2f", difference) + ").");

        context.close();
    }

    @Test
    void leatherHorseArmorUsesDefaultTint() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(128);

        BufferedImage baseline = new ItemRenderer(context, "leather_horse_armor", options).render();
        ItemRenderData explicitDefault =
            new ItemRenderData(new int[]{0xA0, 0x65, 0x40}, null, false, null, null);
        BufferedImage renderedDefault = new ItemRenderer(context, "leather_horse_armor", explicitDefault, options).render();

        assertTrue(imagesAreIdentical(baseline, renderedDefault));
        context.close();
    }

    @Test
    void leatherHorseArmorRespectsCustomTint() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(128);

        BufferedImage baseline = new ItemRenderer(context, "leather_horse_armor", options).render();
        ItemRenderData customTint =
            new ItemRenderData(new int[]{0x35, 0x99, 0xCF}, null, false, null, null);
        BufferedImage custom = new ItemRenderer(context, "leather_horse_armor", customTint, options).render();

        assertFalse(imagesAreIdentical(baseline, custom));
        Vector3f baselineAverage = computeAverageColor(baseline);
        Vector3f customAverage = computeAverageColor(custom);
        float difference = baselineAverage.subtract(customAverage).length();
        assertTrue(difference > 10f,
            "Expected custom tint to alter leather horse armor color (difference "
                + String.format("%.2f", difference) + ").");

        context.close();
    }

    @Test
    void lilyPadUsesMetadataTintWhenNoOverride() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        ItemInfo info = context.getItemInfo("lily_pad");
        assertNotNull(info);
        TintInfo tintInfo = info.getLayerTints().get(0);
        assertNotNull(tintInfo);
        int[] defaultColor = tintInfo.getDefaultColor();
        assertNotNull(defaultColor);

        BlockRenderOptions options = optionsWithSize(128);
        BufferedImage baseline = new ItemRenderer(context, "lily_pad", options).render();
        ItemRenderData explicitDefault =
            new ItemRenderData(defaultColor, null, false, null, null);
        BufferedImage renderedDefault = new ItemRenderer(context, "lily_pad", explicitDefault, options).render();

        assertTrue(imagesAreIdentical(baseline, renderedDefault));
        context.close();
    }

    @Test
    void lilyPadRespectsCustomTint() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(128);

        BufferedImage baseline = new ItemRenderer(context, "lily_pad", options).render();
        ItemRenderData customTint =
            new ItemRenderData(new int[]{0x40, 0xB0, 0x80}, null, false, null, null);
        BufferedImage custom = new ItemRenderer(context, "lily_pad", customTint, options).render();

        assertFalse(imagesAreIdentical(baseline, custom));
        Vector3f baselineAverage = computeAverageColor(baseline);
        Vector3f customAverage = computeAverageColor(custom);
        float difference = baselineAverage.subtract(customAverage).length();
        assertTrue(difference > 5f,
            "Expected custom tint to alter lily pad appearance (difference "
                + String.format("%.2f", difference) + ").");

        context.close();
    }

    @Test
    void potionUsesMetadataTintAndWritesPreview() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(128);

        BufferedImage tinted = new ItemRenderer(context, "potion", options).render();
        ItemRenderData customTintData =
            new ItemRenderData(new int[]{0xD0, 0x40, 0xB0}, null, false, null, null);
        BufferedImage customTinted = new ItemRenderer(context, "potion", customTintData, options).render();

        ItemRenderData disableTint =
            new ItemRenderData(null, null, true, null, null);
        BufferedImage untinted = new ItemRenderer(context, "potion", disableTint, options).render();

        assertFalse(imagesAreIdentical(tinted, untinted));
        assertFalse(imagesAreIdentical(customTinted, untinted));
        assertFalse(imagesAreIdentical(customTinted, tinted));

        Vector3f tintedAverage = computeAverageColor(tinted);
        Vector3f untintedAverage = computeAverageColor(untinted);
        Vector3f customAverage = computeAverageColor(customTinted);
        float metadataDifference = tintedAverage.subtract(untintedAverage).length();
        float customDifference = customAverage.subtract(untintedAverage).length();
        float metadataVsCustomDifference = customAverage.subtract(tintedAverage).length();

        assertTrue(metadataDifference > 5f,
            "Expected potion metadata tint to alter appearance (difference "
                + String.format("%.2f", metadataDifference) + ").");
        assertTrue(customDifference > 5f,
            "Expected custom potion tint to alter appearance (difference "
                + String.format("%.2f", customDifference) + ").");
        assertTrue(metadataVsCustomDifference > 5f,
            "Expected custom potion tint to differ from metadata tint (difference "
                + String.format("%.2f", metadataVsCustomDifference) + ").");

        context.close();
    }

    @Test
    void cubeFaceUvsAreOrientedCorrectly() {
        Vector3f from = new Vector3f(0f, 0f, 0f);
        Vector3f to = new Vector3f(16f, 16f, 16f);

        for (Face dir : Face.values()) {
            Vector4f uv = dir.defaultUv(from, to);
            Vector2f[] map = uv.createUvMap(0);

            assertTrue(map[0].getX() < map[2].getX(),
                dir + ": UV0 (minU) should have smaller U than UV2 (maxU).");
            assertTrue(map[0].getY() < map[1].getY(),
                dir + ": UV0 (minV) should have smaller V than UV1 (maxV).");
            assertEquals(map[0].getX(), map[1].getX(), 0.0001f, dir + ": minU column mismatch");
            assertEquals(map[2].getX(), map[3].getX(), 0.0001f, dir + ": maxU column mismatch");
            assertEquals(map[0].getY(), map[3].getY(), 0.0001f, dir + ": minV row mismatch");
            assertEquals(map[1].getY(), map[2].getY(), 0.0001f, dir + ": maxV row mismatch");
        }
    }

    @Test
    void billboardTexturesAreNotUpsideDown() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        BufferedImage customTexture = createVerticalSplitTexture(
            0xE6, 0x3B, 0x3B, 0xFF,
            0x3B, 0x6B, 0xE6, 0xFF);
        context.getTextureContext().registerTexture(
            "minecraft:block/birch_sapling", customTexture, true);

        List<BlockRenderOptions> testOptions = new ArrayList<>();
        testOptions.add(BlockRenderOptions.DEFAULT);

        for (float yaw : new float[]{0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f}) {
            testOptions.add(BlockRenderOptions.builder()
                .withSize(256)
                .withYawInDegrees(yaw)
                .isUseGuiTransform(false)
                .withPadding(0.05f)
                .build());
        }

        for (BlockRenderOptions options : testOptions) {
            BufferedImage rendered = new BlockRenderer(context, "birch_sapling", options).render();
            int[] topColor = findOpaquePixel(rendered, true);
            int[] bottomColor = findOpaquePixel(rendered, false);
            System.out.println("Options (gui=" + options.isUseGuiTransform()
                + ", yaw=" + options.getYawInDegrees()
                + ") -> top R=" + topColor[0] + " B=" + topColor[2]
                + ", bottom R=" + bottomColor[0] + " B=" + bottomColor[2]);
            assertTrue(topColor[0] > topColor[2],
                "Top of billboarded texture should preserve the top-half color for gui="
                    + options.isUseGuiTransform() + " yaw=" + options.getYawInDegrees() + ".");
            assertTrue(bottomColor[2] > bottomColor[0],
                "Bottom of billboarded texture should preserve the bottom-half color for gui="
                    + options.isUseGuiTransform() + " yaw=" + options.getYawInDegrees() + ".");
        }

        context.close();
    }

    @Test
    void sporeBlossomTopViewHasOpaquePixels() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(256)
            .withPitchInDegrees(90f)
            .build();

        BufferedImage image = new BlockRenderer(context, "spore_blossom", options).render();
        assertTrue(hasOpaquePixels(image), "Spore blossom viewed from above should contain visible pixels.");

        context.close();
    }

    @Test
    void bigDripleafHasRenderedStem() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BufferedImage image = new ItemRenderer(context, "big_dripleaf", null).render();

        // Sample one pixel in the stem area (scaled from 512x512 original coordinates)
        int sampleX = (int) (200.0 / 512 * image.getWidth());
        int sampleY = (int) (300.0 / 512 * image.getHeight());
        if (sampleX >= image.getWidth()) sampleX = image.getWidth() - 1;
        if (sampleY >= image.getHeight()) sampleY = image.getHeight() - 1;

        int argb = image.getRGB(sampleX, sampleY);
        int alpha = (argb >> 24) & 0xFF;

        System.out.println("Stem pixel @ (" + sampleX + "," + sampleY + "): alpha=" + alpha);
        assertTrue(alpha > 10, "Rendered big dripleaf should include the stem.");

        context.close();
    }

    @Test
    void crafterFrontFaceMatchesNorthTexture() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockRenderOptions options = optionsWithSize(256);
        BufferedImage rendered = new BlockRenderer(context, "crafter", options).render();

        int[] rightColor = sampleAverageColor(rendered,
            (int) (rendered.getWidth() * 0.70f), rendered.getWidth() - 1,
            rendered.getHeight() / 2 - 20, rendered.getHeight() / 2 + 20);
        int[] leftColor = sampleAverageColor(rendered,
            0, (int) (rendered.getWidth() * 0.30f),
            rendered.getHeight() / 2 - 20, rendered.getHeight() / 2 + 20);

        BufferedImage northTexture = context.getTextureContext().getTexture("minecraft:block/crafter_north");
        BufferedImage southTexture = context.getTextureContext().getTexture("minecraft:block/crafter_south");
        BufferedImage westTexture = context.getTextureContext().getTexture("minecraft:block/crafter_west");
        BufferedImage eastTexture = context.getTextureContext().getTexture("minecraft:block/crafter_east");

        Vector3f northAvg = computeAverageColor(northTexture);
        Vector3f southAvg = computeAverageColor(southTexture);
        Vector3f westAvg = computeAverageColor(westTexture);
        Vector3f eastAvg = computeAverageColor(eastTexture);

        Vector3f rightVector = new Vector3f(rightColor[0], rightColor[1], rightColor[2]);
        float northError = computeScaledError(rightVector, northAvg);
        float southError = computeScaledError(rightVector, southAvg);
        System.out.println("Right -> north error " + northError + ", south error " + southError);

        assertTrue(southError <= northError,
            "Right face should more closely match crafter south texture than north.");

        assertTrue(isCloserTo(leftColor, eastAvg, westAvg),
            "Left face should more closely match crafter east texture than west.");

        context.close();
    }

    @Test
    void renderBlockFaceReturnsCorrectSizeImage() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockFaceRenderOptions options = BlockFaceRenderOptions.builder()
            .withSize(128)
            .build();
        BufferedImage image = new BlockFaceRenderer(context, "dirt", options).render();

        assertEquals(128, image.getWidth());
        assertEquals(128, image.getHeight());

        context.close();
    }

    @Test
    void renderBlockFaceContainsOpaquePixels() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockFaceRenderOptions options = BlockFaceRenderOptions.builder()
            .withDirection(Face.NORTH)
            .build();
        BufferedImage image = new BlockFaceRenderer(context, "stone", options).render();

        assertTrue(hasOpaquePixels(image), "RenderBlockFace should produce opaque pixels for stone.");

        context.close();
    }

    @Test
    void renderBlockFaceTopDifferentFromSideForGrassBlock() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        BufferedImage top = new BlockFaceRenderer(context, "grass_block",
            BlockFaceRenderOptions.builder().build()).render();
        BufferedImage side = new BlockFaceRenderer(context, "grass_block",
            BlockFaceRenderOptions.builder().withDirection(Face.NORTH).build()).render();

        assertFalse(imagesAreIdentical(top, side),
            "Grass block top face should be different from the side face.");

        context.close();
    }

    @Test
    void renderBlockFaceDefaultSizeIs512() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BufferedImage image = new BlockFaceRenderer(context, "cobblestone", null).render();

        assertEquals(512, image.getWidth());
        assertEquals(512, image.getHeight());

        context.close();
    }

    @Test
    void renderBlockFaceRotationProducesDifferentImage() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);
        BlockFaceRenderOptions baseOptions = BlockFaceRenderOptions.builder()
            .withSize(64)
            .build();
        BlockFaceRenderOptions rotatedOptions = BlockFaceRenderOptions.builder()
            .withSize(64)
            .withRotation(90)
            .build();

        BufferedImage original = new BlockFaceRenderer(context, "oak_log", baseOptions).render();
        BufferedImage rotated = new BlockFaceRenderer(context, "oak_log", rotatedOptions).render();

        assertFalse(imagesAreIdentical(original, rotated),
            "Rotated block face should be different from original.");

        context.close();
    }

    // ----------------------------------------------------------------
    // Skipped tests (animation API not available in Java)
    // ----------------------------------------------------------------

    // Skipped: renderAnimatedItemFromNbtWithResourceIdReturnsSingleFrameForStaticItem
    //   Requires animation API (RenderAnimatedItemFromNbtWithResourceId, CloneAsAnimatedImage)

    // Skipped: cloneAsAnimatedImagePreservesFramesForAnimatedItems
    //   Requires animation API

    // Skipped: cloneAsAnimatedImageSetsAnimationMetadata
    //   Requires animation metadata API

    // Skipped: animatedRenderedResourceSupportsAsyncSaves
    //   Requires async animation saves API

    // Skipped: computeResourceIdMatchesAnimatedRenderForNbtItems
    //   Requires animation API

    // ----------------------------------------------------------------
    // Skipped tests (heavy internal reflection)
    // ----------------------------------------------------------------

    // Skipped: defaultInventoryOrientationShowsFrontOnRight
    //   Requires renderModel with synthetic model + RegisterTexture + face color sampling
    //   through complex internal pipeline. renderModel and registerTexture ARE public in Java,
    //   but the test requires building synthetic models with face-per-direction textures which
    //   needs verification of the full pipeline. Including as-is below.

    @Test
    void defaultInventoryOrientationShowsFrontOnRight() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        // Create per-face solid color textures
        Map<Face, int[]> faceColors = new EnumMap<>(Face.class);
        faceColors.put(Face.NORTH, new int[]{0xFF, 0x33, 0x33});
        faceColors.put(Face.SOUTH, new int[]{0x33, 0x99, 0xFF});
        faceColors.put(Face.EAST, new int[]{0x33, 0xFF, 0x99});
        faceColors.put(Face.WEST, new int[]{0x99, 0x33, 0xFF});
        faceColors.put(Face.UP, new int[]{0xFF, 0xFF, 0x66});
        faceColors.put(Face.DOWN, new int[]{0xFF, 0x99, 0x33});

        for (Map.Entry<Face, int[]> entry : faceColors.entrySet()) {
            Face direction = entry.getKey();
            int[] color = entry.getValue();
            String textureId = "minecraft:block/unit_test_debug_" + direction.name().toLowerCase();
            BufferedImage image = createSolidTexture(color[0], color[1], color[2], 0xFF, 16);
            context.getTextureContext().registerTexture(textureId, image, true);
        }

        Map<Face, FaceData> faces = new EnumMap<>(Face.class);
        Vector4f fullUv = new Vector4f(0, 0, 16, 16);
        faces.put(Face.NORTH, new FaceData("#north", fullUv, null, null, null));
        faces.put(Face.SOUTH, new FaceData("#south", fullUv, null, null, null));
        faces.put(Face.EAST, new FaceData("#east", fullUv, null, null, null));
        faces.put(Face.WEST, new FaceData("#west", fullUv, null, null, null));
        faces.put(Face.UP, new FaceData("#up", fullUv, null, null, null));
        faces.put(Face.DOWN, new FaceData("#down", fullUv, null, null, null));

        Map<String, TextureReference> textures = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        textures.put("particle", TextureReference.of("minecraft:block/unit_test_debug_up"));
        textures.put("north", TextureReference.of("minecraft:block/unit_test_debug_north"));
        textures.put("south", TextureReference.of("minecraft:block/unit_test_debug_south"));
        textures.put("east", TextureReference.of("minecraft:block/unit_test_debug_east"));
        textures.put("west", TextureReference.of("minecraft:block/unit_test_debug_west"));
        textures.put("up", TextureReference.of("minecraft:block/unit_test_debug_up"));
        textures.put("down", TextureReference.of("minecraft:block/unit_test_debug_down"));

        Element element = new Element(
            new Vector3f(0, 0, 0), new Vector3f(16, 16, 16), null, faces, true);
        BlockModel model = new BlockModel(
            "unit_test:debug_cube", Concurrent.newList(), Concurrent.newMap(textures),
            Concurrent.newMap(), Concurrent.newList(List.of(element)));

        BlockRenderOptions options = optionsWithSize(160);
        BufferedImage rendered = new ModelRenderer(context, model, options, null).render();

        int[] rightColor = sampleAverageColor(rendered,
            (int) (rendered.getWidth() * 0.70f), (int) (rendered.getWidth() * 0.95f),
            rendered.getHeight() / 2 - 10, rendered.getHeight() / 2 + 10);
        int[] leftColor = sampleAverageColor(rendered,
            (int) (rendered.getWidth() * 0.05f), (int) (rendered.getWidth() * 0.30f),
            rendered.getHeight() / 2 - 10, rendered.getHeight() / 2 + 10);
        int[] topColor = sampleAverageColor(rendered,
            rendered.getWidth() / 2 - 10, rendered.getWidth() / 2 + 10,
            (int) (rendered.getHeight() * 0.05f), (int) (rendered.getHeight() * 0.25f));

        Vector3f rightVector = new Vector3f(rightColor[0], rightColor[1], rightColor[2]);
        Vector3f northVec = toVector3f(faceColors.get(Face.NORTH));
        Vector3f southVec = toVector3f(faceColors.get(Face.SOUTH));
        Vector3f eastVec = toVector3f(faceColors.get(Face.EAST));
        Vector3f westVec = toVector3f(faceColors.get(Face.WEST));
        Vector3f upVec = toVector3f(faceColors.get(Face.UP));
        Vector3f downVec = toVector3f(faceColors.get(Face.DOWN));

        assertTrue(isCloserTo(rightColor, northVec, southVec),
            "Right face should prefer north color over south.");
        assertTrue(isCloserTo(leftColor, eastVec, westVec),
            "Left face should prefer east color over west.");
        assertTrue(isCloserTo(topColor, upVec, downVec),
            "Top face should prefer up color over down.");

        context.close();
    }

    // Skipped: inventoryLightingCreatesDirectionalHighlights
    //   Requires BuildTriangles, BuildDisplayTransform, CreateRotationMatrix internal methods
    //   which use heavy reflection into the rendering pipeline.

    // Skipped: billboardNorthFaceIsUpright
    //   Requires renderModel with synthetic model. Including below since renderModel is public.

    @Test
    void billboardNorthFaceIsUpright() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        String textureId = "minecraft:block/unit_test_cross_north";
        BufferedImage customTexture = createVerticalSplitTexture(
            0xE6, 0x3B, 0x3B, 0xFF,
            0x3B, 0x6B, 0xE6, 0xFF);
        context.getTextureContext().registerTexture(textureId, customTexture, true);

        Map<Face, FaceData> faces = new EnumMap<>(Face.class);
        faces.put(Face.NORTH,
            new FaceData("#cross", new Vector4f(0f, 0f, 16f, 16f), null, null, null));

        Map<String, TextureReference> textures = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        textures.put("cross", TextureReference.of(textureId));

        Element element = new Element(
            new Vector3f(0.8f, 0f, 7f), new Vector3f(15.2f, 16f, 9f),
            null, faces, false);

        BlockModel model = new BlockModel(
            "unit_test:cross_north", Concurrent.newList(), Concurrent.newMap(textures),
            Concurrent.newMap(), Concurrent.newList(List.of(element)));

        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(256)
            .isUseGuiTransform(false)
            .withPadding(0.05f)
            .build();

        BufferedImage rendered = new ModelRenderer(context, model, options, null).render();
        int[] topColor = findOpaquePixel(rendered, true);
        int[] bottomColor = findOpaquePixel(rendered, false);

        assertTrue(topColor[0] > topColor[2],
            "North billboard face should display the top-half color at the top.");
        assertTrue(bottomColor[2] > bottomColor[0],
            "North billboard face should display the bottom-half color at the bottom.");

        context.close();
    }

    // Skipped: billboardNorthFaceUvTopIsNotFlipped
    //   Requires BuildTriangles internal method reflection.

    // Skipped: rotatedHorizontalFanDownFaceUsesExpectedUvOrientation
    //   Requires BuildTriangles internal method reflection.

    // ----------------------------------------------------------------
    // Helper methods
    // ----------------------------------------------------------------

    /**
     * Creates a BlockRenderOptions with the specified size and all other values at defaults.
     */
    private static BlockRenderOptions optionsWithSize(int size) {
        return BlockRenderOptions.builder()
            .withSize(size)
            .build();
    }

    /**
     * Returns true if two images have identical pixel data.
     */
    private static boolean imagesAreIdentical(BufferedImage left, BufferedImage right) {
        if (left.getWidth() != right.getWidth() || left.getHeight() != right.getHeight()) {
            return false;
        }
        for (int y = 0; y < left.getHeight(); y++) {
            for (int x = 0; x < left.getWidth(); x++) {
                if (left.getRGB(x, y) != right.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns true if the image contains at least one pixel with alpha > 10.
     */
    private static boolean hasOpaquePixels(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 10) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns [minX, maxX] of opaque pixel horizontal bounds, or [-1, -1] if none found.
     */
    private static int[] getOpaqueHorizontalBounds(BufferedImage image) {
        int min = image.getWidth();
        int max = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 10) {
                    if (x < min) min = x;
                    if (x > max) max = x;
                }
            }
        }
        if (max < min) {
            return new int[]{-1, -1};
        }
        return new int[]{min, max};
    }

    /**
     * Computes the average RGB color across all pixels as a Vector3f.
     */
    private static Vector3f computeAverageColor(BufferedImage image) {
        long totalR = 0, totalG = 0, totalB = 0, count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                totalR += (argb >> 16) & 0xFF;
                totalG += (argb >> 8) & 0xFF;
                totalB += argb & 0xFF;
                count++;
            }
        }
        if (count == 0) return Vector3f.ZERO;
        return new Vector3f(totalR / (float) count, totalG / (float) count, totalB / (float) count);
    }

    /**
     * Computes the scaled error between a sample vector and a reference vector.
     * This projects the sample onto the reference direction and measures perpendicular deviation.
     */
    private static float computeScaledError(Vector3f sample, Vector3f reference) {
        float referenceLengthSquared = reference.lengthSquared();
        if (referenceLengthSquared < 1e-3f) {
            return sample.lengthSquared();
        }
        float scale = Vector3f.dot(sample, reference) / referenceLengthSquared;
        Vector3f adjusted = reference.multiply(scale);
        Vector3f diff = sample.subtract(adjusted);
        return diff.lengthSquared();
    }

    /**
     * Returns true if sampleColor is closer to expected than to alternative.
     */
    private static boolean isCloserTo(int[] sampleColor, Vector3f expected, Vector3f alternative) {
        Vector3f sample = new Vector3f(sampleColor[0], sampleColor[1], sampleColor[2]);
        float expectedError = computeScaledError(sample, expected);
        float alternativeError = computeScaledError(sample, alternative);
        return expectedError <= alternativeError;
    }

    /**
     * Converts an int[] {r,g,b} to a Vector3f.
     */
    private static Vector3f toVector3f(int[] color) {
        return new Vector3f(color[0], color[1], color[2]);
    }

    /**
     * Finds the first opaque pixel scanning from the top or bottom.
     * Returns [r, g, b, a].
     */
    private static int[] findOpaquePixel(BufferedImage image, boolean searchFromTop) {
        if (searchFromTop) {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    if (a > 200) {
                        return new int[]{
                            (argb >> 16) & 0xFF,
                            (argb >> 8) & 0xFF,
                            argb & 0xFF,
                            a
                        };
                    }
                }
            }
        } else {
            for (int y = image.getHeight() - 1; y >= 0; y--) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    if (a > 200) {
                        return new int[]{
                            (argb >> 16) & 0xFF,
                            (argb >> 8) & 0xFF,
                            argb & 0xFF,
                            a
                        };
                    }
                }
            }
        }
        throw new RuntimeException("No opaque pixels were found in the rendered image.");
    }

    /**
     * Creates a 16x16 texture with the top half one color and the bottom half another.
     */
    private static BufferedImage createVerticalSplitTexture(
            int topR, int topG, int topB, int topA,
            int bottomR, int bottomG, int bottomB, int bottomA) {
        return createVerticalSplitTexture(topR, topG, topB, topA, bottomR, bottomG, bottomB, bottomA, 16);
    }

    /**
     * Creates a texture with the top half one color and the bottom half another.
     */
    private static BufferedImage createVerticalSplitTexture(
            int topR, int topG, int topB, int topA,
            int bottomR, int bottomG, int bottomB, int bottomA,
            int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int half = size / 2;
        int topArgb = (topA << 24) | (topR << 16) | (topG << 8) | topB;
        int bottomArgb = (bottomA << 24) | (bottomR << 16) | (bottomG << 8) | bottomB;
        for (int y = 0; y < size; y++) {
            int color = y < half ? topArgb : bottomArgb;
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    /**
     * Creates a solid-color texture.
     */
    private static BufferedImage createSolidTexture(int r, int g, int b, int a, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int argb = (a << 24) | (r << 16) | (g << 8) | b;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }

    /**
     * Samples the average color in a rectangular region. Returns [r, g, b, a].
     */
    private static int[] sampleAverageColor(BufferedImage image, int xStart, int xEnd, int yStart, int yEnd) {
        int clampXStart = Math.max(0, Math.min(xStart, image.getWidth() - 1));
        int clampXEnd = Math.max(0, Math.min(xEnd, image.getWidth() - 1));
        int clampYStart = Math.max(0, Math.min(yStart, image.getHeight() - 1));
        int clampYEnd = Math.max(0, Math.min(yEnd, image.getHeight() - 1));

        long totalR = 0, totalG = 0, totalB = 0, totalA = 0, count = 0;
        for (int y = clampYStart; y <= clampYEnd; y++) {
            for (int x = clampXStart; x <= clampXEnd; x++) {
                int argb = image.getRGB(x, y);
                totalA += (argb >> 24) & 0xFF;
                totalR += (argb >> 16) & 0xFF;
                totalG += (argb >> 8) & 0xFF;
                totalB += argb & 0xFF;
                count++;
            }
        }
        if (count == 0) return new int[]{0, 0, 0, 0};
        return new int[]{
            (int) (totalR / count),
            (int) (totalG / count),
            (int) (totalB / count),
            (int) (totalA / count)
        };
    }
}
