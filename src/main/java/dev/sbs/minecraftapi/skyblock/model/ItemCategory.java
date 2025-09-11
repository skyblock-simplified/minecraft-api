package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.data.Model;
import org.jetbrains.annotations.NotNull;

public interface ItemCategory extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull Item.Type getType();

}
