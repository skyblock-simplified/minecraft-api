package dev.sbs.minecraftapi.client.sbs.request;

import dev.sbs.api.client.request.Endpoints;
import dev.sbs.api.client.route.Route;
import dev.sbs.minecraftapi.client.mojang.profile.MojangProfile;
import dev.sbs.minecraftapi.client.mojang.response.MojangUsernameResponse;
import dev.sbs.minecraftapi.client.sbs.exception.SbsApiException;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojis;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojisResponse;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockImagesResponse;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockItemsResponse;
import feign.Param;
import feign.RequestLine;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Route("api.sbs.dev")
public interface SbsEndpoints extends Endpoints {

    @RequestLine("GET /test/{username}")
    @Deprecated
    @NotNull MojangUsernameResponse getTestProfileFromUsername(@NotNull @Param("username") String username) throws SbsApiException;

    @RequestLine("GET /mojang/user/{username}")
    @NotNull MojangProfile getProfileFromUsername(@NotNull @Param("username") String username) throws SbsApiException;

    @RequestLine("GET /mojang/user/{uniqueId}")
    @NotNull MojangProfile getProfileFromUniqueId(@NotNull @Param("uniqueId") UUID uniqueId) throws SbsApiException;

    @RequestLine("GET /skyblock/emojis.json")
    @NotNull SkyBlockEmojisResponse getEmojis() throws SbsApiException;

    @RequestLine("GET /skyblock/images.json")
    @NotNull SkyBlockImagesResponse getImages() throws SbsApiException;

    @RequestLine("GET /skyblock/items.json")
    @NotNull SkyBlockItemsResponse getItems() throws SbsApiException;

    default @NotNull SkyBlockEmojis getItemEmojis() throws SbsApiException {
        return new SkyBlockEmojis(
            this.getItems(),
            this.getEmojis(),
            this.getImages()
        );
    }

}
