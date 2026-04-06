package dev.sbs.minecraftapi.render.engine;

import dev.sbs.minecraftapi.math.Matrix4f;
import dev.sbs.minecraftapi.math.Vector3f;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * An item display transform combining rotation, translation, and scale vectors.
 *
 * <p>Provides a {@link #buildMatrix(boolean)} method that composes the three components into a
 * single 4x4 transformation matrix, with optional left-hand mirroring.
 */
@Getter
@AllArgsConstructor
public final class ItemTransform {

    /** Identity transform with zero rotation, zero translation, and unit scale. */
    public static final @NotNull ItemTransform NO_TRANSFORM = new ItemTransform(Vector3f.ZERO, Vector3f.ZERO, Vector3f.ONE);

    private final @NotNull Vector3f rotation;
    private final @NotNull Vector3f translation;
    private final @NotNull Vector3f scale;

    /**
     * Builds a 4x4 transformation matrix from the rotation, translation, and scale vectors.
     *
     * <p>When {@code isLeftHand} is true, the X translation and Y/Z rotations are negated to
     * produce a mirrored transform suitable for left-hand rendering contexts.
     *
     * @param isLeftHand whether to mirror the transform for left-hand rendering
     * @return the composed transformation matrix
     */
    public @NotNull Matrix4f buildMatrix(boolean isLeftHand) {
        if (this.equals(NO_TRANSFORM)) {
            return Matrix4f.IDENTITY;
        }

        float translationX = isLeftHand ? -translation.getX() : translation.getX();
        float rotationY = isLeftHand ? -rotation.getY() : rotation.getY();
        float rotationZ = isLeftHand ? -rotation.getZ() : rotation.getZ();

        Matrix4f translationMatrix = Matrix4f.createTranslation(
            translationX / 16f,
            translation.getY() / 16f,
            translation.getZ() / 16f
        );

        float degreesToRadians = (float) Math.PI / 180f;

        Matrix4f rotationMatrix = Matrix4f.createRotationZ(rotationZ * degreesToRadians)
            .multiply(Matrix4f.createRotationY(rotationY * degreesToRadians))
            .multiply(Matrix4f.createRotationX(rotation.getX() * degreesToRadians));

        Matrix4f scaleMatrix = Matrix4f.createScale(scale);

        return scaleMatrix.multiply(rotationMatrix).multiply(translationMatrix);
    }

    /**
     * Builds a 4x4 transformation matrix with right-hand orientation.
     *
     * @return the composed transformation matrix
     */
    public @NotNull Matrix4f buildMatrix() {
        return buildMatrix(false);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemTransform other)) return false;
        return rotation.getX() == other.rotation.getX()
            && rotation.getY() == other.rotation.getY()
            && rotation.getZ() == other.rotation.getZ()
            && translation.getX() == other.translation.getX()
            && translation.getY() == other.translation.getY()
            && translation.getZ() == other.translation.getZ()
            && scale.getX() == other.scale.getX()
            && scale.getY() == other.scale.getY()
            && scale.getZ() == other.scale.getZ();
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(rotation.getX());
        result = 31 * result + Float.hashCode(rotation.getY());
        result = 31 * result + Float.hashCode(rotation.getZ());
        result = 31 * result + Float.hashCode(translation.getX());
        result = 31 * result + Float.hashCode(translation.getY());
        result = 31 * result + Float.hashCode(translation.getZ());
        result = 31 * result + Float.hashCode(scale.getX());
        result = 31 * result + Float.hashCode(scale.getY());
        result = 31 * result + Float.hashCode(scale.getZ());
        return result;
    }
}
