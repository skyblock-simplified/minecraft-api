package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import dev.sbs.api.persistence.converter.optional.OptionalStringConverter;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "bestiary_categories",
    indexes = {
        Region.class
    }
)
public class BestiaryCategory implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @Column(name = "region_id")
    @SerializedName("region")
    @Convert(converter = OptionalStringConverter.class)
    private @NotNull Optional<String> regionId = Optional.empty();
    @Enumerated(EnumType.STRING)
    private @NotNull ChatFormat format = ChatFormat.GREEN;
    private int ordinal = -1;

    @ManyToOne
    @JoinColumn(name = "region_id", referencedColumnName = "id")
    @Getter(AccessLevel.NONE)
    private transient Region region;

    public @NotNull Optional<Region> getRegion() {
        return Optional.ofNullable(this.region);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BestiaryCategory that = (BestiaryCategory) o;

        return new EqualsBuilder()
            .append(this.getOrdinal(), that.getOrdinal())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getRegionId(), that.getRegionId())
            .append(this.getFormat(), that.getFormat())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getRegionId())
            .append(this.getFormat())
            .append(this.getOrdinal())
            .build();
    }

}