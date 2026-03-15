package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "potions"
)
public class Potion implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private boolean buff;
    private boolean brewable;
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentList<Stat.Substitute> stats = Concurrent.newList();

    public @NotNull ConcurrentList<Stat.Substitute> getStats() {
        return this.stats;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Potion that = (Potion) o;

        return new EqualsBuilder()
            .append(this.isBuff(), that.isBuff())
            .append(this.isBrewable(), that.isBrewable())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDescription(), that.getDescription())
            .append(this.getStats(), that.getStats())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.isBuff())
            .append(this.isBrewable())
            .append(this.getStats())
            .build();
    }

}