package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import dev.sbs.minecraftapi.skyblock.model.Stat;
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
    name = "stats"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonStat implements Stat, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String symbol = "";
    private @NotNull ChatFormat format = ChatFormat.WHITE;
    private @NotNull String category = "";
    private double base = 0.0;
    private double cap = 0.0;
    private double enrichment = 0.0;
    private double powerMultiplier = 0.0;
    private double tuningMultiplier = 0.0;
    private boolean visible;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonStat jsonStat = (JsonStat) o;

        return new EqualsBuilder()
            .append(this.getBase(), jsonStat.getBase())
            .append(this.getCap(), jsonStat.getCap())
            .append(this.getEnrichment(), jsonStat.getEnrichment())
            .append(this.getPowerMultiplier(), jsonStat.getPowerMultiplier())
            .append(this.getTuningMultiplier(), jsonStat.getTuningMultiplier())
            .append(this.isVisible(), jsonStat.isVisible())
            .append(this.getId(), jsonStat.getId())
            .append(this.getName(), jsonStat.getName())
            .append(this.getSymbol(), jsonStat.getSymbol())
            .append(this.getFormat(), jsonStat.getFormat())
            .append(this.getCategory(), jsonStat.getCategory())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getSymbol())
            .append(this.getFormat())
            .append(this.getCategory())
            .append(this.getBase())
            .append(this.getCap())
            .append(this.getEnrichment())
            .append(this.getPowerMultiplier())
            .append(this.getTuningMultiplier())
            .append(this.isVisible())
            .build();
    }

    @Getter
    public static class JsonSubstitute implements Substitute {

        private @NotNull String id = "";
        private int precision = 0;
        private @NotNull Stat.Type type = Stat.Type.NONE;
        private @NotNull ChatFormat format = ChatFormat.GREEN;
        private @NotNull ConcurrentMap<Integer, Double> values = Concurrent.newMap();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonSubstitute that = (JsonSubstitute) o;

            return new EqualsBuilder()
                .append(this.getPrecision(), that.getPrecision())
                .append(this.getId(), that.getId())
                .append(this.getType(), that.getType())
                .append(this.getFormat(), that.getFormat())
                .append(this.getValues(), that.getValues())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getPrecision())
                .append(this.getType())
                .append(this.getFormat())
                .append(this.getValues())
                .build();
        }

    }

}
