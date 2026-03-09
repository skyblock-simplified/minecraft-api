package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface BestiaryCategory extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull Optional<String> getRegionId();

    default @NotNull Optional<Region> getRegion() {
        return this.getRegionId().flatMap(regionId -> MinecraftApi.getRepository(Region.class)
            .findFirst(Region::getId, regionId)
        );
    }

    @NotNull ChatFormat getFormat();

    int getOrdinal();

}
