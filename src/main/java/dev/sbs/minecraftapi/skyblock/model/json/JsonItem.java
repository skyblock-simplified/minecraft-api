package dev.sbs.minecraftapi.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.mojang.profile.MojangProperty;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import dev.sbs.minecraftapi.skyblock.model.Item;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.awt.*;
import java.util.List;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "items"
)
public class JsonItem implements Item, JsonModel {

    // Expected Data
    private @Id @NotNull String id = "";
    private @NotNull String material = "";
    @SerializedName("name")
    private @NotNull String displayName = "";
    @SerializedName("tier")
    private @NotNull Rarity rarity = Rarity.COMMON;
    @SerializedName("category")
    private @NotNull String categoryId = "OTHER";

    // Attributes
    @Getter(AccessLevel.NONE)
    private boolean can_place = true;
    @Getter(AccessLevel.NONE)
    private boolean can_trade = true;
    @Getter(AccessLevel.NONE)
    private boolean can_auction = true;
    @Getter(AccessLevel.NONE)
    private boolean cannot_reforge = false;
    @Getter(AccessLevel.NONE)
    private boolean can_recombobulate = true;
    @Getter(AccessLevel.NONE)
    private boolean can_burn_in_furnace = false;
    @Getter(AccessLevel.NONE)
    private boolean salvageable_from_recipe = false;
    @Getter(AccessLevel.NONE)
    private boolean museum = false;
    @Getter(AccessLevel.NONE)
    private boolean glowing = false;
    @Getter(AccessLevel.NONE)
    private boolean unstackable = true;
    @Getter(AccessLevel.NONE)
    private boolean dungeon_item = false;
    @Getter(AccessLevel.NONE)
    private boolean rift_transferrable = false;
    @Getter(AccessLevel.NONE)
    private @NotNull Soulbound soulbound = Soulbound.NONE;

    private transient JsonAttributes attributes;

    @Override
    public @NotNull JsonAttributes getAttributes() {
        if (this.attributes == null) {
            this.attributes = new JsonAttributes(
                this.npcSellPrice > 0,
                this.can_place,
                this.can_trade,
                this.can_auction,
                !this.cannot_reforge,
                this.can_recombobulate,
                this.can_burn_in_furnace,
                this.salvageable_from_recipe,
                this.museum,
                this.glowing,
                this.unstackable,
                this.dungeon_item,
                this.rift_transferrable,
                this.rarity != Rarity.ADMIN,
                this.soulbound
            );
        }

        return this.attributes;
    }

    // Possible Data
    private @NotNull Optional<Integer> durability = Optional.empty();
    private @NotNull Optional<String> description = Optional.empty();
    private @NotNull Optional<Color> color = Optional.empty();
    private @NotNull Optional<String> origin = Optional.empty();
    private @NotNull Optional<MojangProperty> skin = Optional.empty();
    private @NotNull Optional<String> furniture = Optional.empty();
    private @NotNull Optional<String> crystal = Optional.empty();
    @SerializedName("museum_data")
    private @NotNull Optional<MuseumData> museumData = Optional.empty();
    @SerializedName("sword_type")
    private @NotNull Optional<String> swordType = Optional.empty();
    @SerializedName("private_island")
    private @NotNull Optional<String> miniIslandGenerator = Optional.empty();
    @SerializedName("npc_sell_price")
    private double npcSellPrice;
    @SerializedName("ability_damage_scaling")
    private double abilityDamageScaling;

    // Dungeons
    @SerializedName("gear_score")
    private int gearScore;
    @SerializedName("dungeon_item_conversion_cost")
    private @NotNull ConcurrentMap<String, Object> dungeonizationCost = Concurrent.newMap();
    @SerializedName("catacombs_requirements")
    private @NotNull ConcurrentList<ConcurrentMap<String, Object>> catacombsRequirements = Concurrent.newList();

