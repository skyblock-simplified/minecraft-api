package dev.sbs.minecraftapi.render.data;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Biome tint classification for Minecraft block and texture rendering.
 *
 * <p>Each constant defines the set of texture and block keys that should receive
 * that tint kind, along with the default colormap sampling coordinates. Constants
 * are ordered by matching priority - {@link #DRY_FOLIAGE} is checked first.
 */
@Getter
@RequiredArgsConstructor
public enum BiomeTint {

    DRY_FOLIAGE(
        new float[]{ 0.5f, 0.25f },
        caseInsensitiveSet(
            "dead_bush", "leaf_litter", "leaf_litter_1", "leaf_litter_2",
            "leaf_litter_3", "leaf_litter_4", "short_dry_grass", "tall_dry_grass"
        ),
        caseInsensitiveSet(
            "dead_bush", "leaf_litter", "leaf_litter_1", "leaf_litter_2",
            "leaf_litter_3", "leaf_litter_4", "short_dry_grass", "tall_dry_grass",
            "potted_dead_bush"
        )
    ),
    GRASS(
        new float[]{ 0.5f, 1.0f },
        caseInsensitiveSet(
            "grass", "tall_grass", "short_grass", "large_fern", "fern",
            "grass_block_top", "grass_block_side_overlay", "grass_block_snow",
            "hanging_roots", "pale_hanging_moss", "pale_hanging_moss_tip",
            "moss", "moss_block", "moss_carpet", "pale_moss_block", "pale_moss_carpet",
            "sugar_cane", "cattail", "kelp", "kelp_top", "kelp_plant",
            "seagrass", "seagrass_top", "tall_seagrass_top", "sea_grass"
        ),
        caseInsensitiveSet(
            "grass_block", "grass", "tall_grass", "short_grass", "large_fern", "fern",
            "hanging_roots", "pale_hanging_moss", "pale_hanging_moss_tip",
            "moss_block", "moss_carpet", "pale_moss_block", "pale_moss_carpet",
            "seagrass", "tall_seagrass", "kelp", "kelp_plant",
            "sugar_cane", "cattail", "potted_fern"
        )
    ),
    FOLIAGE(
        new float[]{ 0.5f, 1.0f },
        caseInsensitiveSet(
            "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves",
            "acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "pale_oak_leaves",
            "azalea_leaves", "flowering_azalea_leaves", "vine",
            "cave_vines", "cave_vines_body", "cave_vines_body_lit",
            "cave_vines_head", "cave_vines_head_lit", "cave_vines_lit",
            "cave_vines_plant", "cave_vines_plant_lit",
            "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling",
            "acacia_sapling", "dark_oak_sapling", "mangrove_propagule", "pale_oak_sapling",
            "azalea", "flowering_azalea",
            "big_dripleaf_top", "big_dripleaf_stem", "big_dripleaf_stem_bottom",
            "big_dripleaf_stem_mid", "small_dripleaf_top", "small_dripleaf_stem",
            "small_dripleaf_stem_top"
        ),
        caseInsensitiveSet(
            "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves",
            "acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "pale_oak_leaves",
            "azalea_leaves", "flowering_azalea_leaves", "vine",
            "cave_vines", "cave_vines_plant", "cave_vines_lit", "cave_vines_plant_lit",
            "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling",
            "acacia_sapling", "dark_oak_sapling", "mangrove_propagule", "pale_oak_sapling",
            "azalea", "flowering_azalea",
            "big_dripleaf", "big_dripleaf_stem", "small_dripleaf", "small_dripleaf_stem",
            "potted_oak_sapling", "potted_spruce_sapling", "potted_birch_sapling",
            "potted_jungle_sapling", "potted_acacia_sapling", "potted_dark_oak_sapling",
            "potted_mangrove_propagule", "potted_pale_oak_sapling",
            "potted_azalea_bush", "potted_flowering_azalea_bush"
        )
    );

    /** The default colormap sampling coordinates (temperature, downfall). */
    private final float @NotNull [] defaultCoordinates;

    /** The texture keys that match this tint kind. */
    private final @NotNull Set<String> textures;

    /** The block keys that match this tint kind. */
    private final @NotNull Set<String> blocks;

    /** Item textures and blocks excluded from default tinting. */
    private static final @NotNull Set<String> ITEM_TINT_EXCLUSIONS = caseInsensitiveSet(
        "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling",
        "acacia_sapling", "dark_oak_sapling", "mangrove_propagule", "pale_oak_sapling",
        "azalea", "flowering_azalea", "cherry_sapling"
    );

    /** Constant tint colors for specific blocks, overriding colormap sampling. */
    private static final @NotNull Map<String, int[]> CONSTANT_COLORS = Map.of(
        "birch_leaves", new int[]{ 128, 167, 85 },
        "spruce_leaves", new int[]{ 97, 153, 97 },
        "lily_pad", new int[]{ 32, 128, 48 }
    );

    /**
     * Checks whether the given key matches this tint kind's textures or blocks.
     *
     * @param key the normalized texture or block key
     * @return true if the key belongs to this tint kind
     */
    public boolean matches(@Nullable String key) {
        return key != null && !key.isEmpty() && (textures.contains(key) || blocks.contains(key));
    }

    /**
     * Checks whether the given key is excluded from default item tinting.
     *
     * @param key the normalized texture or block key
     * @return true if the key should be excluded
     */
    public static boolean isItemTintExcluded(@Nullable String key) {
        return key != null && !key.isEmpty() && ITEM_TINT_EXCLUSIONS.contains(key);
    }

    /**
     * Returns the constant tint color for the given key, if one exists.
     *
     * @param key the normalized texture or block key
     * @return the RGB color as {@code [r, g, b]}, or null if no constant color
     */
    public static int @Nullable [] getConstantColor(@Nullable String key) {
        return key != null ? CONSTANT_COLORS.get(key) : null;
    }

    /**
     * Resolves the biome tint kind for the given texture and block keys.
     *
     * <p>Constants are checked in declaration order ({@link #DRY_FOLIAGE} first).
     *
     * @param textureKey the normalized texture key
     * @param blockKey the normalized block key
     * @return the matching tint kind, or null if neither key matches
     */
    public static @Nullable BiomeTint resolve(@Nullable String textureKey, @Nullable String blockKey) {
        for (BiomeTint kind : values())
            if (kind.matches(textureKey) || kind.matches(blockKey))
                return kind;

        return null;
    }

    private static @NotNull @UnmodifiableView ConcurrentSet<String> caseInsensitiveSet(@NotNull String @NotNull ... values) {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Collections.addAll(set, values);
        return Concurrent.newUnmodifiableSet(set);
    }

}
