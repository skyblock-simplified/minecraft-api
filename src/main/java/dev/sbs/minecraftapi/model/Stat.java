package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "stats",
    indexes = {
        StatCategory.class
    }
)
public class Stat implements JpaModel {

    public static final double MAGIC_CONSTANT = 719.28;

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String symbol = "";
    @Enumerated(EnumType.STRING)
    private @NotNull ChatFormat format = ChatFormat.WHITE;
    @Column(name = "category_id")
    @SerializedName("category")
    private @NotNull String categoryId = "";
    private double base = 0.0;
    private double cap = 0.0;
    private double enrichment = 0.0;
    private double powerMultiplier = 0.0;
    private double tuningMultiplier = 0.0;
    private boolean visible;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private transient StatCategory category;

    public double getPowerCoefficient() {
        return (this.getPowerMultiplier() * MAGIC_CONSTANT) / 100.0;
    }

    public boolean notVisible() {
        return !this.isVisible();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Stat that = (Stat) o;

        return new EqualsBuilder()
            .append(this.getBase(), that.getBase())
            .append(this.getCap(), that.getCap())
            .append(this.getEnrichment(), that.getEnrichment())
            .append(this.getPowerMultiplier(), that.getPowerMultiplier())
            .append(this.getTuningMultiplier(), that.getTuningMultiplier())
            .append(this.isVisible(), that.isVisible())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getSymbol(), that.getSymbol())
            .append(this.getFormat(), that.getFormat())
            .append(this.getCategoryId(), that.getCategoryId())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getSymbol())
            .append(this.getFormat())
            .append(this.getCategoryId())
            .append(this.getBase())
            .append(this.getCap())
            .append(this.getEnrichment())
            .append(this.getPowerMultiplier())
            .append(this.getTuningMultiplier())
            .append(this.isVisible())
            .build();
    }

    @Getter
    public static class Substitute {

        private @NotNull String id = "";
        private int precision = 0;
        private @NotNull Type type = Type.NONE;
        private @NotNull ChatFormat format = ChatFormat.GREEN;
        private @NotNull ConcurrentMap<Integer, Double> values = Concurrent.newMap();

        public @NotNull Optional<Stat> getStat() {
            return MinecraftApi.getRepository(Stat.class)
                .findFirst(Stat::getId, this.getId());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Substitute that = (Substitute) o;

            return new EqualsBuilder()
                .append(this.getPrecision(), that.getPrecision())
                .append(this.getId(), that.getId())
                .append(this.getType(), that.getType())
                .append(this.getFormat(), that.getFormat())
                .append(this.getValues(), that.getValues())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getPrecision())
                .append(this.getType())
                .append(this.getFormat())
                .append(this.getValues())
                .build();
        }

    }

    @Getter
    @RequiredArgsConstructor
    public enum Type {

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