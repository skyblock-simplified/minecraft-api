package dev.sbs.minecraftapi.asset.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.math.Vector3f;
import dev.sbs.api.math.Vector4f;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.type.GsonType;
import dev.sbs.api.util.StringUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A Minecraft block model combining the raw JSON definition with the resolved inheritance chain.
 *
 * <p>Serves as both the Gson-deserialized JSON representation and the runtime model type.
 * Fields from the JSON definition ({@code parent}, {@code guiLight}, {@code ambientOcclusion})
 * coexist with resolved fields ({@code name}, {@code parentChain}) that are populated during
 * model resolution.
 */
@Getter
@Entity
@NoArgsConstructor
@Table(name = "block_model")
public class BlockModel implements JpaModel {

    // --- Resolved fields (not in JSON) ---

    @Id
    @Setter
    @Column(name = "name", nullable = false)
    private @NotNull String name = "";

    @Column(name = "parent_chain", nullable = false)
    private @NotNull ConcurrentList<String> parentChain = Concurrent.newList();

    // --- Shared fields (JSON + resolved) ---

    @SerializedName("textures")
    @Column(name = "textures", nullable = false)
    private @NotNull ConcurrentMap<String, TextureReference> textures = Concurrent.newMap();

    @SerializedName("display")
    @Column(name = "display", nullable = false)
    private @NotNull ConcurrentMap<String, Transform> display = Concurrent.newMap();

    @SerializedName("elements")
    @Column(name = "elements", nullable = false)
    private @NotNull ConcurrentList<Element> elements = Concurrent.newList();

    // --- JSON-only fields ---

    @SerializedName("parent")
    @Column(name = "parent")
    private @Nullable String parent;

    @SerializedName("gui_light")
    @Column(name = "gui_light")
    private @Nullable String guiLight;

    @SerializedName("ambientocclusion")
    @Column(name = "ambient_occlusion")
    private @Nullable Boolean ambientOcclusion;

    /**
     * Constructs a resolved model instance with the five core fields.
     *
     * @param name the resolved model name
     * @param parentChain the resolved parent chain
     * @param textures the merged texture map
     * @param display the merged display transforms
     * @param elements the merged element list
     */
    public BlockModel(@NotNull String name,
                      @NotNull ConcurrentList<String> parentChain,
                      @NotNull ConcurrentMap<String, TextureReference> textures,
                      @NotNull ConcurrentMap<String, Transform> display,
                      @NotNull ConcurrentList<Element> elements) {
        this.name = name;
        this.parentChain = parentChain;
        this.textures = textures;
        this.display = display;
        this.elements = elements;
    }

    /**
     * Returns the display transform for the given context name, or null if not present.
     *
     * @param name the display context name (e.g. "gui", "thirdperson_righthand")
     * @return the matching transform definition, or null if not found
     */
    public @Nullable Transform getDisplayTransform(@NotNull String name) {
        return display.get(name);
    }

    /**
     * Resolves a texture variable reference to a concrete texture path by following
     * {@code #variable} references through this model's texture map.
     *
     * @param texture the texture reference (may start with '#' for a variable), or null
     * @return the resolved texture path, or "minecraft:missingno" if unresolvable
     */
    public @NotNull String resolveTexture(@Nullable String texture) {
        if (StringUtil.isEmpty(texture))
            return "minecraft:missingno";

        String current = expandTextureReference(texture);

        Set<String> visited = new HashSet<>();
        while (current.startsWith("#")) {
            String key = current.substring(1);
            if (!visited.add(key.toLowerCase(Locale.ROOT)))
                return "minecraft:missingno";

            TextureReference ref = textures.get(key);
            if (ref == null || ref.sprite().isBlank())
                return "minecraft:missingno";

            current = expandTextureReference(ref.sprite());
            if (StringUtil.isEmpty(current))
                return "minecraft:missingno";
        }

        return current;
    }

    private @NotNull String expandTextureReference(@NotNull String candidate) {
        String trimmed = candidate.trim();
        if (trimmed.isEmpty())
            return "";
        if (trimmed.startsWith("#"))
            return trimmed;
        if (textures.containsKey(trimmed))
            return "#" + trimmed;
        return trimmed;
    }

    /**
     * Returns a normalized version of this model's name with {@code minecraft:}, {@code block/},
     * and {@code blocks/} prefixes stripped.
     *
     * @return the normalized model name
     */
    public @NotNull String normalizedName() {
        return normalizeName(name);
    }

    /**
     * Normalizes a model name by stripping {@code minecraft:}, {@code block/}, and {@code blocks/}
     * prefixes.
     *
     * @param name the model name to normalize
     * @return the normalized name
     */
    public static @NotNull String normalizeName(@NotNull String name) {
        String normalized = name.trim();

        if (normalized.toLowerCase(Locale.ROOT).startsWith("minecraft:"))
            normalized = normalized.substring(10);

        if (normalized.toLowerCase(Locale.ROOT).startsWith("block/"))
            normalized = normalized.substring(6);
        else if (normalized.toLowerCase(Locale.ROOT).startsWith("blocks/"))
            normalized = normalized.substring(7);

        return normalized;
    }

    // ================================================================
    // Inner types
    // ================================================================

    /**
     * The six cardinal face directions of a Minecraft block.
     */
    @Getter
    @RequiredArgsConstructor
    public enum Face {

