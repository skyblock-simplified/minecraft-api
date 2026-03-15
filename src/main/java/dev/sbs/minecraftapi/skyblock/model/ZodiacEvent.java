package dev.sbs.minecraftapi.skyblock.model;

import dev.sbs.api.persistence.Model;
import org.jetbrains.annotations.NotNull;

public interface ZodiacEvent extends Model {

    @NotNull String getId();

    @NotNull String getName();

    int getReleaseYear();

    default int getRecurringYear() {
        return this.getReleaseYear() % 12;
    }

}
