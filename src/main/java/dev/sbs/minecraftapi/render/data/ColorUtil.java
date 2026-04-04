package dev.sbs.minecraftapi.render.data;

import dev.sbs.minecraftapi.asset.namespace.Namespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Color tinting coordination, resource key normalization, and texture classification.
 */
public final class ColorUtil {

    /**
     * Strength factor for constant tint application.
     */
    public static final float CONSTANT_TINT_STRENGTH = 1.45f;

    private ColorUtil() {
    }

    /**
     * Returns the color for a block name by checking constant colors and block color prefixes.
     *
     * @param blockName the block or texture name
     * @return an RGB triplet, or null if no color applies
     */
    public static @Nullable int[] getColorFromBlockName(@Nullable String blockName) {
        if (blockName == null || blockName.isBlank())
            return null;

        String name = normalizeResourceKey(blockName);
        if (name.endsWith("bundle") || name.contains("_bundle"))
            return null;

        int[] constantColor = BiomeTint.getConstantColor(name);
        if (constantColor != null)
            return constantColor;

        BlockColor blockColor = BlockColor.fromBlockName(name);
        return blockColor != null ? blockColor.getRgb() : null;
    }

    /**
     * Returns the constant tint color for a texture or block name.
     *
     * @param textureId the texture identifier
     * @param blockName the optional block name
     * @return an RGB triplet, or null if no constant tint applies
     */
    public static int @Nullable [] tryGetConstantTint(String textureId, @Nullable String blockName) {
        int[] color = BiomeTint.getConstantColor(normalizeResourceKey(textureId));
        if (color != null)
            return color;

        return BiomeTint.getConstantColor(normalizeResourceKey(blockName));
    }

    /**
     * Returns the biome tint kind for a texture or block name.
     *
     * @param textureId the texture identifier
     * @param blockName the optional block name
     * @return the biome tint kind, or null if none applies
     */
    public static @Nullable BiomeTint tryGetBiomeTint(String textureId, @Nullable String blockName) {
        String textureKey = normalizeResourceKey(textureId);
        String blockKey = normalizeResourceKey(blockName);

        if (isLikelyItemTexture(textureId)
            && (BiomeTint.isItemTintExcluded(textureKey) || BiomeTint.isItemTintExcluded(blockKey)))
            return null;

        return BiomeTint.resolve(textureKey, blockKey);
    }

    /**
     * Normalizes a resource key by stripping namespace, path prefixes, and state brackets.
     *
     * @param identifier the raw resource identifier
     * @return the normalized, lowercase base name
     */
    public static @NotNull String normalizeResourceKey(@Nullable String identifier) {
        if (identifier == null || identifier.isBlank())
            return "";

        String normalized = identifier.replace('\\', '/').trim();
        if (normalized.startsWith("#"))
            return "";

        int stateSeparator = normalized.indexOf('[');
        if (stateSeparator >= 0)
            normalized = normalized.substring(0, stateSeparator);

        normalized = Namespace.of(normalized).path();
        if (normalized.toLowerCase().startsWith("textures/"))
            normalized = normalized.substring(9);
        if (normalized.toLowerCase().startsWith("models/"))
            normalized = normalized.substring(7);
        if (normalized.toLowerCase().startsWith("block/"))
            normalized = normalized.substring(6);
        else if (normalized.toLowerCase().startsWith("blocks/"))
            normalized = normalized.substring(7);
        else if (normalized.toLowerCase().startsWith("item/"))
            normalized = normalized.substring(5);
        else if (normalized.toLowerCase().startsWith("items/"))
            normalized = normalized.substring(6);

        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);

        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0)
            normalized = normalized.substring(slashIndex + 1);

        return normalized.toLowerCase();
    }

    /**
     * Checks whether a texture identifier likely refers to an item texture.
     *
     * @param identifier the texture identifier
     * @return true if the identifier appears to be an item texture
     */
    public static boolean isLikelyItemTexture(@Nullable String identifier) {
        if (identifier == null || identifier.isBlank())
            return false;

        String normalized = identifier.replace('\\', '/');
        if (normalized.toLowerCase().startsWith("item/") || normalized.toLowerCase().startsWith("items/"))
            return true;
        if (normalized.toLowerCase().startsWith("textures/item/"))
            return true;
        String lower = normalized.toLowerCase();
        return lower.contains("/item/") || lower.contains(":item/")
            || lower.contains("/items/") || lower.contains(":items/");
    }
}
