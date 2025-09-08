package dev.sbs.minecraftapi.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.api.data.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.Gemstone;
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
    name = "gemstones"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonGemstone implements Gemstone, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String symbol = "";
    private @NotNull ChatFormat format = ChatFormat.WHITE;
    @SerializedName("stat")
    private @NotNull String statId = "";
    private @NotNull ConcurrentMap<Type, ConcurrentMap<ChatFormat, Double>> values = Concurrent.newMap();

}
