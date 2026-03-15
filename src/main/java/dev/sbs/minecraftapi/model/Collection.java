package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "collections"
)
public class Collection implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, Item> items = Concurrent.newMap();

    public @NotNull ConcurrentMap<String, Item> getItems() {
        return this.items;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Collection that = (Collection) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getItems(), that.getItems())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getItems())
            .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
    public static class Item {

        private @NotNull String name;
        private int maxTiers;
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentList<Tier> tiers = Concurrent.newList();

        public @NotNull ConcurrentList<Tier> getTiers() {
            return this.tiers;
        }

        public int getMaxRequired() {
            return this.getTiers().get(this.getTiers().size() - 1).getAmountRequired();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Item that = (Item) o;

            return new EqualsBuilder()
                .append(this.getMaxTiers(), that.getMaxTiers())
                .append(this.getName(), that.getName())
                .append(this.getTiers(), that.getTiers())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getName())
                .append(this.getMaxTiers())
                .append(this.getTiers())
                .build();
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Tier {

        private int tier;
        private int amountRequired;
        private @NotNull ConcurrentList<String> unlocks = Concurrent.newList();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Tier that = (Tier) o;

            return new EqualsBuilder()
                .append(this.getTier(), that.getTier())
                .append(this.getAmountRequired(), that.getAmountRequired())
                .append(this.getUnlocks(), that.getUnlocks())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getTier())
                .append(this.getAmountRequired())
                .append(this.getUnlocks())
                .build();
        }

    }

}