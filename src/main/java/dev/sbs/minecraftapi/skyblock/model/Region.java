package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

public interface Region extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull ChatFormat getFormat();

    @NotNull String getGameType();

    @NotNull String getMode();

    default @NotNull ConcurrentList<Zone> getZones() {
        return MinecraftApi.getRepository(Zone.class)
            .findAll(Zone::getRegionId, this.getId())
            .collect(Concurrent.toUnmodifiableList());
    }

}
