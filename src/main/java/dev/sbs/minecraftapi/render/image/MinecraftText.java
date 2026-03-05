package dev.sbs.minecraftapi.render.image;

import dev.sbs.api.builder.ClassBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.io.stream.ByteArrayDataOutput;
import dev.sbs.api.math.Range;
import dev.sbs.api.util.SystemUtil;
import dev.sbs.minecraftapi.render.font.Font;
import dev.sbs.minecraftapi.render.font.MinecraftFont;
import dev.sbs.minecraftapi.render.image.exception.ImageException;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import dev.sbs.minecraftapi.render.text.segment.ColorSegment;
import dev.sbs.minecraftapi.render.text.segment.LineSegment;
import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

public class MinecraftText {

    private static final int PIXEL_SIZE = 2;
    private static final int START_XY = PIXEL_SIZE * 5;
    private static final int Y_INCREMENT = PIXEL_SIZE * 10;
    private static final int STRIKETHROUGH_OFFSET = -8;
    private static final int UNDERLINE_OFFSET = 2;
    private static final Range<Integer> LINE_LENGTH = Range.between(38, 80);

    // Current Settings
    @Getter private final ConcurrentList<LineSegment> lines;
    @Getter private final int alpha;
    @Getter private final int padding;
    @Getter private final boolean paddingFirstLine;
    @Getter(AccessLevel.PRIVATE)
    private final Graphics2D graphics;
    @Getter private BufferedImage image;
    @Getter private ChatFormat currentColor;
    private MinecraftFont currentFont;

    // Positioning & Size
    private int locationX = START_XY;
    private int locationY = START_XY + PIXEL_SIZE * 2 + Y_INCREMENT / 2;
    private int largestWidth = 0;

