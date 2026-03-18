package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaModel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Entity
@Table(name = "potion_groups")
public class PotionGroup implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull ConcurrentMap<String, Integer> potions = Concurrent.newMap();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        PotionGroup that = (PotionGroup) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getPotions(), that.getPotions())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getPotions())
            .build();
    }

}