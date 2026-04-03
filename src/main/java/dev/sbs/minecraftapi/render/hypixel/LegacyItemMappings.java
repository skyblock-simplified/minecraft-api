package dev.sbs.minecraftapi.render.hypixel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mappings from pre-flattening numeric item identifiers to namespaced item strings.
 */
public final class LegacyItemMappings {

    private LegacyItemMappings() {
    }

    /**
     * Tries to map a legacy (1.8.9-style) numeric item identifier to a modern namespaced item id.
     *
     * @param numericId the numeric identifier
     * @return the mapped item id, or {@code null} when no mapping exists
     */
    public static @Nullable String tryMapNumericId(short numericId) {
        return tryMapNumericId(numericId, (short) 0);
    }

    /**
     * Tries to map a legacy (1.8.9-style) numeric item identifier to a modern namespaced item id
     * considering the item damage value.
     *
     * @param numericId the numeric identifier
     * @param damage the item damage value
     * @return the mapped item id, or {@code null} when no mapping exists
     */
    public static @Nullable String tryMapNumericId(short numericId, short damage) {
        String itemId = resolveNumericId(numericId, damage);
        return itemId.isEmpty() ? null : itemId;
    }

    /**
     * Maps a legacy numeric item identifier to a modern namespaced item id or returns a default value.
     *
     * @param numericId the numeric identifier
     * @param defaultValue the default value to return when the mapping does not exist
     * @return the resulting item id when found, otherwise the default value
     */
    public static @NotNull String mapNumericIdOrDefault(short numericId, @NotNull String defaultValue) {
        String result = tryMapNumericId(numericId);
        return result != null ? result : defaultValue;
    }

    /**
     * Maps a legacy numeric item identifier to a modern namespaced item id or returns
     * {@code "minecraft:missingno"}.
     *
     * @param numericId the numeric identifier
     * @return the resulting item id when found, otherwise {@code "minecraft:missingno"}
     */
    public static @NotNull String mapNumericIdOrDefault(short numericId) {
        return mapNumericIdOrDefault(numericId, "minecraft:missingno");
    }

    /**
     * Maps a legacy numeric item identifier with damage to a modern namespaced item id or returns a
     * default value.
     *
     * @param numericId the numeric identifier
     * @param damage the item damage value
     * @param defaultValue the default value to return when the mapping does not exist
     * @return the resulting item id when found, otherwise the default value
     */
    public static @NotNull String mapNumericIdOrDefault(short numericId, short damage,
                                                        @NotNull String defaultValue) {
        String result = tryMapNumericId(numericId, damage);
        return result != null ? result : defaultValue;
    }

    /**
     * Maps a legacy numeric item identifier with damage to a modern namespaced item id or returns
     * {@code "minecraft:missingno"}.
     *
     * @param numericId the numeric identifier
     * @param damage the item damage value
     * @return the resulting item id when found, otherwise {@code "minecraft:missingno"}
     */
    public static @NotNull String mapNumericIdOrDefault(short numericId, short damage) {
        return mapNumericIdOrDefault(numericId, damage, "minecraft:missingno");
    }

    /**
     * Tries to map a Bukkit material name (as Hypixel uses) to a modern namespaced item id.
     *
     * @param bukkitId the Bukkit material name
     * @return the mapped item id, or {@code null} when no mapping exists
     */
    public static @Nullable String tryMapBukkitId(@NotNull String bukkitId) {
        short numericId = mapBukkit(bukkitId);
        if (numericId == -1) {
            return null;
        }
        return tryMapNumericId(numericId);
    }

    /**
     * Tries to map a Bukkit material name with damage to a modern namespaced item id.
     *
     * @param bukkitId the Bukkit material name
     * @param damage the item damage value
     * @return the mapped item id, or {@code null} when no mapping exists
     */
    public static @Nullable String tryMapBukkitId(@NotNull String bukkitId, short damage) {
        short numericId = mapBukkit(bukkitId);
        if (numericId == -1) {
            return null;
        }
        return tryMapNumericId(numericId, damage);
    }

    /**
     * Maps a Bukkit material name to a modern namespaced item id or returns a default value.
     *
     * @param bukkitId the Bukkit material name
     * @param defaultValue the default value to return when the mapping does not exist
     * @return the resulting item id when found, otherwise the default value
     */
    public static @NotNull String mapBukkitIdOrDefault(@NotNull String bukkitId,
                                                       @NotNull String defaultValue) {
        String result = tryMapBukkitId(bukkitId);
        return result != null ? result : defaultValue;
    }

    /**
     * Maps a Bukkit material name to a modern namespaced item id or returns
     * {@code "minecraft:missingno"}.
     *
     * @param bukkitId the Bukkit material name
     * @return the resulting item id when found, otherwise {@code "minecraft:missingno"}
     */
    public static @NotNull String mapBukkitIdOrDefault(@NotNull String bukkitId) {
        return mapBukkitIdOrDefault(bukkitId, "minecraft:missingno");
    }

    /**
     * Maps a Bukkit material name with damage to a modern namespaced item id or returns a default value.
     *
     * @param bukkitId the Bukkit material name
     * @param damage the item damage value
     * @param defaultValue the default value to return when the mapping does not exist
     * @return the resulting item id when found, otherwise the default value
     */
    public static @NotNull String mapBukkitIdOrDefault(@NotNull String bukkitId, short damage,
                                                       @NotNull String defaultValue) {
        String result = tryMapBukkitId(bukkitId, damage);
        return result != null ? result : defaultValue;
    }

    /**
     * Maps a Bukkit material name with damage to a modern namespaced item id or returns
     * {@code "minecraft:missingno"}.
     *
     * @param bukkitId the Bukkit material name
     * @param damage the item damage value
     * @return the resulting item id when found, otherwise {@code "minecraft:missingno"}
     */
    public static @NotNull String mapBukkitIdOrDefault(@NotNull String bukkitId, short damage) {
        return mapBukkitIdOrDefault(bukkitId, damage, "minecraft:missingno");
    }

