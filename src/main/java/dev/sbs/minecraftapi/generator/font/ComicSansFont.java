package dev.sbs.minecraftapi.generator.font;

import dev.sbs.annotation.ResourcePath;
import dev.sbs.minecraftapi.generator.text.segment.ColorSegment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public enum ComicSansFont implements Font {

    PLAIN("ComicSans.ttf", Style.PLAIN, 15.5f),
    BOLD("ComicSans-Bold.ttf", Style.BOLD, 20.0f),
    ITALIC("ComicSans-Italic.ttf", Style.ITALIC, 20.5f),
    BOLD_ITALIC("ComicSans-BoldItalic.ttf", Style.BOLD_ITALIC, 20.5f);

    private final @NotNull java.awt.Font actual;
    private final @NotNull Style style;
    private final float size;

    ComicSansFont(@ResourcePath(base = "minecraft/fonts") @NotNull String fileName, @NotNull Style style, float size) {
        this(Font.initFont(String.format("minecraft/fonts/%s", fileName), size), style, size);
    }

    /**
     * Retrieves the {@link ComicSansFont} associated with the given {@link Style}.
     * <p>
     * If no matching font is found, the default {@link ComicSansFont#PLAIN} is returned.
     *
     * @param style the {@link Style} to retrieve the corresponding {@link ComicSansFont} for
     * @return the {@link ComicSansFont} corresponding to the given {@link Style}, or {@link ComicSansFont#PLAIN} if none is found
     */
    public static @NotNull ComicSansFont of(@NotNull Style style) {
        for (ComicSansFont font : values()) {
            if (font.getStyle() == style)
                return font;
        }

        return PLAIN;
    }

    /**
     * Retrieves the {@link ComicSansFont} that corresponds to the style described by the given {@link ColorSegment}.
     * <p>
     * If no matching font is found, the default {@link ComicSansFont#PLAIN} is returned.
     *
     * @param segment the {@link ColorSegment} containing the style information (bold and/or italic) to match
     * @return the {@link ComicSansFont} matching the given style, or {@link ComicSansFont#PLAIN} if no match is found
     */
    public static @NotNull ComicSansFont of(@NotNull ColorSegment segment) {
        for (ComicSansFont font : values()) {
            if (font.getStyle().getId() == (segment.isBold() ? 1 : 0) + (segment.isItalic() ? 2 : 0))
                return font;
        }

        return PLAIN;
    }

}
