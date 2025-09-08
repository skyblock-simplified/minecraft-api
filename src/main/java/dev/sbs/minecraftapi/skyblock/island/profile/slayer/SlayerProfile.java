package dev.sbs.minecraftapi.skyblock.island.profile.slayer;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.minecraftapi.skyblock.type.Weight;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class SlayerProfile implements PostInit {

    private final static @NotNull SlayerEntry UNKNOWN = new SlayerEntry("UNKNOWN", new BossData());
    @SerializedName("slayer_quest")
    private final @NotNull Optional<SlayerEntry.Quest> activeQuest = Optional.empty();
    @Getter(AccessLevel.NONE)
    @SerializedName("slayer_bosses")
    private @NotNull ConcurrentMap<String, BossData> slayerBosses = Concurrent.newMap();
    private transient ConcurrentList<SlayerEntry> slayers = Concurrent.newList();

    @Override
    public void postInit() {
        this.slayers = this.slayerBosses.stream()
            .map(SlayerEntry::new)
            .collect(Concurrent.toUnmodifiableList());
    }

    public @NotNull SlayerEntry getSlayer(@NotNull String id) {
        return this.getSlayers().matchFirst(skill -> skill.getId().equalsIgnoreCase(id)).orElse(UNKNOWN);
    }

    public double getAverage() {
        return this.getSlayers()
            .stream()
            .mapToDouble(SlayerEntry::getLevel)
            .average()
            .orElse(0.0);
    }

    public double getExperience() {
        return this.getSlayers()
            .stream()
            .mapToDouble(SlayerEntry::getExperience)
            .sum();
    }

    public double getProgressPercentage() {
        return this.getSlayers()
            .stream()
            .mapToDouble(SlayerEntry::getTotalProgressPercentage)
            .average()
            .orElse(0.0);
    }

    public @NotNull ConcurrentMap<SlayerEntry, Weight> getWeight() {
        return this.getSlayers()
            .stream()
            .map(slayer -> Pair.of(slayer, slayer.getWeight()))
            .collect(Concurrent.toMap());
    }

}
