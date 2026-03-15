package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "melody_songs"
)
public class MelodySong implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @Enumerated(EnumType.STRING)
    private @NotNull Difficulty difficulty = Difficulty.EASY;
    private int intelligenceReward;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        MelodySong that = (MelodySong) o;

        return new EqualsBuilder()
            .append(this.getIntelligenceReward(), that.getIntelligenceReward())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDifficulty(), that.getDifficulty())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDifficulty())
            .append(this.getIntelligenceReward())
            .build();
    }

    public enum Difficulty {

        EASY,
        HARD,
        EXPERT,
        VIRTUOSO

    }

}