package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "brews"
)
public class Brew implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private @NotNull String rarityId = "";
    private boolean amplified = false;
    private @NotNull Item.Cost cost = new Item.Cost();

    public @NotNull Rarity getRarity() {
        return Rarity.of(this.getRarityId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Brew that = (Brew) o;

        return new EqualsBuilder()
            .append(this.isAmplified(), that.isAmplified())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDescription(), that.getDescription())
            .append(this.getRarityId(), that.getRarityId())
            .append(this.getCost(), that.getCost())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.getRarityId())
            .append(this.isAmplified())
            .append(this.getCost())
            .build();
    }

}