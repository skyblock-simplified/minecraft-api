package dev.sbs.minecraftapi.client.sbs;

import dev.sbs.api.client.Client;
import dev.sbs.api.client.exception.ClientErrorDecoder;
import dev.sbs.api.client.response.CFCacheStatus;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentSet;
import dev.sbs.minecraftapi.client.sbs.exception.SbsClientException;
import dev.sbs.minecraftapi.client.sbs.request.SbsEndpoints;
import org.jetbrains.annotations.NotNull;

public final class SbsClient extends Client<SbsEndpoints> {

    public SbsClient() {
        super("api.sbs.dev");
    }

    @Override
    protected @NotNull ClientErrorDecoder configureErrorDecoder() {
        return (methodKey, response) -> {
            throw new SbsClientException(methodKey, response);
        };
    }

    @Override
    protected @NotNull ConcurrentSet<String> configureResponseHeaders() {
        return Concurrent.newUnmodifiableSet(
            CFCacheStatus.HEADER_KEY,
            "RateLimit-Limit",
            "RateLimit-Remaining",
            "RateLimit-Reset"
        );
    }

}
