package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Entity
@Table(name = "fairy_souls")
public class FairySoul implements JpaModel {

    private @Id int id = 0;
    private double x = 0;
    private double y = 0;
    private double z = 0;
    private boolean walkable = false;
    @Column(name = "zone_id")
    private @NotNull String zoneId = "";

    @ManyToOne
    @JoinColumn(name = "zone_id", referencedColumnName = "id")
    private transient Zone zone;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        FairySoul that = (FairySoul) o;

        return new EqualsBuilder()
            .append(this.getX(), that.getX())
            .append(this.getY(), that.getY())
            .append(this.getZ(), that.getZ())
            .append(this.isWalkable(), that.isWalkable())
            .append(this.getZoneId(), that.getZoneId())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getX())
            .append(this.getY())
            .append(this.getZ())
            .append(this.isWalkable())
            .append(this.getZoneId())
            .build();
    }

}