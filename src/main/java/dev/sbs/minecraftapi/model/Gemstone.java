package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "gemstones",
    indexes = {
        Stat.class
    }
)
public class Gemstone implements JpaModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String symbol = "";
    @Enumerated(EnumType.STRING)
    private @NotNull ChatFormat format = ChatFormat.WHITE;
    @Column(name = "stat_id")
    @SerializedName("stat")
    private @NotNull String statId = "";
    private @NotNull ConcurrentMap<Type, ConcurrentMap<Rarity, Double>> values = Concurrent.newMap();

    @ManyToOne
    @JoinColumn(name = "stat_id", referencedColumnName = "id")
    private transient Stat stat;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Gemstone that = (Gemstone) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getSymbol(), that.getSymbol())
            .append(this.getFormat(), that.getFormat())
            .append(this.getStatId(), that.getStatId())
            .append(this.getValues(), that.getValues())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getSymbol())
            .append(this.getFormat())
            .append(this.getStatId())
            .append(this.getValues())
            .build();
    }

    public enum Type {

        ROUGH,
        FLAWED,
        FINE,
        FLAWLESS,
        PERFECT

    }

}