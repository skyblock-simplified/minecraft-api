package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.Model;
import org.jetbrains.annotations.NotNull;

public interface ShopPerk extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getDescription();

    @NotNull ConcurrentList<String> getZoneIds();

    // TODO: Zone model next

    @NotNull ConcurrentList<Unlock> getUnlocks();

    @NotNull ConcurrentList<Stat.Substitute> getStats();

    interface Unlock {

        int getTier();

        @NotNull Item.Cost getCost();

    }

}
