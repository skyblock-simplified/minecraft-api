package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.data.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import org.jetbrains.annotations.NotNull;

public interface Minion extends Model {

    @NotNull ConcurrentList<Integer> UNIQUE_CRAFTS = Concurrent.newList(
        0, 5, 15, 30, 50, 75,
        100, 125, 150, 175, 200, 225, 250, 275, 300, 350,
        400, 450, 500, 550, 600, 650, 700, 750, 800, 850
    );

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getCollectionId();

    @NotNull ConcurrentList<Tier> getTiers();

    default @NotNull Collection getCollection() {
        return MinecraftApi.getRepositoryOf(Collection.class)
            .findFirstOrNull(Collection::getId, this.getCollectionId());
    }

    interface Tier {

        int getTier();

        double getSpeed();

        @NotNull String getItemId();

        @NotNull Item.UpgradeCost getUpgradeCost();

        default @NotNull Item getItem() {
            return MinecraftApi.getRepositoryOf(Item.class)
                .findFirstOrNull(Item::getId, this.getItemId());
        }

    }

}
