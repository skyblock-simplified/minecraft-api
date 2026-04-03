package dev.sbs.minecraftapi.render.hypixel;

import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HypixelInventoryParserTests {

    @Test
    void canParseInventoryData() throws IOException {
        String base64Data = loadResource("/inventory_base64.txt");

        List<HypixelItemData> items = InventoryParser.parseInventory(base64Data);

        assertFalse(items.isEmpty(), "Should parse at least one item");

        for (HypixelItemData item : items.subList(0, Math.min(5, items.size()))) {
            assertNotNull(item.getItemId());
            System.out.printf("Item: %s, Count: %d, Damage: %d%n", item.getItemId(), item.getCount(), item.getDamage());
            if (item.getSkyblockId() != null) {
                System.out.printf("  Skyblock ID: %s%n", item.getSkyblockId());
            }
            if (item.getDisplayName() != null) {
                System.out.printf("  Display Name: %s%n", item.getDisplayName());
            }
        }
    }

    @Test
    void extractsItemMetadata() throws IOException {
        String base64Data = loadInventoryData();
        List<HypixelItemData> items = InventoryParser.parseInventory(base64Data);

        List<HypixelItemData> skyblockItems = items.stream()
            .filter(i -> i.getSkyblockId() != null)
            .toList();
        assertFalse(skyblockItems.isEmpty(), "Should have at least one item with Skyblock ID");

        for (HypixelItemData item : skyblockItems.subList(0, Math.min(3, skyblockItems.size()))) {
            System.out.printf("%nItem: %s%n", item.getSkyblockId());
            System.out.printf("  Minecraft ID: %s%n", item.getItemId());
            System.out.printf("  Display: %s%n", item.getDisplayName());
            System.out.printf("  Texture ID: %s%n", TextureResolver.getTextureId(item));

            if (item.getEnchantments() != null) {
                System.out.printf("  Enchantments: %d found%n", item.getEnchantments().size());
            }
            if (item.getGems() != null) {
                System.out.printf("  Gems: %d found%n", item.getGems().size());
            }
            if (item.getAttributes() != null) {
                System.out.printf("  Attributes: %d found%n", item.getAttributes().size());
            }
        }
    }

    @Test
    void generatesConsistentTextureIds() throws IOException {
        String base64Data = loadInventoryData();
        List<HypixelItemData> items = InventoryParser.parseInventory(base64Data);

        List<String> textureIds = items.stream()
            .map(TextureResolver::getTextureId)
            .toList();
        assertFalse(textureIds.isEmpty());

        List<HypixelItemData> items2 = InventoryParser.parseInventory(base64Data);
        List<String> textureIds2 = items2.stream()
            .map(TextureResolver::getTextureId)
            .toList();

        assertEquals(textureIds, textureIds2);

        System.out.printf("Generated %d texture IDs%n", textureIds.stream().distinct().count());
        textureIds.stream().distinct().forEach(id -> System.out.printf("  %s%n", id));
    }

    @Test
    void skyblockTextureIdIncludesFallbackMetadata() {
        CompoundTag extraAttributes = new CompoundTag();
        extraAttributes.put("id", "FUNGI_CUTTER");

        CompoundTag tag = new CompoundTag();
        tag.put("ExtraAttributes", extraAttributes);

        HypixelItemData item = new HypixelItemData("minecraft:golden_hoe", 1, (short) 0, tag, (short) 294);
        String textureId = TextureResolver.getTextureId(item);

        String descriptor = TextureResolver.tryDecodeTextureId(textureId);
        assertNotNull(descriptor);
        assertTrue(descriptor.toLowerCase().startsWith(HypixelPrefixes.SKYBLOCK + "fungi_cutter"),
            "Expected descriptor to start with '" + HypixelPrefixes.SKYBLOCK + "fungi_cutter' but was '" + descriptor + "'");
        assertTrue(descriptor.toLowerCase().contains("base=minecraft:golden_hoe"),
            "Expected descriptor to contain 'base=minecraft:golden_hoe' but was '" + descriptor + "'");
        assertTrue(descriptor.toLowerCase().contains("numeric=294"),
            "Expected descriptor to contain 'numeric=294' but was '" + descriptor + "'");
    }

    @Test
    void skyblockTextureIdNormalizesNumericItemId() {
        CompoundTag extraAttributes = new CompoundTag();
        extraAttributes.put("id", "FUNGI_CUTTER");

        CompoundTag tag = new CompoundTag();
        tag.put("ExtraAttributes", extraAttributes);

        HypixelItemData item = new HypixelItemData(HypixelPrefixes.NUMERIC + "293", 1, (short) 0, tag, null);
        String textureId = TextureResolver.getTextureId(item);

        String descriptor = TextureResolver.tryDecodeTextureId(textureId);
        assertNotNull(descriptor);
        assertTrue(descriptor.toLowerCase().contains("base=minecraft:diamond_hoe"),
            "Expected descriptor to contain 'base=minecraft:diamond_hoe' but was '" + descriptor + "'");
    }

    private static String loadResource(String path) throws IOException {
        try (InputStream is = HypixelInventoryParserTests.class.getResourceAsStream(path)) {
            assertNotNull(is, "Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private static String loadInventoryData() throws IOException {
        return loadResource("/inventory_data.txt");
    }
}
