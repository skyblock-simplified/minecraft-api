package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.api.data.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.Enchantment;
import dev.sbs.minecraftapi.skyblock.model.Stat;
import dev.sbs.minecraftapi.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "enchantments"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonEnchantment implements Enchantment, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private @NotNull Type type = Type.NORMAL;
    private int requiredLevel = 0;
    private @NotNull Optional<String> conflict = Optional.empty();
    private @NotNull ConcurrentList<String> categoryIds = Concurrent.newList();
    private @NotNull ConcurrentList<String> itemIds = Concurrent.newList();
    private @NotNull ConcurrentList<JsonLevel> levels = Concurrent.newList();
    private @NotNull ConcurrentList<JsonSubstitute> stats = Concurrent.newList();
    private @NotNull ConcurrentList<String> mobTypeIds = Concurrent.newList();

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonApplyCost implements ApplyCost {

        private int experience = 0;
        private @NotNull Optional<String> itemId = Optional.empty();

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonLevel implements Level {

        private int level = 0;
        private @NotNull JsonApplyCost applyCost = new JsonApplyCost();

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonSubstitute implements Substitute {

        private @NotNull String id = "";
        private int precision = 0;
        private @NotNull Stat.Type type = Stat.Type.NONE;
        private @NotNull ChatFormat format = ChatFormat.GREEN;
        private @NotNull ConcurrentMap<Integer, Double> values = Concurrent.newMap();

    }

}
