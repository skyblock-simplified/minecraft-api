package dev.sbs.minecraftapi.client.sbs.request;

import dev.sbs.api.client.request.Endpoints;
import dev.sbs.minecraftapi.client.mojang.profile.MojangProfile;
import dev.sbs.minecraftapi.client.mojang.response.MojangUsernameResponse;
import dev.sbs.minecraftapi.client.sbs.response.MojangStatusResponse;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojis;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojisResponse;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockImagesResponse;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockItemsResponse;
import feign.Param;
import feign.RequestLine;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface SbsEndpoints extends Endpoints {

    @RequestLine("GET /test/{username}")
    @Deprecated
    @NotNull MojangUsernameResponse getTestProfileFromUsername(@NotNull @Param("username") String username);

    @RequestLine("GET /mojang/user/{username}")
    @NotNull MojangProfile getProfileFromUsername(@NotNull @Param("username") String username);

    @RequestLine("GET /mojang/user/{uniqueId}")
    @NotNull MojangProfile getProfileFromUniqueId(@NotNull @Param("uniqueId") UUID uniqueId);

    @RequestLine("GET /mojang/status")
    @NotNull MojangStatusResponse getStatus();

    @RequestLine("GET /skyblock/emojis.json")
    @NotNull SkyBlockEmojisResponse getEmojis();

    @RequestLine("GET /skyblock/images.json")
    @NotNull SkyBlockImagesResponse getImages();

    @RequestLine("GET /skyblock/items.json")
    @NotNull SkyBlockItemsResponse getItems();

    default @NotNull SkyBlockEmojis getItemEmojis() {
        return new SkyBlockEmojis(
            this.getItems(),
            this.getEmojis(),
            this.getImages()
        );
    }

}
