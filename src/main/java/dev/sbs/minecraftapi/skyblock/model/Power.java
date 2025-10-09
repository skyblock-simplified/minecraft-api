package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.data.Model;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Power extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull Optional<String> getStoneId();

    default @NotNull Optional<Item> getStone() {
        return this.getStoneId().flatMap(stoneId -> MinecraftApi.getRepositoryOf(Item.class)
            .findFirst(Item::getId, stoneId)
        );
    }

    int getRequiredCombatLevel();

    @NotNull Stage getStage();

    @NotNull ConcurrentMap<String, Double> getBaseValues();

    @NotNull ConcurrentMap<String, Double> getBonuses();

    enum Stage {

        STARTER,
        INTERMEDIATE,
        ADVANCED,
        MASTER,
        GRANDIOSE,
        MARVELOUS;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

    }

}
