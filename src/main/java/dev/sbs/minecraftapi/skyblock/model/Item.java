package dev.sbs.minecraftapi.skyblock.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.data.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.mojang.profile.MojangProperty;
import dev.sbs.minecraftapi.skyblock.GameStage;
import dev.sbs.minecraftapi.skyblock.Rarity;
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
        return MinecraftApi.getRepositoryOf(ItemCategory.class)
            .findFirst(ItemCategory::getId, this.getCategoryId())
            .orElse(MinecraftApi.getRepositoryOf(ItemCategory.class)
                .findFirstOrNull(ItemCategory::getId, "OTHER")
            );
    }

    @NotNull Soulbound getSoulboundStatus();

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

    // Booleans

    boolean isGlowing();

    default boolean notGlowing() {
        return !this.isGlowing();
    }

    boolean isUnstackable();

    default boolean notStackable() {
        return !this.isUnstackable();
    }

    boolean isMuseumable();

    default boolean notMuseumable() {
        return !this.isMuseumable();
    }

    /*boolean isAttributable();

    default boolean notAttributable() {
        return !this.isAttributable();
    }*/

    boolean isSalvageableFromRecipe();

    default boolean notSalvageableFromRecipe() {
        return !this.isSalvageableFromRecipe();
    }

    boolean isNotReforgeable();

    default boolean isReforgeable() {
        return !this.isNotReforgeable();
    }

    // Dungeons

    int getGearScore();

    boolean isDungeonItem();

    default boolean notDungeonItem() {
        return !this.isDungeonItem();
    }

    @NotNull ConcurrentMap<String, Object> getDungeonizationCost();

    @NotNull ConcurrentList<ConcurrentMap<String, Object>> getCatacombsRequirements();

    default boolean isDungeonizable() {
        return this.getDungeonizationCost().notEmpty();
    }

    // Rift

    boolean isRiftTransferable();

    default boolean notRiftTransferable() {
        return !this.isRiftTransferable();
    }

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

        boolean isSellable();

        boolean isTradable();

        boolean isAuctionable();

        boolean isReforgeable();

        boolean isMuseumable();

        boolean isGlowing(); // TODO: Default false

        boolean isUnstackable(); // TODO: Default true

        boolean isDungeonable(); // TODO: Default false

        boolean isObtainable(); // TODO: Default true

        @NotNull Soulbound getSoulbound(); // TODO: Default NONE

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

}
