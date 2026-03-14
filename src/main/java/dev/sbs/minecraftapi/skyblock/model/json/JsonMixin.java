package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.Mixin;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "mixins"
)
public class JsonMixin implements Mixin, JsonModel {

    private @Id @NotNull String itemId = "";
    private @NotNull String name = "";
    private @NotNull ConcurrentList<String> regionIds = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonMixin jsonMixin = (JsonMixin) o;

        return new EqualsBuilder()
            .append(this.getItemId(), jsonMixin.getItemId())
            .append(this.getName(), jsonMixin.getName())
            .append(this.getRegionIds(), jsonMixin.getRegionIds())
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
