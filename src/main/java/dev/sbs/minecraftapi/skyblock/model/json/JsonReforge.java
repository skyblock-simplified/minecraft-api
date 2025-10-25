package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.api.data.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.Rarity;
import dev.sbs.minecraftapi.skyblock.model.Reforge;
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
    name = "reforges"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonReforge implements Reforge, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Optional<String> stoneId = Optional.empty();
    private int requiredLevel = 0;
    private @NotNull ConcurrentList<String> categoryIds = Concurrent.newList();
    private @NotNull ConcurrentList<String> itemIds = Concurrent.newList();
    private @NotNull ConcurrentList<JsonSubstitute> stats = Concurrent.newList();

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonSubstitute implements Substitute {

        private @NotNull String id = "";
        private @NotNull ConcurrentMap<Rarity, Double> values = Concurrent.newMap();


    }

}
