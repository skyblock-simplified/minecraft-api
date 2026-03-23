package dev.sbs.minecraftapi.client.sbs.request;

import dev.sbs.api.client.request.Endpoint;
import dev.sbs.api.client.route.Route;
import dev.sbs.minecraftapi.client.mojang.profile.MojangProfile;
import dev.sbs.minecraftapi.client.mojang.response.MojangUsername;
import dev.sbs.minecraftapi.client.sbs.exception.SbsApiException;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojiData;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojis;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockImages;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockItems;
import feign.Param;
import feign.RequestLine;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Route("api.sbs.dev")
public interface SbsEndpoint extends Endpoint {

    @RequestLine("GET /test/{username}")
    @Deprecated
    @NotNull MojangUsername getTestProfileFromUsername(@NotNull @Param("username") String username) throws SbsApiException;

    @RequestLine("GET /mojang/user/{username}")
    @NotNull MojangProfile getProfileFromUsername(@NotNull @Param("username") String username) throws SbsApiException;

    @RequestLine("GET /mojang/user/{uniqueId}")
    @NotNull MojangProfile getProfileFromUniqueId(@NotNull @Param("uniqueId") UUID uniqueId) throws SbsApiException;

    @RequestLine("GET /skyblock/emojis.json")
    @NotNull SkyBlockEmojis getEmojis() throws SbsApiException;

    @RequestLine("GET /skyblock/images.json")
    @NotNull SkyBlockImages getImages() throws SbsApiException;

    @RequestLine("GET /skyblock/items.json")
    @NotNull SkyBlockItems getItems() throws SbsApiException;

    default @NotNull SkyBlockEmojiData getItemEmojis() throws SbsApiException {
        return new SkyBlockEmojiData(
            this.getItems(),
            this.getEmojis(),
            this.getImages()
        );
    }

}
