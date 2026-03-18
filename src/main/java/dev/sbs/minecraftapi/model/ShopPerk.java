package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.minecraftapi.MinecraftApi;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Getter
@Entity
@Table(name = "shop_perks")
public class ShopPerk implements JpaModel, PostInit {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    @SerializedName("regions")
    private @NotNull ConcurrentList<String> regionIds = Concurrent.newList();

    @OneToMany
    private transient ConcurrentList<Region> regions = Concurrent.newList();
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentList<Stat.Substitute> stats = Concurrent.newList();
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentList<Unlock> unlocks = Concurrent.newList();

    public @NotNull ConcurrentList<Stat.Substitute> getStats() {
        return this.stats;
    }

    public @NotNull ConcurrentList<Unlock> getUnlocks() {
        return this.unlocks;
    }

    @Override
    public void postInit() {
        this.regions = MinecraftApi.getRepository(Region.class)
            .matchAll(region -> this.getRegionIds().contains(region.getId()))
            .collect(Concurrent.toUnmodifiableList());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ShopPerk that = (ShopPerk) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDescription(), that.getDescription())
            .append(this.getRegionIds(), that.getRegionIds())
            .append(this.getStats(), that.getStats())
            .append(this.getUnlocks(), that.getUnlocks())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.getRegionIds())
            .append(this.getStats())
            .append(this.getUnlocks())
            .build();
    }

    @Getter
    public static class Unlock {

        private int tier;
        private @NotNull Item.Cost cost = new Item.Cost();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Unlock that = (Unlock) o;

            return new EqualsBuilder()
                .append(this.getTier(), that.getTier())
                .append(this.getCost(), that.getCost())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getTier())
                .append(this.getCost())
                .build();
        }

    }

}