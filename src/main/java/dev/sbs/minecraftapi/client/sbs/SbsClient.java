package dev.sbs.minecraftapi.client.sbs;

import dev.sbs.api.client.Client;
import dev.sbs.api.client.decoder.ClientErrorDecoder;
import dev.sbs.minecraftapi.client.sbs.exception.SbsApiException;
import dev.sbs.minecraftapi.client.sbs.request.SbsEndpoint;
import org.jetbrains.annotations.NotNull;

public final class SbsClient extends Client<SbsEndpoint> {

    @Override
    protected @NotNull ClientErrorDecoder configureErrorDecoder() {
        return (methodKey, response) -> {
            throw new SbsApiException(methodKey, response);
        };
    }

}
