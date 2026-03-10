package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.BestiaryMob;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "bestiary_mobs"
)
public class JsonBestiaryMob implements BestiaryMob, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String familyId = "";
    private int level = 1;
    private long health = 100;
    private long damage = 0;
    private long defense = 0;
    private @NotNull JsonKillStats killStats = new JsonKillStats();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonBestiaryMob that = (JsonBestiaryMob) o;

        return new EqualsBuilder()
            .append(this.getLevel(), that.getLevel())
            .append(this.getHealth(), that.getHealth())
            .append(this.getDamage(), that.getDamage())
            .append(this.getDefense(), that.getDefense())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getFamilyId(), that.getFamilyId())
            .append(this.getKillStats(), that.getKillStats())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getFamilyId())
            .append(this.getLevel())
            .append(this.getHealth())
            .append(this.getDamage())
            .append(this.getDefense())
            .append(this.getKillStats())
            .build();
    }

    @Getter
    public static class JsonExperience implements Experience {

        private @NotNull String skillId = "";
        private int amount = 0;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonExperience that = (JsonExperience) o;

            return new EqualsBuilder()
                .append(this.getAmount(), that.getAmount())
                .append(this.getSkillId(), that.getSkillId())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getSkillId())
                .append(this.getAmount())
                .build();
        }

    }

    @Getter
    public static class JsonKillStats implements KillStats {

        private int coins;
        private @NotNull JsonExperience experience = new JsonExperience();
        private int experienceOrbs;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonKillStats that = (JsonKillStats) o;

            return new EqualsBuilder()
                .append(this.getCoins(), that.getCoins())
                .append(this.getExperienceOrbs(), that.getExperienceOrbs())
                .append(this.getExperience(), that.getExperience())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getCoins())
                .append(this.getExperience())
                .append(this.getExperienceOrbs())
                .build();
        }

    }

}
