package dev.sbs.minecraftapi.render.context;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.util.builder.ClassBuilder;
import dev.sbs.minecraftapi.asset.model.BlockModel.Face;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable options for rendering a single block face.
 *
 * <p>Use {@link #builder()} to construct a new instance or {@link #mutate()} to derive a modified copy.
 *
 * @see Builder
 */
@Getter
@RequiredArgsConstructor
public final class BlockFaceRenderOptions {

    public static final BlockFaceRenderOptions DEFAULT = builder().build();

    private final @NotNull Face direction;
    private final int size;
    private final int rotation;
    private final @Nullable ConcurrentList<String> packIds;

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
    public static @NotNull Builder from(@NotNull BlockFaceRenderOptions options) {
        return builder()
            .withDirection(options.getDirection())
            .withSize(options.getSize())
            .withRotation(options.getRotation())
            .withPackIds(options.getPackIds());
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
     * Fluent builder for constructing {@link BlockFaceRenderOptions} instances.
     */
    public static class Builder implements ClassBuilder<BlockFaceRenderOptions> {

        private Face direction = Face.UP;
        private int size = 512;
        private int rotation;
        private ConcurrentList<String> packIds;

        /**
         * Sets the face direction to render.
         *
         * @param direction the block face direction
         */
        public @NotNull Builder withDirection(@NotNull Face direction) {
            this.direction = direction;
            return this;
        }

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
         * Sets the rotation angle in degrees.
         *
         * @param rotation the rotation angle
         */
        public @NotNull Builder withRotation(int rotation) {
            this.rotation = rotation;
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

        /** {@inheritDoc} */
        @Override
        public @NotNull BlockFaceRenderOptions build() {
            return new BlockFaceRenderOptions(this.direction, this.size, this.rotation, this.packIds);
        }
    }
}
