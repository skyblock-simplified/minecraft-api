package dev.sbs.minecraftapi.client.sbs;

import com.google.gson.Gson;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.sbs.exception.SbsApiException;
import dev.sbs.minecraftapi.client.sbs.request.SbsEndpoint;
import dev.simplified.client.Client;
import dev.simplified.client.decoder.ClientErrorDecoder;
import org.jetbrains.annotations.NotNull;

public final class SbsClient extends Client<SbsEndpoint> {

    @Override
    protected @NotNull Gson getGson() {
        return MinecraftApi.getGson();
    }

    @Override
    protected @NotNull ClientErrorDecoder configureErrorDecoder() {
        return (methodKey, response) -> {
            throw new SbsApiException(methodKey, response);
        };
    }

}
