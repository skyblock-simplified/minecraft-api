package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Optional;

@Getter
@Entity
@Table(name = "bestiary_categories")
public class BestiaryCategory implements JpaModel {

    @Id
    @Column(name = "id", nullable = false)
    private @NotNull String id = "";

    @Column(name = "name", nullable = false)
    private @NotNull String name = "";

    @SerializedName("region")
    @Column(name = "region_id")
    private @NotNull Optional<String> regionId = Optional.empty();

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private @NotNull ChatFormat format = ChatFormat.GREEN;

    @Column(name = "ordinal", nullable = false)
    private int ordinal = -1;

    @ManyToOne
    @Getter(AccessLevel.NONE)
    @JoinColumn(name = "region_id", referencedColumnName = "id")
    private transient @Nullable Region region;

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
