package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.JsonResource;
import dev.sbs.api.persistence.converter.optional.OptionalStringConverter;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.AccessLevel;
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
import javax.persistence.OneToMany;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "bestiary_families",
    indexes = {
        BestiaryCategory.class,
        BestiarySubcategory.class,
        MobType.class
    }
)
public class BestiaryFamily implements JpaModel {

    public static final @NotNull ConcurrentList<ConcurrentList<Integer>> BRACKETS = Concurrent.newUnmodifiableList(
        Concurrent.newUnmodifiableList(
            20, 40, 60, 100, 200,
            400, 800, 1_400, 2_000, 3_000,
            6_000, 12_000, 20_000, 30_000, 40_000,
            50_000, 60_000, 72_000, 86_000, 100_000,
            200_000, 400_000, 600_000, 800_000, 1_000_000
        ),
        Concurrent.newUnmodifiableList(
            5, 10, 15, 25, 50,
            100, 200, 350, 500, 750,
            1_500, 3_000, 5_000, 7_500, 10_000,
            12_500, 15_000, 18_000, 21_500, 25_000,
            50_000, 100_000, 150_000, 200_000, 250_000
        ),
        Concurrent.newUnmodifiableList(
            4, 8, 12, 16, 20,
            40, 80, 140, 200, 300,
            600, 1_200, 2_000, 3_000, 4_000,
            5_000, 6_000, 7_200, 8_600, 10_000,
            20_000, 40_000, 60_000, 80_000, 100_000
        ),
        Concurrent.newUnmodifiableList(
            2, 4, 6, 10, 15,
            20, 25, 35, 50, 75,
            150, 300, 500, 750, 1_000,
            1_350, 1_650, 2_000, 2_500, 3_000,
            5_000, 10_000, 15_000, 20_000, 25_000
        ),
        Concurrent.newUnmodifiableList(
            1, 2, 3, 5, 7,
            10, 15, 20, 25, 30,
            60, 120, 200, 300, 400,
            500, 600, 720, 860, 1_000,
            2_000, 4_000, 6_000, 8_000, 10_000
        ),
        Concurrent.newUnmodifiableList(
            1, 2, 3, 5, 7,
            9, 14, 17, 21, 25,
            50, 80, 125, 175, 250,
            325, 425, 525, 625, 750,
            1_500, 3_000, 4_500, 6_000, 7_500
        ),
        Concurrent.newUnmodifiableList(
            1, 2, 3, 5, 7,
            9, 11, 14, 17, 20,
            30, 40, 55, 75, 100,
            150, 200, 275, 375, 500,
            1_000, 1_500, 2_000, 2_500, 3_000
        )
    );

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    @Enumerated(EnumType.STRING)
    private @NotNull ChatFormat format = ChatFormat.GREEN;
    private int bracket = 1;
    private int maxTier = 25;
    @Column(name = "category_id")
    @SerializedName("category")
    private @NotNull String categoryId = "";
    @Column(name = "subcategory_id")
    @SerializedName("subcategory")
    @Convert(converter = OptionalStringConverter.class)
    private @NotNull Optional<String> subcategoryId = Optional.empty();
    @SerializedName("mobTypes")
    private @NotNull ConcurrentList<String> mobTypeIds = Concurrent.newUnmodifiableList();
    private @NotNull ConcurrentList<String> mobs = Concurrent.newUnmodifiableList();

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private transient BestiaryCategory category;

    @ManyToOne
    @JoinColumn(name = "subcategory_id", referencedColumnName = "id")
    @Getter(AccessLevel.NONE)
    private transient BestiarySubcategory subcategory;

    @OneToMany
    private transient ConcurrentList<MobType> mobTypes = Concurrent.newList();

    public @NotNull Optional<BestiarySubcategory> getSubcategory() {
        return Optional.ofNullable(this.subcategory);
    }

    public int getMaxKills() {
        return BRACKETS
            .get(this.getBracket() - 1)
            .get(this.getMaxTier() - 1);
    }

    public @NotNull ConcurrentList<Integer> getTiers() {
        return BRACKETS.get(this.getBracket() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BestiaryFamily that = (BestiaryFamily) o;

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