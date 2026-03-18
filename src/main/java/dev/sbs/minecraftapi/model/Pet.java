package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Optional;

@Getter
@Entity
@Table(name = "pets")
public class Pet implements JpaModel {

    public static final @NotNull ConcurrentList<Integer> PET_SCORE = Concurrent.newList(
        10, 25, 50, 75, 100, 130, 175,
        225, 275, 325, 375, 450, 500
    );

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @Enumerated(EnumType.STRING)
    private @NotNull Rarity lowestRarity = Rarity.COMMON;
    @Column(name = "skill_id")
    @SerializedName("skill")
    private @NotNull String skillId = "";
    @Enumerated(EnumType.STRING)
    private @NotNull Type type = Type.PET;
    private int maxLevel = 100;
    private boolean passive = false;
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentList<Substitute> stats = Concurrent.newList();
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentList<Ability> abilities = Concurrent.newList();

    @ManyToOne
    @JoinColumn(name = "skill_id", referencedColumnName = "id")
    private transient Skill skill;

    public @NotNull ConcurrentList<Substitute> getStats() {
        return this.stats;
    }

    public @NotNull ConcurrentList<Substitute> getStats(@NotNull Rarity rarity) {
        return this.getStats()
            .stream()
            .filter(stat -> stat.getValues().containsKey(rarity))
            .collect(Concurrent.toUnmodifiableList());
    }

    public @NotNull ConcurrentList<Ability> getAbilities() {
        return this.abilities;
    }

    public @NotNull ConcurrentList<Ability> getAbilities(@NotNull Rarity rarity) {
        return this.getAbilities()
            .stream()
            .filter(ability -> ability.getStats()
                .stream()
                .anyMatch(stat -> stat.getValues().containsKey(rarity))
            )
            .collect(Concurrent.toUnmodifiableList());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Pet that = (Pet) o;

        return new EqualsBuilder()
            .append(this.getMaxLevel(), that.getMaxLevel())
            .append(this.isPassive(), that.isPassive())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getLowestRarity(), that.getLowestRarity())
            .append(this.getSkillId(), that.getSkillId())
            .append(this.getType(), that.getType())
            .append(this.getStats(), that.getStats())
            .append(this.getAbilities(), that.getAbilities())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getLowestRarity())
            .append(this.getSkillId())
            .append(this.getType())
            .append(this.getMaxLevel())
            .append(this.isPassive())
            .append(this.getStats())
            .append(this.getAbilities())
            .build();
    }

    public enum Type {

        PET,
        MOUNT,
        MORPH,
        GABAGOOL;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

    }

    @Getter
    public static class Ability {

        private @NotNull String name = "";
        private @NotNull String description = "";
        private boolean flatStat = false;
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentList<Substitute> stats = Concurrent.newList();

        public @NotNull ConcurrentList<Substitute> getStats() {
            return this.stats;
        }

        public @NotNull ConcurrentList<Substitute> getStats(@NotNull Rarity rarity) {
            return this.getStats()
                .stream()
                .filter(stat -> stat.getValues().containsKey(rarity))
                .collect(Concurrent.toUnmodifiableList());
        }

        public @NotNull String getFormattedDescription() {
            // TODO: Populate STAT:X, VALUE:X
            //  Perform a StringUtil.format on description
            //  given the stats
            return this.getDescription();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Ability that = (Ability) o;

            return new EqualsBuilder()
                .append(this.isFlatStat(), that.isFlatStat())
                .append(this.getName(), that.getName())
                .append(this.getDescription(), that.getDescription())
                .append(this.getStats(), that.getStats())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getName())
                .append(this.getDescription())
                .append(this.isFlatStat())
                .append(this.getStats())
                .build();
        }

    }

    @Getter
    public static class Substitute {

        private @NotNull String id = "";
        private int precision = 0;
        private @NotNull Stat.Type type = Stat.Type.NONE;
        private @NotNull ChatFormat format = ChatFormat.GREEN;
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<Rarity, Value> values = Concurrent.newMap();

        public @NotNull ConcurrentMap<Rarity, Value> getValues() {
            return this.values;
        }

        public @NotNull Optional<Stat> getStat() {
            return dev.sbs.minecraftapi.MinecraftApi.getRepository(Stat.class)
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

        @Getter
        public static class Value {

            private double base = 0.0;
            private double scalar = 0.0;

        }

    }

}