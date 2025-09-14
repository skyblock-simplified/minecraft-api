package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.data.json.JsonModel;
import dev.sbs.api.data.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.StatCategory;
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
    name = "stat_categories"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonStatCategory implements StatCategory, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull ChatFormat format = ChatFormat.WHITE;

}
