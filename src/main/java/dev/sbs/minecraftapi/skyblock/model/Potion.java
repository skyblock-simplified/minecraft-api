package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.Model;
import org.jetbrains.annotations.NotNull;

public interface Potion extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getDescription();

    boolean isBuff();

    boolean isBrewable();

    @NotNull ConcurrentList<Stat.Substitute> getStats();

}
