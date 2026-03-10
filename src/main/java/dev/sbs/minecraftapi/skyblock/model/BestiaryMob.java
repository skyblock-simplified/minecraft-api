package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import org.jetbrains.annotations.NotNull;

public interface BestiaryMob extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getFamilyId();

    default @NotNull BestiaryFamily getFamily() {
        return MinecraftApi.getRepository(BestiaryFamily.class)
            .findFirstOrNull(BestiaryFamily::getId, this.getFamilyId());
    }

    int getLevel();

    long getHealth();

    long getDamage();

    long getDefense();

    @NotNull KillStats getKillStats();

    interface Experience {

        @NotNull String getSkillId();

        default @NotNull Skill getSkill() {
            return MinecraftApi.getRepository(Skill.class)
                .findFirstOrNull(Skill::getId, this.getSkillId());
        }

        int getAmount();

    }

    interface KillStats {

        int getCoins();

        @NotNull Experience getExperience();

        int getExperienceOrbs();

    }

}
