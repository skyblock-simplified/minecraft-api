package dev.sbs.minecraftapi.client.sbs.response;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.tuple.pair.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Optional;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SkyBlockEmojisResponse {

    private final ConcurrentMap<String, ConcurrentMap<Boolean, Emoji>> items;

    public Optional<Emoji> getEmoji(@NotNull String id) {
        return this.getEmoji(id, false);
    }

    public Optional<Emoji> getEmoji(@NotNull String id, boolean enchanted) {
        return Optional.ofNullable(this.items.getOrDefault(id, null)).map(itemMap -> itemMap.get(enchanted));
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Emoji {

        private long id;
        private String name;
        private boolean animated;

        Emoji(String name, Emoji emoji) {
            this.id = emoji.getId();
            this.name = name;
            this.animated = emoji.isAnimated();
        }

        public String getFormat() {
            return String.format("<:%s:%s>", this.getName(), this.getId());
        }

    }

    public static class Deserializer implements JsonDeserializer<SkyBlockEmojisResponse> {

        @Override
        public SkyBlockEmojisResponse deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jdc) throws JsonParseException {
            Gson gson = SimplifiedApi.getGson();

            return new SkyBlockEmojisResponse(
                jsonElement.getAsJsonObject()
                    .entrySet()
                    .stream()
                    .map(entry -> Pair.of(
                        entry.getKey(),
                        Concurrent.newMap(
                            Pair.of(false, gson.fromJson(entry.getValue().getAsJsonObject().get("normal"), Emoji.class)),
                            Pair.of(true, gson.fromJson(entry.getValue().getAsJsonObject().get("enchanted"), Emoji.class))
                        )
                    ))
                    .collect(Concurrent.toUnmodifiableMap())
            );
        }

    }

}
