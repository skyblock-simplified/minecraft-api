package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import dev.sbs.minecraftapi.skyblock.model.BestiaryCategory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "bestiary_categories"
)
public class JsonBestiaryCategory implements BestiaryCategory, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Optional<String> regionId = Optional.empty();
    private @NotNull ChatFormat format = ChatFormat.GREEN;
    private int ordinal = -1;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonBestiaryCategory that = (JsonBestiaryCategory) o;

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
