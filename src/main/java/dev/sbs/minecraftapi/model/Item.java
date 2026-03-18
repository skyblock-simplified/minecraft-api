package dev.sbs.minecraftapi.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.type.GsonType;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.mojang.profile.MojangProperty;
import dev.sbs.minecraftapi.skyblock.common.GameStage;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.awt.*;
import java.util.List;
import java.util.Optional;

@Getter
@Entity
@Table(name = "items")
public class Item implements JpaModel, PostInit {

    // Expected Data
    private @Id @NotNull String id = "";
    private @NotNull String material = "";
    @SerializedName("name")
    private @NotNull String displayName = "";
    @SerializedName("tier")
    @Enumerated(EnumType.STRING)
    private @NotNull Rarity rarity = Rarity.COMMON;
    @Column(name = "category_id")
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
    @Enumerated(EnumType.STRING)
    private @NotNull Soulbound soulbound = Soulbound.NONE;

    private transient Attributes attributes;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private transient ItemCategory category;

    public @NotNull Attributes getAttributes() {
        if (this.attributes == null) {
            this.attributes = new Attributes(
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
    @Transient
    private @NotNull Optional<MojangProperty> skin = Optional.empty();
    private @NotNull Optional<String> furniture = Optional.empty();
    private @NotNull Optional<String> crystal = Optional.empty();
    @SerializedName("museum_data")
    @Transient
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

    public @NotNull String getMinecraftId() {
        return this.getDurability()
            .map(value -> String.format("%s:%s", this.getMaterial(), value))
            .orElse(this.getMaterial());
    }

    public boolean isDungeonizable() {
        return this.getDungeonizationCost().notEmpty();
    }

    public boolean isMinion() {
        return this.getGenerator().isPresent();
    }

    @Override
    public void postInit() {
        if (this.category == null) {
            this.category = MinecraftApi.getRepository(ItemCategory.class)
                .findFirstOrNull(ItemCategory::getId, "OTHER");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Item item = (Item) o;

        return new EqualsBuilder()
            .append(this.getNpcSellPrice(), item.getNpcSellPrice())
            .append(this.getAbilityDamageScaling(), item.getAbilityDamageScaling())
            .append(this.getAttributes(), item.getAttributes())
            .append(this.getGearScore(), item.getGearScore())
            .append(this.isMotesValueLostOnTransfer(), item.isMotesValueLostOnTransfer())
            .append(this.getMotesSellPrice(), item.getMotesSellPrice())
            .append(this.getGeneratorTier(), item.getGeneratorTier())
            .append(this.getMaterial(), item.getMaterial())
            .append(this.getId(), item.getId())
            .append(this.getDisplayName(), item.getDisplayName())
            .append(this.getRarity(), item.getRarity())
            .append(this.getDurability(), item.getDurability())
            .append(this.getCategory(), item.getCategory())
            .append(this.getDescription(), item.getDescription())
            .append(this.getColor(), item.getColor())
            .append(this.getOrigin(), item.getOrigin())
            .append(this.getSkin(), item.getSkin())
            .append(this.getFurniture(), item.getFurniture())
            .append(this.getCrystal(), item.getCrystal())
            .append(this.getMuseumData(), item.getMuseumData())
            .append(this.getSwordType(), item.getSwordType())
            .append(this.getMiniIslandGenerator(), item.getMiniIslandGenerator())
            .append(this.getDungeonizationCost(), item.getDungeonizationCost())
            .append(this.getCatacombsRequirements(), item.getCatacombsRequirements())
            .append(this.getGenerator(), item.getGenerator())
            .append(this.getEnchantments(), item.getEnchantments())
            .append(this.getGemstoneSlots(), item.getGemstoneSlots())
            .append(this.getItemSpecific(), item.getItemSpecific())
            .append(this.getPrestige(), item.getPrestige())
            .append(this.getRequirements(), item.getRequirements())
            .append(this.getSalvages(), item.getSalvages())
            .append(this.getStats(), item.getStats())
            .append(this.getTieredStats(), item.getTieredStats())
            .append(this.getUpgradeCosts(), item.getUpgradeCosts())
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
    public static class Attributes {

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

        public boolean notSellable() { return !this.isNpcSellable(); }
        public boolean notPlaceable() { return !this.isPlaceable(); }
        public boolean notTradable() { return !this.isTradeable(); }
        public boolean notAuctionable() { return !this.isAuctionable(); }
        public boolean notReforgeable() { return !this.isReforgeable(); }
        public boolean notRecombobulatable() { return !this.isRecombobulatable(); }
        public boolean notBurnableInFurnace() { return !this.isBurnableInFurnace(); }
        public boolean notSalvageableFromRecipe() { return !this.isSalvageableFromRecipe(); }
        public boolean notMuseumable() { return !this.isMuseumable(); }
        public boolean notGlowing() { return !this.isGlowing(); }
        public boolean notStackable() { return !this.isUnstackable(); }
        public boolean notDungeonItem() { return !this.isDungeonItem(); }
        public boolean notRiftTransferrable() { return !this.isRiftTransferrable(); }
        public boolean notObtainable() { return !this.isObtainable(); }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Attributes that = (Attributes) o;

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
    @GsonType
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class MuseumData {

        @SerializedName("donation_xp")
        private int donationXP;
        private @NotNull MuseumData.Type type = MuseumData.Type.UNKNOWN;
        private @NotNull ConcurrentMap<String, String> parent = Concurrent.newMap();
        @SerializedName("armor_set_donation_xp")
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, String> armorSetDonationXP = Concurrent.newMap();
        @SerializedName("game_stage")
        private @NotNull GameStage gameStage = GameStage.UNKNOWN;

        public int getActualXP() {
            return this.armorSetDonationXP.stream()
                .findFirst()
                .map(ConcurrentMap.Entry::getValue)
                .map(Integer::parseInt)
                .orElse(this.getDonationXP());
        }

        public @NotNull Optional<String> getArmorSet() {
            return this.armorSetDonationXP.stream()
                .findFirst()
                .map(ConcurrentMap.Entry::getKey);
        }

        public enum Type {

            UNKNOWN,
            ARMOR_SETS,
            WEAPONS,
            RARITIES,
            SPECIAL

        }

    }

    public enum Soulbound {

        NONE,
        SOLO,
        COOP

    }

    public enum Type {

        ARMOR,
        COLLECTIBLE,
        CONSUMABLE,
        EQUIPMENT,
        OTHER,
        TOOL,
        WEAPON

    }

    @Getter
    @GsonType
    public static class Cost {

        private int experience = 0;
        private @NotNull ConcurrentMap<Currency, Double> currencies = Concurrent.newMap();
        private @NotNull ConcurrentMap<String, Double> items = Concurrent.newMap();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            Cost that = (Cost) o;

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

        public enum Currency {

            @SerializedName("coins")
            COINS,
            @SerializedName("essence")
            ESSENCE,
            @SerializedName("motes")
            MOTES,
            @SerializedName("stars")
            STARS,
            @SerializedName("northStars")
            NORTH_STARS,
            @SerializedName("pelts")
            PELTS

        }

    }

}
