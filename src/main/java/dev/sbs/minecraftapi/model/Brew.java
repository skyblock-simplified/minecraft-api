package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Entity
@Table(name = "brews")
public class Brew implements JpaModel {

    @Id
    @Column(name = "id", nullable = false)
    private @NotNull String id = "";

    @Column(name = "name", nullable = false)
    private @NotNull String name = "";

    @Column(name = "description", nullable = false)
    private @NotNull String description = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false)
    private @NotNull Rarity rarity = Rarity.COMMON;

    @Column(name = "amplified", nullable = false)
    private boolean amplified = false;

    @Column(name = "cost", nullable = false)
    private @NotNull Item.Cost cost = new Item.Cost();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Brew that = (Brew) o;

        return new EqualsBuilder()
            .append(this.isAmplified(), that.isAmplified())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDescription(), that.getDescription())
            .append(this.getRarity(), that.getRarity())
            .append(this.getCost(), that.getCost())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.getRarity())
            .append(this.isAmplified())
            .append(this.getCost())
            .build();
    }

}
