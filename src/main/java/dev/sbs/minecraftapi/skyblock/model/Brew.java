package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import org.jetbrains.annotations.NotNull;

public interface Brew extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getDescription();

    boolean isAmplified();

    @NotNull String getRarityId();

    default @NotNull Rarity getRarity() {
        return Rarity.of(this.getRarityId());
    }

    @NotNull Item.Cost getCost();

}
