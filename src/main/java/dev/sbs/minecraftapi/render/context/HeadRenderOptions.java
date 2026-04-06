package dev.sbs.minecraftapi.render.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable options for rendering a Minecraft player head to a 2D image.
 *
 * <p>Use {@link #builder()} to construct a new instance or {@link #mutate()} to derive a modified copy.
 *
 * @see Builder
 */
@Getter
@RequiredArgsConstructor
public final class HeadRenderOptions {

    public static final HeadRenderOptions DEFAULT = builder().build();

    private final int size;
    private final float yawInDegrees;
    private final float pitchInDegrees;
    private final float rollInDegrees;
    private final float perspectiveAmount;
    private final boolean showOverlay;
    private final boolean enableAntiAliasing;

    /**
     * Creates a new empty {@link Builder}.
     *
     * @return a new builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Creates a {@link Builder} pre-populated with the values from the given options.
     *
     * @param options the options to copy from
     * @return a pre-populated builder
     */
    public static @NotNull Builder from(@NotNull HeadRenderOptions options) {
        return builder()
            .withSize(options.getSize())
            .withYawInDegrees(options.getYawInDegrees())
            .withPitchInDegrees(options.getPitchInDegrees())
            .withRollInDegrees(options.getRollInDegrees())
            .withPerspectiveAmount(options.getPerspectiveAmount())
            .isShowOverlay(options.isShowOverlay())
            .isEnableAntiAliasing(options.isEnableAntiAliasing());
    }

    /**
     * Returns a {@link Builder} pre-populated with this instance's values for modification.
     *
     * @return a pre-populated builder
     */
    public @NotNull Builder mutate() {
        return from(this);
    }

    /**
     * The side of an isometric head render.
     */
    public enum IsometricSide {
        LEFT,
        RIGHT
    }

    /**
     * Creates options configured for an isometric head render.
     *
     * @param size the output image size in pixels
     * @param side the isometric viewing side
     * @param showOverlay whether to render the overlay (hat) layer
     * @param enableAntiAliasing whether to apply FXAA anti-aliasing
     * @return configured head render options
     */
    public static @NotNull HeadRenderOptions isometric(int size, @NotNull IsometricSide side,
                                                        boolean showOverlay, boolean enableAntiAliasing) {
        float yaw = side == IsometricSide.RIGHT ? -135f : 45f;
        return builder()
            .withSize(size)
            .withYawInDegrees(yaw)
            .withPitchInDegrees(30f)
            .isShowOverlay(showOverlay)
            .isEnableAntiAliasing(enableAntiAliasing)
            .build();
    }

    /**
     * Creates options configured for an isometric head render with default overlay and anti-aliasing.
     *
     * @param size the output image size in pixels
     * @return configured head render options
     */
    public static @NotNull HeadRenderOptions isometric(int size) {
        return isometric(size, IsometricSide.RIGHT, true, true);
    }

    /**
     * Fluent builder for constructing {@link HeadRenderOptions} instances.
     */
    public static class Builder {

        private int size = 128;
        private float yawInDegrees;
        private float pitchInDegrees;
        private float rollInDegrees;
        private float perspectiveAmount;
        private boolean showOverlay = true;
        private boolean enableAntiAliasing = true;

        /**
         * Sets the output image size in pixels.
         *
         * @param size the image size
         */
        public @NotNull Builder withSize(int size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the yaw rotation angle.
         *
         * @param yawInDegrees the yaw angle in degrees
         */
        public @NotNull Builder withYawInDegrees(float yawInDegrees) {
            this.yawInDegrees = yawInDegrees;
            return this;
        }

        /**
         * Sets the pitch rotation angle.
         *
         * @param pitchInDegrees the pitch angle in degrees
         */
        public @NotNull Builder withPitchInDegrees(float pitchInDegrees) {
            this.pitchInDegrees = pitchInDegrees;
            return this;
        }

        /**
         * Sets the roll rotation angle.
         *
         * @param rollInDegrees the roll angle in degrees
         */
        public @NotNull Builder withRollInDegrees(float rollInDegrees) {
            this.rollInDegrees = rollInDegrees;
            return this;
        }

        /**
         * Sets the perspective projection amount.
         *
         * @param perspectiveAmount the perspective amount (0 for orthographic)
         */
        public @NotNull Builder withPerspectiveAmount(float perspectiveAmount) {
            this.perspectiveAmount = perspectiveAmount;
            return this;
        }

        /**
         * Enables the overlay (hat) layer.
         */
        public @NotNull Builder isShowOverlay() {
            return this.isShowOverlay(true);
        }

        /**
         * Sets whether the overlay (hat) layer is rendered.
         *
         * @param showOverlay {@code true} to show the overlay
         */
        public @NotNull Builder isShowOverlay(boolean showOverlay) {
            this.showOverlay = showOverlay;
            return this;
        }

        /**
         * Enables anti-aliasing.
         */
        public @NotNull Builder isEnableAntiAliasing() {
            return this.isEnableAntiAliasing(true);
        }

        /**
         * Sets whether anti-aliasing is enabled.
         *
         * @param enableAntiAliasing {@code true} to enable FXAA
         */
        public @NotNull Builder isEnableAntiAliasing(boolean enableAntiAliasing) {
            this.enableAntiAliasing = enableAntiAliasing;
            return this;
        }

        /** {@inheritDoc} */
        public @NotNull HeadRenderOptions build() {
            return new HeadRenderOptions(
                this.size, this.yawInDegrees, this.pitchInDegrees, this.rollInDegrees,
                this.perspectiveAmount, this.showOverlay, this.enableAntiAliasing
            );
        }
    }
}
