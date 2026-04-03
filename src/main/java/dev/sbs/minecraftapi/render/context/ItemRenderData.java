package dev.sbs.minecraftapi.render.context;

import dev.sbs.minecraftapi.nbt.tags.Tag;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.ByteTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.NumericalTag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Render data for item-specific tinting, custom data, and profile information.
 */
@Getter
@AllArgsConstructor
public final class ItemRenderData {

    private final @Nullable int[] layer0Tint;
    private final @Nullable Map<Integer, int[]> additionalLayerTints;
    private final boolean disableDefaultLayer0Tint;
    private final @Nullable CompoundTag customData;
    private final @Nullable CompoundTag profile;

    public ItemRenderData() {
        this(null, null, false, null, null);
    }

    /**
     * Returns the tint for a specific layer index, checking additional tints first, then layer0.
     *
     * @param layerIndex the layer index
     * @return the tint as an RGB int array [r, g, b], or null if no tint
     */
    public @Nullable int[] getLayerTint(int layerIndex) {
        if (additionalLayerTints != null && additionalLayerTints.containsKey(layerIndex))
            return additionalLayerTints.get(layerIndex);
        if (layerIndex == 0 && layer0Tint != null)
            return layer0Tint;
        return null;
    }

    /**
     * Checks whether this data has any non-default properties.
     *
     * @return true if all properties are at their defaults
     */
    public boolean isDefault() {
        return layer0Tint == null
            && (additionalLayerTints == null || additionalLayerTints.isEmpty())
            && !disableDefaultLayer0Tint
            && customData == null
            && profile == null;
    }

    /** Creates a copy with the custom data replaced. */
    public @NotNull ItemRenderData withCustomData(@Nullable CompoundTag customData) {
        return new ItemRenderData(layer0Tint, additionalLayerTints, disableDefaultLayer0Tint,
            customData, profile);
    }

    /** Creates a copy with the profile replaced. */
    public @NotNull ItemRenderData withProfile(@Nullable CompoundTag profile) {
        return new ItemRenderData(layer0Tint, additionalLayerTints, disableDefaultLayer0Tint,
            customData, profile);
    }

    // ----------------------------------------------------------------
    // NBT extraction factory methods
    // ----------------------------------------------------------------

    /**
     * Extracts item render data from an NBT compound's components.
     *
     * @param root the root NBT compound
     * @return item render data, or null if none found
     */
    public static @Nullable ItemRenderData fromComponents(@NotNull CompoundTag root) {
        CompoundTag components = resolveComponentsCompound(root);
        if (components == null)
            return null;

        int[] layer0Tint = null;
        boolean disableDefaultLayer0Tint = false;
        Map<Integer, int[]> additionalLayerTints = null;
        CompoundTag customData = null;
        CompoundTag profile = null;

        Tag<?> dyedTag = components.get("minecraft:dyed_color");
        if (dyedTag != null) {
            int[] dyedColor = tryExtractColor(dyedTag);
            if (dyedColor != null)
                layer0Tint = dyedColor;
        }

        Tag<?> customDataTag = components.get("minecraft:custom_data");
        if (customDataTag instanceof CompoundTag customCompound && customCompound.size() > 0)
            customData = customCompound;

        Tag<?> profileTag = components.get("minecraft:profile");
        if (profileTag instanceof CompoundTag profileCompound && profileCompound.size() > 0)
            profile = profileCompound;

        if (layer0Tint != null || (additionalLayerTints != null && !additionalLayerTints.isEmpty())
            || disableDefaultLayer0Tint || customData != null || profile != null)
            return new ItemRenderData(layer0Tint, additionalLayerTints, disableDefaultLayer0Tint,
                customData, profile);

        return null;
    }

    /**
     * Extracts item render data from an NBT compound (alias for {@link #fromComponents}).
     *
     * @param compound the root NBT compound
     * @return item render data, or null if none found
     */
    public static @Nullable ItemRenderData fromNbt(@NotNull CompoundTag compound) {
        return fromComponents(compound);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private static @Nullable CompoundTag resolveComponentsCompound(CompoundTag root) {
        Tag<?> componentsTag = root.get("components");
        if (componentsTag instanceof CompoundTag components)
            return components;

        Tag<?> legacyTag = root.get("tag");
        if (legacyTag instanceof CompoundTag legacyCompound) {
            CompoundTag nested = resolveComponentsCompound(legacyCompound);
            if (nested != null)
                return nested;
        }

        return null;
    }

    /**
     * Attempts to extract an RGB color from a tag.
     *
     * @param tag the NBT tag
     * @return an RGB triplet, or null if extraction fails
     */
    public static @Nullable int[] tryExtractColor(Tag<?> tag) {
        if (tag instanceof NumericalTag<?> n)
            return colorFromRgb(n.intValue());
        if (tag instanceof CompoundTag compound) {
            if (compound.containsKey("rgb")) {
                int[] c = tryExtractColor(compound.get("rgb"));
                if (c != null) return c;
            }
            if (compound.containsKey("value")) {
                int[] c = tryExtractColor(compound.get("value"));
                if (c != null) return c;
            }
            if (compound.containsKey("color")) {
                int[] c = tryExtractColor(compound.get("color"));
                if (c != null) return c;
            }
            return tryExtractChannelColor(compound);
        }
        return null;
    }

    private static @Nullable int[] tryExtractChannelColor(CompoundTag compound) {
        Byte r = tryGetByte(compound, "red");
        Byte g = tryGetByte(compound, "green");
        Byte b = tryGetByte(compound, "blue");
        if (r != null && g != null && b != null)
            return new int[]{r & 0xFF, g & 0xFF, b & 0xFF};

        r = tryGetByte(compound, "r");
        g = tryGetByte(compound, "g");
        b = tryGetByte(compound, "b");
        if (r != null && g != null && b != null)
            return new int[]{r & 0xFF, g & 0xFF, b & 0xFF};

        return null;
    }

    private static @Nullable Byte tryGetByte(CompoundTag compound, String key) {
        if (!compound.containsKey(key)) return null;
        return tryGetByteFromTag(compound.get(key));
    }

    private static @Nullable Byte tryGetByteFromTag(Tag<?> tag) {
        if (!(tag instanceof NumericalTag<?> n)) return null;
        if (tag instanceof ByteTag)
            return n.byteValue();
        int v = n.intValue();
        if (v >= 0 && v <= 255)
            return (byte) v;
        return null;
    }

    private static int[] colorFromRgb(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return new int[]{r, g, b};
    }
}
