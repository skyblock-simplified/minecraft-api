package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

public interface BestiarySubcategory extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull ChatFormat getFormat();

    @NotNull String getCategoryId();

    default @NotNull BestiaryCategory getCategory() {
        return MinecraftApi.getRepository(BestiaryCategory.class)
            .findFirstOrNull(BestiaryCategory::getId, this.getCategoryId());
    }

    int getOrdinal();

}
