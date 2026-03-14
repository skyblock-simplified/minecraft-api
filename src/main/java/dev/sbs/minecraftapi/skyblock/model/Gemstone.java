package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import org.jetbrains.annotations.NotNull;

public interface Gemstone extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getSymbol();

    @NotNull ChatFormat getFormat();

    @NotNull String getStatId();

    default @NotNull Stat getStat() {
        return MinecraftApi.getRepository(Stat.class)
            .findFirstOrNull(Stat::getId, this.getStatId());
    }

    @NotNull ConcurrentMap<Type, ConcurrentMap<Rarity, Double>> getValues();

    enum Type {

        ROUGH,
        FLAWED,
        FINE,
        FLAWLESS,
        PERFECT;

    }

}
