package dev.sbs.minecraftapi.render.context;

import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import dev.sbs.minecraftapi.render.IntegrationTestBase;
import dev.sbs.minecraftapi.render.ItemRenderer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SkullResolverContextTests extends IntegrationTestBase {

    @Test
    void skullResolverContext_providesFullItemData() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        CompoundTag customData = new CompoundTag();
        customData.put("id", "CUSTOM_SKULL_ITEM");
        customData.put("metadata", "extra_info");
        customData.put("level", 5);

        AtomicReference<SkullResolverContext> capturedContext = new AtomicReference<>();

        ItemRenderData itemData =
            new ItemRenderData(null, null, false, customData, null);
        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(64)
            .withItemData(itemData)
            .withSkullTextureResolver(resolverCtx -> {
                capturedContext.set(resolverCtx);
                return null;
            })
            .build();

        BufferedImage image = new ItemRenderer(context, "minecraft:player_head", itemData, options).render();
        assertNotNull(image);

        SkullResolverContext ctx = capturedContext.get();
        assertNotNull(ctx);
        assertEquals("minecraft:player_head", ctx.getItemId());
        assertEquals("CUSTOM_SKULL_ITEM", ctx.getCustomDataId());
        assertNotNull(ctx.getCustomData());
        assertTrue(ctx.getCustomData().containsKey("metadata"));
        assertTrue(ctx.getCustomData().containsKey("level"));
        context.close();
    }

    @Test
    void skullResolverContext_canAccessNestedNbtData() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        CompoundTag nested = new CompoundTag();
        nested.put("value", "deep_data");

        CompoundTag customData = new CompoundTag();
        customData.put("id", "COMPLEX_ITEM");
        customData.put("nested", nested);

        AtomicReference<String> extractedNestedValue = new AtomicReference<>();

        ItemRenderData itemData =
            new ItemRenderData(null, null, false, customData, null);
        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(64)
            .withItemData(itemData)
            .withSkullTextureResolver(resolverCtx -> {
                if (resolverCtx.getCustomData() != null) {
                    CompoundTag nestedCompound = resolverCtx.getCustomData().getTag("nested");
                    if (nestedCompound != null) {
                        StringTag valueTag = nestedCompound.getTag("value");
                        if (valueTag != null) {
                            extractedNestedValue.set(valueTag.getValue());
                        }
                    }
                }
                return null;
            })
            .build();

        BufferedImage image = new ItemRenderer(context, "minecraft:player_head", itemData, options).render();
        assertNotNull(image);
        assertEquals("deep_data", extractedNestedValue.get());
        context.close();
    }

    @Test
    void skullResolverContext_handlesNullCustomData() throws Exception {
        String assetsDir = getAssetsDirectory();
        RenderContext context = createRenderContext(assetsDir);

        AtomicReference<SkullResolverContext> capturedContext = new AtomicReference<>();

        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(64)
            .withSkullTextureResolver(resolverCtx -> {
                capturedContext.set(resolverCtx);
                return null;
            })
            .build();

        BufferedImage image = new ItemRenderer(context, "minecraft:player_head", options).render();
        assertNotNull(image);

        SkullResolverContext ctx = capturedContext.get();
        assertNotNull(ctx);
        assertEquals("minecraft:player_head", ctx.getItemId());
        assertNull(ctx.getCustomDataId());
        assertNull(ctx.getCustomData());
        assertNull(ctx.getProfile());
        context.close();
    }
}
