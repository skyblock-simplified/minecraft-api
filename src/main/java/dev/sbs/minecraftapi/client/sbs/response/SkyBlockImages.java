package dev.sbs.minecraftapi.client.sbs.response;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Type;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SkyBlockImages {

    private final ConcurrentMap<String, Image> items;

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Image {

        private final String normal;
        private final String enchanted;

    }

    public static class Deserializer implements JsonDeserializer<SkyBlockImages> {

        @Override
        public SkyBlockImages deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jdc) throws JsonParseException {
            return new SkyBlockImages(
                jsonElement.getAsJsonObject()
                    .entrySet()
                    .stream()
                    .map(entry -> Pair.of(
                        entry.getKey(),
                        new Image(
                            entry.getValue().getAsJsonObject().get("normal").getAsString(),
                            entry.getValue().getAsJsonObject().get("enchanted").getAsString()
                        )
                    ))
                    .collect(Concurrent.toUnmodifiableMap())
            );
        }

    }

}
