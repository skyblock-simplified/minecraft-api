package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Stat extends Model {

    double MAGIC_CONSTANT = 719.28;

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getSymbol();

    @NotNull ChatFormat getFormat();

    @NotNull String getCategoryId();

    default @NotNull StatCategory getCategory() {
        return MinecraftApi.getRepository(StatCategory.class)
            .findFirstOrNull(StatCategory::getId, this.getCategoryId());
    }

    double getBase();

    double getCap();

    double getEnrichment();

    double getPowerMultiplier();

    default double getPowerCoefficient() {
        return (this.getPowerMultiplier() * MAGIC_CONSTANT) / 100.0;
    }

    double getTuningMultiplier();

    boolean isVisible();

    default boolean notVisible() {
        return !this.isVisible();
    }

    interface Substitute {

        @NotNull String getId();

        default @NotNull Optional<Stat> getStat() {
            return MinecraftApi.getRepository(Stat.class)
                .findFirst(Stat::getId, this.getId());
        }

        int getPrecision();

        @NotNull Type getType();

        @NotNull ChatFormat getFormat();

        @NotNull ConcurrentMap<Integer, Double> getValues();

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
