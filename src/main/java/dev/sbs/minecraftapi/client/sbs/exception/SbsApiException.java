package dev.sbs.minecraftapi.client.sbs.exception;

import com.google.gson.Gson;
import dev.simplified.client.exception.JsonApiException;
import org.jetbrains.annotations.NotNull;

public final class SbsApiException extends JsonApiException {

    public SbsApiException(@NotNull Gson gson, @NotNull String methodKey, @NotNull feign.Response response) {
        super(methodKey, response, "SBS", gson, SbsErrorResponse.class, SbsErrorResponse.Unknown::new);
    }

    @Override
    public @NotNull SbsErrorResponse getResponse() {
        return (SbsErrorResponse) super.getResponse();
    }

}
