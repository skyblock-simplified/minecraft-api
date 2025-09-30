package dev.sbs.minecraftapi.skyblock.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.mutable.MutableDouble;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.minecraftapi.skyblock.NbtContent;
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
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SkyBlockMember implements PostInit {
    
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

    private @NotNull Weight getTotalWeight(@NotNull Function<SkyBlockMember, ConcurrentMap<?, Weight>> weightMapFunction) {
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

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BagContents {

        @SerializedName("fishing_bag")
        private NbtContent fishing = new NbtContent();
        private NbtContent quiver = new NbtContent();
        @SerializedName("fishing_bag")
        private NbtContent accessories = new NbtContent();
        @SerializedName("potion_bag")
        private NbtContent potions = new NbtContent();

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Bestiary {

        private @NotNull ConcurrentMap<String, Integer> kills = Concurrent.newMap();
        private @NotNull ConcurrentMap<String, Integer> deaths = Concurrent.newMap();
        @SerializedPath("milestone.last_claimed_milestone")
        private int lastClaimedMilestone;

    }
    
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Currencies {

        @SerializedName("motes_purse")
        private int motes;
        @SerializedName("coin_purse")
        private double purse;
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, ConcurrentMap<String, Integer>> essence = Concurrent.newMap();

        public @NotNull ConcurrentMap<String, Integer> getEssence() {
            return this.essence.stream()
                .mapValue(value -> value.get("current"))
                .collect(Concurrent.toMap());
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Experimentation implements PostInit {

        @SerializedName("claims_resets")
        private int resetClaims;
        @SerializedName("claims_resets_timestamp")
        private Optional<SkyBlockDate.RealTime> resetClaimsTimestamp = Optional.empty();
        @SerializedName("serums_drank")
        private int serumsDrank;

        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Long> pairings = Concurrent.newMap();
        private transient @NotNull Optional<Table> superpairs = Optional.empty();

        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Long> simon = Concurrent.newMap();
        private transient @NotNull Optional<Table> chronomatron = Optional.empty();

        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Long> numbers = Concurrent.newMap();
        private transient @NotNull Optional<Table> ultrasequencer = Optional.empty();

        @Override
        public void postInit() {
            this.superpairs = Optional.of(new Table(this.pairings));
            this.chronomatron = Optional.of(new Table(this.simon));
            this.ultrasequencer = Optional.of(new Table(this.numbers));
        }

        @Getter
        public static class Table {

            private final @NotNull SkyBlockDate.RealTime lastAttempt;
            private final @NotNull SkyBlockDate.RealTime lastClaimed;
            private final int bonusClicks;
            private final @NotNull ConcurrentMap<Integer, Integer> attempts;
            private final @NotNull ConcurrentMap<Integer, Integer> claims;
            private final @NotNull ConcurrentMap<Integer, Integer> bestScore;

            private Table(@NotNull ConcurrentMap<String, Long> tableData) {
                this.lastAttempt = new SkyBlockDate.RealTime(tableData.removeOrGet("last_attempt", 0L));
                this.lastClaimed = new SkyBlockDate.RealTime(tableData.removeOrGet("last_claimed", 0L));
                this.bonusClicks = tableData.removeOrGet("bonus_clicks", 0L).intValue();

                ConcurrentMap<String, ConcurrentMap<Integer, Integer>> filteredData = Concurrent.newMap();

                tableData.forEach((key, value) -> {
                    if (!filteredData.containsKey(key))
                        filteredData.put(key, Concurrent.newMap());

                    String actual = key.substring(0, key.lastIndexOf("_"));
                    filteredData.get(key).put(Integer.parseInt(key.replace(String.format("%s_", actual), "")), value.intValue());
                });

                this.attempts = filteredData.removeOrGet("attempts", Concurrent.newMap());
                this.claims = filteredData.removeOrGet("claims", Concurrent.newMap());
                this.bestScore = filteredData.removeOrGet("best_score", Concurrent.newMap());
            }

        }

    }
    
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FairySouls {

        @SerializedName("total_collected")
        private int totalCollected;
        @SerializedName("fairy_exchanges")
        private int exchanges;
        @SerializedName("unspent_souls")
        private int unspent;

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Inventory {

        @SerializedName("inv_armor")
        private NbtContent armor = new NbtContent();
        @SerializedName("equipment_contents")
        private NbtContent equipment = new NbtContent();
        @SerializedName("wardrobe_contents")
        private NbtContent wardrobe = new NbtContent();
        @SerializedName("bag_contents")
        private BagContents bags = new BagContents();
        @SerializedName("inv_contents")
        private NbtContent content = new NbtContent();
        @SerializedName("wardrobe_equipped_slot")
        private int equippedWardrobeSlot;
        @SerializedName("backpack_icons")
        private ConcurrentMap<Integer, NbtContent> backpackIcons = Concurrent.newMap();
        @SerializedName("personal_vault_contents")
        private NbtContent personalVault = new NbtContent();
        @SerializedName("sacks_counts")
        private ConcurrentLinkedMap<String, Integer> sacks = Concurrent.newLinkedMap();
        @SerializedName("backpack_contents")
        private ConcurrentMap<Integer, NbtContent> backpacks = Concurrent.newMap();
        @SerializedName("ender_chest_contents")
        private NbtContent enderChest = new NbtContent();

    }
    
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ItemSettings {

        @SerializedName("teleporter_pill_consumed")
        private boolean teleporterPillConsumed;
        @SerializedName("favorite_arrow")
        private String favoriteArrow;
        private int soulflow;

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Leveling {

        private int experience;
        private @NotNull ConcurrentMap<String, Integer> completions = Concurrent.newMap();
        private @NotNull ConcurrentList<String> completed = Concurrent.newList();
        @Accessors(fluent = true)
        @SerializedName("migrated_completions")
        private boolean hasMigratedCompletions;
        @SerializedName("category_expanded")
        private boolean categoryExpanded;
        @SerializedName("last_viewed_tasks")
        private @NotNull ConcurrentList<String> lastViewedTasks = Concurrent.newList();
        @SerializedName("completed_tasks")
        private @NotNull ConcurrentList<String> completedTasks = Concurrent.newList();
        @SerializedName("highest_pet_score")
        private int highestPetScore;
        @SerializedName("mining_fiesta_ores_mined")
        private int miningFiestaOresMined;
        private boolean migrated;
        @Accessors(fluent = true)
        @SerializedName("migrated_completions_2")
        private boolean hasMigratedCompletions2;
        @Accessors(fluent = true)
        @SerializedName("claimed_talisman")
        private boolean hasClaimedTalisman;
        @SerializedName("bop_bonus")
        private String bopBonus;
        @SerializedName("selected_symbol")
        private String selectedSymbol;
        @SerializedName("emblem_unlocks")
        private @NotNull ConcurrentList<String> emblemUnlocks = Concurrent.newList();
        @SerializedName("fishing_festival_sharks_killed")
        private int fishingFestivalSharksKilled;

        public double getLevel() {
            return Math.floor(this.getExperience() / 100.0);
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Quests implements PostInit {

        @Getter(AccessLevel.NONE)
        @SerializedName("harp_quest")
        private @NotNull ConcurrentMap<String, Object> melodyHarpMap = Concurrent.newMap();
        private transient MelodyHarp melodyHarp;
        @SerializedName("trapper_quest")
        private @NotNull Trapper trapper = new Trapper();

        @Override
        public void postInit() {
            this.melodyHarp = new MelodyHarp(this.melodyHarpMap);
        }

        @Getter
        public static class MelodyHarp {

            private final boolean talismanClaimed;
            private final @NotNull Optional<String> selectedSong;
            private final @NotNull SkyBlockDate.RealTime selectedSongTimestamp;
            private final @NotNull ConcurrentMap<String, MelodyHarp.Song> songs;

            MelodyHarp(@NotNull ConcurrentMap<String, Object> harpQuest) {
                this.talismanClaimed = (boolean) harpQuest.removeOrGet("claimed_talisman", false);
                this.selectedSong = harpQuest.getOptional("selected_song").map(String::valueOf);
                long epoch = NumberUtil.createNumber(String.valueOf(harpQuest.removeOrGet("selected_song_epoch", 0))).longValue();
                this.selectedSongTimestamp = new SkyBlockDate.RealTime(epoch * 1000);

                ConcurrentLinkedMap<String, ConcurrentMap<String, Integer>> songMap = Concurrent.newLinkedMap();
                harpQuest.stream()
                    .filterValue(Number.class::isInstance)
                    .forEach((key, value) -> {
                        String songKey = key.replace("song_", "");
                        String songName = songKey.replaceAll("_((best|perfect)_)?completions?", "");
                        String category = songKey.replace(String.format("%s_", songName), "");

                        if (!songMap.containsKey(songName))
                            songMap.put(songName, Concurrent.newMap());

                        songMap.get(songName).put(category, NumberUtil.createNumber(value.toString()).intValue());
                    });

                this.songs = songMap.stream()
                    .map((key, value) -> Pair.of(
                        key,
                        new MelodyHarp.Song(
                            value.getOrDefault("best_completion", 0),
                            value.getOrDefault("completions", 0),
                            value.getOrDefault("perfect_completions", 0)
                        )
                    ))
                    .collect(Concurrent.toUnmodifiableMap());
            }

            @Getter
            @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Song {

                private final int bestCompletion;
                private final int completions;
                private final int perfectCompletions;

            }

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Trapper {

            @SerializedName("last_task_time")
            private SkyBlockDate.RealTime lastTask;
            @SerializedName("pelt_count")
            private int peltCount;

        }

    }
    
}
