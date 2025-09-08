package dev.sbs.minecraftapi.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.api.data.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.Minion;
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
    name = "minions"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonMinion implements Minion, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @SerializedName("collection")
    private @NotNull String collectionId = "";
    private @NotNull ConcurrentList<JsonMinionTier> tiers = Concurrent.newList();

    @Getter
    public static class JsonMinionTier implements Tier {

        private int tier;
        private double speed;
        @SerializedName("item")
        private @NotNull String itemId = "";
        private @NotNull JsonMinionUpgradeCost upgradeCost = new JsonMinionUpgradeCost();

    }

    @Getter
    public static class JsonMinionUpgradeCost implements UpgradeCost {

        private @NotNull ConcurrentMap<Currency, Double> currencies = Concurrent.newMap();
        private @NotNull ConcurrentMap<String, Double> items = Concurrent.newMap();

    }

}
