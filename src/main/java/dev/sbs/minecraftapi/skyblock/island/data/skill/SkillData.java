package dev.sbs.minecraftapi.skyblock.island.data.skill;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.minecraftapi.skyblock.island.Member;
import dev.sbs.minecraftapi.skyblock.type.Weight;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class SkillData {

    private final @NotNull ConcurrentList<SkillEntry> skills;

    public SkillData(@NotNull ConcurrentMap<String, Double> skillExperience, @NotNull Member member) {
        this.skills = skillExperience.stream()
            .mapKey(id -> id.replace("SKILL_", ""))
            .map((id, experience) -> new SkillEntry(id, experience, member))
            .collect(Concurrent.toUnmodifiableList());
    }

    public @NotNull SkillEntry getSkill(@NotNull String id) {
        return this.getSkills().matchFirstOrNull(skill -> skill.getId().equalsIgnoreCase(id));
    }

    public @NotNull ConcurrentList<SkillEntry> getSkills(boolean includeCosmetic) {
        return this.getSkills()
            .stream()
            .filter(skill -> includeCosmetic || !skill.getSkill().isCosmetic())
            .collect(Concurrent.toList());
    }

    public double getAverage() {
        return this.getSkills(false)
            .stream()
            .mapToDouble(SkillEntry::getLevel)
            .average()
            .orElse(0.0);
    }

    public double getExperience() {
        return this.getSkills(false)
            .stream()
            .mapToDouble(SkillEntry::getExperience)
            .sum();
    }

    public double getProgressPercentage() {
        return this.getSkills(false)
            .stream()
            .mapToDouble(SkillEntry::getTotalProgressPercentage)
            .average()
            .orElse(0.0);
    }

    public @NotNull ConcurrentMap<SkillEntry, Weight> getWeight() {
        return this.getSkills(false)
            .stream()
            .map(skill -> Pair.of(skill, skill.getWeight()))
            .collect(Concurrent.toMap());
    }

}
