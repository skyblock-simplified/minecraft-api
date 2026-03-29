package dev.sbs.minecraftapi.client.sbs.exception;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.client.exception.ApiErrorResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SbsErrorResponse implements ApiErrorResponse {

    protected int statusCode;
    @SerializedName("error")
    protected @NotNull String error;
    @SerializedName("reason")
    protected @NotNull String reason;
    @SerializedName("route")
    protected @NotNull String route;

    /**
     * Creates a new {@code SbsErrorResponse} for server-side error construction.
     *
     * @param statusCode the HTTP status code
     * @param error the HTTP status reason phrase
     * @param reason a human-readable description of the error
     * @param route the HTTP method and request URI
     * @return a new error response
     */
    public static @NotNull SbsErrorResponse of(int statusCode, @NotNull String error, @NotNull String reason, @NotNull String route) {
        SbsErrorResponse response = new SbsErrorResponse();
        response.statusCode = statusCode;
        response.error = error;
        response.reason = reason;
        response.route = route;
        return response;
    }

    public static class Unknown extends SbsErrorResponse {

        public Unknown() {
            super.error = "UNKNOWN";
            super.reason = "Unknown reason.";
        }

    }

}
