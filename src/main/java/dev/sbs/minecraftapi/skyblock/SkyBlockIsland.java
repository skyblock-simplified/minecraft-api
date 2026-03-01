package dev.sbs.minecraftapi.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.sbs.minecraftapi.skyblock.profile_stats.ProfileStats;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class SkyBlockIsland {

    private static final DecimalFormat smallDecimalFormat = new DecimalFormat("#0.#");

    @SerializedName("profile_id")
    private @NotNull UUID islandId;
    @SerializedName("community_upgrades")
    private @NotNull Optional<CommunityUpgrades> communityUpgrades = Optional.empty();
    private @NotNull Optional<Banking> banking = Optional.empty();
    @SerializedName("game_mode")
    private @NotNull GameMode gameMode = GameMode.CLASSIC;
    @SerializedName("cute_name")
    private @NotNull Profile profile;
    private boolean selected;
    private @NotNull ConcurrentLinkedMap<UUID, SkyBlockMember> members = Concurrent.newLinkedMap();

    public boolean hasMember(@NotNull UUID uniqueId) {
        return this.getMembers().containsKey(uniqueId);
    }

    public @NotNull ConcurrentLinkedMap<String, Long> getCollection() {
        return this.getMembers()
            .stream()
            .flatMap((uniqueId, member) -> member.getCollection().stream())
            .collect(Concurrent.toLinkedMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Long::sum
            ));
    }

    public @NotNull ConcurrentLinkedMap<String, Integer> getCollectionUnlocked() {
        return this.getMembers()
            .stream()
            .flatMap((uniqueId, member) -> member.getCollectionUnlocked().stream())
            .collect(Concurrent.toLinkedMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Integer::sum
            ));
    }

    public @NotNull ConcurrentList<Integer> getCraftedMinions(@NotNull String itemId) {
        return this.getMembers()
            .stream()
            .values()
            .flatMap(member -> member.getCraftedMinions(itemId).stream())
            .distinct()
            .collect(Concurrent.toUnmodifiableList())
            .sorted(Comparator.naturalOrder());
    }

    public @NotNull ProfileStats getProfileStats(@NotNull SkyBlockMember member) {
        return this.getProfileStats(member, true);
    }

    public @NotNull ProfileStats getProfileStats(@NotNull SkyBlockMember member, boolean calculateBonus) {
        return new ProfileStats(this, member, calculateBonus);
    }

    public int getUniqueMinions() {
        return this.getMembers()
            .stream()
            .values()
            .mapToInt(member -> member.getPlayerData().getCraftedMinions().size())
            .sum() + this.getCommunityUpgrades()
            .map(communityUpgrades -> communityUpgrades.getUpgraded()
                .stream()
                .filter(upgraded -> upgraded.getUpgrade() == CommunityUpgrades.Type.MINION_SLOTS)
                .count()
            )
            .map(Long::intValue)
            .orElse(0);
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Banking {

        private double balance;
        private @NotNull ConcurrentList<Transaction> transactions = Concurrent.newList();

        @Getter
        public static class Transaction {

            private double amount;
            private SkyBlockDate.RealTime timestamp;
            private Action action;
            @Getter(AccessLevel.NONE)
            @SerializedName("initiator_name")
            private String initiatorName;

            public String getInitiatorName() {
                return this.initiatorName.replace("Â", ""); // API Artifact
            }

            public enum Action {

                WITHDRAW,
                DEPOSIT

            }

        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CommunityUpgrades {

        @SerializedName("currently_upgrading")
        private @NotNull Optional<Upgrading> currentlyUpgrading = Optional.empty();
        @SerializedName("upgrade_states")
        private @NotNull ConcurrentList<Upgraded> upgraded = Concurrent.newList();

        public int getHighestTier(@NotNull Type type) {
            return this.getUpgraded()
                .stream()
                .filter(upgraded -> upgraded.getUpgrade().name().equalsIgnoreCase(type.name()))
                .sorted((o1, o2) -> Comparator.comparing(Upgraded::getTier).compare(o2, o1))
                .map(Upgraded::getTier)
                .findFirst()
                .orElse(0);
        }

        public @NotNull ConcurrentList<Upgraded> getUpgrades(@NotNull Type type) {
            return this.getUpgraded()
                .stream()
                .filter(upgraded -> upgraded.getUpgrade().name().equalsIgnoreCase(type.name()))
                .sorted((o1, o2) -> Comparator.comparing(Upgraded::getTier).compare(o1, o2))
                .collect(Concurrent.toList());
        }

        @Getter
        public static class Upgraded extends Upgrading {

            @SerializedName("claimed_ms")
            private SkyBlockDate.RealTime claimed;
            @SerializedName("claimed_by")
            private String claimedBy;
            @SerializedName("fasttracked")
            private boolean fastTracked;

        }

        @Getter
        public static class Upgrading {

            private Type upgrade;
            @SerializedName(alternate = "new_tier", value = "tier")
            private int tier;
            @SerializedName("start_ms")
            private SkyBlockDate.RealTime started;
            @SerializedName(alternate = "who_started", value = "started_by")
            private String startedBy;

        }

        @Getter
        @RequiredArgsConstructor
        public enum Type {

            @SerializedName("minion_slots")
            MINION_SLOTS(5),
            @SerializedName("coins_allowance")
            COINS_ALLOWANCE(5),
            @SerializedName("guests_count")
            GUESTS_COUNT(5),
            @SerializedName("island_size")
            ISLAND_SIZE(10),
            @SerializedName("coop_slots")
            COOP_SLOTS(3);

            private final int maxLevel;

            public @NotNull String getName() {
                return StringUtil.capitalizeFully(this.name().replace("_", " "));
            }

        }

    }

}
