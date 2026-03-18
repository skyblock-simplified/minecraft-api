package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.minecraftapi.MinecraftApi;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Map;

@Getter
@Entity
@Table(name = "slayers")
public class Slayer implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private int maxLevel = 9;
    private int maxTier = 5;
    @Column(name = "mob_type_id")
    @SerializedName("mobType")
    private @NotNull String mobTypeId = "";
    private double weightModifier;
    private int weightDivider;
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentList<Level> levels = Concurrent.newList();

    @ManyToOne
    @JoinColumn(name = "mob_type_id")
    private transient MobType mobType;

    public @NotNull ConcurrentList<Level> getLevels() {
        return this.levels;
    }

    public @NotNull ConcurrentMap<String, Double> getEffects() {
        return this.getLevels()
            .stream()
            .flatMap(level -> level.getEffects().stream())
            .collect(Concurrent.toUnmodifiableMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Double::sum
            ));
    }

    public @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return this.getLevels()
            .stream()
            .map(Level::getTotalRequiredXP)
            .collect(Concurrent.toUnmodifiableList());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Slayer that = (Slayer) o;

        return new EqualsBuilder()
            .append(this.getMaxLevel(), that.getMaxLevel())
            .append(this.getMaxTier(), that.getMaxTier())
            .append(this.getWeightModifier(), that.getWeightModifier())
            .append(this.getWeightDivider(), that.getWeightDivider())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDescription(), that.getDescription())
            .append(this.getMobTypeId(), that.getMobTypeId())
            .append(this.getLevels(), that.getLevels())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.getMaxLevel())
            .append(this.getMaxTier())
            .append(this.getMobTypeId())
            .append(this.getWeightModifier())
            .append(this.getWeightDivider())
            .append(this.getLevels())
            .build();
    }

    @Getter
    public static class Level {

        private int level;
        private int totalRequiredXP;
        private @NotNull String title = "";
        private @NotNull ConcurrentList<String> unlocks = Concurrent.newList();

        public @NotNull ConcurrentMap<String, Double> getEffects() {
            return MinecraftApi.getRepository(Stat.class)
                .stream()
                .map(stat -> Pair.of(
                    stat.getId(),
                    this.getUnlocks()
                        .indexedStream()
                        .collapseToSingle((line, index, size) -> {
                            String value = "0.0";

                            if (line.startsWith("+") && line.endsWith(stat.getName())) // Flat
                                value = line.split("\\s+")[0];
                            else if (line.contains("Grants +") && line.contains(stat.getName()) && index == size - 1) // Tiered
                                value = line.split("\\s+")[2];

                            value = value.replace("+", "");
                            value = value.replace("%", "");

                            if (value.contains("➜")) // Tiered
                                value = value.split("➜")[1];

                            return value;
                        })
                        .mapToDouble(Double::parseDouble)
                        .sum()
                ))
                .filter(entry -> entry.getValue() > 0.0)
                .collect(Concurrent.toUnmodifiableMap());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Level that = (Level) o;

            return new EqualsBuilder()
                .append(this.getLevel(), that.getLevel())
                .append(this.getTotalRequiredXP(), that.getTotalRequiredXP())
                .append(this.getTitle(), that.getTitle())
                .append(this.getUnlocks(), that.getUnlocks())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getLevel())
                .append(this.getTotalRequiredXP())
                .append(this.getTitle())
                .append(this.getUnlocks())
                .build();
        }

    }

}