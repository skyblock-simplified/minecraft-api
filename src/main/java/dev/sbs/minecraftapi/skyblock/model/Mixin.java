package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import org.jetbrains.annotations.NotNull;

public interface Mixin extends Model {

    @NotNull String getItemId();

    default @NotNull Item getItem() {
        return MinecraftApi.getRepository(Item.class)
            .findFirstOrNull(Item::getId, this.getItemId());
    }

    @NotNull String getName();

    @NotNull ConcurrentList<String> getRegionIds();

    default @NotNull ConcurrentList<Region> getRegions() {
        return MinecraftApi.getRepository(Region.class)
            .matchAll(region -> this.getRegionIds().contains(region.getId()))
            .collect(Concurrent.toUnmodifiableList());
    }

}
