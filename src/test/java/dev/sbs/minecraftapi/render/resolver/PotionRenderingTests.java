package dev.sbs.minecraftapi.render.resolver;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.ByteTag;
import dev.sbs.minecraftapi.render.IntegrationTestBase;
import dev.sbs.minecraftapi.render.ItemRenderer;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.RenderContext;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PotionRenderingTests extends IntegrationTestBase {

    @Test
    void harvestHarbingerPotionUsesHypixelPlusTexture() throws Exception {
        String assetsDir = getAssetsDirectory();
        String texturePacksDir = getTexturePacksDirectory();

        Path hypixelPackPath = Path.of(texturePacksDir, "Hypixel+ 0.23.4 for 1.21.8");
        assumeTrue(Files.isDirectory(hypixelPackPath),
            "Hypixel+ texture pack not found - skipping test");

        RenderContext context = createRenderContext(assetsDir, null);

        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(128)
            .withPackIds(Concurrent.newUnmodifiableList("hypixelplus"))
            .build();

        CompoundTag customData = new CompoundTag();
        customData.put("id", "POTION");
        customData.put("potion", "harvest_harbinger");
        customData.put("splash", "0");
        customData.put("potion_type", "POTION");
        customData.put("potion_level", "5");

        CompoundTag components = new CompoundTag();
        components.put("minecraft:custom_data", customData);

        CompoundTag root = new CompoundTag();
        root.put("id", "minecraft:potion");
        root.put("count", new ByteTag((byte) 1));
        root.put("components", components);

        BufferedImage image = ItemRenderer.fromNbt(context, root, options).render();

        assertNotNull(image);
        assertEquals(128, image.getWidth());
        assertEquals(128, image.getHeight());

        boolean hasContent = false;
        for (int y = 0; y < image.getHeight() && !hasContent; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 128) {
                    hasContent = true;
                    break;
                }
            }
        }

        assertTrue(hasContent, "Rendered potion should have visible pixels");
        context.close();
    }

    @Test
    void vanillaPotionRendersWithoutPack() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(128)
            .build();

        CompoundTag root = new CompoundTag();
        root.put("id", "minecraft:potion");
        root.put("count", new ByteTag((byte) 1));

        BufferedImage image = ItemRenderer.fromNbt(context, root, options).render();
        assertNotNull(image);
        assertEquals(128, image.getWidth());
        assertEquals(128, image.getHeight());

        context.close();
    }
}
