package dev.sbs.minecraftapi.skyblock.model.json;

import dev.sbs.api.data.json.JsonModel;
import dev.sbs.api.data.json.JsonResource;
import dev.sbs.minecraftapi.skyblock.model.Keyword;
import dev.sbs.minecraftapi.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "keywords"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonKeyword implements Keyword, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Optional<String> plural = Optional.empty();
    private @NotNull Optional<String> symbol = Optional.empty();
    private @NotNull ChatFormat format = ChatFormat.LIGHT_PURPLE;

}
