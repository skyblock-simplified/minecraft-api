package dev.sbs.minecraftapi.skyblock.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dev.sbs.minecraftapi.skyblock.NbtContent;

import java.io.IOException;

public class NbtContentTypeAdapter extends TypeAdapter<NbtContent> {

    @Override
    public void write(JsonWriter out, NbtContent value) throws IOException {
        out.beginObject()
            .name("type")
            .value(0)
            .name("data")
            .value(value.getRawData())
            .endObject();
    }

    @Override
    public NbtContent read(JsonReader in) throws IOException {
        String data;

        if (in.peek() == JsonToken.BEGIN_OBJECT) { // Auctions are bad
            in.beginObject();
            data = in.nextString();
            in.endObject();
        } else
            data = in.nextString();

        return new NbtContent(0, data);
    }

}
