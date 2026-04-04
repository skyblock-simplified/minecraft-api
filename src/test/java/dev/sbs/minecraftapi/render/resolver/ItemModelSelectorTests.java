package dev.sbs.minecraftapi.render.resolver;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.minecraftapi.nbt.tags.array.IntArrayTag;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.collection.ListTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.IntTag;
import dev.sbs.minecraftapi.render.IntegrationTestBase;
import dev.sbs.minecraftapi.render.ItemRenderer;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.ItemRenderData;
import dev.sbs.minecraftapi.render.context.RenderContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemModelSelectorTests extends IntegrationTestBase {

    private Path tempRoot;

    @BeforeEach
    void setUp() throws IOException {
        tempRoot = Files.createTempDirectory("MinecraftRenderer_ItemModelSelector");
    }

    @AfterEach
    void tearDown() {
        deleteRecursive(tempRoot);
    }

    @Test
    void playerHeadCustomDataUsesTexturePackModel() throws Exception {
        String assetsDir = getAssetsDirectory();
        String packId = "customheadpack";
        Path packRoot = createCustomHeadPack(packId, new Color(0xD4, 0x34, 0x2C));

        registerPacks(List.of(packRoot.toString()));

        RenderContext context = createRenderContext(assetsDir, null);

        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(64)
            .withPackIds(Concurrent.newUnmodifiableList(packId))
            .build();

        CompoundTag customData = new CompoundTag();
        customData.put("id", "custom_head_test");

        ItemRenderData itemData = new ItemRenderData(
            null, null, false, customData, null);
        BufferedImage customRender = new ItemRenderer(context, "player_head", itemData, options).render();
        int[] customPixel = sampleOpaquePixel(customRender);
        assertEquals(0xD4, customPixel[0], "Red channel should match pack color");
        assertEquals(0x34, customPixel[1], "Green channel should match pack color");
        assertEquals(0x2C, customPixel[2], "Blue channel should match pack color");

        // Fallback render (no custom data) should differ from custom render.
        // Without a skin texture, player_head may render as empty/transparent, which
        // still differs from the solid-color custom render.
        BufferedImage fallbackRender = new ItemRenderer(context, "player_head", options).render();
        int[] fallbackPixel = trySampleOpaquePixel(fallbackRender);
        if (fallbackPixel != null) {
            assertFalse(customPixel[0] == fallbackPixel[0]
                    && customPixel[1] == fallbackPixel[1]
                    && customPixel[2] == fallbackPixel[2],
                "Custom render should differ from fallback render");
        }
        // If fallback has no opaque pixels, it trivially differs from the custom render.

        context.close();
    }

    @Test
    void playerHeadNestedCustomDataUsesTexturePackModel() throws Exception {
        String assetsDir = getAssetsDirectory();
        String packId = "nestedcustomheadpack";
        String modelName = "nested_custom_player_head";
        String itemDefinition = """
            {
                "model": {
                    "type": "condition",
                    "property": "component",
                    "predicate": "custom_data",
                    "value": {
                        "id": "nested_head_test",
                        "runes": {
                            "AXE_FADING_GREEN": 2
                        }
                    },
                    "on_true": {
                        "type": "model",
                        "model": "minecraft:item/nested_custom_player_head"
                    },
                    "on_false": {
                        "type": "model",
                        "model": "minecraft:item/player_head"
                    }
                }
            }
            """;

        Path packRoot = createCustomHeadPack(packId, new Color(0x12, 0x34, 0x56), itemDefinition, modelName);

        registerPacks(Concurrent.newUnmodifiableList(packRoot.toString()));

        RenderContext context = createRenderContext(assetsDir, null);

        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(64)
            .withPackIds(Concurrent.newUnmodifiableList(packId))
            .build();

        CompoundTag runesCompound = new CompoundTag();
        runesCompound.put("AXE_FADING_GREEN", new IntTag(2));

        CompoundTag customData = new CompoundTag();
        customData.put("id", "nested_head_test");
        customData.put("runes", runesCompound);

        ItemRenderData itemData = new ItemRenderData(
            null, null, false, customData, null);
        BufferedImage customRender = new ItemRenderer(context, "player_head", itemData, options).render();
        int[] customPixel = sampleOpaquePixel(customRender);
        assertEquals(0x12, customPixel[0]);
        assertEquals(0x34, customPixel[1]);
        assertEquals(0x56, customPixel[2]);

        BufferedImage fallbackRender = new ItemRenderer(context, "player_head", options).render();
        int[] fallbackPixel = trySampleOpaquePixel(fallbackRender);
        if (fallbackPixel != null) {
            assertFalse(customPixel[0] == fallbackPixel[0]
                    && customPixel[1] == fallbackPixel[1]
                    && customPixel[2] == fallbackPixel[2],
                "Custom render should differ from fallback render");
        }

        context.close();
    }

    @Test
    void playerHeadProfileAndCustomDataStillUsesTexturePackModel() throws Exception {
        String assetsDir = getAssetsDirectory();
        String packId = "profilecustomheadpack";
        Color packColor = new Color(0x3C, 0x91, 0xE0);
        Path packRoot = createCustomHeadPack(packId, packColor);

        registerPacks(List.of(packRoot.toString()));

        RenderContext context = createRenderContext(assetsDir, null);

        BlockRenderOptions options = BlockRenderOptions.builder()
            .withSize(64)
            .withPackIds(Concurrent.newUnmodifiableList(packId))
            .build();

        CompoundTag customData = new CompoundTag();
        customData.put("id", "custom_head_test");

        String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/placeholder\"}}}";
        String textureValue = Base64.getEncoder().encodeToString(textureJson.getBytes(StandardCharsets.UTF_8));

        CompoundTag propertyCompound = new CompoundTag();
        propertyCompound.put("name", "textures");
        propertyCompound.put("value", textureValue);

        CompoundTag profile = new CompoundTag();
        profile.put("id", new IntArrayTag(new Integer[]{123456789, 987654321, -135792468, 246813579}));
        profile.put("properties", new ListTag<>(List.of(propertyCompound)));

        ItemRenderData itemData = new ItemRenderData(
            null, null, false, customData, profile);
        BufferedImage rendered = new ItemRenderer(context, "player_head", itemData, options).render();
        int[] pixel = sampleOpaquePixel(rendered);
        assertEquals(packColor.getRed(), pixel[0]);
        assertEquals(packColor.getGreen(), pixel[1]);
        assertEquals(packColor.getBlue(), pixel[2]);

        context.close();
    }

    // ---- Helpers ----

    private Path createCustomHeadPack(String id, Color color) throws IOException {
        return createCustomHeadPack(id, color, null, null);
    }

    private Path createCustomHeadPack(String id, Color color, String itemDefinitionOverride, String modelNameOverride)
        throws IOException {

        Path packRoot = tempRoot.resolve(id);
        Files.createDirectories(packRoot);

        Files.writeString(packRoot.resolve("meta.json"),
            "{\n  \"id\": \"%s\",\n  \"name\": \"%s\",\n  \"version\": \"1.0.0\",\n  \"description\": \"Test pack\",\n  \"authors\": [\"tests\"]\n}\n"
                .formatted(id, id));
        Files.writeString(packRoot.resolve("pack.mcmeta"),
            "{\"pack\": {\"pack_format\": 32, \"description\": \"Test\"}}\n");

        Path itemsDir = packRoot.resolve("assets").resolve("minecraft").resolve("items");
        Files.createDirectories(itemsDir);
        String modelName = modelNameOverride != null ? modelNameOverride : "custom_player_head";
        Files.writeString(itemsDir.resolve("player_head.json"),
            itemDefinitionOverride != null ? itemDefinitionOverride : buildDefaultPlayerHeadDefinition(modelName));

        Path modelsDir = packRoot.resolve("assets").resolve("minecraft").resolve("models").resolve("item");
        Files.createDirectories(modelsDir);
        Files.writeString(modelsDir.resolve(modelName + ".json"), buildDefaultPlayerHeadModel(modelName));

        Path texturesDir = packRoot.resolve("assets").resolve("minecraft").resolve("textures").resolve("item");
        Files.createDirectories(texturesDir);
        writeSolidColorPng(texturesDir.resolve(modelName + ".png"), 16, 16, color);

        return packRoot;
    }

    private static String buildDefaultPlayerHeadDefinition(String modelName) {
        return "{\n  \"model\": {\n    \"type\": \"condition\",\n    \"property\": \"component\",\n    \"predicate\": \"custom_data\",\n    \"value\": { \"id\": \"custom_head_test\" },\n    \"on_true\": {\n      \"type\": \"model\",\n      \"model\": \"minecraft:item/"
            + modelName
            + "\"\n    },\n    \"on_false\": {\n      \"type\": \"model\",\n      \"model\": \"minecraft:item/player_head\"\n    }\n  }\n}\n";
    }

    private static String buildDefaultPlayerHeadModel(String modelName) {
        return "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n    \"layer0\": \"minecraft:item/"
            + modelName + "\"\n  }\n}\n";
    }

    private static int[] trySampleOpaquePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                if (a > 200) {
                    return new int[]{
                        (argb >> 16) & 0xFF,
                        (argb >> 8) & 0xFF,
                        argb & 0xFF
                    };
                }
            }
        }
        return null;
    }

    private static int[] sampleOpaquePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                if (a > 200) {
                    return new int[]{
                        (argb >> 16) & 0xFF,
                        (argb >> 8) & 0xFF,
                        argb & 0xFF
                    };
                }
            }
        }
        fail("No opaque pixel found in rendered item.");
        return null; // unreachable
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
