package dev.sbs.minecraftapi.client.sbs.response;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.client.response.HttpStatus;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.client.mojang.MojangClient;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class MojangStatusResponse {

    private @NotNull ConcurrentMap<Service, State> allStates = Concurrent.newMap();

    public State getState(Service service) {
        return this.allStates.get(service);
    }

    @Getter
    public enum Service {

        @SerializedName("minecraft.net")
        MINECRAFT(MojangClient.Domain.MINECRAFT),
        @SerializedName("session.minecraft.net")
        SESSION(MojangClient.Domain.MINECRAFT_SESSION),
        @SerializedName("account.minecraft.net")
        ACCOUNT(MojangClient.Domain.MOJANG_ACCOUNT),
        @SerializedName("authserver.minecraft.net")
        AUTHENTICATION_SERVER(MojangClient.Domain.MOJANG_AUTHSERVER),
        @SerializedName("sessionserver.minecraft.net")
        SESSION_SERVER(MojangClient.Domain.MOJANG_SESSIONSERVER),
        @SerializedName("api.minecraft.net")
        API(MojangClient.Domain.MOJANG_API),
        @SerializedName("textures.minecraft.net")
        TEXTURES(MojangClient.Domain.MINECRAFT_TEXTURES),
        @SerializedName("mojang.net")
        MOJANG(MojangClient.Domain.MOJANG);

        private final @NotNull MojangClient.Domain domain;

        Service(@NotNull MojangClient.Domain domain) {
            this.domain = domain;
        }

    }

    public static class State {

        @Getter private boolean success;
        private int code;

        public @NotNull HttpStatus getHttpStatus() {
            return HttpStatus.of(this.code);
        }

    }

}
