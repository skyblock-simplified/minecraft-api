package dev.sbs.minecraftapi.render.hypixel;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.nbt.NbtFactory;
import dev.sbs.minecraftapi.nbt.tags.Tag;
import dev.sbs.minecraftapi.nbt.tags.TagType;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.collection.ListTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.ByteTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.ShortTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Parses Hypixel inventory data from base64-encoded, gzipped NBT format.
 */
@UtilityClass
public final class InventoryParser {

    /**
     * Parses base64-encoded, gzipped NBT inventory data into structured items.
     *
     * @param base64Data the base64 string from Hypixel API
     * @return list of parsed items
     */
    public static @NotNull ConcurrentList<HypixelItemData> parseInventory(@NotNull String base64Data) {
        CompoundTag compound = new NbtFactory().fromBase64(base64Data);
        return extractItems(compound);
    }

    /**
     * Parses already-decoded NBT data into structured items.
     *
     * @param root the root NBT tag (compound or list)
     * @return list of parsed items
     */
    public static @NotNull ConcurrentList<HypixelItemData> parseInventory(@NotNull Tag<?> root) {
        return extractItems(root);
    }

    private static @NotNull ConcurrentList<HypixelItemData> extractItems(@NotNull Tag<?> root) {
        ConcurrentList<HypixelItemData> items = Concurrent.newList();

        // Try to find the item list - Hypixel uses various structures
        ListTag<?> itemList = null;

        if (root instanceof CompoundTag compound) {
            // Common patterns: nested under "i", "items", "inventory", or "data" key
            itemList = compound.getTag("i");
            if (itemList == null) itemList = compound.getTag("items");
            if (itemList == null) itemList = compound.getTag("inventory");
            if (itemList == null) itemList = compound.getTag("data");
        } else if (root instanceof ListTag<?> list) {
            // Root is already a list
            itemList = list;
        }

        if (itemList == null) {
            return items;
        }

        for (Tag<?> element : itemList) {
            if (!(element instanceof CompoundTag itemCompound)) {
                continue;
            }

            HypixelItemData item = parseItem(itemCompound);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    private static @Nullable HypixelItemData parseItem(@NotNull CompoundTag itemCompound) {
        // Skip empty slots
        if (!itemCompound.containsKey("id") && !itemCompound.containsKey("ID")) {
            return null;
        }

        // Extract item ID - can be string (modern) or short (1.8.9 numeric ID)
        String itemIdTag = null;
        if (itemCompound.containsType("id", TagType.STRING)) {
            StringTag idTag = itemCompound.getTagOrDefault("id", StringTag.EMPTY);
            itemIdTag = idTag.getValue();
        } else if (itemCompound.containsType("ID", TagType.STRING)) {
            StringTag idTagAlt = itemCompound.getTagOrDefault("ID", StringTag.EMPTY);
            itemIdTag = idTagAlt.getValue();
        }
        Short numericId = null;

        if (itemIdTag == null) {
            // Try short for 1.8.9 numeric IDs
            if (itemCompound.containsType("id", TagType.SHORT)) {
                ShortTag shortIdTag = itemCompound.getTagOrDefault("id", ShortTag.EMPTY);
                numericId = shortIdTag.getValue();
            } else if (itemCompound.containsType("ID", TagType.SHORT)) {
                ShortTag shortIdTagAlt = itemCompound.getTagOrDefault("ID", ShortTag.EMPTY);
                numericId = shortIdTagAlt.getValue();
            }
            if (numericId == null) {
                return null;
            }

            itemIdTag = Short.toString(numericId);
        }

        // Normalize item ID (1.8.9 uses numeric IDs, newer uses namespaced)
        String itemId = normalizeItemId(itemIdTag, itemCompound);

        // Extract count
        int count = 1;
        if (itemCompound.containsKey("Count")) {
            ByteTag countTag = itemCompound.getTagOrDefault("Count", ByteTag.EMPTY);
            count = Byte.toUnsignedInt(countTag.getValue());
        } else if (itemCompound.containsKey("count")) {
            ByteTag countTagAlt = itemCompound.getTagOrDefault("count", ByteTag.EMPTY);
            count = Byte.toUnsignedInt(countTagAlt.getValue());
        }

        // Extract damage/data value (important for 1.8.9 item variants)
        short damage = 0;
        if (itemCompound.containsKey("Damage")) {
            ShortTag damageTag = itemCompound.getTagOrDefault("Damage", ShortTag.EMPTY);
            damage = damageTag.getValue();
        } else if (itemCompound.containsKey("damage")) {
            ShortTag damageTagAlt = itemCompound.getTagOrDefault("damage", ShortTag.EMPTY);
            damage = damageTagAlt.getValue();
        }

        // Extract tag compound (contains all the rich data)
        CompoundTag tag = itemCompound.containsKey("tag")
            ? itemCompound.getTag("tag")
            : itemCompound.getTag("Tag");

        return new HypixelItemData(itemId, count, damage, tag, numericId);
    }

    private static @NotNull String normalizeItemId(@NotNull String rawId, @NotNull CompoundTag itemCompound) {
        // If it's already a namespaced ID, use it
        if (rawId.contains(":")) {
            return rawId.toLowerCase(Locale.ROOT);
        }

        // For numeric IDs (1.8.9), we need to map to modern namespaced IDs
        try {
            short numericId = Short.parseShort(rawId);

            // Common Skyblock items often have the actual ID in ExtraAttributes
            if (itemCompound.containsPath("tag.ExtraAttributes.id")) {
                StringTag skyblockIdTag = itemCompound.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY);

                if (skyblockIdTag.notEmpty())
                    return HypixelPrefixes.SKYBLOCK + skyblockIdTag.getValue().toLowerCase(Locale.ROOT);
            }

            // Fallback: return numeric ID with prefix for later mapping
            return HypixelPrefixes.NUMERIC + numericId;
        } catch (NumberFormatException ignored) {
            // Not a numeric ID
        }

        // Unknown format, return as-is with minecraft namespace
        return "minecraft:" + rawId.toLowerCase(Locale.ROOT);
    }

}
