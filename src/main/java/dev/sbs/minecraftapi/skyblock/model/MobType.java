package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.data.Model;
import dev.sbs.minecraftapi.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

public interface MobType extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getSymbol();

    @NotNull ChatFormat getFormat();

}
