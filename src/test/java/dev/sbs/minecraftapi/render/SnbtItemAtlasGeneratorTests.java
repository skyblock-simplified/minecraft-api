package dev.sbs.minecraftapi.render;

import dev.sbs.minecraftapi.nbt.NbtFactory;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.ItemRenderData;
import dev.sbs.minecraftapi.render.context.RenderContext;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
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

import static org.junit.jupiter.api.Assertions.*;

class SnbtItemAtlasGeneratorTests extends IntegrationTestBase {

    private Path tempRoot;

    @BeforeEach
    void setUp() throws IOException {
        tempRoot = Files.createTempDirectory("MinecraftRenderer_SnbtAtlas");
    }

    @AfterEach
    void tearDown() {
        deleteRecursive(tempRoot);
    }

    @Test
    void renderedAtlasUsesTexturePackForCustomData() throws Exception {
        String assetsDir = getAssetsDirectory();
        String packId = "snbt_custom_head_pack";
        Path packRoot = createCustomHeadPack(packId, new Color(0xD4, 0x34, 0x2C));

        registerPacks(Concurrent.newUnmodifiableList(packRoot.toString()));

        RenderContext context = createRenderContext(
            assetsDir, Concurrent.newList(packId));

        // Verify direct render with custom data uses the pack texture
        CompoundTag customData = new CompoundTag();
        customData.put("id", "custom_head_test");

        ItemRenderData itemData = new ItemRenderData(
            null, null, false, customData, null);

        BlockRenderOptions directOptions = BlockRenderOptions.builder()
            .withSize(64)
            .withPackIds(Concurrent.newUnmodifiableList(packId))
            .build();

        BufferedImage directRender = new ItemRenderer(context, "player_head", itemData, directOptions).render();
        int[] directPixel = sampleOpaquePixel(directRender);
        assertEquals(0xD4, directPixel[0]);
        assertEquals(0x34, directPixel[1]);
        assertEquals(0x2C, directPixel[2]);

        // Test SNBT atlas generation
        String snbtPayload = """
            {
                components: {
                    "minecraft:custom_data": {
                        id: "custom_head_test"
                    }
                },
                id: "minecraft:player_head"
            }
            """;

        CompoundTag rootCompound = new NbtFactory().fromSnbt(snbtPayload);

        AtlasGenerator.SnbtItemEntry entry = new AtlasGenerator.SnbtItemEntry(
            "custom_head",
            tempRoot.resolve("custom_head.snbt").toString(),
            rootCompound,
            null);

        ConcurrentList<AtlasGenerator.AtlasView> views = Concurrent.newUnmodifiableList(
            new AtlasGenerator.AtlasView("front", BlockRenderOptions.builder()
                .withSize(64)
                .withPackIds(Concurrent.newUnmodifiableList(packId))
                .build())
        );

        Path outputDirectory = tempRoot.resolve("output");
        ConcurrentList<AtlasGenerator.AtlasResult> results = AtlasGenerator.generateSnbtAtlases(
            context,
            outputDirectory.toString(),
            views,
            32, 1, 1,
            Concurrent.newUnmodifiableList(entry)
        );

        assertEquals(1, results.size());
        AtlasGenerator.AtlasResult atlas = results.get(0);

        BufferedImage atlasImage = ImageIO.read(Path.of(atlas.getImagePath()).toFile());
        int[] pixel = sampleOpaquePixel(atlasImage);
        assertEquals(0xD4, pixel[0]);
        assertEquals(0x34, pixel[1]);
        assertEquals(0x2C, pixel[2]);

        assertTrue(Files.exists(Path.of(atlas.getManifestPath())));

        context.close();
    }

    // ---- Helpers ----

    private Path createCustomHeadPack(String id, Color color) throws IOException {
        Path packRoot = tempRoot.resolve(id);
        Files.createDirectories(packRoot);

        Files.writeString(packRoot.resolve("meta.json"),
            "{\n  \"id\": \"%s\",\n  \"name\": \"%s\",\n  \"version\": \"1.0.0\",\n  \"description\": \"Test pack\",\n  \"authors\": [\"tests\"]\n}\n"
                .formatted(id, id));
        Files.writeString(packRoot.resolve("pack.mcmeta"),
            "{\"pack\": {\"pack_format\": 32, \"description\": \"Test\"}}\n");

        Path itemsDir = packRoot.resolve("assets").resolve("minecraft").resolve("items");
        Files.createDirectories(itemsDir);
        Files.writeString(itemsDir.resolve("player_head.json"),
            "{\n  \"model\": {\n    \"type\": \"condition\",\n    \"property\": \"component\",\n    \"predicate\": \"custom_data\",\n    \"value\": { \"id\": \"custom_head_test\" },\n    \"on_true\": {\n      \"type\": \"model\",\n      \"model\": \"minecraft:item/custom_player_head\"\n    },\n    \"on_false\": {\n      \"type\": \"model\",\n      \"model\": \"minecraft:item/player_head\"\n    }\n  }\n}\n");

        Path modelsDir = packRoot.resolve("assets").resolve("minecraft").resolve("models").resolve("item");
        Files.createDirectories(modelsDir);
        Files.writeString(modelsDir.resolve("custom_player_head.json"),
            "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n    \"layer0\": \"minecraft:item/custom_player_head\"\n  }\n}\n");

        Path texturesDir = packRoot.resolve("assets").resolve("minecraft").resolve("textures").resolve("item");
        Files.createDirectories(texturesDir);
        writeSolidColorPng(texturesDir.resolve("custom_player_head.png"), 16, 16, color);

        return packRoot;
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
        fail("No opaque pixel found in rendered image.");
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
                    walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
                }
            }
        } catch (IOException ignored) {}
    }
}
