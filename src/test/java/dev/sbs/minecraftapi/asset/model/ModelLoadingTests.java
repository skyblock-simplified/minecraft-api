package dev.sbs.minecraftapi.asset.model;

import dev.sbs.minecraftapi.asset.AssetNamespaceRegistry;
import dev.sbs.minecraftapi.asset.MinecraftAssetFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModelLoadingTests {

    private Path tempPath;

    @BeforeEach
    void setUp() throws IOException {
        tempPath = Files.createTempDirectory("MinecraftApiModelTests");
    }

    @AfterEach
    void tearDown() {
        deleteRecursively(tempPath);
    }

    @Test
    void shouldLoadModelFromCustomNamespace() throws IOException {
        Path packPath = tempPath.resolve("TestPack");
        Files.createDirectories(packPath);

        Path assetsPath = packPath.resolve("assets");
        Files.createDirectories(assetsPath);

        Path minecraftPath = assetsPath.resolve("minecraft");
        Files.createDirectories(minecraftPath);

        String customNamespace = "cittofirmgenerated";
        Path customNamespacePath = assetsPath.resolve(customNamespace);
        Path customModelPath = customNamespacePath.resolve("models").resolve("item");
        Files.createDirectories(customModelPath);

        String modelJson = """
            {
                "textures": {
                    "0": "minecraft:block/dirt"
                },
                "elements": [
                    {
                        "from": [0, 0, 0],
                        "to": [16, 16, 16],
                        "faces": {
                            "north": {"uv": [0, 0, 16, 16], "texture": "#0"}
                        }
                    }
                ]
            }
            """;
        Files.writeString(customModelPath.resolve("custom_model.json"), modelJson);

        AssetNamespaceRegistry registry = new AssetNamespaceRegistry();
        registry.addNamespace("minecraft", minecraftPath.toString(), "test_pack", false);
        registry.addNamespace(customNamespace, customNamespacePath.toString(), "test_pack", false);

        Path baseAssetsPath = tempPath.resolve("BaseAssets");
        Files.createDirectories(baseAssetsPath.resolve("assets").resolve("minecraft"));

        Map<String, BlockModel> defs = MinecraftAssetFactory.loadModelDefinitions(baseAssetsPath.toString(), null, registry);

        BlockModel model = MinecraftAssetFactory.resolveAllModels(defs).get(BlockModel.normalizeName("cittofirmgenerated:item/custom_model"));

        assertNotNull(model);
        assertEquals(1, model.getElements().size());
        assertEquals("cittofirmgenerated:item/custom_model", model.getName());
    }

    @Test
    void shouldLoadModelFromNestedFolderStructure() throws IOException {
        Path packPath = tempPath.resolve("TestPackNested");
        Files.createDirectories(packPath);

        Path assetsPath = packPath.resolve("assets");
        Files.createDirectories(assetsPath);

        Path minecraftPath = assetsPath.resolve("minecraft");
        Files.createDirectories(minecraftPath);

        String customNamespace = "cittofirmgenerated";
        Path customNamespacePath = assetsPath.resolve(customNamespace);
        Path customModelPath = customNamespacePath.resolve("models").resolve("item").resolve("helmet_icon");
        Files.createDirectories(customModelPath);

        String modelJson = """
            {
                "textures": {
                    "0": "minecraft:block/gold_block"
                },
                "elements": [
                    {
                        "from": [0, 0, 0],
                        "to": [16, 16, 16],
                        "faces": {
                            "north": {"uv": [0, 0, 16, 16], "texture": "#0"}
                        }
                    }
                ]
            }
            """;
        Files.writeString(customModelPath.resolve("crown_of_avarice.json"), modelJson);

        AssetNamespaceRegistry registry = new AssetNamespaceRegistry();
        registry.addNamespace("minecraft", minecraftPath.toString(), "test_pack_nested", false);
        registry.addNamespace(customNamespace, customNamespacePath.toString(), "test_pack_nested", false);

        Path baseAssetsPath = tempPath.resolve("BaseAssetsNested");
        Files.createDirectories(baseAssetsPath.resolve("assets").resolve("minecraft"));

        Map<String, BlockModel> defs = MinecraftAssetFactory.loadModelDefinitions(baseAssetsPath.toString(), null, registry);

        BlockModel model = MinecraftAssetFactory.resolveAllModels(defs).get(BlockModel.normalizeName("cittofirmgenerated:item/helmet_icon/crown_of_avarice"));

        assertNotNull(model);
        assertEquals(1, model.getElements().size());
        assertEquals("cittofirmgenerated:item/helmet_icon/crown_of_avarice", model.getName());
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path))
            return;
        try {
            Files.walk(path)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
        } catch (IOException ignored) {
        }
    }
}
