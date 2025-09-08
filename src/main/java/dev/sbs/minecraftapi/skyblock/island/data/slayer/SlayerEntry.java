package dev.sbs.minecraftapi.skyblock.island.data.slayer;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.skyblock.data.Slayer;
import dev.sbs.minecraftapi.skyblock.type.Experience;
import dev.sbs.minecraftapi.skyblock.type.Weight;
import dev.sbs.minecraftapi.skyblock.type.Weighted;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Getter
public class SlayerEntry implements Experience, Weighted {

    private final @NotNull String id;
    private final double experience;
    private final @NotNull ConcurrentMap<Integer, Boolean> claimed;
    private final @NotNull ConcurrentMap<Integer, Boolean> claimedSpecial;
    private final @NotNull ConcurrentMap<Integer, Integer> kills;
    private final @NotNull ConcurrentMap<Integer, Integer> attempts;

    SlayerEntry(@NotNull String id, @NotNull BossData bossData) {
        this.id = id;
        this.experience = Math.max(bossData.getExperience(), 0);
        this.claimed = bossData.getClaimed();
        this.claimedSpecial = bossData.getClaimedSpecial();
        this.kills = bossData.getKills();
        this.attempts = bossData.getAttempts();
    }

    public @NotNull Slayer getSlayer() {
        return MinecraftApi.getRepositoryOf(Slayer.class).findFirstOrNull(Slayer::getId, this.getId());
    }

    @Override
    public @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return this.getSlayer().getExperienceTiers();
    }

    @Override
    public int getMaxLevel() {
        return this.getSlayer().getMaxLevel();
    }

    @Override
    public @NotNull Weight getWeight() {
        if (this.getSlayer().getWeightDivider() == 0.0)
            return Weight.of(0, 0);

        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();
        double maxSlayerExperienceRequired = experienceTiers.get(experienceTiers.size() - 1);
        double base = Math.min(this.getExperience(), maxSlayerExperienceRequired) / this.getSlayer().getWeightDivider();
        double weightValue = NumberUtil.round(base, 2);
        double weightOverflow = 0;

        if (this.getExperience() > maxSlayerExperienceRequired) {
            double remaining = this.getExperience() - maxSlayerExperienceRequired;
            double overflow = 0;
            double modifier = this.getSlayer().getWeightModifier();

            while (remaining > 0) {
                double left = Math.min(remaining, maxSlayerExperienceRequired);
                overflow += Math.pow(left / (this.getSlayer().getWeightDivider() * (1.5 + modifier)), 0.942);
                remaining -= left;
                modifier += modifier;
            }

            weightOverflow = NumberUtil.round(overflow, 2);
        }

        return Weight.of(weightValue, weightOverflow);
    }

    public boolean isClaimed(int level) {
        return this.getClaimed().getOrDefault(level, false);
    }

    @Getter
    public static class Quest {

        private @NotNull String id = "UNKNOWN";
        private int tier;
        @SerializedName("start_timestamp")
        private Instant start;
        @SerializedName("completion_state")
        private int completionState;
        @SerializedName("used_armor")
        private boolean usedArmor;
        private boolean solo;

    }

}
