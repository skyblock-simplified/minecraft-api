package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.type.GsonType;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Optional;

@Getter
@Entity
@Table(name = "enchantments")
public class Enchantment implements JpaModel {

    @Id
    @Column(name = "id", nullable = false)
    private @NotNull String id = "";

    @Column(name = "name", nullable = false)
    private @NotNull String name = "";

    @Column(name = "description", nullable = false)
    private @NotNull String description = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private @NotNull Type type = Type.NORMAL;

    @Column(name = "required_level", nullable = false)
    private int requiredLevel = 0;

    @Column(name = "conflict")
    private @NotNull Optional<String> conflict = Optional.empty();

    @Column(name = "categories", nullable = false)
    private @NotNull ConcurrentList<String> categoryIds = Concurrent.newList();

    @Column(name = "items", nullable = false)
    private @NotNull ConcurrentList<String> itemIds = Concurrent.newList();

    @Column(name = "levels", nullable = false)
    private @NotNull ConcurrentList<Level> levels = Concurrent.newList();

    @Column(name = "stats", nullable = false)
    private @NotNull ConcurrentList<Stat.Substitute> stats = Concurrent.newList();

    @Column(name = "mob_types", nullable = false)
    private @NotNull ConcurrentList<String> mobTypeIds = Concurrent.newList();

    @OneToMany
    private transient @NotNull ConcurrentList<ItemCategory> categories = Concurrent.newList();

    @OneToMany
    private transient @NotNull ConcurrentList<Item> items = Concurrent.newList();

    @OneToMany
    private transient @NotNull ConcurrentList<MobType> mobTypes = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Enchantment that = (Enchantment) o;

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
    @RequiredArgsConstructor
    public enum Type {

        NORMAL(ChatFormat.BLUE, false),
        ULTIMATE(ChatFormat.LIGHT_PURPLE, true);

        private final @NotNull ChatFormat format;
        private final boolean bold;

        @Override
        public String toString() {
            return this.getFormat() + (this.isBold() ? ChatFormat.BOLD.toString() : "");
        }

    }

    @Getter
    @GsonType
    public static class Level {

        private int level = 0;
        @SerializedName("cost")
        private @NotNull Item.Cost applyCost = new Item.Cost();

        public @NotNull Rarity getRarity() {
            return switch (this.getLevel()) {
                case 9, 10 -> Rarity.MYTHIC;
                case 8 -> Rarity.LEGENDARY;
                case 7 -> Rarity.EPIC;
                case 6 -> Rarity.RARE;
                case 5 -> Rarity.UNCOMMON;
                default -> Rarity.COMMON;
            };
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Level that = (Level) o;

            return new EqualsBuilder()
                .append(this.getLevel(), that.getLevel())
                .append(this.getApplyCost(), that.getApplyCost())
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
