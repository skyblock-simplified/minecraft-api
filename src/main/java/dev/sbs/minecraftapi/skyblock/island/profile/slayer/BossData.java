package dev.sbs.minecraftapi.skyblock.island.profile.slayer;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.reflection.Reflection;
import lombok.Getter;

import java.util.Map;

class BossData implements PostInit {

    private final static Reflection<BossData> slayerRef = Reflection.of(BossData.class);
    @SerializedName("xp")
    @Getter protected double experience;

    protected ConcurrentMap<String, Boolean> claimed_levels = Concurrent.newMap(); // level_#: true
    protected int boss_kills_tier_0;
    protected int boss_kills_tier_1;
    protected int boss_kills_tier_2;
    protected int boss_kills_tier_3;
    protected int boss_kills_tier_4;

    protected int boss_attempts_tier_0;
    protected int boss_attempts_tier_1;
    protected int boss_attempts_tier_2;
    protected int boss_attempts_tier_3;
    protected int boss_attempts_tier_4;

    @Getter protected ConcurrentMap<Integer, Boolean> claimed;
    @Getter protected ConcurrentMap<Integer, Boolean> claimedSpecial;
    @Getter protected ConcurrentMap<Integer, Integer> kills;
    @Getter protected ConcurrentMap<Integer, Integer> attempts;

    @Override
    @SuppressWarnings("all")
    public void postInit() {
        ConcurrentMap<Integer, Boolean> claimed = Concurrent.newMap();
        ConcurrentMap<Integer, Boolean> claimedSpecial = Concurrent.newMap();
        ConcurrentMap<Integer, Integer> kills = Concurrent.newMap();
        ConcurrentMap<Integer, Integer> attempts = Concurrent.newMap();

        for (Map.Entry<String, Boolean> entry : this.claimed_levels.entrySet()) {
            String entryKey = entry.getKey().replace("level_", "");
            boolean special = entryKey.endsWith("_special");
            entryKey = special ? entryKey.replace("_special", "") : entryKey;
            (special ? claimedSpecial : claimed).put(Integer.parseInt(entryKey), entry.getValue());
        }

        for (int i = 0; i < 5; i++) {
            kills.put(i + 1, (int) slayerRef.getValue(String.format("boss_kills_tier_%s", i), this));
            attempts.put(i + 1, (int) slayerRef.getValue(String.format("boss_attempts_tier_%s", i), this));
        }

        this.claimed = claimed.toUnmodifiableMap();
        this.claimedSpecial = claimedSpecial.toUnmodifiableMap();
        this.kills = kills.toUnmodifiableMap();
        this.attempts = attempts.toUnmodifiableMap();
    }

}