    // Rift
    @SerializedName("lose_motes_value_on_transfer")
    private boolean motesValueLostOnTransfer;
    @SerializedName("motes_sell_price")
    private double motesSellPrice;

    // Minions
    private @NotNull Optional<String> generator = Optional.empty();
    @SerializedName("generator_tier")
    private int generatorTier;

    // Other
    private @NotNull ConcurrentMap<String, Double> enchantments = Concurrent.newMap();
    @SerializedName("gemstone_slots")
    private @NotNull ConcurrentList<ConcurrentMap<String, Object>> gemstoneSlots = Concurrent.newList();
    @SerializedName("item_specific")
    private @NotNull ConcurrentMap<String, Object> itemSpecific = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Object> prestige = Concurrent.newMap();
    private @NotNull ConcurrentList<ConcurrentMap<String, Object>> requirements = Concurrent.newList();
    private @NotNull ConcurrentList<ConcurrentMap<String, Object>> salvages = Concurrent.newList();
    private @NotNull ConcurrentMap<String, Double> stats = Concurrent.newMap();
    @SerializedName("tiered_stats")
    private @NotNull ConcurrentMap<String, List<Double>> tieredStats = Concurrent.newMap();
    @SerializedName("upgrade_costs")
    private @NotNull ConcurrentList<ConcurrentList<ConcurrentMap<String, Object>>> upgradeCosts = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonItem jsonItem = (JsonItem) o;

