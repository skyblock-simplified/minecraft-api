package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.ShopPerk;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "shop_perks"
)
public class JsonShopPerk implements ShopPerk, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private @NotNull ConcurrentList<String> regionIds = Concurrent.newList();
    private @NotNull ConcurrentList<JsonStat.JsonSubstitute> stats = Concurrent.newList();
    private @NotNull ConcurrentList<JsonUnlock> unlocks = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonShopPerk that = (JsonShopPerk) o;

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
    public static class JsonUnlock implements Unlock {

        private int tier;
        private @NotNull JsonItem.JsonCost cost = new JsonItem.JsonCost();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonUnlock that = (JsonUnlock) o;

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
