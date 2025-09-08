package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.data.Model;
import dev.sbs.minecraftapi.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

public interface Stat extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getSymbol();

    @NotNull ChatFormat getFormat();

    @NotNull String getCategory();

    double getBase();

    double getCap();

    double getTuningMultiplier();

    boolean isVisible();

    default boolean notVisible() {
        return !this.isVisible();
    }

}
