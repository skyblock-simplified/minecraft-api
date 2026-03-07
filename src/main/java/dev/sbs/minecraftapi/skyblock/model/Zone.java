package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

public interface Zone extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull ChatFormat getFormat();

    @NotNull String getRegionId();

    default @NotNull Region getRegion() {
        return MinecraftApi.getRepository(Region.class)
            .findFirstOrNull(Region::getId, this.getRegionId());
    }

}
