package dev.sbs.minecraftapi.client.sbs;

import dev.sbs.api.client.Client;
import dev.sbs.api.client.exception.ApiErrorDecoder;
import dev.sbs.api.client.response.CFCacheStatus;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentSet;
import dev.sbs.minecraftapi.client.sbs.exception.SbsApiException;
import dev.sbs.minecraftapi.client.sbs.request.SbsEndpoints;
import feign.FeignException;
import org.jetbrains.annotations.NotNull;

public final class SbsClient extends Client<SbsEndpoints> {

    public SbsClient() {
        super("api.sbs.dev");
    }

    @Override
    protected @NotNull ApiErrorDecoder configureErrorDecoder() {
        return (methodKey, response) -> {
            throw new SbsApiException(FeignException.errorStatus(methodKey, response));
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