        @SerializedName("down") DOWN(new int[]{ 4, 0, 1, 5 }),
        @SerializedName("up") UP(new int[]{ 3, 7, 6, 2 }),
        @SerializedName("north") NORTH(new int[]{ 2, 1, 0, 3 }),
        @SerializedName("south") SOUTH(new int[]{ 7, 4, 5, 6 }),
        @SerializedName("west") WEST(new int[]{ 3, 0, 4, 7 }),
        @SerializedName("east") EAST(new int[]{ 6, 5, 1, 2 });

        /**
         * The four vertex indices (in winding order) within the standard 8-vertex block box.
         */
        private final int @NotNull [] vertexIndices;

        /**
         * Computes the default UV rectangle for this face based on element bounds.
         *
         * @param from the minimum corner of the element (0-16 range)
         * @param to the maximum corner of the element (0-16 range)
         * @return a {@link Vector4f} where (x, y) is the UV min and (z, w) is the UV max
         */
        public @NotNull Vector4f defaultUv(@NotNull Vector3f from, @NotNull Vector3f to) {
            return switch (this) {
                case DOWN -> new Vector4f(from.getX(), 16f - to.getZ(), to.getX(), 16f - from.getZ());
                case UP -> new Vector4f(from.getX(), from.getZ(), to.getX(), to.getZ());
                case NORTH -> new Vector4f(16f - to.getX(), 16f - to.getY(), 16f - from.getX(), 16f - from.getY());
                case SOUTH -> new Vector4f(from.getX(), 16f - to.getY(), to.getX(), 16f - from.getY());
                case WEST -> new Vector4f(from.getZ(), 16f - to.getY(), to.getZ(), 16f - from.getY());
                case EAST -> new Vector4f(16f - to.getZ(), 16f - to.getY(), 16f - from.getZ(), 16f - from.getY());
            };
        }

        /**
         * Parses a face direction from a Minecraft JSON string (case-insensitive).
         *
         * @param name the direction name
         * @return the matching direction, or null if not recognized
         */
        public static @Nullable Face fromString(@Nullable String name) {
            if (name == null) return null;
            return switch (name.toLowerCase()) {
                case "north" -> NORTH;
                case "south" -> SOUTH;
                case "east" -> EAST;
                case "west" -> WEST;
                case "up" -> UP;
                case "down" -> DOWN;
                default -> null;
            };
        }
    }

    /**
     * A model element cuboid with from/to coordinates, optional rotation, face map, and shade flag.
     *
     * <p>Serves as both the Gson-deserialized JSON representation and the runtime model type.
     * When deserialized from JSON, face keys are matched to {@link Face} via their
     * {@code @SerializedName} annotations.
     */
    @Getter
    @GsonType
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Element {

        @SerializedName("from")
        private @NotNull Vector3f from = Vector3f.ZERO;

        @SerializedName("to")
        private @NotNull Vector3f to = Vector3f.ZERO;

        @SerializedName("rotation")
        private @Nullable Rotation rotation;

        @SerializedName("faces")
        private @NotNull Map<Face, FaceData> faces = new LinkedHashMap<>();

        @SerializedName("shade")
        private boolean shade = true;
    }

    /**
     * A model face with texture reference, UV coordinates, rotation, tint index, and cull face.
     *
     * <p>Serves as both the Gson-deserialized JSON representation and the runtime model type.
     */
    @Getter
    @GsonType
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class FaceData {

        @SerializedName("texture")
        private @NotNull String texture = "";

        @SerializedName("uv")
        private @Nullable Vector4f uv;

        @SerializedName("rotation")
        private @Nullable Integer rotation;

        @SerializedName("tintindex")
        private @Nullable Integer tintIndex;

        @SerializedName("cullface")
        private @Nullable String cullFace;
    }

    /**
     * An element rotation describing angle, axis, origin, and optional rescale.
     *
     * <p>Serves as both the Gson-deserialized JSON representation and the runtime model type.
     */
    @Getter
    @GsonType
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Rotation {

        @SerializedName("angle")
        private float angle;

        @SerializedName("origin")
        private @NotNull Vector3f origin = Vector3f.ZERO;

        @SerializedName("axis")
        private @NotNull String axis = "y";

        @SerializedName("rescale")
        private boolean rescale;
    }

    /**
     * A raw display transform definition from a Minecraft block model JSON.
     *
     * <p>Contains optional rotation, translation, and scale arrays as read directly from the
     * {@code display} section of the model file.
     */
    @Getter
    @GsonType
    public static final class Transform {

        @SerializedName("rotation")
        private float @Nullable [] rotation;

        @SerializedName("translation")
        private float @Nullable [] translation;

        @SerializedName("scale")
        private float @Nullable [] scale;

        /**
         * Creates a new transform definition with the given rotation, translation, and scale arrays.
         *
         * @param rotation the rotation array [rx, ry, rz] in degrees, or null
         * @param translation the translation array [tx, ty, tz], or null
         * @param scale the scale array [sx, sy, sz], or null
         * @return a new transform definition
         */
        public static Transform create(float @Nullable [] rotation, float @Nullable [] translation, float @Nullable [] scale) {
            Transform td = new Transform();
            td.rotation = rotation;
            td.translation = translation;
            td.scale = scale;
            return td;
        }
    }
}
