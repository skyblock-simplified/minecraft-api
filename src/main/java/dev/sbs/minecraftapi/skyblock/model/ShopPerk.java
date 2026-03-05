package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ShopPerk extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getDescription();

    @NotNull Optional<String> getStatId();

    default @NotNull Optional<Stat> getStat() {
        return this.getStatId().flatMap(statId -> MinecraftApi.getRepository(Stat.class)
            .findFirst(Stat::getId, statId)
        );
    }

    @NotNull Stat.Type getType();

    interface Upgrade {

        int getTier();

        @NotNull Item.UpgradeCost getUpgradeCost();

    }

}
