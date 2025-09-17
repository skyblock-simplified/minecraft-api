package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.api.data.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.Rarity;
import dev.sbs.minecraftapi.skyblock.model.Pet;
import dev.sbs.minecraftapi.skyblock.model.Stat;
import dev.sbs.minecraftapi.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "pets"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonPet implements Pet, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Rarity lowestRarity = Rarity.COMMON;
    private @NotNull String skillId = "";
    private @NotNull Type type = Type.PET;
    private int maxLevel = 100;
    private boolean passive = false;
    private @NotNull ConcurrentList<JsonSubstitute> stats = Concurrent.newList();
    private @NotNull ConcurrentList<JsonAbility> abilities = Concurrent.newList();

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonAbility implements Ability {

        private @NotNull String name = "";
        private @NotNull String description = "";
        private boolean flatStat = false;
        private @NotNull ConcurrentList<JsonSubstitute> stats = Concurrent.newList();

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonSubstitute implements Substitute {

        private @NotNull String id = "";
        private int precision = 0;
        private @NotNull Stat.Type type = Stat.Type.NONE;
        private @NotNull ChatFormat format = ChatFormat.GREEN;
        private @NotNull ConcurrentMap<Rarity, JsonValue> values = Concurrent.newMap();

        @Getter
        @NoArgsConstructor(access = AccessLevel.NONE)
        public static class JsonValue implements Value {

            private double base = 0.0;
            private double scalar = 0.0;

        }

    }

}
