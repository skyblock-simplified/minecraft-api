package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.Model;
import org.jetbrains.annotations.NotNull;

public interface PotionGroup extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull ConcurrentMap<String, Integer> getPotions();

}
