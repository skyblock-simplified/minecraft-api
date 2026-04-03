package dev.sbs.minecraftapi.render.hypixel;

import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Resolves Hypixel Skyblock items to texture identifiers for rendering.
 */
public final class TextureResolver {

    private TextureResolver() {
    }

    /**
     * Returns a deterministic texture ID for a Hypixel item that can be used for texture pack lookups.
     * <p>
     * The raw descriptor is Base64-encoded with URL and filesystem safe characters.
     *
     * @param item the parsed Hypixel item data
     * @return a string identifier suitable for texture lookup or caching
     */
    public static @NotNull String getTextureId(@NotNull HypixelItemData item) {
        String id = buildRawString(item);

        // Base64 encode to shorten length and ensure filesystem safety
        byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder()
            .encodeToString(bytes)
            .replace('/', '_')  // URL and filesystem safe
            .replace('+', '-')  // URL and filesystem safe
            .replaceAll("=+$", ""); // Remove padding for brevity
    }

    /**
     * Decodes a texture identifier produced by {@link #getTextureId} back to its raw descriptor string.
     *
     * @param textureId the encoded texture identifier
     * @return the decoded descriptor string
     * @throws IllegalArgumentException if the texture id is null, blank, or not a valid encoded identifier
     */
    static @NotNull String decodeTextureId(@NotNull String textureId) {
        if (textureId.isBlank()) {
            throw new IllegalArgumentException("Texture id must not be blank");
        }
        String decoded = tryDecodeTextureId(textureId);
        if (decoded == null) {
            throw new IllegalArgumentException("Texture id '%s' is not a valid encoded identifier".formatted(textureId));
        }
        return decoded;
    }

    /**
     * Tries to decode an encoded texture identifier.
     *
     * @param textureId the encoded texture identifier
     * @return the decoded descriptor, or {@code null} when the identifier could not be decoded
     */
    public static @Nullable String tryDecodeTextureId(@Nullable String textureId) {
        if (textureId == null || textureId.isBlank()) {
            return null;
        }

        String base64 = textureId
            .replace('-', '+')
            .replace('_', '/');

        int padding = (4 - base64.length() % 4) % 4;
        if (padding > 0) {
            base64 = base64 + "=".repeat(padding);
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extracts a potential texture path from CustomData if present (Firmament format).
     *
     * @param item the parsed Hypixel item data
     * @return the texture path string, or {@code null} if not present
     */
    static @Nullable String getCustomTexturePath(@NotNull HypixelItemData item) {
        CompoundTag customData = item.getCustomData();
        if (customData == null || !customData.containsKey("texture")) return null;
        StringTag textureTag = customData.getTag("texture");
        return textureTag.getValue();
    }

    // -- Internal --

    private static @NotNull String buildRawString(@NotNull HypixelItemData item) {
        CompoundTag customData = item.getCustomData();
        if (customData != null && customData.containsKey("texture")) {
            // Check for Firmament's texture override
            StringTag textureTag = customData.getTag("texture");
            if (textureTag.notEmpty())
                return "custom:" + textureTag.getValue();
        }

        String skyblockId = item.getSkyblockId();
        if (skyblockId != null) {
            String descriptor = HypixelPrefixes.SKYBLOCK + skyblockId.toLowerCase(Locale.ROOT);
            String query = buildSkyblockQuery(item);
            return query.isEmpty() ? descriptor : descriptor + "?" + query;
        }

        // Fallback to Minecraft ID with damage for 1.8.9 items
        return item.getDamage() != 0
            ? item.getItemId() + ":" + item.getDamage()
            : item.getItemId();
    }

    private static @NotNull String buildSkyblockQuery(@NotNull HypixelItemData item) {
        List<String> parameters = new ArrayList<>();

        CompoundTag attributes = item.getAttributes();
        if (attributes != null && attributes.size() > 0) {
            TreeSet<String> sortedKeys = new TreeSet<>(attributes.keySet());
            String attrs = String.join(",", sortedKeys);
            if (!attrs.isEmpty()) {
                parameters.add("attrs=" + attrs);
            }
        }

        String fallbackBase = determineSkyblockFallbackBase(item);
        if (fallbackBase != null && !fallbackBase.isBlank()) {
            parameters.add("base=" + fallbackBase);
        }

        if (item.getNumericId() != null) {
            parameters.add("numeric=" + item.getNumericId());
        }

        return String.join("&", parameters);
    }

    private static @Nullable String determineSkyblockFallbackBase(@NotNull HypixelItemData item) {
        if (item.getNumericId() != null) {
            String mapped = LegacyItemMappings.tryMapNumericId(item.getNumericId(), item.getDamage());
            if (mapped != null && !mapped.isBlank()) {
                return mapped;
            }
        }

        String itemId = item.getItemId();
        if (itemId != null && !itemId.isBlank()) {
            if (itemId.toLowerCase(Locale.ROOT).startsWith(HypixelPrefixes.NUMERIC.toLowerCase(Locale.ROOT))) {
                String numericPart = itemId.substring(HypixelPrefixes.NUMERIC.length());
                try {
                    short numericBase = Short.parseShort(numericPart);
                    String mapped = LegacyItemMappings.tryMapNumericId(numericBase, item.getDamage());
                    if (mapped != null && !mapped.isBlank()) {
                        return mapped;
                    }
                } catch (NumberFormatException ignored) {
                    // Not a valid numeric ID
                }
            }

            return itemId;
        }

        return null;
    }

}
