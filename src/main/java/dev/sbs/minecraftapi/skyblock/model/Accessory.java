package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Accessory extends Model {

    @NotNull String getId();

    @NotNull Optional<String> getDescription();

    @NotNull Source getSource();

    @NotNull Limit getLimit();

    @NotNull Optional<Family> getFamily();

    default @NotNull Item getItem() {
        return MinecraftApi.getRepository(Item.class)
            .findFirstOrNull(Item::getId, this.getId());
    }

    interface Family {

        @NotNull String getId();

        int getRank();

    }

    enum Limit {

        NONE,
        BINGO,
        RIFT

    }

    enum Source {

        COLLECTION,
        NPC,
        EVENT,
        MOB_DROP,
        SLAYER,
        QUEST,
        DUNGEON,
        MISCELLANEOUS,
        RIFT

    }

    interface Substitute {

        @NotNull String getId();

        double getValue();

        default @NotNull Stat getStat() {
            return MinecraftApi.getRepository(Stat.class)
                .findFirstOrNull(Stat::getId, this.getId());
        }

    }

}
