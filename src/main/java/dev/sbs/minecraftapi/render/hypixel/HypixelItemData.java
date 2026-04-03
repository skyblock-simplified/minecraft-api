package dev.sbs.minecraftapi.render.hypixel;

import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parsed item data from Hypixel inventories (1.8.9 format).
 * <p>
 * Designed to be versatile and map cleanly to texture lookups.
 */
@Getter
@AllArgsConstructor
public final class HypixelItemData {

    private final @NotNull String itemId;
    private final int count;
    private final short damage;
    private final @Nullable CompoundTag tag;
    private final @Nullable Short numericId;

    /**
     * Constructs a new {@code HypixelItemData} with sensible defaults.
     *
     * @param itemId the normalized item identifier
     */
    public HypixelItemData(@NotNull String itemId) {
        this(itemId, 1, (short) 0, null, null);
    }

    /**
     * The Skyblock ID extracted from {@code ExtraAttributes.id}, or {@code null} if not present.
     */
    public @Nullable String getSkyblockId() {
        if (tag == null || !tag.containsPath("ExtraAttributes.id")) return null;
        return tag.<StringTag>getPath("ExtraAttributes.id").getValue();
    }

    /**
     * Custom texture data extracted from {@code ExtraAttributes.customData}, or {@code null} if not present.
     */
    public @Nullable CompoundTag getCustomData() {
        if (tag == null) return null;
        return tag.getPath("ExtraAttributes.customData");
    }

    /**
     * Gem data extracted from {@code ExtraAttributes.gems}, or {@code null} if not present.
     */
    public @Nullable CompoundTag getGems() {
        if (tag == null) return null;
        return tag.getPath("ExtraAttributes.gems");
    }

    /**
     * Enchantment data extracted from {@code ExtraAttributes.enchantments}, or {@code null} if not present.
     */
    public @Nullable CompoundTag getEnchantments() {
        if (tag == null) return null;
        return tag.getPath("ExtraAttributes.enchantments");
    }

    /**
     * Attribute data extracted from {@code ExtraAttributes.attributes}, or {@code null} if not present.
     */
    public @Nullable CompoundTag getAttributes() {
        if (tag == null) return null;
        return tag.getPath("ExtraAttributes.attributes");
    }

    /**
     * The display name from the item tag (may include formatting codes), or {@code null} if not present.
     */
    public @Nullable String getDisplayName() {
        if (tag == null || !tag.containsPath("display.Name")) return null;
        return tag.<StringTag>getPath("display.Name").getValue();
    }

}
