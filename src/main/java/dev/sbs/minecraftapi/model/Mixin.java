package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "mixins",
    indexes = {
        Item.class,
        Region.class
    }
)
public class Mixin implements JpaModel {

    @Column(name = "item_id")
    private @Id @NotNull String itemId = "";
    private @NotNull String name = "";
    private @NotNull ConcurrentList<String> regionIds = Concurrent.newList();

    @ManyToOne
    @JoinColumn(name = "item_id", referencedColumnName = "id")
    private transient Item item;

    @OneToMany
    private transient ConcurrentList<Region> regions = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Mixin that = (Mixin) o;

        return new EqualsBuilder()
            .append(this.getItemId(), that.getItemId())
            .append(this.getName(), that.getName())
            .append(this.getRegionIds(), that.getRegionIds())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getItemId())
            .append(this.getName())
            .append(this.getRegionIds())
            .build();
    }

}