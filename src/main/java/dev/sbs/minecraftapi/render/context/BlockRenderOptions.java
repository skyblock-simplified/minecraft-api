package dev.sbs.minecraftapi.render.context;

import dev.sbs.minecraftapi.asset.model.BlockModel.Transform;
import dev.sbs.minecraftapi.math.Vector3f;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable options for rendering a block or item model to a 2D image.
 *
 * <p>Use {@link #builder()} to construct a new instance or {@link #mutate()} to derive a modified copy.
 *
 * @see Builder
 */
@Getter
@RequiredArgsConstructor
public final class BlockRenderOptions {

    public static final BlockRenderOptions DEFAULT = builder().build();

    private final int size;
    private final float yawInDegrees;
    private final float pitchInDegrees;
    private final float rollInDegrees;
    private final float perspectiveAmount;
    private final boolean useGuiTransform;
    private final float padding;
    private final float additionalScale;
    private final @NotNull Vector3f additionalTranslation;
    private final @Nullable Transform overrideGuiTransform;
    private final @Nullable ConcurrentList<String> packIds;
    private final @Nullable ItemRenderData itemData;
    private final @Nullable SkullTextureResolver skullTextureResolver;
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
    public static @NotNull Builder from(@NotNull BlockRenderOptions options) {
        return builder()
            .withSize(options.getSize())
            .withYawInDegrees(options.getYawInDegrees())
            .withPitchInDegrees(options.getPitchInDegrees())
            .withRollInDegrees(options.getRollInDegrees())
            .withPerspectiveAmount(options.getPerspectiveAmount())
            .isUseGuiTransform(options.isUseGuiTransform())
            .withPadding(options.getPadding())
            .withAdditionalScale(options.getAdditionalScale())
            .withAdditionalTranslation(options.getAdditionalTranslation())
            .withOverrideGuiTransform(options.getOverrideGuiTransform())
            .withPackIds(options.getPackIds())
            .withItemData(options.getItemData())
            .withSkullTextureResolver(options.getSkullTextureResolver())
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
     * Fluent builder for constructing {@link BlockRenderOptions} instances.
     */
    public static class Builder {

        private int size = 512;
        private float yawInDegrees;
        private float pitchInDegrees;
        private float rollInDegrees;
        private float perspectiveAmount;
        private boolean useGuiTransform = true;
        private float padding = 0.12f;
        private float additionalScale = 1f;
        private Vector3f additionalTranslation = Vector3f.ZERO;
        private Transform overrideGuiTransform;
        private ConcurrentList<String> packIds;
        private ItemRenderData itemData;
        private SkullTextureResolver skullTextureResolver;
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
         * Enables the GUI transform.
         */
        public @NotNull Builder isUseGuiTransform() {
            return this.isUseGuiTransform(true);
        }

        /**
         * Sets whether the GUI transform is applied.
         *
         * @param useGuiTransform {@code true} to apply the GUI transform
         */
        public @NotNull Builder isUseGuiTransform(boolean useGuiTransform) {
            this.useGuiTransform = useGuiTransform;
            return this;
        }

        /**
         * Sets the padding fraction around the rendered image.
         *
         * @param padding the padding fraction
         */
        public @NotNull Builder withPadding(float padding) {
            this.padding = padding;
            return this;
        }

        /**
         * Sets the additional scale factor.
         *
         * @param additionalScale the additional scale multiplier
         */
        public @NotNull Builder withAdditionalScale(float additionalScale) {
            this.additionalScale = additionalScale;
            return this;
        }

        /**
         * Sets the additional translation offset.
         *
         * @param additionalTranslation the translation vector
         */
        public @NotNull Builder withAdditionalTranslation(@NotNull Vector3f additionalTranslation) {
            this.additionalTranslation = additionalTranslation;
            return this;
        }

        /**
         * Sets the override GUI transform definition.
         *
         * @param overrideGuiTransform the transform override, or null to clear
         */
        public @NotNull Builder withOverrideGuiTransform(@Nullable Transform overrideGuiTransform) {
            this.overrideGuiTransform = overrideGuiTransform;
            return this;
        }

        /**
         * Sets the texture pack IDs to use for rendering.
         *
         * @param packIds the pack IDs, or null for no pack override
         */
        public @NotNull Builder withPackIds(@Nullable ConcurrentList<String> packIds) {
            this.packIds = packIds;
            return this;
        }

        /**
         * Sets the item render data.
         *
         * @param itemData the item data, or null to clear
         */
        public @NotNull Builder withItemData(@Nullable ItemRenderData itemData) {
            this.itemData = itemData;
            return this;
        }

        /**
         * Sets the skull texture resolver.
         *
         * @param skullTextureResolver the resolver, or null to clear
         */
        public @NotNull Builder withSkullTextureResolver(@Nullable SkullTextureResolver skullTextureResolver) {
            this.skullTextureResolver = skullTextureResolver;
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
        public @NotNull BlockRenderOptions build() {
            return new BlockRenderOptions(
                this.size, this.yawInDegrees, this.pitchInDegrees, this.rollInDegrees,
                this.perspectiveAmount, this.useGuiTransform, this.padding, this.additionalScale,
                this.additionalTranslation, this.overrideGuiTransform, this.packIds,
                this.itemData, this.skullTextureResolver, this.enableAntiAliasing
            );
        }
    }
}
