package dev.sbs.minecraftapi.client.sbs.exception;

import dev.sbs.api.client.exception.ClientException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public final class SbsClientException extends ClientException {

    private final @NotNull SbsErrorResponse response;

    public SbsClientException(@NotNull String methodKey, @NotNull feign.Response response) {
        super(methodKey, response, "Sbs");
        this.response = this.getBody()
            .map(json -> super.fromJson(json, SbsErrorResponse.class))
            .orElse(new SbsErrorResponse.Unknown());
    }

}