        return new EqualsBuilder()
            .append(this.getNpcSellPrice(), jsonItem.getNpcSellPrice())
            .append(this.getAbilityDamageScaling(), jsonItem.getAbilityDamageScaling())
            .append(this.getAttributes(), jsonItem.getAttributes())
            .append(this.getGearScore(), jsonItem.getGearScore())
            .append(this.isMotesValueLostOnTransfer(), jsonItem.isMotesValueLostOnTransfer())
            .append(this.getMotesSellPrice(), jsonItem.getMotesSellPrice())
            .append(this.getGeneratorTier(), jsonItem.getGeneratorTier())
            .append(this.getMaterial(), jsonItem.getMaterial())
            .append(this.getId(), jsonItem.getId())
            .append(this.getDisplayName(), jsonItem.getDisplayName())
            .append(this.getRarity(), jsonItem.getRarity())
            .append(this.getDurability(), jsonItem.getDurability())
            .append(this.getCategory(), jsonItem.getCategory())
            .append(this.getDescription(), jsonItem.getDescription())
            .append(this.getColor(), jsonItem.getColor())
            .append(this.getOrigin(), jsonItem.getOrigin())
            .append(this.getSkin(), jsonItem.getSkin())
            .append(this.getFurniture(), jsonItem.getFurniture())
            .append(this.getCrystal(), jsonItem.getCrystal())
            .append(this.getMuseumData(), jsonItem.getMuseumData())
            .append(this.getSwordType(), jsonItem.getSwordType())
            .append(this.getMiniIslandGenerator(), jsonItem.getMiniIslandGenerator())
            .append(this.getDungeonizationCost(), jsonItem.getDungeonizationCost())
            .append(this.getCatacombsRequirements(), jsonItem.getCatacombsRequirements())
            .append(this.getGenerator(), jsonItem.getGenerator())
            .append(this.getEnchantments(), jsonItem.getEnchantments())
            .append(this.getGemstoneSlots(), jsonItem.getGemstoneSlots())
            .append(this.getItemSpecific(), jsonItem.getItemSpecific())
            .append(this.getPrestige(), jsonItem.getPrestige())
            .append(this.getRequirements(), jsonItem.getRequirements())
            .append(this.getSalvages(), jsonItem.getSalvages())
            .append(this.getStats(), jsonItem.getStats())
            .append(this.getTieredStats(), jsonItem.getTieredStats())
            .append(this.getUpgradeCosts(), jsonItem.getUpgradeCosts())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getMaterial())
            .append(this.getId())
            .append(this.getDisplayName())
            .append(this.getRarity())
            .append(this.getDurability())
            .append(this.getCategory())
            .append(this.getDescription())
            .append(this.getColor())
            .append(this.getOrigin())
            .append(this.getSkin())
            .append(this.getFurniture())
            .append(this.getCrystal())
            .append(this.getMuseumData())
            .append(this.getSwordType())
            .append(this.getMiniIslandGenerator())
            .append(this.getNpcSellPrice())
            .append(this.getAbilityDamageScaling())
            .append(this.getAttributes())
            .append(this.getGearScore())
            .append(this.getDungeonizationCost())
            .append(this.getCatacombsRequirements())
            .append(this.isMotesValueLostOnTransfer())
            .append(this.getMotesSellPrice())
            .append(this.getGenerator())
            .append(this.getGeneratorTier())
            .append(this.getEnchantments())
            .append(this.getGemstoneSlots())
            .append(this.getItemSpecific())
            .append(this.getPrestige())
            .append(this.getRequirements())
            .append(this.getSalvages())
            .append(this.getStats())
            .append(this.getTieredStats())
            .append(this.getUpgradeCosts())
            .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JsonAttributes implements Attributes {

        private boolean npcSellable;
        private boolean placeable;
        private boolean tradeable;
        private boolean auctionable;
        private boolean reforgeable;
        private boolean recombobulatable;
        private boolean burnableInFurnace;
        private boolean salvageableFromRecipe;
        private boolean museumable;
        private boolean glowing;
        private boolean unstackable;
        private boolean dungeonItem;
        private boolean riftTransferrable;
        private boolean obtainable;
        private @NotNull Soulbound soulbound;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonAttributes that = (JsonAttributes) o;

            return new EqualsBuilder()
                .append(this.isNpcSellable(), that.isNpcSellable())
                .append(this.isPlaceable(), that.isPlaceable())
                .append(this.isTradeable(), that.isTradeable())
                .append(this.isAuctionable(), that.isAuctionable())
                .append(this.isReforgeable(), that.isReforgeable())
                .append(this.isRecombobulatable(), that.isRecombobulatable())
                .append(this.isBurnableInFurnace(), that.isBurnableInFurnace())
                .append(this.isSalvageableFromRecipe(), that.isSalvageableFromRecipe())
                .append(this.isMuseumable(), that.isMuseumable())
                .append(this.isGlowing(), that.isGlowing())
                .append(this.isUnstackable(), that.isUnstackable())
                .append(this.isDungeonItem(), that.isDungeonItem())
                .append(this.isRiftTransferrable(), that.isRiftTransferrable())
                .append(this.isObtainable(), that.isObtainable())
                .append(this.getSoulbound(), that.getSoulbound())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.isNpcSellable())
                .append(this.isPlaceable())
                .append(this.isTradeable())
                .append(this.isAuctionable())
                .append(this.isReforgeable())
                .append(this.isRecombobulatable())
                .append(this.isBurnableInFurnace())
                .append(this.isSalvageableFromRecipe())
                .append(this.isMuseumable())
                .append(this.isGlowing())
                .append(this.isUnstackable())
                .append(this.isDungeonItem())
                .append(this.isRiftTransferrable())
                .append(this.isObtainable())
                .append(this.getSoulbound())
                .build();
        }

    }

    @Getter
    public static class JsonCost implements Cost {

        private int experience = 0;
        private @NotNull ConcurrentMap<Currency, Double> currencies = Concurrent.newMap();
        private @NotNull ConcurrentMap<String, Double> items = Concurrent.newMap();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonCost that = (JsonCost) o;

            return new EqualsBuilder()
                .append(this.getCurrencies(), that.getCurrencies())
                .append(this.getExperience(), that.getExperience())
                .append(this.getItems(), that.getItems())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getCurrencies())
                .append(this.getExperience())
                .append(this.getItems())
                .build();
        }

    }

}
