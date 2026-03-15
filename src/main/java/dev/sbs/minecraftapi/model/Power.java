package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import dev.sbs.api.persistence.converter.optional.OptionalStringConverter;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "powers"
)
public class Power implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @Convert(converter = OptionalStringConverter.class)
    private @NotNull Optional<String> stoneId = Optional.empty();
    @SerializedName("requiredLevel")
    private int requiredCombatLevel = 0;
    @Enumerated(EnumType.STRING)
    private @NotNull Stage stage = Stage.STARTER;
    private @NotNull ConcurrentMap<String, Double> baseValues = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Double> bonuses = Concurrent.newMap();

    public @NotNull Optional<Item> getStone() {
        return this.stoneId.flatMap(stoneId -> MinecraftApi.getRepository(Item.class)
            .findFirst(Item::getId, stoneId)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Power that = (Power) o;

        return new EqualsBuilder()
            .append(this.getRequiredCombatLevel(), that.getRequiredCombatLevel())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getStoneId(), that.getStoneId())
            .append(this.getStage(), that.getStage())
            .append(this.getBaseValues(), that.getBaseValues())
            .append(this.getBonuses(), that.getBonuses())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getStoneId())
            .append(this.getRequiredCombatLevel())
            .append(this.getStage())
            .append(this.getBaseValues())
            .append(this.getBonuses())
            .build();
    }

    public enum Stage {

        STARTER,
        INTERMEDIATE,
        ADVANCED,
        MASTER,
        GRANDIOSE,
        MARVELOUS;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

    }

}