package dev.sbs.minecraftapi.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import dev.sbs.minecraftapi.skyblock.model.BestiaryFamily;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "bestiary_families"
)
public class JsonBestiaryFamily implements BestiaryFamily, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private @NotNull ChatFormat format = ChatFormat.GREEN;
    private int bracket = 1;
    private int maxTier = 25;
    @SerializedName("category")
    private @NotNull String categoryId = "";
    @SerializedName("subCategory")
    private @NotNull String subcategoryId = "";
    @SerializedName("mobTypes")
    private @NotNull ConcurrentList<String> mobTypeIds = Concurrent.newUnmodifiableList();
    private @NotNull ConcurrentList<String> mobs = Concurrent.newUnmodifiableList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonBestiaryFamily that = (JsonBestiaryFamily) o;

        return new EqualsBuilder()
            .append(this.getBracket(), that.getBracket())
            .append(this.getMaxTier(), that.getMaxTier())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDescription(), that.getDescription())
            .append(this.getFormat(), that.getFormat())
            .append(this.getCategoryId(), that.getCategoryId())
            .append(this.getSubcategoryId(), that.getSubcategoryId())
            .append(this.getMobTypeIds(), that.getMobTypeIds())
            .append(this.getMobs(), that.getMobs())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.getFormat())
            .append(this.getBracket())
            .append(this.getMaxTier())
            .append(this.getCategoryId())
            .append(this.getSubcategoryId())
            .append(this.getMobTypeIds())
            .append(this.getMobs())
            .build();
    }

}
