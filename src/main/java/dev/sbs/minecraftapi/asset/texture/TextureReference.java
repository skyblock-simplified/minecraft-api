package dev.sbs.minecraftapi.asset.texture;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dev.simplified.persistence.type.GsonType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * A texture map entry supporting both the legacy string format and the 1.21.5+ object format.
 *
 * <p>Legacy format stores only the sprite path as a plain string. The object format adds
 * rendering hints such as {@code force_translucent}.
 *
 * @param sprite the namespaced texture path (e.g. "minecraft:block/stone")
 * @param forceTranslucent true if the texture requires translucent rendering
 */
@GsonType
public record TextureReference(@NotNull String sprite, boolean forceTranslucent) {

    /**
     * Creates a texture reference from a plain sprite path with no rendering hints.
     *
     * @param sprite the namespaced texture path
     * @return a new texture reference
     */
    public static @NotNull TextureReference of(@NotNull String sprite) {
        return new TextureReference(sprite, false);
    }

    /**
     * Gson adapter handling both the legacy string format and the 1.21.5+ object format.
     *
     * <p>Reads a bare string as a simple sprite path. Reads an object by extracting the
     * {@code sprite} and {@code force_translucent} fields, skipping any unknown properties
     * for forward compatibility. Writes the compact string form when no rendering hints are
     * set, otherwise writes the full object form.
     */
    public static class Adapter extends TypeAdapter<TextureReference> {

        @Override
        public void write(@NotNull JsonWriter out, TextureReference value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            if (value.forceTranslucent()) {
                out.beginObject();
                out.name("sprite").value(value.sprite());
                out.name("force_translucent").value(true);
                out.endObject();
            } else {
                out.value(value.sprite());
            }
        }

        @Override
        public @NotNull TextureReference read(@NotNull JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return TextureReference.of("");
            }

            if (in.peek() == JsonToken.STRING)
                return TextureReference.of(in.nextString());

            String sprite = "";
            boolean forceTranslucent = false;

            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                switch (name) {
                    case "sprite" -> sprite = in.nextString();
                    case "force_translucent" -> forceTranslucent = in.nextBoolean();
                    default -> in.skipValue();
                }
            }
            in.endObject();

            return new TextureReference(sprite, forceTranslucent);
        }
    }

}
