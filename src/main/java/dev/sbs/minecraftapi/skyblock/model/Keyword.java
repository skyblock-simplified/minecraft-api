package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.builder.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Keyword extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull Optional<String> getPlural();

    default boolean hasPlural() {
        return this.getPlural().isPresent();
    }

    @NotNull Optional<String> getSymbol();

    default boolean hasSymbol() {
        return this.getSymbol().isPresent();
    }

    @NotNull ChatFormat getFormat();

}