    // -- Numeric ID resolution --

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private static @NotNull String resolveNumericId(short numericId, short damage) {
        return switch (numericId) {
            case 0 -> "minecraft:air";
            case 1 -> switch (damage) {
                case 1 -> "minecraft:granite";
                case 2 -> "minecraft:polished_granite";
                case 3 -> "minecraft:diorite";
                case 4 -> "minecraft:polished_diorite";
                case 5 -> "minecraft:andesite";
                case 6 -> "minecraft:polished_andesite";
                default -> "minecraft:stone";
            };
            case 2 -> "minecraft:grass_block";
            case 3 -> switch (damage) {
                case 1 -> "minecraft:coarse_dirt";
                case 2 -> "minecraft:podzol";
                default -> "minecraft:dirt";
            };
            case 4 -> "minecraft:cobblestone";
            case 5 -> switch (damage) {
                case 1 -> "minecraft:spruce_planks";
                case 2 -> "minecraft:birch_planks";
                case 3 -> "minecraft:jungle_planks";
                case 4 -> "minecraft:acacia_planks";
                case 5 -> "minecraft:dark_oak_planks";
                default -> "minecraft:oak_planks";
            };
            case 6 -> switch (damage) {
                case 1 -> "minecraft:spruce_sapling";
                case 2 -> "minecraft:birch_sapling";
                case 3 -> "minecraft:jungle_sapling";
                case 4 -> "minecraft:acacia_sapling";
                case 5 -> "minecraft:dark_oak_sapling";
                default -> "minecraft:oak_sapling";
            };
            case 7 -> "minecraft:bedrock";
            case 8 -> "minecraft:flowing_water";
            case 9 -> "minecraft:water";
            case 10 -> "minecraft:flowing_lava";
            case 11 -> "minecraft:lava";
            case 12 -> switch (damage) {
                case 1 -> "minecraft:red_sand";
                default -> "minecraft:sand";
            };
            case 13 -> "minecraft:gravel";
            case 14 -> "minecraft:gold_ore";
            case 15 -> "minecraft:iron_ore";
            case 16 -> "minecraft:coal_ore";
            case 17 -> switch (damage) {
                case 1, 5, 9 -> "minecraft:spruce_log";
                case 2, 6, 10 -> "minecraft:birch_log";
                case 3, 7, 11 -> "minecraft:jungle_log";
                case 13 -> "minecraft:spruce_wood";
                case 14 -> "minecraft:birch_wood";
                case 15 -> "minecraft:jungle_wood";
                default -> "minecraft:oak_log";
            };
            case 18 -> switch (damage) {
                case 1 -> "minecraft:spruce_leaves";
                case 2 -> "minecraft:birch_leaves";
                case 3 -> "minecraft:jungle_leaves";
                default -> "minecraft:oak_leaves";
            };
            case 19 -> switch (damage) {
                case 1 -> "minecraft:wet_sponge";
                default -> "minecraft:sponge";
            };
            case 20 -> "minecraft:glass";
            case 21 -> "minecraft:lapis_ore";
            case 22 -> "minecraft:lapis_block";
            case 23 -> "minecraft:dispenser";
            case 24 -> switch (damage) {
                case 1 -> "minecraft:cut_sandstone";
                case 2 -> "minecraft:chiseled_sandstone";
                case 3 -> "minecraft:smooth_sandstone";
                default -> "minecraft:sandstone";
            };
            case 25 -> "minecraft:note_block";
            case 26 -> "minecraft:red_bed";
            case 27 -> "minecraft:golden_rail";
            case 28 -> "minecraft:detector_rail";
            case 29 -> "minecraft:sticky_piston";
            case 30 -> "minecraft:cobweb";
            case 31 -> switch (damage) {
                case 1 -> "minecraft:tall_grass";
                case 2 -> "minecraft:fern";
                default -> "minecraft:dead_bush";
            };
            case 32 -> "minecraft:dead_bush";
            case 33 -> "minecraft:piston";
            case 34 -> "minecraft:piston_head";
            case 35 -> switch (damage) {
                case 1 -> "minecraft:orange_wool";
                case 2 -> "minecraft:magenta_wool";
                case 3 -> "minecraft:light_blue_wool";
                case 4 -> "minecraft:yellow_wool";
                case 5 -> "minecraft:lime_wool";
                case 6 -> "minecraft:pink_wool";
                case 7 -> "minecraft:gray_wool";
                case 8 -> "minecraft:light_gray_wool";
                case 9 -> "minecraft:cyan_wool";
                case 10 -> "minecraft:purple_wool";
                case 11 -> "minecraft:blue_wool";
                case 12 -> "minecraft:brown_wool";
                case 13 -> "minecraft:green_wool";
                case 14 -> "minecraft:red_wool";
                case 15 -> "minecraft:black_wool";
                default -> "minecraft:white_wool";
            };
            case 36 -> "minecraft:moving_piston";
            case 37 -> "minecraft:dandelion";
            case 38 -> switch (damage) {
                case 1 -> "minecraft:blue_orchid";
                case 2 -> "minecraft:allium";
                case 3 -> "minecraft:azure_bluet";
                case 4 -> "minecraft:red_tulip";
                case 5 -> "minecraft:orange_tulip";
                case 6 -> "minecraft:white_tulip";
                case 7 -> "minecraft:pink_tulip";
                case 8 -> "minecraft:oxeye_daisy";
                case 9 -> "minecraft:cornflower";
                case 10 -> "minecraft:lily_of_the_valley";
                default -> "minecraft:poppy";
            };
            case 39 -> "minecraft:brown_mushroom";
            case 40 -> "minecraft:red_mushroom";
            case 41 -> "minecraft:gold_block";
            case 42 -> "minecraft:iron_block";
            case 43 -> switch (damage) {
                case 1 -> "minecraft:double_sandstone_slab";
                case 2 -> "minecraft:double_oak_slab";
                case 3 -> "minecraft:double_cobblestone_slab";
                case 4 -> "minecraft:double_brick_slab";
                case 5 -> "minecraft:double_stone_brick_slab";
                case 6 -> "minecraft:double_nether_brick_slab";
                case 7 -> "minecraft:double_quartz_slab";
                default -> "minecraft:double_stone_slab";
            };
            case 44 -> switch (damage) {
                case 1 -> "minecraft:sandstone_slab";
                case 2 -> "minecraft:oak_slab";
                case 3 -> "minecraft:cobblestone_slab";
                case 4 -> "minecraft:brick_slab";
                case 5 -> "minecraft:stone_brick_slab";
                case 6 -> "minecraft:nether_brick_slab";
                case 7 -> "minecraft:quartz_slab";
                default -> "minecraft:stone_slab";
            };
            case 45 -> "minecraft:brick_block";
            case 46 -> "minecraft:tnt";
            case 47 -> "minecraft:bookshelf";
            case 48 -> "minecraft:mossy_cobblestone";
            case 49 -> "minecraft:obsidian";
            case 50 -> "minecraft:torch";
            case 51 -> "minecraft:fire";
            case 52 -> "minecraft:mob_spawner";
            case 53 -> "minecraft:oak_stairs";
            case 54 -> "minecraft:chest";
            case 55 -> "minecraft:redstone_wire";
            case 56 -> "minecraft:diamond_ore";
            case 57 -> "minecraft:diamond_block";
            case 58 -> "minecraft:crafting_table";
            case 59 -> "minecraft:wheat_block";
            case 60 -> "minecraft:farmland";
            case 61 -> "minecraft:furnace";
            case 62 -> "minecraft:lit_furnace";
            case 63 -> "minecraft:oak_sign";
            case 64 -> "minecraft:oak_door";
            case 65 -> "minecraft:ladder";
            case 66 -> "minecraft:rail";
            case 67 -> "minecraft:stone_stairs";
            case 68 -> "minecraft:oak_wall_sign";
            case 69 -> "minecraft:lever";
            case 70 -> "minecraft:stone_pressure_plate";
            case 71 -> "minecraft:iron_door_block";
            case 72 -> "minecraft:oak_pressure_plate";
            case 73 -> "minecraft:redstone_ore";
            case 74 -> "minecraft:lit_redstone_ore";
            case 75 -> "minecraft:unlit_redstone_torch";
            case 76 -> "minecraft:redstone_torch";
            case 77 -> "minecraft:stone_button";
            case 78 -> "minecraft:snow_layer";
            case 79 -> "minecraft:ice";
            case 80 -> "minecraft:snow_block";
            case 81 -> "minecraft:cactus";
            case 82 -> "minecraft:clay";
            case 83 -> "minecraft:reeds";
            case 84 -> "minecraft:jukebox";
            case 85 -> "minecraft:oak_fence";
            case 86 -> "minecraft:pumpkin";
            case 87 -> "minecraft:netherrack";
            case 88 -> "minecraft:soul_sand";
            case 89 -> "minecraft:glowstone";
            case 90 -> "minecraft:portal";
            case 91 -> "minecraft:jack_o_lantern";
            case 92 -> "minecraft:cake_block";
            case 93 -> "minecraft:unpowered_repeater";
            case 94 -> "minecraft:powered_repeater";
            case 95 -> switch (damage) {
                case 1 -> "minecraft:orange_stained_glass";
                case 2 -> "minecraft:magenta_stained_glass";
                case 3 -> "minecraft:light_blue_stained_glass";
                case 4 -> "minecraft:yellow_stained_glass";
                case 5 -> "minecraft:lime_stained_glass";
                case 6 -> "minecraft:pink_stained_glass";
                case 7 -> "minecraft:gray_stained_glass";
                case 8 -> "minecraft:light_gray_stained_glass";
                case 9 -> "minecraft:cyan_stained_glass";
                case 10 -> "minecraft:purple_stained_glass";
                case 11 -> "minecraft:blue_stained_glass";
                case 12 -> "minecraft:brown_stained_glass";
                case 13 -> "minecraft:green_stained_glass";
                case 14 -> "minecraft:red_stained_glass";
                case 15 -> "minecraft:black_stained_glass";
                default -> "minecraft:white_stained_glass";
            };
            case 96 -> "minecraft:oak_trapdoor";
            case 97 -> switch (damage) {
                case 1 -> "minecraft:infested_cobblestone";
                case 2 -> "minecraft:infested_stone_bricks";
                case 3 -> "minecraft:infested_mossy_stone_bricks";
                case 4 -> "minecraft:infested_cracked_stone_bricks";
                case 5 -> "minecraft:infested_chiseled_stone_bricks";
                default -> "minecraft:infested_stone";
            };
            case 98 -> switch (damage) {
                case 1 -> "minecraft:mossy_stone_bricks";
                case 2 -> "minecraft:cracked_stone_bricks";
                case 3 -> "minecraft:chiseled_stone_bricks";
                default -> "minecraft:stone_bricks";
            };
            case 99 -> "minecraft:brown_mushroom_block";
            case 100 -> "minecraft:red_mushroom_block";
            case 101 -> "minecraft:iron_bars";
            case 102 -> "minecraft:glass_pane";
            case 103 -> "minecraft:melon";
            case 104 -> "minecraft:pumpkin_stem";
            case 105 -> "minecraft:melon_stem";
            case 106 -> "minecraft:vine";
            case 107 -> "minecraft:oak_fence_gate";
            case 108 -> "minecraft:brick_stairs";
            case 109 -> "minecraft:stone_brick_stairs";
            case 110 -> "minecraft:mycelium";
            case 111 -> "minecraft:lily_pad";
            case 112 -> "minecraft:nether_brick";
            case 113 -> "minecraft:nether_brick_fence";
            case 114 -> "minecraft:nether_brick_stairs";
            case 115 -> "minecraft:nether_wart_block";
            case 116 -> "minecraft:enchanting_table";
            case 117 -> "minecraft:brewing_stand";
            case 118 -> "minecraft:cauldron";
            case 119 -> "minecraft:end_portal";
            case 120 -> "minecraft:end_portal_frame";
            case 121 -> "minecraft:end_stone";
            case 122 -> "minecraft:dragon_egg";
            case 123 -> "minecraft:redstone_lamp";
            case 124 -> "minecraft:lit_redstone_lamp";
            case 125 -> switch (damage) {
                case 1 -> "minecraft:double_spruce_slab";
                case 2 -> "minecraft:double_birch_slab";
                case 3 -> "minecraft:double_jungle_slab";
                case 4 -> "minecraft:double_acacia_slab";
                case 5 -> "minecraft:double_dark_oak_slab";
                default -> "minecraft:double_oak_slab";
            };
            case 126 -> switch (damage) {
                case 1 -> "minecraft:spruce_slab";
                case 2 -> "minecraft:birch_slab";
                case 3 -> "minecraft:jungle_slab";
                case 4 -> "minecraft:acacia_slab";
                case 5 -> "minecraft:dark_oak_slab";
                default -> "minecraft:oak_slab";
            };
            case 127 -> "minecraft:cocoa";
            case 128 -> "minecraft:sandstone_stairs";
            case 129 -> "minecraft:emerald_ore";
            case 130 -> "minecraft:ender_chest";
            case 131 -> "minecraft:tripwire_hook";
            case 132 -> "minecraft:tripwire";
            case 133 -> "minecraft:emerald_block";
            case 134 -> "minecraft:spruce_stairs";
            case 135 -> "minecraft:birch_stairs";
            case 136 -> "minecraft:jungle_stairs";
            case 137 -> "minecraft:command_block";
            case 138 -> "minecraft:beacon";
            case 139 -> switch (damage) {
                case 1 -> "minecraft:mossy_cobblestone_wall";
                default -> "minecraft:cobblestone_wall";
            };
            case 140 -> "minecraft:flower_pot";
            case 141 -> "minecraft:carrot";
            case 142 -> "minecraft:potato";
            case 143 -> "minecraft:wooden_button";
            case 144 -> "minecraft:skeleton_skull";
            case 145 -> "minecraft:anvil";
            case 146 -> "minecraft:trapped_chest";
            case 147 -> "minecraft:light_weighted_pressure_plate";
            case 148 -> "minecraft:heavy_weighted_pressure_plate";
            case 149 -> "minecraft:unpowered_comparator";
            case 150 -> "minecraft:powered_comparator";
            case 151 -> "minecraft:daylight_detector";
            case 152 -> "minecraft:redstone_block";
            case 153 -> "minecraft:quartz_ore";
            case 154 -> "minecraft:hopper";
            case 155 -> switch (damage) {
                case 1 -> "minecraft:chiseled_quartz_block";
                case 2 -> "minecraft:quartz_pillar";
                default -> "minecraft:quartz_block";
            };
            case 156 -> "minecraft:quartz_stairs";
            case 157 -> "minecraft:activator_rail";
            case 158 -> "minecraft:dropper";
            case 159 -> switch (damage) {
                case 1 -> "minecraft:orange_terracotta";
                case 2 -> "minecraft:magenta_terracotta";
                case 3 -> "minecraft:light_blue_terracotta";
                case 4 -> "minecraft:yellow_terracotta";
                case 5 -> "minecraft:lime_terracotta";
                case 6 -> "minecraft:pink_terracotta";
                case 7 -> "minecraft:gray_terracotta";
                case 8 -> "minecraft:light_gray_terracotta";
                case 9 -> "minecraft:cyan_terracotta";
                case 10 -> "minecraft:purple_terracotta";
                case 11 -> "minecraft:blue_terracotta";
                case 12 -> "minecraft:brown_terracotta";
                case 13 -> "minecraft:green_terracotta";
                case 14 -> "minecraft:red_terracotta";
                case 15 -> "minecraft:black_terracotta";
                default -> "minecraft:white_terracotta";
            };
            case 160 -> switch (damage) {
                case 1 -> "minecraft:orange_stained_glass_pane";
                case 2 -> "minecraft:magenta_stained_glass_pane";
                case 3 -> "minecraft:light_blue_stained_glass_pane";
                case 4 -> "minecraft:yellow_stained_glass_pane";
                case 5 -> "minecraft:lime_stained_glass_pane";
                case 6 -> "minecraft:pink_stained_glass_pane";
                case 7 -> "minecraft:gray_stained_glass_pane";
                case 8 -> "minecraft:light_gray_stained_glass_pane";
                case 9 -> "minecraft:cyan_stained_glass_pane";
                case 10 -> "minecraft:purple_stained_glass_pane";
                case 11 -> "minecraft:blue_stained_glass_pane";
                case 12 -> "minecraft:brown_stained_glass_pane";
                case 13 -> "minecraft:green_stained_glass_pane";
                case 14 -> "minecraft:red_stained_glass_pane";
                case 15 -> "minecraft:black_stained_glass_pane";
                default -> "minecraft:white_stained_glass_pane";
            };
            case 161 -> switch (damage) {
                case 1 -> "minecraft:dark_oak_leaves";
                default -> "minecraft:acacia_leaves";
            };
            case 162 -> switch (damage) {
                case 1 -> "minecraft:dark_oak_log";
                default -> "minecraft:acacia_log";
            };
            case 163 -> "minecraft:acacia_stairs";
            case 164 -> "minecraft:dark_oak_stairs";
            case 165 -> "minecraft:slime_block";
            case 166 -> "minecraft:barrier";
            case 167 -> "minecraft:iron_trapdoor";
            case 168 -> switch (damage) {
                case 1 -> "minecraft:prismarine_bricks";
                case 2 -> "minecraft:dark_prismarine";
                default -> "minecraft:prismarine";
            };
            case 169 -> "minecraft:sea_lantern";
            case 170 -> "minecraft:hay_block";
            case 171 -> switch (damage) {
                case 1 -> "minecraft:orange_carpet";
                case 2 -> "minecraft:magenta_carpet";
                case 3 -> "minecraft:light_blue_carpet";
                case 4 -> "minecraft:yellow_carpet";
                case 5 -> "minecraft:lime_carpet";
                case 6 -> "minecraft:pink_carpet";
                case 7 -> "minecraft:gray_carpet";
                case 8 -> "minecraft:light_gray_carpet";
                case 9 -> "minecraft:cyan_carpet";
                case 10 -> "minecraft:purple_carpet";
                case 11 -> "minecraft:blue_carpet";
                case 12 -> "minecraft:brown_carpet";
                case 13 -> "minecraft:green_carpet";
                case 14 -> "minecraft:red_carpet";
                case 15 -> "minecraft:black_carpet";
                default -> "minecraft:white_carpet";
            };
            case 172 -> "minecraft:terracotta";
            case 173 -> "minecraft:coal_block";
            case 174 -> "minecraft:packed_ice";
            case 175 -> switch (damage) {
                case 1 -> "minecraft:lilac";
                case 2 -> "minecraft:tall_grass";
                case 3 -> "minecraft:large_fern";
                case 4 -> "minecraft:rose_bush";
                case 5 -> "minecraft:peony";
                default -> "minecraft:sunflower";
            };
            case 176 -> "minecraft:standing_banner";
            case 177 -> "minecraft:wall_banner";
            case 178 -> "minecraft:daylight_detector_inverted";
            case 179 -> switch (damage) {
                case 1 -> "minecraft:chiseled_red_sandstone";
                case 2 -> "minecraft:smooth_red_sandstone";
                default -> "minecraft:red_sandstone";
            };
            case 180 -> "minecraft:red_sandstone_stairs";
            case 181 -> "minecraft:double_stone_slab2";
            case 182 -> "minecraft:stone_slab2";
            case 183 -> "minecraft:spruce_fence_gate";
            case 184 -> "minecraft:birch_fence_gate";
            case 185 -> "minecraft:jungle_fence_gate";
            case 186 -> "minecraft:dark_oak_fence_gate";
            case 187 -> "minecraft:acacia_fence_gate";
            case 188 -> "minecraft:spruce_fence";
            case 189 -> "minecraft:birch_fence";
            case 190 -> "minecraft:jungle_fence";
            case 191 -> "minecraft:dark_oak_fence";
            case 192 -> "minecraft:acacia_fence";
            case 193 -> "minecraft:spruce_door_block";
            case 194 -> "minecraft:birch_door_block";
            case 195 -> "minecraft:jungle_door_block";
            case 196 -> "minecraft:acacia_door_block";
            case 197 -> "minecraft:dark_oak_door_block";
            case 198 -> "minecraft:end_rod";
            case 199 -> "minecraft:chorus_plant";
            case 200 -> "minecraft:chorus_flower";
            case 201 -> "minecraft:purpur_block";
            case 202 -> "minecraft:purpur_pillar";
            case 203 -> "minecraft:purpur_stairs";
            case 204 -> "minecraft:purpur_double_slab";
            case 205 -> "minecraft:purpur_slab";
            case 206 -> "minecraft:end_bricks";
            case 207 -> "minecraft:beetroots";
            case 208 -> "minecraft:grass_path";
            case 209 -> "minecraft:end_gateway";
            case 210 -> "minecraft:repeating_command_block";
            case 211 -> "minecraft:chain_command_block";
            case 212 -> "minecraft:frosted_ice";
            case 213 -> "minecraft:magma";
            case 214 -> "minecraft:nether_wart_block";
            case 215 -> "minecraft:red_nether_brick";
            case 216 -> "minecraft:bone_block";
            case 217 -> "minecraft:structure_void";
            case 218 -> "minecraft:observer";
            case 219 -> "minecraft:white_shulker_box";
            case 220 -> "minecraft:orange_shulker_box";
            case 221 -> "minecraft:magenta_shulker_box";
            case 222 -> "minecraft:light_blue_shulker_box";
            case 223 -> "minecraft:yellow_shulker_box";
            case 224 -> "minecraft:lime_shulker_box";
            case 225 -> "minecraft:pink_shulker_box";
            case 226 -> "minecraft:gray_shulker_box";
            case 227 -> "minecraft:light_gray_shulker_box";
            case 228 -> "minecraft:cyan_shulker_box";
            case 229 -> "minecraft:purple_shulker_box";
            case 230 -> "minecraft:blue_shulker_box";
            case 231 -> "minecraft:brown_shulker_box";
            case 232 -> "minecraft:green_shulker_box";
            case 233 -> "minecraft:red_shulker_box";
            case 234 -> "minecraft:black_shulker_box";
            case 235 -> "minecraft:white_glazed_terracotta";
            case 236 -> "minecraft:orange_glazed_terracotta";
            case 237 -> "minecraft:magenta_glazed_terracotta";
            case 238 -> "minecraft:light_blue_glazed_terracotta";
            case 239 -> "minecraft:yellow_glazed_terracotta";
            case 240 -> "minecraft:lime_glazed_terracotta";
            case 241 -> "minecraft:pink_glazed_terracotta";
            case 242 -> "minecraft:gray_glazed_terracotta";
            case 243 -> "minecraft:light_gray_glazed_terracotta";
            case 244 -> "minecraft:cyan_glazed_terracotta";
            case 245 -> "minecraft:purple_glazed_terracotta";
            case 246 -> "minecraft:blue_glazed_terracotta";
            case 247 -> "minecraft:brown_glazed_terracotta";
            case 248 -> "minecraft:green_glazed_terracotta";
            case 249 -> "minecraft:red_glazed_terracotta";
            case 250 -> "minecraft:black_glazed_terracotta";
            case 251 -> switch (damage) {
                case 1 -> "minecraft:orange_concrete";
                case 2 -> "minecraft:magenta_concrete";
                case 3 -> "minecraft:light_blue_concrete";
                case 4 -> "minecraft:yellow_concrete";
                case 5 -> "minecraft:lime_concrete";
                case 6 -> "minecraft:pink_concrete";
                case 7 -> "minecraft:gray_concrete";
                case 8 -> "minecraft:light_gray_concrete";
                case 9 -> "minecraft:cyan_concrete";
                case 10 -> "minecraft:purple_concrete";
                case 11 -> "minecraft:blue_concrete";
                case 12 -> "minecraft:brown_concrete";
                case 13 -> "minecraft:green_concrete";
                case 14 -> "minecraft:red_concrete";
                case 15 -> "minecraft:black_concrete";
                default -> "minecraft:white_concrete";
            };
            case 252 -> switch (damage) {
                case 1 -> "minecraft:orange_concrete_powder";
                case 2 -> "minecraft:magenta_concrete_powder";
                case 3 -> "minecraft:light_blue_concrete_powder";
                case 4 -> "minecraft:yellow_concrete_powder";
                case 5 -> "minecraft:lime_concrete_powder";
                case 6 -> "minecraft:pink_concrete_powder";
                case 7 -> "minecraft:gray_concrete_powder";
                case 8 -> "minecraft:light_gray_concrete_powder";
                case 9 -> "minecraft:cyan_concrete_powder";
                case 10 -> "minecraft:purple_concrete_powder";
                case 11 -> "minecraft:blue_concrete_powder";
                case 12 -> "minecraft:brown_concrete_powder";
                case 13 -> "minecraft:green_concrete_powder";
                case 14 -> "minecraft:red_concrete_powder";
                case 15 -> "minecraft:black_concrete_powder";
                default -> "minecraft:white_concrete_powder";
            };
            case 253, 254 -> "minecraft:air"; // Reserved
            case 255 -> "minecraft:structure_block";
            case 256 -> "minecraft:iron_shovel";
            case 257 -> "minecraft:iron_pickaxe";
            case 258 -> "minecraft:iron_axe";
            case 259 -> "minecraft:flint_and_steel";
            case 260 -> "minecraft:apple";
            case 261 -> "minecraft:bow";
            case 262 -> "minecraft:arrow";
            case 263 -> "minecraft:coal";
            case 264 -> "minecraft:diamond";
            case 265 -> "minecraft:iron_ingot";
            case 266 -> "minecraft:gold_ingot";
            case 267 -> "minecraft:iron_sword";
            case 268 -> "minecraft:wooden_sword";
            case 269 -> "minecraft:wooden_shovel";
            case 270 -> "minecraft:wooden_pickaxe";
            case 271 -> "minecraft:wooden_axe";
            case 272 -> "minecraft:stone_sword";
            case 273 -> "minecraft:stone_shovel";
            case 274 -> "minecraft:stone_pickaxe";
            case 275 -> "minecraft:stone_axe";
            case 276 -> "minecraft:diamond_sword";
            case 277 -> "minecraft:diamond_shovel";
            case 278 -> "minecraft:diamond_pickaxe";
            case 279 -> "minecraft:diamond_axe";
            case 280 -> "minecraft:stick";
            case 281 -> "minecraft:bowl";
            case 282 -> "minecraft:mushroom_stew";
            case 283 -> "minecraft:golden_sword";
            case 284 -> "minecraft:golden_shovel";
            case 285 -> "minecraft:golden_pickaxe";
            case 286 -> "minecraft:golden_axe";
            case 287 -> "minecraft:string";
            case 288 -> "minecraft:feather";
            case 289 -> "minecraft:gunpowder";
            case 290 -> "minecraft:wooden_hoe";
            case 291 -> "minecraft:stone_hoe";
            case 292 -> "minecraft:iron_hoe";
            case 293 -> "minecraft:diamond_hoe";
            case 294 -> "minecraft:golden_hoe";
            case 295 -> "minecraft:wheat_seeds";
            case 296 -> "minecraft:wheat";
            case 297 -> "minecraft:bread";
            case 298 -> "minecraft:leather_helmet";
            case 299 -> "minecraft:leather_chestplate";
            case 300 -> "minecraft:leather_leggings";
            case 301 -> "minecraft:leather_boots";
            case 302 -> "minecraft:chainmail_helmet";
            case 303 -> "minecraft:chainmail_chestplate";
            case 304 -> "minecraft:chainmail_leggings";
            case 305 -> "minecraft:chainmail_boots";
            case 306 -> "minecraft:iron_helmet";
            case 307 -> "minecraft:iron_chestplate";
            case 308 -> "minecraft:iron_leggings";
            case 309 -> "minecraft:iron_boots";
            case 310 -> "minecraft:diamond_helmet";
            case 311 -> "minecraft:diamond_chestplate";
            case 312 -> "minecraft:diamond_leggings";
            case 313 -> "minecraft:diamond_boots";
            case 314 -> "minecraft:golden_helmet";
            case 315 -> "minecraft:golden_chestplate";
            case 316 -> "minecraft:golden_leggings";
            case 317 -> "minecraft:golden_boots";
            case 318 -> "minecraft:flint";
            case 319 -> "minecraft:porkchop";
            case 320 -> "minecraft:cooked_porkchop";
            case 321 -> "minecraft:painting";
            case 322 -> switch (damage) {
                case 1 -> "minecraft:enchanted_golden_apple";
                default -> "minecraft:golden_apple";
            };
            case 323 -> "minecraft:oak_sign";
            case 324 -> "minecraft:oak_door";
            case 325 -> "minecraft:bucket";
            case 326 -> "minecraft:water_bucket";
            case 327 -> "minecraft:lava_bucket";
            case 328 -> "minecraft:minecart";
            case 329 -> "minecraft:saddle";
            case 330 -> "minecraft:iron_door";
            case 331 -> "minecraft:redstone";
            case 332 -> "minecraft:snowball";
            case 333 -> "minecraft:oak_boat";
            case 334 -> "minecraft:leather";
            case 335 -> "minecraft:milk_bucket";
            case 336 -> "minecraft:brick";
            case 337 -> "minecraft:clay_ball";
            case 338 -> "minecraft:sugar_cane";
            case 339 -> "minecraft:paper";
            case 340 -> "minecraft:book";
            case 341 -> "minecraft:slime_ball";
            case 342 -> "minecraft:chest_minecart";
            case 343 -> "minecraft:furnace_minecart";
            case 344 -> "minecraft:egg";
            case 345 -> "minecraft:compass";
            case 346 -> "minecraft:fishing_rod";
            case 347 -> "minecraft:clock";
            case 348 -> "minecraft:glowstone_dust";
            case 349 -> switch (damage) {
                case 1 -> "minecraft:salmon";
                case 2 -> "minecraft:tropical_fish";
                case 3 -> "minecraft:pufferfish";
                default -> "minecraft:cod";
            };
            case 350 -> switch (damage) {
                case 1 -> "minecraft:cooked_salmon";
                default -> "minecraft:cooked_cod";
            };
            case 351 -> switch (damage) {
                case 1 -> "minecraft:red_dye";
                case 2 -> "minecraft:green_dye";
                case 3 -> "minecraft:cocoa_beans";
                case 4 -> "minecraft:lapis_lazuli";
                case 5 -> "minecraft:purple_dye";
                case 6 -> "minecraft:cyan_dye";
                case 7 -> "minecraft:light_gray_dye";
                case 8 -> "minecraft:gray_dye";
                case 9 -> "minecraft:pink_dye";
                case 10 -> "minecraft:lime_dye";
                case 11 -> "minecraft:yellow_dye";
                case 12 -> "minecraft:light_blue_dye";
                case 13 -> "minecraft:magenta_dye";
                case 14 -> "minecraft:orange_dye";
                case 15 -> "minecraft:bone_meal";
                default -> "minecraft:ink_sac";
            };
            case 352 -> "minecraft:bone";
            case 353 -> "minecraft:sugar";
            case 354 -> "minecraft:cake";
            case 355 -> "minecraft:bed";
            case 356 -> "minecraft:repeater";
            case 357 -> "minecraft:cookie";
            case 358 -> "minecraft:filled_map";
            case 359 -> "minecraft:shears";
            case 360 -> "minecraft:melon_slice";
            case 361 -> "minecraft:pumpkin_seeds";
            case 362 -> "minecraft:melon_seeds";
            case 363 -> "minecraft:beef";
            case 364 -> "minecraft:cooked_beef";
            case 365 -> "minecraft:chicken";
            case 366 -> "minecraft:cooked_chicken";
            case 367 -> "minecraft:rotten_flesh";
            case 368 -> "minecraft:ender_pearl";
            case 369 -> "minecraft:blaze_rod";
            case 370 -> "minecraft:ghast_tear";
            case 371 -> "minecraft:gold_nugget";
            case 372 -> "minecraft:nether_wart";
            case 373 -> "minecraft:potion";
            case 374 -> "minecraft:glass_bottle";
            case 375 -> "minecraft:spider_eye";
            case 376 -> "minecraft:fermented_spider_eye";
            case 377 -> "minecraft:blaze_powder";
            case 378 -> "minecraft:magma_cream";
            case 379 -> "minecraft:brewingstand";
            case 380 -> "minecraft:cauldron";
            case 381 -> "minecraft:ender_eye";
            case 382 -> "minecraft:glistering_melon_slice";
            case 383 -> switch (damage) {
                case 4 -> "minecraft:elder_guardian_spawn_egg";
                case 5 -> "minecraft:wither_skeleton_spawn_egg";
                case 6 -> "minecraft:stray_spawn_egg";
                case 23 -> "minecraft:husk_spawn_egg";
                case 27 -> "minecraft:zombie_villager_spawn_egg";
                case 28 -> "minecraft:skeleton_horse_spawn_egg";
                case 29 -> "minecraft:zombie_horse_spawn_egg";
                case 31 -> "minecraft:donkey_spawn_egg";
                case 32 -> "minecraft:mule_spawn_egg";
                case 34 -> "minecraft:evoker_spawn_egg";
                case 35 -> "minecraft:vex_spawn_egg";
                case 36 -> "minecraft:vindicator_spawn_egg";
                case 50 -> "minecraft:creeper_spawn_egg";
                case 51 -> "minecraft:skeleton_spawn_egg";
                case 52 -> "minecraft:spider_spawn_egg";
                case 54 -> "minecraft:zombie_spawn_egg";
                case 55 -> "minecraft:slime_spawn_egg";
                case 56 -> "minecraft:ghast_spawn_egg";
                case 57 -> "minecraft:zombie_pigman_spawn_egg";
                case 58 -> "minecraft:enderman_spawn_egg";
                case 59 -> "minecraft:cave_spider_spawn_egg";
                case 60 -> "minecraft:silverfish_spawn_egg";
                case 61 -> "minecraft:blaze_spawn_egg";
                case 62 -> "minecraft:magma_cube_spawn_egg";
                case 65 -> "minecraft:bat_spawn_egg";
                case 66 -> "minecraft:witch_spawn_egg";
                case 67 -> "minecraft:endermite_spawn_egg";
                case 68 -> "minecraft:guardian_spawn_egg";
                case 69 -> "minecraft:shulker_spawn_egg";
                case 90 -> "minecraft:pig_spawn_egg";
                case 91 -> "minecraft:sheep_spawn_egg";
                case 92 -> "minecraft:cow_spawn_egg";
                case 93 -> "minecraft:chicken_spawn_egg";
                case 94 -> "minecraft:squid_spawn_egg";
                case 95 -> "minecraft:wolf_spawn_egg";
                case 96 -> "minecraft:mooshroom_spawn_egg";
                case 98 -> "minecraft:ocelot_spawn_egg";
                case 100 -> "minecraft:horse_spawn_egg";
                case 101 -> "minecraft:rabbit_spawn_egg";
                case 102 -> "minecraft:polar_bear_spawn_egg";
                case 103 -> "minecraft:llama_spawn_egg";
                case 105 -> "minecraft:parrot_spawn_egg";
                case 120 -> "minecraft:villager_spawn_egg";
                default -> "minecraft:zombie_spawn_egg";
            };
            case 384 -> "minecraft:experience_bottle";
            case 385 -> "minecraft:fire_charge";
            case 386 -> "minecraft:writable_book";
            case 387 -> "minecraft:written_book";
            case 388 -> "minecraft:emerald";
            case 389 -> "minecraft:item_frame";
            case 390 -> "minecraft:flower_pot";
            case 391 -> "minecraft:carrot";
            case 392 -> "minecraft:potato";
            case 393 -> "minecraft:baked_potato";
            case 394 -> "minecraft:poisonous_potato";
            case 395 -> "minecraft:map";
            case 396 -> "minecraft:golden_carrot";
            case 397 -> switch (damage) {
                case 0 -> "minecraft:skeleton_skull";
                case 1 -> "minecraft:wither_skeleton_skull";
                case 2 -> "minecraft:zombie_head";
                case 4 -> "minecraft:creeper_head";
                case 5 -> "minecraft:dragon_head";
                default -> "minecraft:player_head";
            };
            case 398 -> "minecraft:carrot_on_a_stick";
            case 399 -> "minecraft:nether_star";
            case 400 -> "minecraft:pumpkin_pie";
            case 401 -> "minecraft:firework_rocket";
            case 402 -> "minecraft:firework_star";
            case 403 -> "minecraft:enchanted_book";
            case 404 -> "minecraft:comparator";
            case 405 -> "minecraft:nether_brick";
            case 406 -> "minecraft:quartz";
            case 407 -> "minecraft:tnt_minecart";
            case 408 -> "minecraft:hopper_minecart";
            case 409 -> "minecraft:prismarine_shard";
            case 410 -> "minecraft:prismarine_crystals";
            case 411 -> "minecraft:rabbit";
            case 412 -> "minecraft:cooked_rabbit";
            case 413 -> "minecraft:rabbit_stew";
            case 414 -> "minecraft:rabbit_foot";
            case 415 -> "minecraft:rabbit_hide";
            case 416 -> "minecraft:armor_stand";
            case 417 -> "minecraft:iron_horse_armor";
            case 418 -> "minecraft:golden_horse_armor";
            case 419 -> "minecraft:diamond_horse_armor";
            case 420 -> "minecraft:lead";
            case 421 -> "minecraft:name_tag";
            case 422 -> "minecraft:command_block_minecart";
            case 423 -> "minecraft:mutton";
            case 424 -> "minecraft:cooked_mutton";
            case 425 -> "minecraft:white_banner";
            case 426 -> "minecraft:end_crystal";
            case 427 -> "minecraft:spruce_door";
            case 428 -> "minecraft:birch_door";
            case 429 -> "minecraft:jungle_door";
            case 430 -> "minecraft:acacia_door";
            case 431 -> "minecraft:dark_oak_door";
            case 432 -> "minecraft:chorus_fruit";
            case 433 -> "minecraft:chorus_fruit_popped";
            case 434 -> "minecraft:beetroot";
            case 435 -> "minecraft:beetroot_seeds";
            case 436 -> "minecraft:beetroot_soup";
            case 437 -> "minecraft:dragon_breath";
            case 438 -> "minecraft:splash_potion";
            case 439 -> "minecraft:spectral_arrow";
            case 440 -> "minecraft:tipped_arrow";
            case 441 -> "minecraft:lingering_potion";
            case 442 -> "minecraft:shield";
            case 443 -> "minecraft:elytra";
            case 444 -> "minecraft:spruce_boat";
            case 445 -> "minecraft:birch_boat";
            case 446 -> "minecraft:jungle_boat";
            case 447 -> "minecraft:acacia_boat";
            case 448 -> "minecraft:dark_oak_boat";
            case 2256 -> "minecraft:music_disc_13";
            case 2257 -> "minecraft:music_disc_cat";
            case 2258 -> "minecraft:music_disc_blocks";
            case 2259 -> "minecraft:music_disc_chirp";
            case 2260 -> "minecraft:music_disc_far";
            case 2261 -> "minecraft:music_disc_mall";
            case 2262 -> "minecraft:music_disc_mellohi";
            case 2263 -> "minecraft:music_disc_stal";
            case 2264 -> "minecraft:music_disc_strad";
            case 2265 -> "minecraft:music_disc_ward";
            case 2266 -> "minecraft:music_disc_11";
            case 2267 -> "minecraft:music_disc_wait";
            default -> "";
        };
    }

