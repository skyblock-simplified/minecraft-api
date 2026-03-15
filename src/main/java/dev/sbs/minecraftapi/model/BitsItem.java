package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import dev.sbs.api.util.StringUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "bits_items"
)
public class BitsItem implements JpaModel {

    private @Id @NotNull String id = "";
    @Enumerated(EnumType.STRING)
    private @NotNull Type type = Type.ITEM;
    private int cost;
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentList<Variant> variants = Concurrent.newList();

    public @NotNull ConcurrentList<Variant> getVariants() {
        return this.variants;
    }

    public boolean hasVariants() {
        return !this.variants.isEmpty();
    }

    public boolean noVariants() {
        return !this.hasVariants();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BitsItem that = (BitsItem) o;

        return new EqualsBuilder()
            .append(this.getCost(), that.getCost())
            .append(this.getId(), that.getId())
            .append(this.getType(), that.getType())
            .append(this.getVariants(), that.getVariants())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getType())
            .append(this.getCost())
            .append(this.getVariants())
            .build();
    }

    public enum Type {

        ACCESSORY,
        ATTRIBUTE_SHARD,
        CONSUMABLE,
        COSMETIC,
        DYE,
        ENCHANTED_BOOK,
        ENRICHMENT,
        ITEM,
        SACK,
        UPGRADE;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

    }

    @Getter
    public static class Variant {

        private @NotNull String id = "";
        private int cost;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Variant that = (Variant) o;

            return new EqualsBuilder()
                .append(this.getCost(), that.getCost())
                .append(this.getId(), that.getId())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getCost())
                .build();
        }

    }

}