package dev.sbs.minecraftapi.client.sbs.response;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.tuple.pair.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Optional;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SkyBlockItemsResponse {

    private final ConcurrentMap<String, String> items;

    public Optional<String> getItemId(@NotNull String itemId) {
        return Optional.ofNullable(this.getItems().get(itemId));
    }

    public Optional<String> getPetId(@NotNull String petName) {
        return Optional.ofNullable(this.getItems().get("PET_" + petName));
    }

    public Optional<String> getRuneId(@NotNull String runeName) {
        return Optional.ofNullable(this.getItems().get("RUNE_" + runeName));
    }

    public static class Deserializer implements JsonDeserializer<SkyBlockItemsResponse> {

        @Override
        public SkyBlockItemsResponse deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jdc) throws JsonParseException {
            return new SkyBlockItemsResponse(
                jsonElement.getAsJsonObject()
                    .entrySet()
                    .stream()
                    .map(entry -> Pair.of(entry.getKey(), entry.getValue().getAsString()))
                    .collect(Concurrent.toUnmodifiableMap())
            );
        }

    }

}
