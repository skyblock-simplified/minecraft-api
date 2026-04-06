package dev.sbs.minecraftapi.generator.font;

import dev.sbs.minecraftapi.generator.image.exception.FontException;
import dev.simplified.util.SystemUtil;
import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Represents a customizable font with functionality for style, size, and actual rendered form.
 * <p>
 * Provides utility methods to initialize font instances from resources and supports different text styles
 * through the {@link Font.Style} enumeration.
 */
public interface Font {

    /**
     * Retrieves the actual {@link java.awt.Font} object represented by this font.
     * <p>
     * This method provides access to the rendered form of the font, which can be used for graphical
     * operations such as drawing text or measuring characters with the corresponding font attributes.
     *
     * @return the {@link java.awt.Font} object representing the font
     */
    @NotNull java.awt.Font getActual();

    /**
     * Retrieves the style of the font.
     *
     * @return the {@link Style} of the font
     */
    @NotNull Style getStyle();

    /**
     * Retrieves the size of the font.
     *
     * @return the size of the font as a floating-point value
     */
    float getSize();

    /**
     * Initializes a font by loading it from the specified resource path and applying the given font size.
     *
     * @param resourcePath the relative path to the font file resource
     * @param size the desired font size to apply to the loaded font
     * @return the {@link java.awt.Font} object initialized with the specified resource and size
     * @throws FontException if the font file cannot be loaded or is in an invalid format
     */
    static @NotNull java.awt.Font initFont(/*@ResourcePath */@NotNull String resourcePath, float size) throws FontException {
        try {
            @Cleanup InputStream inputStream = SystemUtil.getResource(resourcePath);
            java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, Objects.requireNonNull(inputStream)).deriveFont(size);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException | NullPointerException ex) {
            throw new FontException(ex, resourcePath);
        }
    }

    /**
     * Represents different text styles that can be applied to fonts.
     */
    @Getter
    @RequiredArgsConstructor
    enum Style {

        /**
         * Represents the plain text style with no additional formatting applied.
         * <p>
         * This style serves as the default style in cases where no specific style is defined.
         */
        PLAIN(0),
        /**
         * Represents the bold text style, where the font is displayed with thicker and more prominent strokes.
         * <p>
         * This style is typically used to emphasize text or headings for better visibility and distinction.
         */
        BOLD(1),
        /**
         * Represents the italic text style, where the font is displayed with a slanted and stylized appearance.
         * <p>
         * This style is generally used to convey emphasis, distinguish titles, or provide a decorative aesthetic.
         */
        ITALIC(2),
        /**
         * Represents the combined bold and italic text style.
         * <p>
         * This style applies both bold (thicker strokes) and italic (slanted appearance)
         * formatting to the text, typically used to emphasize text with added visual distinction.
         */
        BOLD_ITALIC(3);

        public final int id;

        /**
         * Returns the Style corresponding to the specified id.
         * <p>
         * If no matching Style is found, the default Style {@link #PLAIN} is returned.
         *
         * @param id the identifier of the Style to be retrieved
         * @return the Style corresponding to the specified id, or {@link #PLAIN} if no match is found
         */
        public static @NotNull Style of(int id) {
            for (Style style : values()) {
                if (style.getId() == id)
                    return style;
            }

            return PLAIN;
        }

    }

}