    // -- Bukkit material name resolution --

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private static short mapBukkit(@NotNull String id) {
        return switch (id) {
            case "AIR" -> 0;
            case "STONE" -> 1;
            case "GRASS" -> 2;
            case "DIRT" -> 3;
            case "COBBLESTONE" -> 4;
            case "WOOD" -> 5;
            case "SAPLING" -> 6;
            case "BEDROCK" -> 7;
            case "WATER" -> 8;
            case "STATIONARY_WATER" -> 9;
            case "LAVA" -> 10;
            case "STATIONARY_LAVA" -> 11;
            case "SAND" -> 12;
            case "GRAVEL" -> 13;
            case "GOLD_ORE" -> 14;
            case "IRON_ORE" -> 15;
            case "COAL_ORE" -> 16;
            case "LOG" -> 17;
            case "LEAVES" -> 18;
            case "SPONGE" -> 19;
            case "GLASS" -> 20;
            case "LAPIS_ORE" -> 21;
            case "LAPIS_BLOCK" -> 22;
            case "DISPENSER" -> 23;
            case "SANDSTONE" -> 24;
            case "NOTE_BLOCK" -> 25;
            case "BED_BLOCK" -> 26;
            case "POWERED_RAIL" -> 27;
            case "DETECTOR_RAIL" -> 28;
            case "PISTON_STICKY_BASE" -> 29;
            case "WEB" -> 30;
            case "LONG_GRASS" -> 31;
            case "DEAD_BUSH" -> 32;
            case "PISTON_BASE" -> 33;
            case "PISTON_EXTENSION" -> 34;
            case "WOOL" -> 35;
            case "PISTON_MOVING_PIECE" -> 36;
            case "YELLOW_FLOWER" -> 37;
            case "RED_ROSE" -> 38;
            case "BROWN_MUSHROOM" -> 39;
            case "RED_MUSHROOM" -> 40;
            case "GOLD_BLOCK" -> 41;
            case "IRON_BLOCK" -> 42;
            case "DOUBLE_STEP" -> 43;
            case "STEP" -> 44;
            case "BRICK" -> 45;
            case "TNT" -> 46;
            case "BOOKSHELF" -> 47;
            case "MOSSY_COBBLESTONE" -> 48;
            case "OBSIDIAN" -> 49;
            case "TORCH" -> 50;
            case "FIRE" -> 51;
            case "MOB_SPAWNER" -> 52;
            case "WOOD_STAIRS" -> 53;
            case "CHEST" -> 54;
            case "REDSTONE_WIRE" -> 55;
            case "DIAMOND_ORE" -> 56;
            case "DIAMOND_BLOCK" -> 57;
            case "WORKBENCH" -> 58;
            case "CROPS" -> 59;
            case "SOIL" -> 60;
            case "FURNACE" -> 61;
            case "BURNING_FURNACE" -> 62;
            case "SIGN_POST" -> 63;
            case "WOODEN_DOOR" -> 64;
            case "LADDER" -> 65;
            case "RAILS" -> 66;
            case "COBBLESTONE_STAIRS" -> 67;
            case "WALL_SIGN" -> 68;
            case "LEVER" -> 69;
            case "STONE_PLATE" -> 70;
            case "IRON_DOOR_BLOCK" -> 71;
            case "WOOD_PLATE" -> 72;
            case "REDSTONE_ORE" -> 73;
            case "GLOWING_REDSTONE_ORE" -> 74;
            case "REDSTONE_TORCH_OFF" -> 75;
            case "REDSTONE_TORCH_ON" -> 76;
            case "STONE_BUTTON" -> 77;
            case "SNOW" -> 78;
            case "ICE" -> 79;
            case "SNOW_BLOCK" -> 80;
            case "CACTUS" -> 81;
            case "CLAY" -> 82;
            case "SUGAR_CANE_BLOCK" -> 83;
            case "JUKEBOX" -> 84;
            case "FENCE" -> 85;
            case "PUMPKIN" -> 86;
            case "NETHERRACK" -> 87;
            case "SOUL_SAND" -> 88;
            case "GLOWSTONE" -> 89;
            case "PORTAL" -> 90;
            case "JACK_O_LANTERN" -> 91;
            case "CAKE_BLOCK" -> 92;
            case "DIODE_BLOCK_OFF" -> 93;
            case "DIODE_BLOCK_ON" -> 94;
            case "STAINED_GLASS" -> 95;
            case "TRAP_DOOR" -> 96;
            case "MONSTER_EGGS" -> 97;
            case "SMOOTH_BRICK" -> 98;
            case "HUGE_MUSHROOM_1" -> 99;
            case "HUGE_MUSHROOM_2" -> 100;
            case "IRON_FENCE" -> 101;
            case "THIN_GLASS" -> 102;
            case "MELON_BLOCK" -> 103;
            case "PUMPKIN_STEM" -> 104;
            case "MELON_STEM" -> 105;
            case "VINE" -> 106;
            case "FENCE_GATE" -> 107;
            case "BRICK_STAIRS" -> 108;
            case "SMOOTH_STAIRS" -> 109;
            case "MYCEL" -> 110;
            case "WATER_LILY" -> 111;
            case "NETHER_BRICK" -> 112;
            case "NETHER_FENCE" -> 113;
            case "NETHER_BRICK_STAIRS" -> 114;
            case "NETHER_WARTS" -> 115;
            case "ENCHANTMENT_TABLE" -> 116;
            case "BREWING_STAND" -> 117;
            case "CAULDRON" -> 118;
            case "ENDER_PORTAL" -> 119;
            case "ENDER_PORTAL_FRAME" -> 120;
            case "ENDER_STONE" -> 121;
            case "DRAGON_EGG" -> 122;
            case "REDSTONE_LAMP_OFF" -> 123;
            case "REDSTONE_LAMP_ON" -> 124;
            case "WOOD_DOUBLE_STEP" -> 125;
            case "WOOD_STEP" -> 126;
            case "COCOA" -> 127;
            case "SANDSTONE_STAIRS" -> 128;
            case "EMERALD_ORE" -> 129;
            case "ENDER_CHEST" -> 130;
            case "TRIPWIRE_HOOK" -> 131;
            case "TRIPWIRE" -> 132;
            case "EMERALD_BLOCK" -> 133;
            case "SPRUCE_WOOD_STAIRS" -> 134;
            case "BIRCH_WOOD_STAIRS" -> 135;
            case "JUNGLE_WOOD_STAIRS" -> 136;
            case "COMMAND" -> 137;
            case "BEACON" -> 138;
            case "COBBLE_WALL" -> 139;
            case "FLOWER_POT" -> 140;
            case "CARROT" -> 141;
            case "POTATO" -> 142;
            case "WOOD_BUTTON" -> 143;
            case "SKULL" -> 144;
            case "ANVIL" -> 145;
            case "TRAPPED_CHEST" -> 146;
            case "GOLD_PLATE" -> 147;
            case "IRON_PLATE" -> 148;
            case "REDSTONE_COMPARATOR_OFF" -> 149;
            case "REDSTONE_COMPARATOR_ON" -> 150;
            case "DAYLIGHT_DETECTOR" -> 151;
            case "REDSTONE_BLOCK" -> 152;
            case "QUARTZ_ORE" -> 153;
            case "HOPPER" -> 154;
            case "QUARTZ_BLOCK" -> 155;
            case "QUARTZ_STAIRS" -> 156;
            case "ACTIVATOR_RAIL" -> 157;
            case "DROPPER" -> 158;
            case "STAINED_CLAY" -> 159;
            case "STAINED_GLASS_PANE" -> 160;
            case "LEAVES_2" -> 161;
            case "LOG_2" -> 162;
            case "ACACIA_STAIRS" -> 163;
            case "DARK_OAK_STAIRS" -> 164;
            case "SLIME_BLOCK" -> 165;
            case "BARRIER" -> 166;
            case "IRON_TRAPDOOR" -> 167;
            case "PRISMARINE" -> 168;
            case "SEA_LANTERN" -> 169;
            case "HAY_BLOCK" -> 170;
            case "CARPET" -> 171;
            case "HARD_CLAY" -> 172;
            case "COAL_BLOCK" -> 173;
            case "PACKED_ICE" -> 174;
            case "DOUBLE_PLANT" -> 175;
            case "STANDING_BANNER" -> 176;
            case "WALL_BANNER" -> 177;
            case "DAYLIGHT_DETECTOR_INVERTED" -> 178;
            case "RED_SANDSTONE" -> 179;
            case "RED_SANDSTONE_STAIRS" -> 180;
            case "DOUBLE_STONE_SLAB2" -> 181;
            case "STONE_SLAB2" -> 182;
            case "SPRUCE_FENCE_GATE" -> 183;
            case "BIRCH_FENCE_GATE" -> 184;
            case "JUNGLE_FENCE_GATE" -> 185;
            case "DARK_OAK_FENCE_GATE" -> 186;
            case "ACACIA_FENCE_GATE" -> 187;
            case "SPRUCE_FENCE" -> 188;
            case "BIRCH_FENCE" -> 189;
            case "JUNGLE_FENCE" -> 190;
            case "DARK_OAK_FENCE" -> 191;
            case "ACACIA_FENCE" -> 192;
            case "SPRUCE_DOOR" -> 193;
            case "BIRCH_DOOR" -> 194;
            case "JUNGLE_DOOR" -> 195;
            case "ACACIA_DOOR" -> 196;
            case "DARK_OAK_DOOR" -> 197;
            case "END_ROD" -> 198;
            case "CHORUS_PLANT" -> 199;
            case "CHORUS_FLOWER" -> 200;
            case "PURPUR_BLOCK" -> 201;
            case "PURPUR_PILLAR" -> 202;
            case "PURPUR_STAIRS" -> 203;
            case "PURPUR_DOUBLE_SLAB" -> 204;
            case "PURPUR_SLAB" -> 205;
            case "END_BRICKS" -> 206;
            case "BEETROOT_BLOCK" -> 207;
            case "GRASS_PATH" -> 208;
            case "END_GATEWAY" -> 209;
            case "COMMAND_REPEATING" -> 210;
            case "COMMAND_CHAIN" -> 211;
            case "FROSTED_ICE" -> 212;
            case "MAGMA" -> 213;
            case "NETHER_WART_BLOCK" -> 214;
            case "RED_NETHER_BRICK" -> 215;
            case "BONE_BLOCK" -> 216;
            case "STRUCTURE_VOID" -> 217;
            case "OBSERVER" -> 218;
            case "WHITE_SHULKER_BOX" -> 219;
            case "ORANGE_SHULKER_BOX" -> 220;
            case "MAGENTA_SHULKER_BOX" -> 221;
            case "LIGHT_BLUE_SHULKER_BOX" -> 222;
            case "YELLOW_SHULKER_BOX" -> 223;
            case "LIME_SHULKER_BOX" -> 224;
            case "PINK_SHULKER_BOX" -> 225;
            case "GRAY_SHULKER_BOX" -> 226;
            case "SILVER_SHULKER_BOX" -> 227;
            case "CYAN_SHULKER_BOX" -> 228;
            case "PURPLE_SHULKER_BOX" -> 229;
            case "BLUE_SHULKER_BOX" -> 230;
            case "BROWN_SHULKER_BOX" -> 231;
            case "GREEN_SHULKER_BOX" -> 232;
            case "RED_SHULKER_BOX" -> 233;
            case "BLACK_SHULKER_BOX" -> 234;
            case "WHITE_GLAZED_TERRACOTTA" -> 235;
            case "ORANGE_GLAZED_TERRACOTTA" -> 236;
            case "MAGENTA_GLAZED_TERRACOTTA" -> 237;
            case "LIGHT_BLUE_GLAZED_TERRACOTTA" -> 238;
            case "YELLOW_GLAZED_TERRACOTTA" -> 239;
            case "LIME_GLAZED_TERRACOTTA" -> 240;
            case "PINK_GLAZED_TERRACOTTA" -> 241;
            case "GRAY_GLAZED_TERRACOTTA" -> 242;
            case "SILVER_GLAZED_TERRACOTTA" -> 243;
            case "CYAN_GLAZED_TERRACOTTA" -> 244;
            case "PURPLE_GLAZED_TERRACOTTA" -> 245;
            case "BLUE_GLAZED_TERRACOTTA" -> 246;
            case "BROWN_GLAZED_TERRACOTTA" -> 247;
            case "GREEN_GLAZED_TERRACOTTA" -> 248;
            case "RED_GLAZED_TERRACOTTA" -> 249;
            case "BLACK_GLAZED_TERRACOTTA" -> 250;
            case "CONCRETE" -> 251;
            case "CONCRETE_POWDER" -> 252;
            case "STRUCTURE_BLOCK" -> 255;
            case "IRON_SPADE" -> 256;
            case "IRON_PICKAXE" -> 257;
            case "IRON_AXE" -> 258;
            case "FLINT_AND_STEEL" -> 259;
            case "APPLE" -> 260;
            case "BOW" -> 261;
            case "ARROW" -> 262;
            case "COAL" -> 263;
            case "DIAMOND" -> 264;
            case "IRON_INGOT" -> 265;
            case "GOLD_INGOT" -> 266;
            case "IRON_SWORD" -> 267;
            case "WOOD_SWORD" -> 268;
            case "WOOD_SPADE" -> 269;
            case "WOOD_PICKAXE" -> 270;
            case "WOOD_AXE" -> 271;
            case "STONE_SWORD" -> 272;
            case "STONE_SPADE" -> 273;
            case "STONE_PICKAXE" -> 274;
            case "STONE_AXE" -> 275;
            case "DIAMOND_SWORD" -> 276;
            case "DIAMOND_SPADE" -> 277;
            case "DIAMOND_PICKAXE" -> 278;
            case "DIAMOND_AXE" -> 279;
            case "STICK" -> 280;
            case "BOWL" -> 281;
            case "MUSHROOM_SOUP" -> 282;
            case "GOLD_SWORD" -> 283;
            case "GOLD_SPADE" -> 284;
            case "GOLD_PICKAXE" -> 285;
            case "GOLD_AXE" -> 286;
            case "STRING" -> 287;
            case "FEATHER" -> 288;
            case "SULPHUR" -> 289;
            case "WOOD_HOE" -> 290;
            case "STONE_HOE" -> 291;
            case "IRON_HOE" -> 292;
            case "DIAMOND_HOE" -> 293;
            case "GOLD_HOE" -> 294;
            case "SEEDS" -> 295;
            case "WHEAT" -> 296;
            case "BREAD" -> 297;
            case "LEATHER_HELMET" -> 298;
            case "LEATHER_CHESTPLATE" -> 299;
            case "LEATHER_LEGGINGS" -> 300;
            case "LEATHER_BOOTS" -> 301;
            case "CHAINMAIL_HELMET" -> 302;
            case "CHAINMAIL_CHESTPLATE" -> 303;
            case "CHAINMAIL_LEGGINGS" -> 304;
            case "CHAINMAIL_BOOTS" -> 305;
            case "IRON_HELMET" -> 306;
            case "IRON_CHESTPLATE" -> 307;
            case "IRON_LEGGINGS" -> 308;
            case "IRON_BOOTS" -> 309;
            case "DIAMOND_HELMET" -> 310;
            case "DIAMOND_CHESTPLATE" -> 311;
            case "DIAMOND_LEGGINGS" -> 312;
            case "DIAMOND_BOOTS" -> 313;
            case "GOLD_HELMET" -> 314;
            case "GOLD_CHESTPLATE" -> 315;
            case "GOLD_LEGGINGS" -> 316;
            case "GOLD_BOOTS" -> 317;
            case "FLINT" -> 318;
            case "PORK" -> 319;
            case "GRILLED_PORK" -> 320;
            case "PAINTING" -> 321;
            case "GOLDEN_APPLE" -> 322;
            case "SIGN" -> 323;
            case "WOOD_DOOR" -> 324;
            case "BUCKET" -> 325;
            case "WATER_BUCKET" -> 326;
            case "LAVA_BUCKET" -> 327;
            case "MINECART" -> 328;
            case "SADDLE" -> 329;
            case "IRON_DOOR" -> 330;
            case "REDSTONE" -> 331;
            case "SNOW_BALL" -> 332;
            case "BOAT" -> 333;
            case "LEATHER" -> 334;
            case "MILK_BUCKET" -> 335;
            case "CLAY_BRICK" -> 336;
            case "CLAY_BALL" -> 337;
            case "SUGAR_CANE" -> 338;
            case "PAPER" -> 339;
            case "BOOK" -> 340;
            case "SLIME_BALL" -> 341;
            case "STORAGE_MINECART" -> 342;
            case "POWERED_MINECART" -> 343;
            case "EGG" -> 344;
            case "COMPASS" -> 345;
            case "FISHING_ROD" -> 346;
            case "WATCH" -> 347;
            case "GLOWSTONE_DUST" -> 348;
            case "RAW_FISH" -> 349;
            case "COOKED_FISH" -> 350;
            case "INK_SACK" -> 351;
            case "BONE" -> 352;
            case "SUGAR" -> 353;
            case "CAKE" -> 354;
            case "BED" -> 355;
            case "DIODE" -> 356;
            case "COOKIE" -> 357;
            case "MAP" -> 358;
            case "SHEARS" -> 359;
            case "MELON" -> 360;
            case "PUMPKIN_SEEDS" -> 361;
            case "MELON_SEEDS" -> 362;
            case "RAW_BEEF" -> 363;
            case "COOKED_BEEF" -> 364;
            case "RAW_CHICKEN" -> 365;
            case "COOKED_CHICKEN" -> 366;
            case "ROTTEN_FLESH" -> 367;
            case "ENDER_PEARL" -> 368;
            case "BLAZE_ROD" -> 369;
            case "GHAST_TEAR" -> 370;
            case "GOLD_NUGGET" -> 371;
            case "NETHER_STALK" -> 372;
            case "POTION" -> 373;
            case "GLASS_BOTTLE" -> 374;
            case "SPIDER_EYE" -> 375;
            case "FERMENTED_SPIDER_EYE" -> 376;
            case "BLAZE_POWDER" -> 377;
            case "MAGMA_CREAM" -> 378;
            case "BREWING_STAND_ITEM" -> 379;
            case "CAULDRON_ITEM" -> 380;
            case "EYE_OF_ENDER" -> 381;
            case "SPECKLED_MELON" -> 382;
            case "MONSTER_EGG" -> 383;
            case "EXP_BOTTLE" -> 384;
            case "FIREBALL" -> 385;
            case "BOOK_AND_QUILL" -> 386;
            case "WRITTEN_BOOK" -> 387;
            case "EMERALD" -> 388;
            case "ITEM_FRAME" -> 389;
            case "FLOWER_POT_ITEM" -> 390;
            case "CARROT_ITEM" -> 391;
            case "POTATO_ITEM" -> 392;
            case "BAKED_POTATO" -> 393;
            case "POISONOUS_POTATO" -> 394;
            case "EMPTY_MAP" -> 395;
            case "GOLDEN_CARROT" -> 396;
            case "SKULL_ITEM" -> 397;
            case "CARROT_STICK" -> 398;
            case "NETHER_STAR" -> 399;
            case "PUMPKIN_PIE" -> 400;
            case "FIREWORK" -> 401;
            case "FIREWORK_CHARGE" -> 402;
            case "ENCHANTED_BOOK" -> 403;
            case "REDSTONE_COMPARATOR" -> 404;
            case "NETHER_BRICK_ITEM" -> 405;
            case "QUARTZ" -> 406;
            case "EXPLOSIVE_MINECART" -> 407;
            case "HOPPER_MINECART" -> 408;
            case "PRISMARINE_SHARD" -> 409;
            case "PRISMARINE_CRYSTALS" -> 410;
            case "RABBIT" -> 411;
            case "COOKED_RABBIT" -> 412;
            case "RABBIT_STEW" -> 413;
            case "RABBIT_FOOT" -> 414;
            case "RABBIT_HIDE" -> 415;
            case "ARMOR_STAND" -> 416;
            case "IRON_BARDING" -> 417;
            case "GOLD_BARDING" -> 418;
            case "DIAMOND_BARDING" -> 419;
            case "LEASH" -> 420;
            case "NAME_TAG" -> 421;
            case "COMMAND_MINECART" -> 422;
            case "MUTTON" -> 423;
            case "COOKED_MUTTON" -> 424;
            case "BANNER" -> 425;
            case "END_CRYSTAL" -> 426;
            case "SPRUCE_DOOR_ITEM" -> 427;
            case "BIRCH_DOOR_ITEM" -> 428;
            case "JUNGLE_DOOR_ITEM" -> 429;
            case "ACACIA_DOOR_ITEM" -> 430;
            case "DARK_OAK_DOOR_ITEM" -> 431;
            case "CHORUS_FRUIT" -> 432;
            case "CHORUS_FRUIT_POPPED" -> 433;
            case "BEETROOT" -> 434;
            case "BEETROOT_SEEDS" -> 435;
            case "BEETROOT_SOUP" -> 436;
            case "DRAGONS_BREATH" -> 437;
            case "SPLASH_POTION" -> 438;
            case "SPECTRAL_ARROW" -> 439;
            case "TIPPED_ARROW" -> 440;
            case "LINGERING_POTION" -> 441;
            case "SHIELD" -> 442;
            case "ELYTRA" -> 443;
            case "BOAT_SPRUCE" -> 444;
            case "BOAT_BIRCH" -> 445;
            case "BOAT_JUNGLE" -> 446;
            case "BOAT_ACACIA" -> 447;
            case "BOAT_DARK_OAK" -> 448;
            case "TOTEM" -> 449;
            case "SHULKER_SHELL" -> 450;
            case "IRON_NUGGET" -> 452;
            case "KNOWLEDGE_BOOK" -> 453;
            case "GOLD_RECORD" -> 2256;
            case "GREEN_RECORD" -> 2257;
            case "RECORD_3" -> 2258;
            case "RECORD_4" -> 2259;
            case "RECORD_5" -> 2260;
            case "RECORD_6" -> 2261;
            case "RECORD_7" -> 2262;
            case "RECORD_8" -> 2263;
            case "RECORD_9" -> 2264;
            case "RECORD_10" -> 2265;
            case "RECORD_11" -> 2266;
            case "RECORD_12" -> 2267;
            default -> -1;
        };
    }
}
