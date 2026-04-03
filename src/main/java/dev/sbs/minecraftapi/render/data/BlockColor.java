package dev.sbs.minecraftapi.render.data;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The sixteen standard Minecraft dye colors used by colored blocks such as wool, concrete,
 * terracotta, stained glass, beds, candles, and shulker boxes.
 *
 * <p>Each constant stores the default RGB tint applied when a block or texture name begins
 * with that color's prefix.
 */
@Getter
public enum BlockColor {

    WHITE(249, 255, 254),
    ORANGE(249, 128, 29),
    MAGENTA(199, 78, 189),
    LIGHT_BLUE(58, 179, 218),
    YELLOW(254, 216, 61),
    LIME(128, 199, 31),
    PINK(243, 139, 170),
    GRAY(71, 79, 82),
    LIGHT_GRAY(157, 157, 151),
    CYAN(22, 156, 156),
    PURPLE(137, 50, 184),
    BLUE(60, 68, 170),
    BROWN(131, 84, 50),
    GREEN(94, 124, 22),
    RED(176, 46, 38),
    BLACK(29, 29, 33);

    /**
     * Blend factor applied when tinting a texture with a block color.
     */
    public static final float TINT_BLEND = 0.82f;

    private static final BlockColor[] VALUES = values();

    private final int red;
    private final int green;
    private final int blue;
    private final @NotNull String prefix;
    private final int argb;

    BlockColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.prefix = name().toLowerCase();
        this.argb = (0xFF << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /**
     * Returns the RGB components as a three-element array.
     *
     * @return array of [r, g, b]
     */
    public int @NotNull [] getRgb() {
        return new int[]{ red, green, blue };
    }

    /**
     * Finds the block color whose prefix matches the start of the given name.
     *
     * @param name the normalized block or texture name to match
     * @return the matching block color, or null if none matches
     */
    public static @Nullable BlockColor fromBlockName(@NotNull String name) {
        String lower = name.toLowerCase();
        for (BlockColor color : VALUES) {
            if (lower.startsWith(color.prefix)) {
                return color;
            }
        }
        return null;
    }

    /**
     * Packs an RGB triplet into an ARGB int (fully opaque).
     *
     * @param rgb array of [r, g, b]
     * @return the packed ARGB int
     */
    public static int packArgb(int @NotNull [] rgb) {
        return (0xFF << 24) | ((rgb[0] & 0xFF) << 16) | ((rgb[1] & 0xFF) << 8) | (rgb[2] & 0xFF);
    }

    /**
     * Converts an ARGB int to an RGB array.
     *
     * @param argb the packed ARGB int
     * @return array of [r, g, b]
     */
    public static int @NotNull [] unpackRgb(int argb) {
        return new int[]{(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
    }

    /**
     * Converts an RGB int to an RGB array.
     *
     * @param rgb the packed RGB int
     * @return array of [r, g, b]
     */
    public static int @NotNull [] colorFromRgb(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return new int[]{r, g, b};
    }
}
