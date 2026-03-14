package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.Brew;
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
public class JsonBrew implements Brew, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private @NotNull String rarityId = "";
    private boolean amplified = false;
    private @NotNull JsonItem.JsonCost cost = new JsonItem.JsonCost();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonBrew jsonBrew = (JsonBrew) o;

        return new EqualsBuilder()
            .append(this.isAmplified(), jsonBrew.isAmplified())
            .append(this.getId(), jsonBrew.getId())
            .append(this.getName(), jsonBrew.getName())
            .append(this.getDescription(), jsonBrew.getDescription())
            .append(this.getRarityId(), jsonBrew.getRarityId())
            .append(this.getCost(), jsonBrew.getCost())
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
