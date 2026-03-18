package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Entity
@Table(
    name = "zones"
)
public class Zone implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @Enumerated(EnumType.STRING)
    private @NotNull ChatFormat format = ChatFormat.GRAY;
    @Column(name = "region_id")
    @SerializedName("region")
    private @NotNull String regionId = "";

    @ManyToOne
    @JoinColumn(name = "region_id", referencedColumnName = "id")
    private transient Region region;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Zone that = (Zone) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getFormat(), that.getFormat())
            .append(this.getRegionId(), that.getRegionId())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getFormat())
            .append(this.getRegionId())
            .build();
    }

}