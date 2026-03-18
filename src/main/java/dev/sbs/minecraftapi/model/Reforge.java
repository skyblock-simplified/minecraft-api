package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Optional;

@Getter
@Entity
@Table(name = "reforges")
public class Reforge implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @Column(name = "stone_id")
    private @NotNull Optional<String> stoneId = Optional.empty();
    private int requiredLevel = 0;
    private @NotNull ConcurrentList<String> categoryIds = Concurrent.newList();
    private @NotNull ConcurrentList<String> itemIds = Concurrent.newList();
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentList<Substitute> stats = Concurrent.newList();

    @ManyToOne
    @JoinColumn(name = "stone_id", referencedColumnName = "id")
    @Getter(AccessLevel.NONE)
    private transient Item stone;

    @OneToMany
    private transient ConcurrentList<ItemCategory> categories = Concurrent.newList();

    @OneToMany
    private transient ConcurrentList<Item> items = Concurrent.newList();

    public @NotNull ConcurrentList<Substitute> getStats() {
        return this.stats;
    }

    public @NotNull Optional<Item> getStone() {
        return Optional.ofNullable(this.stone);
    }

    public boolean hasStone() {
        return this.stoneId.isPresent();
    }

    public boolean notStone() {
        return !this.hasStone();
    }

    public @NotNull ConcurrentList<Substitute> getStats(@NotNull Rarity rarity) {
        return this.getStats()
            .stream()
            .filter(stat -> stat.getValues().containsKey(rarity))
            .collect(Concurrent.toUnmodifiableList());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Reforge that = (Reforge) o;

        return new EqualsBuilder()
            .append(this.getRequiredLevel(), that.getRequiredLevel())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getStoneId(), that.getStoneId())
            .append(this.getCategoryIds(), that.getCategoryIds())
            .append(this.getItemIds(), that.getItemIds())
            .append(this.getStats(), that.getStats())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getStoneId())
            .append(this.getRequiredLevel())
            .append(this.getCategoryIds())
            .append(this.getItemIds())
            .append(this.getStats())
            .build();
    }

    @Getter
    public static class Substitute {

        private @NotNull String id = "";
        private @NotNull ConcurrentMap<Rarity, Double> values = Concurrent.newMap();

        public @NotNull Optional<Stat> getStat() {
            return MinecraftApi.getRepository(Stat.class)
                .findFirst(Stat::getId, this.getId());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Substitute that = (Substitute) o;

            return new EqualsBuilder()
                .append(this.getId(), that.getId())
                .append(this.getValues(), that.getValues())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getValues())
                .build();
        }

    }

}