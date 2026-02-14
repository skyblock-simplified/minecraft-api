package dev.sbs.minecraftapi.builder.generator.font;

import dev.sbs.annotation.ResourcePath;
import dev.sbs.minecraftapi.builder.text.segment.ColorSegment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public enum MinecraftFont implements Font {

    PLAIN("Minecraft.otf", Style.PLAIN, 15.5f),
    BOLD("Minecraft-Bold.otf", Style.BOLD, 20.0f),
    ITALIC("Minecraft-Italic.otf", Style.ITALIC, 20.5f),
    BOLD_ITALIC("Minecraft-BoldItalic.otf", Style.BOLD_ITALIC, 20.5f),
    SANS_SERIF(new java.awt.Font("", java.awt.Font.PLAIN, 20));

    private final @NotNull java.awt.Font actual;
    private final @NotNull Style style;
    private final float size;

    MinecraftFont(@ResourcePath(base = "minecraft/fonts") @NotNull String fileName, @NotNull Style style, float size) {
        this(Font.initFont(String.format("minecraft/fonts/%s", fileName), size), style, size);
    }

    MinecraftFont(@NotNull java.awt.Font font) {
        this(font, Style.of(font.getStyle()), font.getSize2D());
    }

    /**
     * Retrieves the {@link MinecraftFont} associated with the given {@link Style}.
     * <p>
     * If no matching font is found, the default {@link MinecraftFont#PLAIN} is returned.
     *
     * @param style the {@link Style} to retrieve the corresponding {@link MinecraftFont} for
     * @return the {@link MinecraftFont} corresponding to the given {@link Style}, or {@link MinecraftFont#PLAIN} if none is found
     */
    public static @NotNull MinecraftFont of(@NotNull Style style) {
        for (MinecraftFont font : values()) {
            if (font.getStyle() == style)
                return font;
        }

        return PLAIN;
    }

    /**
     * Retrieves the {@link MinecraftFont} that corresponds to the style described by the given {@link ColorSegment}.
     * <p>
     * If no matching font is found, the default {@link MinecraftFont#PLAIN} is returned.
     *
     * @param segment the {@link ColorSegment} containing the style information (bold and/or italic) to match
     * @return the {@link MinecraftFont} matching the given style, or {@link MinecraftFont#PLAIN} if no match is found
     */
    public static @NotNull MinecraftFont of(@NotNull ColorSegment segment) {
        for (MinecraftFont font : values()) {
            if (font.getStyle().getId() == (segment.isBold() ? 1 : 0) + (segment.isItalic() ? 2 : 0))
                return font;
        }

        return PLAIN;
    }

}
