package dev.sbs.minecraftapi.skyblock.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@UtilityClass
public class SkyBlockDateTypeAdapter {

    public static class RealTime extends TypeAdapter<SkyBlockDate.RealTime> {

        @Override
        public void write(@NotNull JsonWriter out, @NotNull SkyBlockDate.RealTime value) throws IOException {
            out.value(value.getRealTime());
        }

        @Override
        public SkyBlockDate.RealTime read(@NotNull JsonReader in) throws IOException {
            return new SkyBlockDate.RealTime(in.nextLong());
        }

    }

    public static class SkyBlockTime extends TypeAdapter<SkyBlockDate.SkyBlockTime> {

        @Override
        public void write(@NotNull JsonWriter out, @NotNull SkyBlockDate.SkyBlockTime value) throws IOException {
            out.value(value.getRealTime());
        }

        @Override
        public SkyBlockDate.SkyBlockTime read(@NotNull JsonReader in) throws IOException {
            return new SkyBlockDate.SkyBlockTime(in.nextLong());
        }

    }

}
