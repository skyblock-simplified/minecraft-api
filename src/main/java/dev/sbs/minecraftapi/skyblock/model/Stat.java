package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.data.Model;
import dev.sbs.minecraftapi.text.ChatFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

public interface Stat extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getSymbol();

    @NotNull ChatFormat getFormat();

    @NotNull String getCategory();

    double getBase();

    double getCap();

    double getAccessoryEnrichment();

    double getTuningMultiplier();

    boolean isVisible();

    default boolean notVisible() {
        return !this.isVisible();
    }

    @Getter
    @RequiredArgsConstructor
    enum Type {

        NONE("", ""),
        FLAT("+", ""),
        MULTIPLY("", "x"),
        PERCENT("", "%"),
        PLUS_MULTIPLY("+", "x"),
        PLUS_PERCENT("+", "%"),
        SECONDS("", "s");

        private final @NotNull String prefix;
        private final @NotNull String suffix;

        public @NotNull String format(int level, @NotNull Pet.Substitute.Value value) {
            return String.format(
                "%s%s%s",
                this.getPrefix(),
                value.getBase() + (level * value.getScalar()),
                this.getSuffix()
            );
        }

    }

}
