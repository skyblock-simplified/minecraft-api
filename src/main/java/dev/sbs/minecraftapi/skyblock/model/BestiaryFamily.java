package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface BestiaryFamily extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getDescription();

    @NotNull ChatFormat getFormat();

    int getBracket();

    int getMaxTier();

    @NotNull String getCategoryId();

    default @NotNull BestiaryCategory getCategory() {
        return MinecraftApi.getRepository(BestiaryCategory.class)
            .findFirstOrNull(BestiaryCategory::getId, this.getCategoryId());
    }

    @NotNull Optional<String> getSubcategoryId();

    default @NotNull Optional<BestiarySubcategory> getSubcategory() {
        return this.getSubcategoryId()
            .flatMap(subCategoryId -> MinecraftApi.getRepository(BestiarySubcategory.class)
                .findFirst(BestiarySubcategory::getId, subCategoryId)
            );
    }

    @NotNull ConcurrentList<String> getMobTypeIds();

    default @NotNull ConcurrentList<MobType> getMobTypes() {
        return MinecraftApi.getRepository(MobType.class)
            .matchAll(mobType -> this.getMobTypeIds().contains(mobType.getId()))
            .collect(Concurrent.toUnmodifiableList());
    }

    @NotNull ConcurrentList<ConcurrentList<Integer>> BRACKETS = Concurrent.newUnmodifiableList(
        Concurrent.newUnmodifiableList(
            20, 40, 60, 100, 200,
            400, 800, 1_400, 2_000, 3_000,
            6_000, 12_000, 20_000, 30_000, 40_000,
            50_000, 60_000, 72_000, 86_000, 100_000,
            200_000, 400_000, 600_000, 800_000, 1_000_000
        ),
        Concurrent.newUnmodifiableList(
            5, 10, 15, 25, 50,
            100, 200, 350, 500, 750,
            1_500, 3_000, 5_000, 7_500, 10_000,
            12_500, 15_000, 18_000, 21_500, 25_000,
            50_000, 100_000, 150_000, 200_000, 250_000
        ),
        Concurrent.newUnmodifiableList(
            4, 8, 12, 16, 20,
            40, 80, 140, 200, 300,
            600, 1_200, 2_000, 3_000, 4_000,
            5_000, 6_000, 7_200, 8_600, 10_000,
            20_000, 40_000, 60_000, 80_000, 100_000
        ),
        Concurrent.newUnmodifiableList(
            2, 4, 6, 10, 15,
            20, 25, 35, 50, 75,
            150, 300, 500, 750, 1_000,
            1_350, 1_650, 2_000, 2_500, 3_000,
            5_000, 10_000, 15_000, 20_000, 25_000
        ),
        Concurrent.newUnmodifiableList(
            1, 2, 3, 5, 7,
            10, 15, 20, 25, 30,
            60, 120, 200, 300, 400,
            500, 600, 720, 860, 1_000,
            2_000, 4_000, 6_000, 8_000, 10_000
        ),
        Concurrent.newUnmodifiableList(
            1, 2, 3, 5, 7,
            9, 14, 17, 21, 25,
            50, 80, 125, 175, 250,
            325, 425, 525, 625, 750,
            1_500, 3_000, 4_500, 6_000, 7_500
        ),
        Concurrent.newUnmodifiableList(
            1, 2, 3, 5, 7,
            9, 11, 14, 17, 20,
            30, 40, 55, 75, 100,
            150, 200, 275, 375, 500,
            1_000, 1_500, 2_000, 2_500, 3_000
        )
    );

}
