package dev.sbs.minecraftapi;

import com.google.gson.Gson;
import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.io.gson.GsonSettings;
import dev.sbs.minecraftapi.client.hypixel.HypixelClient;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelEndpoints;
import dev.sbs.minecraftapi.client.mojang.MinecraftServicesClient;
import dev.sbs.minecraftapi.client.mojang.MojangApiClient;
import dev.sbs.minecraftapi.client.mojang.MojangProxy;
import dev.sbs.minecraftapi.client.mojang.MojangSessionClient;
import dev.sbs.minecraftapi.client.mojang.request.MinecraftServerPing;
import dev.sbs.minecraftapi.client.mojang.request.MojangApiEndpoints;
import dev.sbs.minecraftapi.client.mojang.request.MojangSessionEndpoints;
import dev.sbs.minecraftapi.client.mojang.response.MojangMultiUsernameResponse;
import dev.sbs.minecraftapi.client.sbs.SbsClient;
import dev.sbs.minecraftapi.client.sbs.request.SbsEndpoints;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojisResponse;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockImagesResponse;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockItemsResponse;
import dev.sbs.minecraftapi.nbt.NbtFactory;
import dev.sbs.minecraftapi.skyblock.data.NbtContent;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.sbs.minecraftapi.skyblock.gson.NbtContentTypeAdapter;
import dev.sbs.minecraftapi.skyblock.gson.SkyBlockDateTypeAdapter;
import dev.sbs.minecraftapi.text.segment.ColorSegment;
import dev.sbs.minecraftapi.text.segment.LineSegment;
import dev.sbs.minecraftapi.text.segment.TextSegment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code MinecraftApi} is a non-instantiable extension of the {@link SimplifiedApi} utility class for managing and
 * accessing various managers, services, builders, and API clients used across the application.
 * <p>
 * This class centralizes the initialization and retrieval of dependent resources to ensure a simplified and
 * consistent interface for interacting with API components.
 * <ul>
 *     <li>Adds Minecraft and Hypixel related {@link Gson} configurations.</li>
 *     <li>Adds service support for {@link NbtFactory}.</li>
 *     <li>Adds JSON text support through {@link TextSegment}.</li>
 *     <li>Adds Mojang, Hypixel and Sbs client configurations.</li>
 *     <li>and more...</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MinecraftApi extends SimplifiedApi {

    static {
        // Update Gson
        GsonSettings gsonSettings = serviceManager.get(GsonSettings.class)
            .mutate()
            .withTypeAdapter(NbtContent.class, new NbtContentTypeAdapter())
            .withTypeAdapter(MojangMultiUsernameResponse.class, new MojangMultiUsernameResponse.Deserializer())
            .withTypeAdapter(SkyBlockDate.RealTime.class, new SkyBlockDateTypeAdapter.RealTime())
            .withTypeAdapter(SkyBlockDate.SkyBlockTime.class, new SkyBlockDateTypeAdapter.SkyBlockTime())
            .withTypeAdapter(SkyBlockEmojisResponse.class, new SkyBlockEmojisResponse.Deserializer())
            .withTypeAdapter(SkyBlockImagesResponse.class, new SkyBlockImagesResponse.Deserializer())
            .withTypeAdapter(SkyBlockItemsResponse.class, new SkyBlockItemsResponse.Deserializer())
            .build();
        serviceManager.update(GsonSettings.class, gsonSettings);
        serviceManager.update(Gson.class, gsonSettings.create());

        // Provide Services
        serviceManager.add(NbtFactory.class, new NbtFactory());

        // Provide Class Builders
        compilerManager.add(MojangApiEndpoints.class, MojangApiClient.class);
        compilerManager.add(MojangSessionEndpoints.class, MojangSessionClient.class);
        compilerManager.add(SbsEndpoints.class, SbsClient.class);
        compilerManager.add(HypixelEndpoints.class, HypixelClient.class);

        // Provide Builders
        builderManager.add(LineSegment.class, LineSegment.Builder.class);
        builderManager.add(ColorSegment.class, ColorSegment.Builder.class);
        builderManager.add(TextSegment.class, TextSegment.Builder.class);

        // Provide Api Clients
        MojangProxy mojangProxy = new MojangProxy();
        serviceManager.add(MojangProxy.class, mojangProxy);
        serviceManager.add(MojangApiClient.class, mojangProxy.getApiClient());
        serviceManager.add(MinecraftServicesClient.class, mojangProxy.getServicesClient());
        serviceManager.add(MojangSessionClient.class, mojangProxy.getSessionClient());
        serviceManager.add(SbsClient.class, new SbsClient());
        serviceManager.add(HypixelClient.class, new HypixelClient());
        serviceManager.add(MinecraftServerPing.class, new MinecraftServerPing());
    }

    public static @NotNull NbtFactory getNbtFactory() {
        return serviceManager.get(NbtFactory.class);
    }

    /**
     * Gets the {@link MojangProxy} used to interact with the Mojang API.
     */
    public static @NotNull MojangProxy getMojangProxy() {
        return serviceManager.get(MojangProxy.class);
    }

}