    private MinecraftText(ConcurrentList<LineSegment> lines, ChatFormat defaultColor, int alpha, int padding, boolean paddingFirstLine) {
        this.alpha = alpha;
        this.padding = padding;
        this.paddingFirstLine = paddingFirstLine;
        this.lines = lines.toUnmodifiableList();
        int lineLength = lines.stream()
            .mapToInt(LineSegment::length)
            .max()
            .orElse(LINE_LENGTH.getMaximum());
        this.graphics = this.initG2D(LINE_LENGTH.fit(lineLength) * 25, this.lines.size() * Y_INCREMENT + START_XY + PIXEL_SIZE * 4);
        this.currentColor = defaultColor;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an image, then initialized a Graphics2D object from that image.
     *
     * @return G2D object
     */
    private Graphics2D initG2D(int width, int height) {
        // Create Image
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Draw Primary Background
        Graphics2D graphics = this.getImage().createGraphics();
        graphics.setColor(new Color(18, 3, 18, this.getAlpha()));
        graphics.fillRect(
            PIXEL_SIZE * 2,
            PIXEL_SIZE * 2,
            width - PIXEL_SIZE * 4,
            height - PIXEL_SIZE * 4
        );

        return graphics;
    }

    /**
     * Crops the image to fit the space taken up by the borders.
     */
    public void cropImage() {
        this.image = this.getImage().getSubimage(
            0,
            0,
            this.largestWidth + START_XY,
            this.getImage().getHeight()
        );
    }

    /**
     * Resizes the image to add padding.
     */
    public void addPadding() {
        if (this.getPadding() > 0) {
            BufferedImage resizedImage = new BufferedImage(
                this.getImage().getWidth() + this.getPadding() * 2,
                this.getImage().getHeight() + this.getPadding() * 2,
                BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D graphics2D = resizedImage.createGraphics();
            graphics2D.drawImage(this.getImage(), this.getPadding(), this.getPadding(), this.getImage().getWidth(), this.getImage().getHeight(), null);
            graphics2D.dispose();
            this.image = resizedImage;
        }
    }

    /**
     * Creates the inner and outer purple borders around the image.
     */
    public void drawBorders() {
        final int width = this.getImage().getWidth();
        final int height = this.getImage().getHeight();

        // Draw Darker Purple Border Around Purple Border
        this.getGraphics().setColor(new Color(18, 3, 18, this.getAlpha()));
        this.getGraphics().fillRect(0, PIXEL_SIZE, PIXEL_SIZE, height - PIXEL_SIZE * 2);
        this.getGraphics().fillRect(PIXEL_SIZE, 0, width - PIXEL_SIZE * 2, PIXEL_SIZE);
        this.getGraphics().fillRect(width - PIXEL_SIZE, PIXEL_SIZE, PIXEL_SIZE, height - PIXEL_SIZE * 2);
        this.getGraphics().fillRect(PIXEL_SIZE, height - PIXEL_SIZE, width - PIXEL_SIZE * 2, PIXEL_SIZE);

        // Draw Purple Border
        this.getGraphics().setColor(new Color(37, 0, 94, this.getAlpha()));
        this.getGraphics().drawRect(PIXEL_SIZE, PIXEL_SIZE, width - PIXEL_SIZE * 2 - 1, height - PIXEL_SIZE * 2 - 1);
        this.getGraphics().drawRect(PIXEL_SIZE + 1, PIXEL_SIZE + 1, width - PIXEL_SIZE * 3 - 1, height - PIXEL_SIZE * 3 - 1);
    }

    /**
     * Draws the lines on the image.
     */
    public void drawLines() {
        this.getLines().forEach(line -> {
            line.getSegments().forEach(segment -> {
                // Change Fonts and Color
                this.currentFont = MinecraftFont.of(segment);
                this.getGraphics().setFont(this.currentFont.getActual());
                this.currentColor = segment.getColor().orElse(ChatFormat.GRAY);

                StringBuilder subWord = new StringBuilder();
                String segmentText = segment.getText();

                // Iterate through all characters on the current segment until there is a character which cannot be displayed
                for (int charIndex = 0; charIndex < segmentText.length(); charIndex++) {
                    char character = segmentText.charAt(charIndex);

                    if (!this.currentFont.getActual().canDisplay(character)) {
                        this.drawString(subWord.toString(), segment);
                        subWord.setLength(0);
                        this.drawSymbol(character, segment);
                        continue;
                    }

                    // Prevent Monospace
                    subWord.append(character);
                }

                this.drawString(subWord.toString(), segment);
            });

            this.updatePositionAndSize(this.getLines().indexOf(line) == 0 && this.isPaddingFirstLine());
        });
    }

    /**
     * Draws a symbol on the image, and updates the pointer location.
     *
     * @param symbol The symbol to draw.
     */
    private void drawSymbol(char symbol, @NotNull ColorSegment colorSegment) {
        this.drawString(Character.toString(symbol), colorSegment, MinecraftFont.SANS_SERIF);
    }

    /**
     * Draws a string at the current location, and updates the pointer location.
     *
     * @param value The value to draw.
     */
    private void drawString(@NotNull String value, @NotNull ColorSegment colorSegment) {
        this.drawString(value, colorSegment, this.currentFont);
    }

    private void drawString(@NotNull String value, @NotNull ColorSegment colorSegment, @NotNull Font font) {
        // Change Font
        this.getGraphics().setFont(font.getActual());

        // Next Draw Position
        int nextBounds = (int) font.getActual().getStringBounds(value, this.getGraphics().getFontRenderContext()).getWidth();

        // Draw Strikethrough Drop Shadow
        if (colorSegment.isStrikethrough())
            this.drawThickLine(nextBounds, this.locationX, this.locationY, -1, STRIKETHROUGH_OFFSET, true);

        // Draw Underlined Drop Shadow
        if (colorSegment.isUnderlined())
            this.drawThickLine(nextBounds, this.locationX - PIXEL_SIZE, this.locationY, 1, UNDERLINE_OFFSET, true);

        // Draw Drop Shadow Text
        this.getGraphics().setColor(this.currentColor.getBackgroundColor());
        this.getGraphics().drawString(value, this.locationX + PIXEL_SIZE, this.locationY + PIXEL_SIZE);

        // Draw Text
        this.getGraphics().setColor(this.currentColor.getColor());
        this.getGraphics().drawString(value, this.locationX, this.locationY);

        // Draw Strikethrough
        if (colorSegment.isStrikethrough())
            this.drawThickLine(nextBounds, this.locationX, this.locationY, -1, STRIKETHROUGH_OFFSET, false);

        // Draw Underlined
        if (colorSegment.isUnderlined())
            this.drawThickLine(nextBounds, this.locationX - PIXEL_SIZE, this.locationY, 1, UNDERLINE_OFFSET, false);

        // Update Draw Pointer Location
        this.locationX += nextBounds;

        // Reset Font
        this.getGraphics().setFont(this.currentFont.getActual());
    }

    private void drawThickLine(int width, int xPosition, int yPosition, int xOffset, int yOffset, boolean dropShadow) {
        int xPosition1 = xPosition;
        int xPosition2 = xPosition + width + xOffset;
        yPosition += yOffset;

        if (dropShadow) {
            xPosition1 += PIXEL_SIZE;
            xPosition2 += PIXEL_SIZE;
            yPosition += PIXEL_SIZE;
        }

        this.getGraphics().setColor(dropShadow ? this.currentColor.getBackgroundColor() : this.currentColor.getColor());
        this.getGraphics().drawLine(xPosition1, yPosition, xPosition2, yPosition);
        this.getGraphics().drawLine(xPosition1, yPosition + 1, xPosition2, yPosition + 1);
    }

    /**
     * Draws the Lines, Resizes the Image and Draws the Borders.
     */
    public MinecraftText render() {
        this.drawLines();
        this.cropImage();
        this.drawBorders();
        this.addPadding();
        return this;
    }

    @SneakyThrows
    public InputStream toStream() {
        @Cleanup ByteArrayDataOutput dataOutput = new ByteArrayDataOutput();
        dataOutput.writeImage(this.getImage(), "PNG");
        return new ByteArrayInputStream(dataOutput.toByteArray());
    }

    public File toFile() throws ImageException {
        try {
            // TODO: Fix minecrafttext/ writing
            File tempFile = new File(SystemUtil.getJavaIoTmpDir(), String.format("minecrafttext/%s.png", UUID.randomUUID()));
            ImageIO.write(this.getImage(), "PNG", tempFile);
            return tempFile;
        } catch (IOException ioex) {
            throw new ImageException(ioex);
        }
    }

    /**
     * Moves the pointer to draw on the next line.
     *
     * @param increaseGap Increase number of pixels between the next line
     */
    private void updatePositionAndSize(boolean increaseGap) {
        this.locationY += Y_INCREMENT + (increaseGap ? PIXEL_SIZE * 2 : 0);
        this.largestWidth = Math.max(this.locationX, this.largestWidth);
        this.locationX = START_XY;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder implements ClassBuilder<MinecraftText> {

        private ConcurrentList<LineSegment> lines = Concurrent.newList();
        private ChatFormat defaultColor = ChatFormat.GRAY;
        private int alpha = 255;
        private int padding = 0;
        private boolean paddingFirstLine = true;

        public Builder isPaddingFirstLine() {
            return this.isPaddingFirstLine(true);
        }

        public Builder isPaddingFirstLine(boolean value) {
            this.paddingFirstLine = value;
            return this;
        }

        public Builder withAlpha(int value) {
            this.alpha = Range.between(0, 255).fit(value);
            return this;
        }

        public Builder withDefaultColor(@NotNull ChatFormat chatColor) {
            this.defaultColor = chatColor;
            return this;
        }

        public Builder withEmptyLine() {
            return this.withSegments(ColorSegment.builder().build());
        }

        public Builder withLines(@NotNull LineSegment... lines) {
            return this.withLines(Arrays.asList(lines));
        }

        public Builder withLines(@NotNull Iterable<LineSegment> lines) {
            lines.forEach(this.lines::add);
            return this;
        }

        public Builder withPadding(int padding) {
            this.padding = Math.max(0, padding);
            return this;
        }

        public Builder withSegments(@NotNull ColorSegment... segments) {
            return this.withSegments(Arrays.asList(segments));
        }

        public Builder withSegments(@NotNull Iterable<ColorSegment> segments) {
            this.lines.add(LineSegment.builder().withSegments(segments).build());
            return this;
        }

        @Override
        public @NotNull MinecraftText build() {
            return new MinecraftText(
                this.lines,
                this.defaultColor,
                this.alpha,
                this.padding,
                this.paddingFirstLine
            );
        }

    }

}
