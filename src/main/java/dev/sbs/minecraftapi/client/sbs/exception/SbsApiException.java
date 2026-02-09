package dev.sbs.minecraftapi.client.sbs.exception;

import dev.sbs.api.client.exception.ApiException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public final class SbsApiException extends ApiException {

    private final @NotNull SbsErrorResponse response;

    public SbsApiException(@NotNull String methodKey, @NotNull feign.Response response) {
        super(methodKey, response, "Sbs");
        this.response = this.getBody()
            .map(json -> super.fromJson(json, SbsErrorResponse.class))
            .orElse(new SbsErrorResponse.Unknown());
    }

}
