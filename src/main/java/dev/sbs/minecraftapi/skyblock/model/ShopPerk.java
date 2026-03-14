package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.query.SearchFunction;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import org.jetbrains.annotations.NotNull;

public interface ShopPerk extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getDescription();

    @NotNull ConcurrentList<String> getRegionIds();

    default @NotNull ConcurrentList<Region> getRegions() {
        return MinecraftApi.getRepository(Region.class)
            .matchAll(
                SearchFunction.Match.ANY,
                region -> this.getRegionIds().contains(region.getId())
            )
            .collect(Concurrent.toUnmodifiableList());
    }

    @NotNull ConcurrentList<Stat.Substitute> getStats();

    @NotNull ConcurrentList<Unlock> getUnlocks();

    interface Unlock {

        int getTier();

        @NotNull Item.Cost getCost();

    }

}
