package dev.sbs.minecraftapi.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.mutable.MutableDouble;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.api.stream.pair.PairOptional;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.api.util.Range;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.skyblock.data.PetData;
import dev.sbs.minecraftapi.skyblock.data.PlayerData;
import dev.sbs.minecraftapi.skyblock.data.SkillData;
import dev.sbs.minecraftapi.skyblock.data.SlayerData;
import dev.sbs.minecraftapi.skyblock.data.StatData;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.sbs.minecraftapi.skyblock.dungeon.DungeonProfile;
import dev.sbs.minecraftapi.skyblock.mining.ForgeItem;
import dev.sbs.minecraftapi.skyblock.mining.GlaciteTunnels;
import dev.sbs.minecraftapi.skyblock.mining.Mining;
import dev.sbs.minecraftapi.skyblock.model.TrophyFish;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
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
    private @NotNull SlayerData slayerData = new SlayerData();
    @SerializedName("pet_data")
    private @NotNull PetData petData = new PetData();
    @SerializedName("dungeons")
    private @NotNull DungeonProfile dungeonData = new DungeonProfile();
    @SerializedName("player_stats")
    private @NotNull StatData statData = new StatData();
    private transient SkillData skillData;

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
        this.skillData = new SkillData(this.getPlayerData().getSkillExperience(), this);

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
    public static class AccessoryBag {

        private @NotNull Tuning tuning = new Tuning();
        @SerializedName("selected_power")
        private @NotNull Optional<String> selectedPower = Optional.empty();
        @SerializedName("bag_upgrades_purchased")
        private int bagUpgradesPurchased;
        @SerializedName("unlocked_powers")
        private @NotNull ConcurrentList<String> unlockedPowers = Concurrent.newList();
        @SerializedName("highest_magical_power")
        private int highestMagicalPower;
        private transient NbtContent contents = new NbtContent();
        @Accessors(fluent = true)
        private transient boolean hasConsumedPrism;
        private transient int abiphoneContacts;

        private transient int magicalPower;
        private transient int tuningPoints;
        private transient double magicalPowerMultiplier;

        protected void initialize(@NotNull SkyBlockMember member) {
            this.contents = member.getInventory().getBags().getAccessories();
            this.hasConsumedPrism = member.getRift().getAccess().hasConsumedPrism();
            this.abiphoneContacts = member.getCrimsonIsle().getAbiphone().getContacts().size();
        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Tuning implements PostInit {

            @SerializedName("highest_unlocked_slot")
            private int highestUnlockedSlot;
            @SerializedName("refund_1")
            @Accessors(fluent = true)
            private boolean hasClaimedRefund;

            // Slots
            @SerializedName("slot_0")
            private @NotNull ConcurrentMap<String, Integer> current = Concurrent.newMap();
            @Getter(AccessLevel.NONE)
            private @NotNull ConcurrentMap<String, Integer> slot_1 = Concurrent.newMap();
            @Getter(AccessLevel.NONE)
            private @NotNull ConcurrentMap<String, Integer> slot_2 = Concurrent.newMap();
            @Getter(AccessLevel.NONE)
            private @NotNull ConcurrentMap<String, Integer> slot_3 = Concurrent.newMap();
            @Getter(AccessLevel.NONE)
            private @NotNull ConcurrentMap<String, Integer> slot_4 = Concurrent.newMap();

            // PostInit
            private @NotNull ConcurrentList<ConcurrentMap<String, Integer>> slots = Concurrent.newList();

            @Override
            public void postInit() {
                this.slots = Concurrent.newUnmodifiableList(
                    this.slot_1,
                    this.slot_2,
                    this.slot_3,
                    this.slot_4
                );
            }

        }

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
    public static class CrimsonIsle implements PostInit {

        private @NotNull Abiphone abiphone = new Abiphone();
        private @NotNull Matriarch matriarch = new Matriarch();
        @SerializedName("last_minibosses_killed")
        private @NotNull ConcurrentList<String> lastMinibossesKilled = Concurrent.newList();

        // Factions
        @SerializedName("selected_faction")
        private @NotNull Faction selectedFaction = Faction.NONE;
        @SerializedName("mages_reputation")
        private int mageReputation;
        @SerializedName("barbarians_reputation")
        private int barbarianReputation;

        // Kuudra
        private transient @NotNull Kuudra kuudra = new Kuudra();
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Integer> kuudra_completed_tiers = Concurrent.newMap();
        @Getter(AccessLevel.NONE)
        @SerializedPath("kuudra_party_finder.search_settings")
        private Kuudra.SearchSettings kuudra_search_settings = new Kuudra.SearchSettings();
        @Getter(AccessLevel.NONE)
        @SerializedPath("kuudra_party_finder.group_builder")
        private Kuudra.GroupBuilder kuudra_group_builder = new Kuudra.GroupBuilder();

        // Dojo
        private transient @NotNull Dojo dojo = new Dojo();
        @Getter(AccessLevel.NONE)
        @SerializedName("dojo")
        private @NotNull ConcurrentMap<String, Integer> dojoMap = Concurrent.newMap();

        @Override
        public void postInit() {
            this.dojo = new Dojo(this.dojoMap);
            this.kuudra = new Kuudra(
                this.kuudra_completed_tiers,
                this.kuudra_search_settings,
                this.kuudra_group_builder
            );
        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Abiphone {

            @SerializedName("contact_data")
            private @NotNull ConcurrentMap<String, Abiphone.Contact> contacts = Concurrent.newMap();
            private @NotNull ConcurrentMap<String, Integer> games = Concurrent.newMap();
            @SerializedName("active_contacts")
            private @NotNull ConcurrentList<String> collectedContacts = Concurrent.newList();

            @SerializedPath("operator_chip.repaired_index")
            private int repairedOperatorRelays;
            @SerializedName("trio_contact_addons")
            private int trioContactAddons;
            @SerializedName("selected_ringtone")
            private String selectedRingtone;

            @Getter
            @NoArgsConstructor(access = AccessLevel.NONE)
            public static class Contact {

                @SerializedName("talked_to")
                private boolean talkedTo;
                @SerializedName("completed_quest")
                private boolean questCompleted;
                @SerializedName("dnd_enabled")
                private boolean doNotDisturb;
                private @NotNull ConcurrentMap<String, Object> specific = Concurrent.newMap();

                // Calls
                @SerializedName("incoming_calls_count")
                private int incomingCalls;
                @SerializedName("last_call")
                private @NotNull Optional<SkyBlockDate.RealTime> lastOutgoingCall = Optional.empty();
                @SerializedName("last_call_incoming")
                private @NotNull Optional<SkyBlockDate.RealTime> lastIncomingCall = Optional.empty();

            }

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Matriarch {

            @SerializedName("pearls_collected")
            private int lastCollectedPearls;
            @SerializedName("last_attempt")
            private SkyBlockDate.RealTime lastAttempt;
            @SerializedName("recent_refreshes")
            private ConcurrentList<SkyBlockDate.RealTime> recentRefreshes = Concurrent.newList();

        }

        @Getter
        public static class Dojo {

            private final @NotNull ConcurrentMap<Dojo.Type, Integer> points;

            private Dojo() {
                this(Concurrent.newMap());
            }

            private Dojo(@NotNull ConcurrentMap<String, Integer> dojo) {
                this.points = Concurrent.newUnmodifiableMap(
                    dojo.stream()
                        .filter(entry -> !entry.getKey().contains("time_"))
                        .map(entry -> Pair.of(Dojo.Type.of(entry.getKey().replace("dojo_points_", "")), entry.getValue()))
                        .collect(Concurrent.toMap())
                );
            }

            public int getPoints(@NotNull Dojo.Type type) {
                return this.getPoints().getOrDefault(type, 0);
            }

            @Getter
            @RequiredArgsConstructor
            public enum Type {

                UNKNOWN(""),
                FORCE("mob_kb"),
                STAMINA("wall_jump"),
                MASTERY("archer"),
                DISCIPLINE("sword_swap"),
                SWIFTNESS("snake"),
                CONTROL("fireball"),
                TENACITY("lock_head");

                private final @NotNull String internalName;

                public static @NotNull Dojo.Type of(@NotNull String name) {
                    return Arrays.stream(values())
                        .filter(type -> type.name().equalsIgnoreCase(name) || type.getInternalName().equalsIgnoreCase(name))
                        .findFirst()
                        .orElse(UNKNOWN);
                }

            }

        }

        @Getter
        public static class Kuudra {

            private final @NotNull ConcurrentMap<Kuudra.Tier, Integer> completedTiers;
            private final @NotNull ConcurrentMap<Kuudra.Tier, Integer> highestWave;
            private final @NotNull Kuudra.SearchSettings searchSettings;
            private final @NotNull Kuudra.GroupBuilder groupBuilder;

            private Kuudra() {
                this(Concurrent.newMap(), null, null);
            }

            private Kuudra(@NotNull ConcurrentMap<String, Integer> kuudraCompletedTiers, @Nullable Kuudra.SearchSettings kuudraSearchSettings, @Nullable Kuudra.GroupBuilder kuudraGroupBuilder) {
                this.searchSettings = (kuudraSearchSettings != null ? kuudraSearchSettings : new Kuudra.SearchSettings());
                this.groupBuilder = (kuudraGroupBuilder != null ? kuudraGroupBuilder : new Kuudra.GroupBuilder());

                this.completedTiers = kuudraCompletedTiers.stream()
                    .filter(entry -> !entry.getKey().startsWith("highest_"))
                    .map(entry -> Pair.of(Kuudra.Tier.of(entry.getKey()), entry.getValue()))
                    .collect(Concurrent.toUnmodifiableMap());

                this.highestWave = Concurrent.newUnmodifiableMap(
                    kuudraCompletedTiers.stream()
                        .filter(entry -> entry.getKey().startsWith("highest_"))
                        .map(entry -> Pair.of(Kuudra.Tier.of(entry.getKey()), entry.getValue()))
                        .collect(Concurrent.toUnmodifiableMap())
                );
            }

            @Getter
            @RequiredArgsConstructor
            public enum Tier {

                UNKNOWN,
                BASIC("NONE"),
                HOT,
                BURNING,
                FIERY,
                INFERNAL;

                private final @NotNull String internalName;

                Tier() {
                    this.internalName = name();
                }

                public @NotNull String getName() {
                    return StringUtil.capitalizeFully(this.name());
                }

                public static @NotNull Kuudra.Tier of(@NotNull String name) {
                    return Arrays.stream(values())
                        .filter(tier -> tier.name().equalsIgnoreCase(name) || tier.getInternalName().equalsIgnoreCase(name))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No tier with name " + name));
                }

            }

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class SearchSettings {

                private @NotNull Kuudra.Tier tier = Kuudra.Tier.BASIC;
                private @NotNull Optional<String> search = Optional.empty();
                private @NotNull Kuudra.SearchSettings.Sort sort = Kuudra.SearchSettings.Sort.RECENTLY_CREATED;
                @Getter(AccessLevel.NONE)
                private @NotNull Optional<String> combat_level = Optional.empty();

                public @NotNull Range<Integer> getCombatLevel() {
                    return this.combat_level.map(range -> StringUtil.split(range, "-"))
                        .map(parts -> Range.between(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])))
                        .orElse(Range.between(0, 60));
                }

                public enum Sort {

                    RECENTLY_CREATED,
                    HIGHEST_COMBAT_LEVEL,
                    LARGEST_GROUP_SIZE

                }

            }

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class GroupBuilder {

                private Kuudra.Tier tier = Kuudra.Tier.BASIC;
                private Optional<String> note = Optional.empty();
                @SerializedName("combat_level_required")
                private int requiredCombatLevel;

            }

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Quests {

            // TODO

        }

        public enum Faction {

            NONE,
            @SerializedName("mages")
            MAGE,
            @SerializedName("barbarians")
            BARBARIAN

        }

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
    public static class JacobsContest {

        @SerializedName("medals_inv")
        private @NotNull ConcurrentMap<Medal, Integer> medals = Concurrent.newMap();
        @SerializedPath("perks.double_drops")
        private int doubleDrops;
        @SerializedPath("perks.farming_level_cap")
        private int farmingLevelCap;
        @Accessors(fluent = true)
        @SerializedName("talked")
        private boolean hasTalked;
        @Getter(AccessLevel.NONE)
        @SerializedName("contests")
        private @NotNull ConcurrentMap<String, Contest> contestMap = Concurrent.newMap();
        private @NotNull ConcurrentList<Contest> contestList = Concurrent.newList();
        @SerializedName("unique_brackets")
        private @NotNull ConcurrentMap<Medal, ConcurrentList<String>> uniqueBrackets = Concurrent.newMap();
        private boolean migration;
        @SerializedName("personal_bests")
        private @NotNull ConcurrentMap<String, Integer> personalBests = Concurrent.newMap();

        public @NotNull ConcurrentList<Contest> getContests() {
            if (this.contestList.isEmpty()) {
                this.contestList = this.contestMap.stream()
                    .map(entry -> {
                        Contest contest = entry.getValue();

                        String[] dataString = entry.getKey().split(":");
                        String[] calendarString = dataString[1].split("_");
                        int year = NumberUtil.toInt(dataString[0]);
                        int month = NumberUtil.toInt(calendarString[0]);
                        int day = NumberUtil.toInt(calendarString[1]);
                        String collectionName = StringUtil.join(dataString, ":", 2, dataString.length);

                        contest.skyBlockDate = new SkyBlockDate(year, month, day);
                        contest.collectionName = collectionName;
                        return contest;
                    })
                    .collect(Concurrent.toUnmodifiableList());
            }

            return this.contestList;
        }

        public enum Medal {

            BRONZE,
            SILVER,
            GOLD,
            PLATINUM,
            DIAMOND

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Contest {

            private int collected;
            @Accessors(fluent = true)
            @SerializedName("claimed_rewards")
            private boolean hasClaimedRewards;
            @SerializedName("claimed_position")
            private int position;
            @SerializedName("claimed_participants")
            private int participants;
            private SkyBlockDate skyBlockDate;
            private String collectionName;

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Data {

                private SkyBlockDate skyBlockDate;
                private String collectionName;

            }

        }

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

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Rift {

        private Access access = new Access();
        @SerializedName("slayer_quest")
        private SlayerQuest slayerQuest = new SlayerQuest();

        // Locations
        @SerializedName("wizard_tower")
        private WizardTower wizardTower = new WizardTower();
        @SerializedName("wyld_woods")
        private WyldWoods wyldWoods = new WyldWoods();
        @SerializedName("black_lagoon")
        private BlackLagoon blackLagoon = new BlackLagoon();
        @SerializedName("west_village")
        private WestVillage westVillage = new WestVillage();
        private Dreadfarm dreadfarm = new Dreadfarm();
        @SerializedName("village_plaza")
        private VillagePlaza villagePlaza = new VillagePlaza();
        @SerializedName("castle")
        private StillgoreChateau stillgoreChateau = new StillgoreChateau();

        // Special Locations
        @SerializedName("enigma")
        private EnigmasCrib enigmasCrib = new EnigmasCrib();
        @SerializedName("wither_cage")
        private Porhtal porhtal = new Porhtal();
        @SerializedName("dead_cats")
        private DeadCats deadCats = new DeadCats();
        @SerializedName("gallery")
        private TimecharmGallery timecharmGallery = new TimecharmGallery();
        @SerializedName("lifetime_purchased_boundaries")
        private @NotNull ConcurrentList<String> purchasedBoundaries = Concurrent.newList();

        // Inventories
        @SerializedPath("inventory.inv_contents")
        private NbtContent inventory = new NbtContent();
        @SerializedPath("inventory.inv_armor")
        private NbtContent armor = new NbtContent();
        @SerializedPath("inventory.ender_chest_contents")
        private NbtContent enderChest = new NbtContent();
        @SerializedPath("inventory.equipment_contents")
        private NbtContent equipment = new NbtContent();

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Access {

            @SerializedName("last_free")
            private SkyBlockDate.RealTime lastFree;
            @SerializedName("charge_track_timestamp")
            private SkyBlockDate.RealTime chargeTrack;
            @Accessors(fluent = true)
            @SerializedName("consumed_prism")
            private boolean hasConsumedPrism;
            private Access.Pass pass = new Access.Pass();

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Pass {

                @SerializedName("issued_at")
                private SkyBlockDate.RealTime issuedAt;
                @SerializedName("rift_server_joins")
                private int serverJoins;
                @Accessors(fluent = true)
                @SerializedName("used_prism")
                private boolean hasUsedPrism;

            }

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class SlayerQuest extends SlayerData.Quest {

            @SerializedName("combat_xp")
            private int combatXP;
            @SerializedName("recent_mob_kills")
            private @NotNull ConcurrentList<SlayerQuest.MobKill> recentMobKills = Concurrent.newList();
            @SerializedName("last_killed_mob_island")
            private String lastKilledMobIsland;

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class MobKill {

                private int xp;
                private SkyBlockDate.RealTime timestamp;

            }

        }

        // Locations

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class WizardTower {

            @SerializedName("wizard_quest_step")
            private int wizardQuestStep;
            @SerializedName("crumbs_laid_out")
            private int crumbsLaidOut;

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class WyldWoods {

            @SerializedName("talked_threebrothers")
            private @NotNull ConcurrentList<String> talkedThreebrothers = Concurrent.newList();
            @SerializedName("bughunter_step")
            private int bughunterStep;
            @Accessors(fluent = true)
            @SerializedName("sirius_started_q_a")
            private boolean hasStartedSiriusQA;
            @SerializedName("sirius_q_a_chain_done")
            private boolean siriusQAChainDone;
            @Accessors(fluent = true)
            @SerializedName("sirius_completed_q_a")
            private boolean hasCompletedSiriusQA;
            @Accessors(fluent = true)
            @SerializedName("sirius_claimed_doubloon")
            private boolean hasClaimedSiriusDoubloon;

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class BlackLagoon {

            @Accessors(fluent = true)
            @SerializedName("talked_to_edwin")
            private boolean hasTalkedToEdwin;
            @Accessors(fluent = true)
            @SerializedName("received_science_paper")
            private boolean hasReceivedSciencePaper;
            @Accessors(fluent = true)
            @SerializedName("delivered_science_paper")
            private boolean hasDeliveredSciencePaper;
            @SerializedName("completed_step")
            private int completedStep;

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class WestVillage {

            @SerializedName("crazy_kloon")
            private WestVillage.CrazyKloon crazyKloon = new WestVillage.CrazyKloon();
            private WestVillage.Mirrorverse mirrorverse = new WestVillage.Mirrorverse();
            @SerializedName("kat_house")
            private WestVillage.KatHouse katHouse = new WestVillage.KatHouse();
            private WestVillage.Glyphs glyphs = new WestVillage.Glyphs();

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class CrazyKloon {

                @SerializedName("selected_colors")
                private @NotNull ConcurrentMap<String, String> selectedColors = Concurrent.newMap();
                @Accessors(fluent = true)
                @SerializedName("talked")
                private boolean hasTalked;
                @SerializedName("hacked_terminals")
                private @NotNull ConcurrentList<String> hackedTerminals = Concurrent.newList();
                @SerializedName("quest_complete")
                private boolean questComplete;

            }

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Mirrorverse {

                @SerializedName("visited_rooms")
                private @NotNull ConcurrentList<String> visitedRooms = Concurrent.newList();
                @SerializedName("upside_down_hard")
                private boolean upsideDownHardCompleted;
                @SerializedName("claimed_chest_items")
                private @NotNull ConcurrentList<String> claimedChestItems = Concurrent.newList();
                @SerializedName("claimed_reward")
                private boolean claimedReward;

            }

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class KatHouse {

                @SerializedName("bin_collected_mosquito")
                private int collectedMosquito;
                @SerializedName("bin_collected_spider")
                private int collectedSpider;
                @SerializedName("bin_collected_silverfish")
                private int collectedSilverfish;

            }

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Glyphs {

                @SerializedName("claimed_wand")
                private boolean claimedWand;
                @SerializedName("current_glyph_delivered")
                private boolean currentGlyphDelivered;
                @SerializedName("current_glyph_completed")
                private boolean currentGlyphCompleted;
                @SerializedName("current_glyph")
                private int currentGlyph;
                private boolean completed;
                @SerializedName("claimed_bracelet")
                private boolean claimedBracelet;

            }

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Dreadfarm {

            @SerializedName("shania_stage")
            private int shaniaStage;
            @SerializedName("caducous_feeder_uses")
            private @NotNull ConcurrentList<Instant> caducousFeederUses = Concurrent.newList();

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class VillagePlaza {

            @Accessors(fluent = true)
            @SerializedName("got_scammed")
            private boolean hasGotScammed;
            private VillagePlaza.Murder murder = new VillagePlaza.Murder();
            @SerializedName("barry_center")
            private VillagePlaza.BarryCenter barryCenter = new VillagePlaza.BarryCenter();
            private VillagePlaza.Cowboy cowboy = new VillagePlaza.Cowboy();
            private VillagePlaza.Lonely lonely = new VillagePlaza.Lonely();
            private VillagePlaza.Seraphine seraphine = new VillagePlaza.Seraphine();

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Murder {

                @SerializedName("step_index")
                private int stepIndex;
                @SerializedName("room_clues")
                private @NotNull ConcurrentList<String> roomClues = Concurrent.newList();

            }

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class BarryCenter {

                @SerializedName("first_talk_to_barry")
                private boolean firstTalkToBarry;
                @SerializedName("received_reward")
                private boolean receivedReward;
                private @NotNull ConcurrentList<String> convinced = Concurrent.newList();

            }

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Cowboy {

                private int stage;
                @SerializedName("hay_eaten")
                private int hayEaten;
                @SerializedName("rabbit_name")
                private String rabbitName;
                @SerializedName("exported_carrots")
                private int exportedCarrots;

            }

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Lonely {

                @SerializedName("seconds_sitting")
                private int secondsSitting;

            }

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Seraphine {

                @SerializedName("step_index")
                private int stepIndex;

            }

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class StillgoreChateau {

            @Accessors(fluent = true)
            @SerializedName("unlocked_pathway_skip")
            private boolean hasUnlockedPathwaySkip;
            @SerializedName("fairy_step")
            private int fairyStep;
            @SerializedName("grubber_stacks")
            private int grubberStacks;

        }

        // Special Locations

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class EnigmasCrib {

            @Accessors(fluent = true)
            @SerializedName("bought_cloak")
            private boolean hasBoughtCloak;
            @SerializedName("found_souls")
            private @NotNull ConcurrentList<String> foundSouls = Concurrent.newList();
            @SerializedName("claimed_bonus_index")
            private int claimedBonusIndex;

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Porhtal {

            @SerializedName("killed_eyes")
            private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class DeadCats {

            @Accessors(fluent = true)
            @SerializedName("talked_to_jacquelle")
            private boolean hasTalkedToJacquelle;
            @Accessors(fluent = true)
            @SerializedName("picked_up_detector")
            private boolean hasPickedUpDetector;
            @SerializedName("found_cats")
            private @NotNull ConcurrentList<String> foundCats = Concurrent.newList();
            @Accessors(fluent = true)
            @SerializedName("unlocked_pet")
            private boolean hasUnlockedPet;
            private Optional<PetData.Entry> montezuma = Optional.empty();

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class TimecharmGallery {

            @SerializedName("elise_step")
            private int eliseStep;
            @SerializedName("secured_trophies")
            private @NotNull ConcurrentList<TimecharmGallery.Trophy> securedTrophies = Concurrent.newList();
            @SerializedName("sent_trophy_dialogues")
            private @NotNull ConcurrentList<String> sentTrophyDialogues = Concurrent.newList();

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Trophy {

                private String type;
                private Instant timestamp;
                private int visits;

            }

        }

    }

    @Getter
    public static class TrophyFishing {

        private final @NotNull ConcurrentMap<String, ConcurrentMap<TrophyFish.Tier, Integer>> fish;
        private final int totalCaught;
        private final @NotNull PairOptional<String, TrophyFish.Tier> lastCaught;

        public TrophyFishing(@NotNull ConcurrentMap<String, Object> trophy_fish) {
            this.totalCaught = (int) trophy_fish.removeOrGet("total_caught", 0);

            this.lastCaught = PairOptional.of(
                trophy_fish.getOptional("last_caught")
                    .map(String::valueOf)
                    .map(value -> value.split("/"))
                    .map(parts -> Pair.of(parts[0], TrophyFish.Tier.valueOf(parts[1])))
            );

            this.fish = MinecraftApi.getRepositoryOf(TrophyFish.class)
                .stream()
                .map(type -> Pair.of(
                    type.getId(),
                    trophy_fish.stream()
                        .filter(entry -> entry.getKey().startsWith(type.getId().toLowerCase()))
                        .map(entry -> Pair.of(
                            TrophyFish.Tier.valueOf(entry.getKey().replace(type.getId(), "")),
                            (int) entry.getValue()
                        ))
                        .collect(Concurrent.toMap())
                ))
                .collect(Concurrent.toMap());
        }

    }
    
}
