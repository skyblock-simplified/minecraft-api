package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import dev.sbs.api.persistence.converter.optional.OptionalStringConverter;
import dev.sbs.minecraftapi.MinecraftApi;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "accessories",
    indexes = {Item.class}
)
public class Accessory implements JpaModel {

    @Column(name = "id")
    private @Id @NotNull String id;
    @Convert(converter = OptionalStringConverter.class)
    private @NotNull Optional<String> description = Optional.empty();
    @Enumerated(EnumType.STRING)
    private @NotNull Source source = Source.MISCELLANEOUS;
    @Enumerated(EnumType.STRING)
    private @NotNull Limit limit = Limit.NONE;
    @Getter(AccessLevel.NONE)
    @Transient
    private @NotNull Optional<Family> family = Optional.empty();

    @ManyToOne
    @JoinColumn(name = "id", referencedColumnName = "id")
    private transient Item item;

    public @NotNull Optional<Family> getFamily() {
        return this.family;
    }

    public @NotNull Item getItem() {
        return MinecraftApi.getRepository(Item.class)
            .findFirstOrNull(Item::getId, this.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Accessory that = (Accessory) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getDescription(), that.getDescription())
            .append(this.getSource(), that.getSource())
            .append(this.getLimit(), that.getLimit())
            .append(this.getFamily(), that.getFamily())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getDescription())
            .append(this.getSource())
            .append(this.getLimit())
            .append(this.getFamily())
            .build();
    }

    public enum Limit {

        NONE,
        BINGO,
        RIFT

    }

    public enum Source {

        COLLECTION,
        NPC,
        EVENT,
        MOB_DROP,
        SLAYER,
        QUEST,
        DUNGEON,
        MISCELLANEOUS,
        RIFT

    }

    @Getter
    public static class Family {

        private @NotNull String id = "";
        private int rank;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Family that = (Family) o;

            return new EqualsBuilder()
                .append(this.getRank(), that.getRank())
                .append(this.getId(), that.getId())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getRank())
                .build();
        }

    }

    @Getter
    @AllArgsConstructor
    public static class Substitute {

        private @NotNull String id;
        private double value;

        public @NotNull Stat getStat() {
            return MinecraftApi.getRepository(Stat.class)
                .findFirstOrNull(Stat::getId, this.getId());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Substitute that = (Substitute) o;

            return new EqualsBuilder()
                .append(this.getValue(), that.getValue())
                .append(this.getId(), that.getId())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getValue())
                .build();
        }

    }

}