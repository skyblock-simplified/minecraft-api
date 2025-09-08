package dev.sbs.minecraftapi.skyblock.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.mutable.MutableDouble;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.sbs.minecraftapi.skyblock.island.crimson_isle.CrimsonIsle;
import dev.sbs.minecraftapi.skyblock.island.crimson_isle.TrophyFishing;
import dev.sbs.minecraftapi.skyblock.island.mining.ForgeItem;
import dev.sbs.minecraftapi.skyblock.island.mining.GlaciteTunnels;
import dev.sbs.minecraftapi.skyblock.island.mining.Mining;
import dev.sbs.minecraftapi.skyblock.island.profile.PlayerData;
import dev.sbs.minecraftapi.skyblock.island.profile.dungeon.DungeonProfile;
import dev.sbs.minecraftapi.skyblock.island.profile.pet.PetProfile;
import dev.sbs.minecraftapi.skyblock.island.profile.skill.SkillProfile;
import dev.sbs.minecraftapi.skyblock.island.profile.slayer.SlayerProfile;
import dev.sbs.minecraftapi.skyblock.type.Weight;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Member implements PostInit {
    
    // Profile
    @SerializedName("player_id")
    private @NotNull UUID uniqueId;
    @SerializedName("first_join_hub")
    private SkyBlockDate.SkyBlockTime firstJoinHub;
    @SerializedPath("profile.first_join")
    private SkyBlockDate.RealTime firstJoin;
    @SerializedPath("profile.personal_bank_upgrade")
    private int personalBankUpgrade;
    @SerializedPath("profile.cookie_buff_active")
    private boolean boosterCookieActive;

    // Data
    @SerializedName("player_data")
    private @NotNull PlayerData playerData = new PlayerData();
    @SerializedName("slayer")
    private @NotNull SlayerProfile slayerData = new SlayerProfile();
    private transient SkillProfile skillData;
    @SerializedName("dungeons")
    private @NotNull DungeonProfile dungeonData = new DungeonProfile();
    @SerializedName("pet_data")
    private @NotNull PetProfile petData = new PetProfile();

    // Locations
    private @NotNull Rift rift = new Rift();

    // Mining
    private @NotNull Mining mining = new Mining();
    @SerializedPath("forge.forge_processes.forge_1")
    private @NotNull ConcurrentMap<Integer, ForgeItem> forge = Concurrent.newMap();
    @SerializedName("glacite_player_data")
    private @NotNull GlaciteTunnels glaciteTunnels = new GlaciteTunnels();

    private @NotNull Bestiary bestiary = new Bestiary();
    @SerializedName("accessory_bag_storage")
    private @NotNull AccessoryBag accessoryBag = new AccessoryBag();
    private @NotNull Leveling leveling = new Leveling();
    @SerializedName("nether_island_player_data")
    private @NotNull CrimsonIsle crimsonIsle = new CrimsonIsle();
    private @NotNull Experimentation experimentation = new Experimentation();
    @SerializedName("player_stats")
    private @NotNull PlayerStats playerStats = new PlayerStats();
    @SerializedName("fairy_soul")
    private @NotNull FairySouls fairySouls = new FairySouls();
    private @NotNull Currencies currencies = new Currencies();
    @SerializedName("item_data")
    private @NotNull ItemSettings itemSettings = new ItemSettings();
    @SerializedName("jacobs_contest")
    private @NotNull JacobsContest jacobsContest = new JacobsContest();
    private @NotNull Inventory inventory = new Inventory();
    private @NotNull Optional<Quests> quests = Optional.empty();

    // Maps
    @SerializedName("trophy_fish")
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, Object> trophyFishMap = Concurrent.newMap();
    private transient TrophyFishing trophyFish;
    private @NotNull ConcurrentMap<String, Long> collection = Concurrent.newMap();
    private transient @NotNull ConcurrentMap<String, Integer> collectionUnlocked = Concurrent.newMap();
    @SerializedPath("objectives.tutorial")
    private @NotNull ConcurrentList<String> tutorialObjectives = Concurrent.newList();

    @Override
    public void postInit() {
        this.accessoryBag.initialize(this);
        this.trophyFish = new TrophyFishing(this.trophyFishMap);
        this.skillData = new SkillProfile(this.getPlayerData().getSkillExperience(), this);

        this.collectionUnlocked = this.getCollection()
            .stream()
            .map((itemId, value) -> Pair.of(itemId, this.getPlayerData()
                .getUnlockedCollectionTiers()
                .stream()
                .filter(tier -> tier.matches(String.format("^%s_[\\d]+$", itemId)))
                .map(tier -> Integer.parseInt(tier.replace(String.format("%s_", itemId), "")))
                .max(Comparator.naturalOrder())
                .orElse(0)
            ))
            .collect(Concurrent.toUnmodifiableMap());
    }

    public @NotNull ConcurrentList<Integer> getCraftedMinions(@NotNull String itemId) {
        return this.getPlayerData().getCraftedMinions(itemId);
    }

    // Weight

    public @NotNull Weight getTotalWeight() {
        // Load Weights
        Weight skillWeight = this.getTotalWeight(member -> member.getSkillData().getWeight());
        Weight slayerWeight = this.getTotalWeight(member -> member.getSlayerData().getWeight());
        Weight dungeonWeight = this.getTotalWeight(member -> member.getDungeonData().getWeight());
        Weight dungeonClassWeight = this.getTotalWeight(member -> member.getDungeonData().getClassWeight());

        return Weight.of(
            skillWeight.getValue() + slayerWeight.getValue() + dungeonWeight.getValue() + dungeonClassWeight.getValue(),
            skillWeight.getOverflow() + slayerWeight.getOverflow() + dungeonWeight.getOverflow() + dungeonClassWeight.getOverflow()
        );
    }

    private Weight getTotalWeight(Function<Member, ConcurrentMap<?, Weight>> weightMapFunction) {
        MutableDouble totalWeight = new MutableDouble();
        MutableDouble totalOverflow = new MutableDouble();

        weightMapFunction.apply(this)
            .stream()
            .map(Map.Entry::getValue)
            .forEach(skillWeight -> {
                totalWeight.add(skillWeight.getValue());
                totalOverflow.add(skillWeight.getOverflow());
            });

        return Weight.of(totalWeight.get(), totalOverflow.get());
    }

}
