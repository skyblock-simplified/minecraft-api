package dev.sbs.minecraftapi;

import com.google.gson.Gson;
import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.client.Client;
import dev.sbs.api.client.request.Endpoint;
import dev.sbs.api.io.gson.GsonSettings;
import dev.sbs.api.persistence.CacheMissingStrategy;
import dev.sbs.api.persistence.JpaConfig;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.Repository;
import dev.sbs.api.persistence.SessionManager;
import dev.sbs.api.persistence.driver.H2MemoryDriver;
import dev.sbs.api.scheduler.Scheduler;
import dev.sbs.api.util.builder.ClassBuilder;
import dev.sbs.minecraftapi.asset.MinecraftAssetFactory;
import dev.sbs.minecraftapi.asset.MinecraftAssetOptions;
import dev.sbs.minecraftapi.asset.context.AssetContext;
import dev.sbs.minecraftapi.asset.context.VanillaContext;
import dev.sbs.minecraftapi.asset.texture.TextureReference;
import dev.sbs.minecraftapi.client.hypixel.HypixelClient;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelEndpoint;
import dev.sbs.minecraftapi.client.mojang.MojangClient;
import dev.sbs.minecraftapi.client.mojang.MojangProxy;
import dev.sbs.minecraftapi.client.mojang.request.MinecraftServerPing;
import dev.sbs.minecraftapi.client.mojang.request.MojangEndpoint;
import dev.sbs.minecraftapi.client.mojang.response.MojangMultiUsername;
import dev.sbs.minecraftapi.client.sbs.SbsClient;
import dev.sbs.minecraftapi.client.sbs.request.SbsEndpoint;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojis;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockImages;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockItems;
import dev.sbs.minecraftapi.generator.text.segment.ColorSegment;
import dev.sbs.minecraftapi.generator.text.segment.LineSegment;
import dev.sbs.minecraftapi.generator.text.segment.TextSegment;
import dev.sbs.minecraftapi.nbt.NbtFactory;
import dev.sbs.minecraftapi.persistence.SkyBlockFactory;
import dev.sbs.minecraftapi.persistence.model.Item;
import dev.sbs.minecraftapi.skyblock.common.NbtContent;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Level;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Minecraft- and Hypixel-specific extension of {@link SimplifiedApi} that bootstraps all
 * game-related services, Feign clients, Gson adapters, and an H2-backed JPA session for
 * JSON-sourced models.
 * <p>
 * All bootstrapping happens in a static initializer block:
 * <ul>
 *     <li>Registers Minecraft/Hypixel {@link Gson} type adapters for NBT content,
 *         SkyBlock dates, and SBS API response types.</li>
 *     <li>Registers {@link NbtFactory} as a service for reading/writing NBT data.</li>
 *     <li>Registers {@link ClassBuilder ClassBuilder} entries for
 *         {@link SbsClient}, {@link MojangClient}, {@link HypixelClient}, and text segment types.</li>
 *     <li>Instantiates and registers the {@link MojangProxy} (with IPv6 rotation),
 *         {@link SbsClient}, {@link HypixelClient}, and {@link MinecraftServerPing} clients.</li>
 *     <li>Connects an H2 in-memory JPA session that loads JSON model files from
 *         the {@code skyblock/} classpath resource directory.</li>
 * </ul>
 *
 * <p>Typical access patterns:
 * <pre>{@code
 * MinecraftApi.getClient(HypixelClient.class);
 * MinecraftApi.getRepository(Item.class);
 * MinecraftApi.getNbtFactory();
 * MinecraftApi.getMojangProxy();
 * }</pre>
 *
 * @see SimplifiedApi
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MinecraftApi extends SimplifiedApi {

    static {
        // Update Gson
        GsonSettings gsonSettings = serviceManager.get(GsonSettings.class)
            .mutate()
            .withTypeAdapter(NbtContent.class, new NbtContent.Adapter())
            .withTypeAdapter(MojangMultiUsername.class, new MojangMultiUsername.Deserializer())
            .withTypeAdapter(SkyBlockDate.RealTime.class, new SkyBlockDate.RealTime.Adapter())
            .withTypeAdapter(SkyBlockDate.SkyBlockTime.class, new SkyBlockDate.SkyBlockTime.Adapter())
            .withTypeAdapter(SkyBlockEmojis.class, new SkyBlockEmojis.Deserializer())
            .withTypeAdapter(SkyBlockImages.class, new SkyBlockImages.Deserializer())
            .withTypeAdapter(TextureReference.class, new TextureReference.Adapter())
            .withTypeAdapter(SkyBlockItems.class, new SkyBlockItems.Deserializer())
            .build();
        serviceManager.update(GsonSettings.class, gsonSettings);
        serviceManager.update(Gson.class, gsonSettings.create());

        // Provide Services
        serviceManager.add(NbtFactory.class, new NbtFactory());

        // Provide Builders
        builderManager.add(SbsEndpoint.class, SbsClient.class);
        builderManager.add(MojangEndpoint.class, MojangClient.class);
        builderManager.add(HypixelEndpoint.class, HypixelClient.class);
        builderManager.add(LineSegment.class, LineSegment.Builder.class);
        builderManager.add(ColorSegment.class, ColorSegment.Builder.class);
        builderManager.add(TextSegment.class, TextSegment.Builder.class);

        // Provide Api Clients
        MojangProxy mojangProxy = new MojangProxy();
        serviceManager.add(MojangProxy.class, mojangProxy);
        serviceManager.add(MojangClient.class, mojangProxy.getApiClient());
        serviceManager.add(SbsClient.class, new SbsClient());
        serviceManager.add(HypixelClient.class, new HypixelClient());
        serviceManager.add(MinecraftServerPing.class, new MinecraftServerPing());

        // Provide Json Persistence (H2-backed JPA session)
        connectSkyBlockSession();
    }

    /**
     * Retrieves a registered {@link Client} instance for the given client class.
     * <p>
     * Available clients include {@link HypixelClient}, {@link MojangClient},
     * {@link SbsClient}, and {@link MinecraftServerPing}.
     *
     * @param tClass the client class to look up in the {@link #serviceManager}
     * @param <T>    the endpoint type the client operates on
     * @param <A>    the client type
     * @return the registered client instance
     */
    public static <T extends Endpoint, A extends Client<T>> @NotNull A getClient(@NotNull Class<A> tClass) {
        return serviceManager.get(tClass);
    }

    /**
     * Returns the directory containing the running application's JAR or class files.
     *
     * @return the parent directory of this class's code source location
     */
    @SneakyThrows
    public static @NotNull File getCurrentDirectory() {
        return new File(SimplifiedApi.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
    }

    /**
     * Returns the globally configured {@link Gson} instance, which includes
     * Minecraft-specific type adapters for {@link NbtContent}, {@link SkyBlockDate},
     * and SBS API response types in addition to the base adapters from {@link SimplifiedApi}.
     *
     * @return the shared {@link Gson} instance registered in the {@link #serviceManager}
     */
    public static @NotNull Gson getGson() {
        return serviceManager.get(Gson.class);
    }

    /**
     * Returns the {@link MojangProxy} that manages a pool of {@link MojangClient} instances
     * with IPv6 rotation to avoid Mojang API rate limits.
     *
     * @return the shared {@link MojangProxy} instance
     */
    public static @NotNull MojangProxy getMojangProxy() {
        return serviceManager.get(MojangProxy.class);
    }

    /**
     * Returns the {@link NbtFactory} for reading and writing Minecraft NBT data
     * in multiple formats (Base64, byte arrays, files, streams, SNBT, and JSON).
     *
     * @return the shared {@link NbtFactory} instance
     */
    public static @NotNull NbtFactory getNbtFactory() {
        return serviceManager.get(NbtFactory.class);
    }

    /**
     * Retrieves the {@link Repository} that caches all entities of the given model type.
     * <p>
     * For JSON-backed models (e.g. {@link Item}), entities are loaded from classpath
     * JSON resources in the {@code skyblock/} directory into an embedded H2 database.
     *
     * @param tClass the {@link JpaModel} class to find a repository for
     * @param <T>    the entity type
     * @return the repository caching entities of type {@code T}
     */
    public static <T extends JpaModel> @NotNull Repository<T> getRepository(@NotNull Class<T> tClass) {
        return getSessionManager().getRepository(tClass);
    }

    /**
     * Returns the global {@link Scheduler} for executing asynchronous and recurring tasks.
     *
     * @return the shared {@link Scheduler} instance registered in the {@link #serviceManager}
     */
    public static @NotNull Scheduler getScheduler() {
        return serviceManager.get(Scheduler.class);
    }

    /**
     * Returns the global {@link SessionManager} that manages all active
     * {@link dev.sbs.api.persistence.JpaSession JpaSession} instances, including the
     * H2 session for JSON-backed models bootstrapped by this class.
     *
     * @return the shared {@link SessionManager} instance registered in the {@link #serviceManager}
     */
    public static @NotNull SessionManager getSessionManager() {
        return serviceManager.get(SessionManager.class);
    }

    public static void connectSkyBlockSession() {
        getSessionManager().connect(
            JpaConfig.builder()
                .withDriver(new H2MemoryDriver())
                .withSchema("skyblock")
                .withRepositoryFactory(new SkyBlockFactory())
                .withGsonSettings(
                    getServiceManager()
                        .get(GsonSettings.class)
                        .mutate()
                        .withStringType(GsonSettings.StringType.DEFAULT)
                        .build()
                )
                .withLogLevel(Level.WARN)
                .isUsingQueryCache()
                .isUsing2ndLevelCache()
                .withCacheConcurrencyStrategy(CacheConcurrencyStrategy.READ_WRITE)
                .withCacheMissingStrategy(CacheMissingStrategy.CREATE_WARN)
                .withQueryResultsTTL(30)
                .build()
        );
    }

    // ---- Asset loading ----

    /**
     * Loads Minecraft assets using the given options, connects the H2 asset session, and
     * registers the factory and asset context in the service manager.
     *
     * @param options the asset loading configuration
     * @return the initialized asset factory
     * @throws IOException if asset loading fails
     */
    public static @NotNull MinecraftAssetFactory loadAssets(@NotNull MinecraftAssetOptions options) throws IOException {
        MinecraftAssetFactory factory = MinecraftAssetFactory.initialize(options);

        serviceManager.put(MinecraftAssetFactory.class, factory);
        AssetContext assetContext = VanillaContext.fromFactory(factory);
        serviceManager.put(AssetContext.class, assetContext);

        GsonSettings gsonSettings = serviceManager.get(GsonSettings.class);
        JpaConfig assetJpaConfig = JpaConfig.builder()
            .withDriver(new H2MemoryDriver())
            .withSchema("assets")
            .withRepositoryFactory(factory)
            .withGsonSettings(
                gsonSettings.mutate()
                    .withStringType(GsonSettings.StringType.DEFAULT)
                    .build()
            )
            .withLogLevel(org.apache.logging.log4j.Level.WARN)
            .isUsingQueryCache()
            .isUsing2ndLevelCache()
            .withCacheConcurrencyStrategy(org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
            .withCacheMissingStrategy(CacheMissingStrategy.CREATE_WARN)
            .withQueryResultsTTL(30)
            .build();

        factory.setAssetJpaConfig(assetJpaConfig);
        getSessionManager().connect(assetJpaConfig);

        return factory;
    }

    /**
     * Returns the registered asset factory instance.
     *
     * @return the asset factory
     */
    public static @NotNull MinecraftAssetFactory getAssetFactory() {
        return serviceManager.get(MinecraftAssetFactory.class);
    }

}
