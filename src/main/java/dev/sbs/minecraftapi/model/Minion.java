package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import dev.sbs.minecraftapi.MinecraftApi;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "minions",
    indexes = {Collection.class}
)
public class Minion implements JpaModel {

    public static final @NotNull ConcurrentList<Integer> UNIQUE_CRAFTS = Concurrent.newList(
        0, 5, 15, 30, 50, 75,
        100, 125, 150, 175, 200, 225, 250, 275, 300, 350,
        400, 450, 500, 550, 600, 650, 700, 750, 800, 850
    );

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @Column(name = "collection_id")
    @SerializedName("collection")
    private @NotNull String collectionId = "";
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentList<Tier> tiers = Concurrent.newList();

    @ManyToOne
    @JoinColumn(name = "collection_id", referencedColumnName = "id")
    private transient Collection collection;

    public @NotNull ConcurrentList<Tier> getTiers() {
        return this.tiers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Minion that = (Minion) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getCollectionId(), that.getCollectionId())
            .append(this.getTiers(), that.getTiers())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getCollectionId())
            .append(this.getTiers())
            .build();
    }

    @Getter
    public static class Tier {

        private int tier;
        private double speed;
        @SerializedName("item")
        private @NotNull String itemId = "";
        private @NotNull Item.Cost upgradeCost = new Item.Cost();

        public @NotNull Item getItem() {
            return MinecraftApi.getRepository(Item.class)
                .findFirstOrNull(Item::getId, this.getItemId());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Tier that = (Tier) o;

            return new EqualsBuilder()
                .append(this.getTier(), that.getTier())
                .append(this.getSpeed(), that.getSpeed())
                .append(this.getItemId(), that.getItemId())
                .append(this.getUpgradeCost(), that.getUpgradeCost())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getTier())
                .append(this.getSpeed())
                .append(this.getItemId())
                .append(this.getUpgradeCost())
                .build();
        }

    }

}