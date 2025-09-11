package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.data.json.JsonModel;
import dev.sbs.api.data.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.Item;
import dev.sbs.minecraftapi.skyblock.model.ItemCategory;
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
    name = "item_categories"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonItemCategory implements ItemCategory, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Item.Type type = Item.Type.OTHER;

}
