package dev.sbs.minecraftapi.skyblock.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.mojang.profile.MojangProperty;
import dev.sbs.minecraftapi.skyblock.common.GameStage;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Optional;

public interface Item extends Model {

    // Expected Data

    @NotNull String getMaterial();

    @NotNull String getId();

    @NotNull String getDisplayName();

    @NotNull Rarity getRarity();

    @NotNull String getCategoryId();

    default @NotNull ItemCategory getCategory() {
        return MinecraftApi.getRepository(ItemCategory.class)
            .findFirst(ItemCategory::getId, this.getCategoryId())
            .orElse(MinecraftApi.getRepository(ItemCategory.class)
                .findFirstOrNull(ItemCategory::getId, "OTHER")
            );
    }

    @NotNull Attributes getAttributes();

    default @NotNull String getMinecraftId() {
        return this.getDurability()
            .map(value -> String.format("%s:%s", this.getMaterial(), value))
            .orElse(this.getMaterial());
    }

    // Possible Data

    @NotNull Optional<Integer> getDurability();

    @NotNull Optional<String> getDescription();

    @NotNull Optional<Color> getColor();

    @NotNull Optional<String> getOrigin();

    @NotNull Optional<MojangProperty> getSkin();

    @NotNull Optional<String> getFurniture();

    @NotNull Optional<String> getSwordType();

    @NotNull Optional<String> getCrystal();

    @NotNull Optional<String> getMiniIslandGenerator();

    @NotNull Optional<MuseumData> getMuseumData();

    double getNpcSellPrice();

    double getAbilityDamageScaling();

    // Dungeons

    int getGearScore();

    @NotNull ConcurrentMap<String, Object> getDungeonizationCost();

    @NotNull ConcurrentList<ConcurrentMap<String, Object>> getCatacombsRequirements();

    default boolean isDungeonizable() {
        return this.getDungeonizationCost().notEmpty();
    }

    // Rift

    boolean isMotesValueLostOnTransfer();

    double getMotesSellPrice();

    // Minions

    @NotNull Optional<String> getGenerator();

    int getGeneratorTier();

    default boolean isMinion() {
        return this.getGenerator().isPresent();
    }

    // Other

    @NotNull ConcurrentMap<String, Double> getEnchantments();

    @NotNull ConcurrentList<ConcurrentMap<String, Object>> getGemstoneSlots();

    @NotNull ConcurrentMap<String, Object> getItemSpecific();

    @NotNull ConcurrentMap<String, Object> getPrestige();

    @NotNull ConcurrentList<ConcurrentMap<String, Object>> getRequirements();

    @NotNull ConcurrentList<ConcurrentMap<String, Object>> getSalvages();

    @NotNull ConcurrentMap<String, Double> getStats();

    @NotNull ConcurrentMap<String, List<Double>> getTieredStats();

    @NotNull ConcurrentList<ConcurrentList<ConcurrentMap<String, Object>>> getUpgradeCosts();

    interface Attributes {

        boolean isNpcSellable();

        default boolean notSellable() {
            return !this.isNpcSellable();
        }

        boolean isPlaceable();

        default boolean notPlaceable() {
            return !this.isPlaceable();
        }

        boolean isTradeable();

        default boolean notTradable() {
            return !this.isTradeable();
        }

        boolean isAuctionable();

        default boolean notAuctionable() {
            return !this.isAuctionable();
        }

        boolean isReforgeable();

        default boolean notReforgeable() {
            return !this.isReforgeable();
        }

        boolean isRecombobulatable();

        default boolean notRecombobulatable() {
            return !this.isRecombobulatable();
        }

        boolean isBurnableInFurnace();

        default boolean notBurnableInFurnace() {
            return !this.isBurnableInFurnace();
        }

        boolean isSalvageableFromRecipe();

        default boolean notSalvageableFromRecipe() {
            return !this.isSalvageableFromRecipe();
        }

        boolean isMuseumable();

        default boolean notMuseumable() {
            return !this.isMuseumable();
        }

        boolean isGlowing();

        default boolean notGlowing() {
            return !this.isGlowing();
        }

        boolean isUnstackable();

        default boolean notStackable() {
            return !this.isUnstackable();
        }

        boolean isDungeonItem();

        default boolean notDungeonItem() {
            return !this.isDungeonItem();
        }

        boolean isRiftTransferrable();

        default boolean notRiftTransferrable() {
            return !this.isRiftTransferrable();
        }

        boolean isObtainable();

        default boolean notObtainable() {
            return !this.isObtainable();
        }

        @NotNull Soulbound getSoulbound();

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    class MuseumData {

        @SerializedName("donation_xp")
        private int donationXP;
        private @NotNull Type type = Type.UNKNOWN;
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

    enum Soulbound {

        NONE,
        SOLO,
        COOP

    }

    enum Type {

        ARMOR,
        COLLECTIBLE,
        CONSUMABLE,
        EQUIPMENT,
        OTHER,
        TOOL,
        WEAPON

    }

    interface Cost {

        @NotNull ConcurrentMap<Currency, Double> getCurrencies();

        int getExperience();

        @NotNull ConcurrentMap<String, Double> getItems();

        enum Currency {

            COINS,
            ESSENCE,
            MOTES,
            NORTH_STARS,
            PELTS

        }

    }

}
