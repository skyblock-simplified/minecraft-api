package dev.sbs.minecraftapi.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.api.data.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.Slayer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "slayers"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonSlayer implements Slayer, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private int maxLevel = 9;
    private int maxTier = 5;
    @SerializedName("mobType")
    private @NotNull String mobTypeId = "";
    private double weightModifier;
    private int weightDivider;
    private @NotNull ConcurrentList<JsonSlayerLevel> levels = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonSlayer jsonSkill = (JsonSlayer) o;

        return new EqualsBuilder()
            .append(this.getMaxLevel(), jsonSkill.getMaxLevel())
            .append(this.getId(), jsonSkill.getId())
            .append(this.getName(), jsonSkill.getName())
            .append(this.getDescription(), jsonSkill.getDescription())
            .append(this.getLevels(), jsonSkill.getLevels())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.getMaxLevel())
            .append(this.getLevels())
            .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonSlayerLevel implements Level {

        private int level;
        private double totalRequiredXP;
        private @NotNull String title = "";
        private @NotNull ConcurrentList<String> unlocks = Concurrent.newList();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonSlayerLevel that = (JsonSlayerLevel) o;

            return new EqualsBuilder()
                .append(this.getLevel(), that.getLevel())
                .append(this.getTotalRequiredXP(), that.getTotalRequiredXP())
                .append(this.getTitle(), that.getTitle())
                .append(this.getUnlocks(), that.getUnlocks())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getLevel())
                .append(this.getTotalRequiredXP())
                .append(this.getTitle())
                .append(this.getUnlocks())
                .build();
        }

    }

}
