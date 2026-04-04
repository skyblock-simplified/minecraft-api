package dev.sbs.minecraftapi.render.resolver;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.render.IntegrationTestBase;
import dev.sbs.minecraftapi.render.ItemRenderer;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.ItemRenderData;
import dev.sbs.minecraftapi.render.context.RenderContext;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class HypixelPackTests extends IntegrationTestBase {

    @Test
    void hypixelPlayerHeadSelectorLoadsAndResolvesCorrectly() throws Exception {
        String assetsDir = getAssetsDirectory();
        String texturePacksDir = getTexturePacksDirectory();

        Path hypixelPackPath = Path.of(texturePacksDir, "Hypixel+ 0.23.4 for 1.21.8");
        assumeTrue(Files.isDirectory(hypixelPackPath),
            "Hypixel+ texture pack not found - skipping integration test");

        RenderContext context = createRenderContext(
            assetsDir, Concurrent.newList("hypixelplus"));

        CompoundTag customData = new CompoundTag();
        customData.put("id", "AATROX_BATPHONE");
        ItemRenderData itemData = new ItemRenderData(
            null, null, false, customData, null);

        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(64)
            .withPackIds(List.of("hypixelplus"))
            .withItemData(itemData)
            .build();

        BufferedImage image = new ItemRenderer(context, "player_head", itemData, options).render();
        assertNotNull(image);
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);

        context.close();
    }
}
