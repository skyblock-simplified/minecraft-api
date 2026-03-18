package dev.sbs.minecraftapi.model;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Entity
@Table(name = "mayors")
public class Mayor implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private boolean special;
    @Enumerated(EnumType.STRING)
    private @NotNull ChatFormat format = ChatFormat.LIGHT_PURPLE;
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentList<Perk> perks = Concurrent.newList();

    public @NotNull ConcurrentList<Perk> getPerks() {
        return this.perks;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Mayor that = (Mayor) o;

        return new EqualsBuilder()
            .append(this.isSpecial(), that.isSpecial())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getFormat(), that.getFormat())
            .append(this.getPerks(), that.getPerks())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.isSpecial())
            .append(this.getFormat())
            .append(this.getPerks())
            .build();
    }

    @Getter
    public static class Perk {

        private @NotNull String id = "";
        private @NotNull String name = "";
        private @NotNull String description = "";
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentList<Substitute> stats = Concurrent.newList();

        public @NotNull ConcurrentList<Substitute> getStats() {
            return this.stats;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Perk that = (Perk) o;

            return new EqualsBuilder()
                .append(this.getId(), that.getId())
                .append(this.getName(), that.getName())
                .append(this.getDescription(), that.getDescription())
                .append(this.getStats(), that.getStats())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getName())
                .append(this.getDescription())
                .append(this.getStats())
                .build();
        }

    }

    @Getter
    public static class Substitute {

        private @NotNull String id = "";
        private int precision = 0;
        private @NotNull Stat.Type type = Stat.Type.NONE;
        private @NotNull ChatFormat format = ChatFormat.GREEN;
        private double value;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Substitute that = (Substitute) o;

            return new EqualsBuilder()
                .append(this.getPrecision(), that.getPrecision())
                .append(this.getValue(), that.getValue())
                .append(this.getId(), that.getId())
                .append(this.getType(), that.getType())
                .append(this.getFormat(), that.getFormat())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getPrecision())
                .append(this.getType())
                .append(this.getFormat())
                .append(this.getValue())
                .build();
        }

    }

}