package dev.sbs.minecraftapi.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.Enchantment;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "enchantments"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonEnchantment implements Enchantment, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private @NotNull Type type = Type.NORMAL;
    private int requiredLevel = 0;
    private @NotNull Optional<String> conflict = Optional.empty();
    private @NotNull ConcurrentList<String> categoryIds = Concurrent.newList();
    private @NotNull ConcurrentList<String> itemIds = Concurrent.newList();
    private @NotNull ConcurrentList<JsonLevel> levels = Concurrent.newList();
    private @NotNull ConcurrentList<JsonStat.JsonSubstitute> stats = Concurrent.newList();
    private @NotNull ConcurrentList<String> mobTypeIds = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonEnchantment that = (JsonEnchantment) o;

        return new EqualsBuilder()
            .append(this.getRequiredLevel(), that.getRequiredLevel())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDescription(), that.getDescription())
            .append(this.getType(), that.getType())
            .append(this.getConflict(), that.getConflict())
            .append(this.getCategoryIds(), that.getCategoryIds())
            .append(this.getItemIds(), that.getItemIds())
            .append(this.getLevels(), that.getLevels())
            .append(this.getStats(), that.getStats())
            .append(this.getMobTypeIds(), that.getMobTypeIds())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.getType())
            .append(this.getRequiredLevel())
            .append(this.getConflict())
            .append(this.getCategoryIds())
            .append(this.getItemIds())
            .append(this.getLevels())
            .append(this.getStats())
            .append(this.getMobTypeIds())
            .build();
    }

    @Getter
    public static class JsonLevel implements Level {

        private int level = 0;
        @SerializedName("cost")
        private @NotNull JsonItem.JsonCost applyCost = new JsonItem.JsonCost();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonLevel jsonLevel = (JsonLevel) o;

            return new EqualsBuilder()
                .append(this.getLevel(), jsonLevel.getLevel())
                .append(this.getApplyCost(), jsonLevel.getApplyCost())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getLevel())
                .append(this.getApplyCost())
                .build();
        }

    }

}
