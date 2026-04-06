package dev.sbs.minecraftapi;

import com.google.gson.Gson;
import dev.sbs.minecraftapi.asset.MinecraftAssetFactory;
import dev.sbs.minecraftapi.asset.MinecraftAssetOptions;
import dev.sbs.minecraftapi.asset.context.AssetContext;
import dev.sbs.minecraftapi.asset.context.VanillaContext;
import dev.sbs.minecraftapi.asset.texture.TextureReference;
import dev.sbs.minecraftapi.client.hypixel.HypixelClient;
import dev.sbs.minecraftapi.client.mojang.MojangClient;
import dev.sbs.minecraftapi.client.mojang.MojangProxy;
import dev.sbs.minecraftapi.client.mojang.request.MinecraftServerPing;
import dev.sbs.minecraftapi.client.mojang.response.MojangMultiUsername;
import dev.sbs.minecraftapi.client.sbs.SbsClient;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojis;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockImages;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockItems;
import dev.sbs.minecraftapi.nbt.NbtFactory;
import dev.sbs.minecraftapi.persistence.SkyBlockFactory;
import dev.sbs.minecraftapi.persistence.model.Item;
import dev.sbs.minecraftapi.skyblock.common.NbtContent;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.client.Client;
import dev.simplified.client.request.Endpoint;
import dev.simplified.gson.GsonSettings;
import dev.simplified.image.ImageFactory;
import dev.simplified.manager.KeyManager;
import dev.simplified.manager.Manager;
import dev.simplified.manager.ServiceManager;
import dev.simplified.persistence.CacheMissingStrategy;
import dev.simplified.persistence.JpaConfig;
import dev.simplified.persistence.JpaExclusionStrategy;
import dev.simplified.persistence.JpaModel;
import dev.simplified.persistence.JpaSession;
import dev.simplified.persistence.Repository;
import dev.simplified.persistence.SessionManager;
import dev.simplified.persistence.driver.H2MemoryDriver;
import dev.simplified.scheduler.Scheduler;
import dev.simplified.util.Logging;
import dev.simplified.util.SystemUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Non-instantiable central service locator that bootstraps and exposes the core
 * infrastructure shared across all Minecraft- and Hypixel-related modules: managers,
 * serialization, scheduling, persistence, Feign clients, and an H2-backed JPA session
 * for JSON-sourced game models.
 * <p>
 * All bootstrapping happens in a single static initializer block:
 * <ul>
 *     <li>Configures a {@link Gson} instance with the {@link JpaExclusionStrategy}
 *         and Minecraft/Hypixel type adapters for {@link NbtContent},
 *         {@link MojangMultiUsername}, {@link SkyBlockDate.RealTime},
 *         {@link SkyBlockDate.SkyBlockTime}, {@link SkyBlockEmojis},
 *         {@link SkyBlockImages}, {@link TextureReference}, and {@link SkyBlockItems}.</li>
 *     <li>Registers a {@link Scheduler} for asynchronous and recurring tasks.</li>
 *     <li>Registers a {@link SessionManager} for managing JPA database sessions.</li>
 *     <li>Registers an {@link ImageFactory} for reading and writing images.</li>
 *     <li>Registers an {@link NbtFactory} for reading and writing Minecraft NBT data.</li>
 *     <li>Registers {@link MojangProxy} (with IPv6 rotation), {@link SbsClient},
 *         {@link HypixelClient}, and {@link MinecraftServerPing} clients.</li>
 *     <li>Connects an H2 in-memory JPA session that loads JSON model files from
 *         the {@code skyblock/} classpath resource directory.</li>
 * </ul>
 *
 * <p>Two static managers are exposed for registration and lookup:
 * <ul>
 *     <li>{@link KeyManager} - string key-value registry for API keys and tokens.</li>
 *     <li>{@link ServiceManager} - class-keyed singleton locator.</li>
 * </ul>
 *
 * <p>Typical access patterns:
 * <pre>{@code
 * MinecraftApi.getClient(HypixelClient.class);
 * MinecraftApi.getRepository(Item.class);
 * MinecraftApi.getNbtFactory();
 * MinecraftApi.getMojangProxy();
 * MinecraftApi.getKeyManager().add("HYPIXEL_API_KEY", value);
 * }</pre>
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MinecraftApi {

    /**
     * Global string key-value registry for API keys, tokens, and other named configuration values.
     * <p>
     * Uses case-insensitive key matching and {@link Manager.Mode#UPDATE} mode, allowing
     * both registration and replacement of entries.
     */
    @Getter protected static final @NotNull KeyManager keyManager = new KeyManager((entry, key) -> key.equalsIgnoreCase(entry.getKey()), Manager.Mode.UPDATE);

    /**
     * Global service locator for singleton instances indexed by their class type.
     * <p>
     * Pre-populated in the static initializer with {@link Gson}, {@link GsonSettings},
     * {@link Scheduler}, {@link SessionManager}, {@link ImageFactory}, {@link NbtFactory},
     * {@link MojangProxy}, {@link SbsClient}, {@link HypixelClient}, and
     * {@link MinecraftServerPing}. Uses {@link Manager.Mode#UPDATE} mode, allowing both
     * registration and replacement of services.
     */
    @Getter protected static final @NotNull ServiceManager serviceManager = new ServiceManager(Manager.Mode.UPDATE);

    static {
        // Configure Gson (base exclusion + Minecraft/Hypixel adapters in one build)
        GsonSettings gsonSettings = GsonSettings.defaults()
            .mutate()
            .withExclusionStrategies(JpaExclusionStrategy.INSTANCE)
            .withTypeAdapter(NbtContent.class, new NbtContent.Adapter())
            .withTypeAdapter(MojangMultiUsername.class, new MojangMultiUsername.Deserializer())
            .withTypeAdapter(SkyBlockDate.RealTime.class, new SkyBlockDate.RealTime.Adapter())
            .withTypeAdapter(SkyBlockDate.SkyBlockTime.class, new SkyBlockDate.SkyBlockTime.Adapter())
            .withTypeAdapter(SkyBlockEmojis.class, new SkyBlockEmojis.Deserializer())
            .withTypeAdapter(SkyBlockImages.class, new SkyBlockImages.Deserializer())
            .withTypeAdapter(TextureReference.class, new TextureReference.Adapter())
            .withTypeAdapter(SkyBlockItems.class, new SkyBlockItems.Deserializer())
            .build();
        serviceManager.add(GsonSettings.class, gsonSettings);
        serviceManager.add(Gson.class, gsonSettings.create());

        // Provide Core Services
        serviceManager.add(Scheduler.class, new Scheduler());
        serviceManager.add(SessionManager.class, new SessionManager());
        serviceManager.add(ImageFactory.class, new ImageFactory());
        serviceManager.add(NbtFactory.class, new NbtFactory());

        // Provide Api Clients
        serviceManager.add(MojangProxy.class, new MojangProxy());
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
     * @param <T> the endpoint type the client operates on
     * @param <A> the client type
     * @return the registered client instance
     */
    public static <T extends Endpoint, A extends Client<T>> @NotNull A getClient(@NotNull Class<A> tClass) {
        return serviceManager.get(tClass);
    }

    /**
     * Returns the directory containing the running application's JAR or class files.
     *
     * @return the parent directory of this class's code source location
     * @see SystemUtil#getCurrentDirectory()
     */
    public static @NotNull File getCurrentDirectory() {
        return SystemUtil.getCurrentDirectory();
    }

    /**
     * Returns the globally configured {@link Gson} instance, which carries the
     * Minecraft/Hypixel type adapters for {@link NbtContent}, {@link SkyBlockDate},
     * and SBS API response types alongside the {@link JpaExclusionStrategy}.
     *
     * @return the shared {@link Gson} instance registered in the {@link #serviceManager}
     */
    public static @NotNull Gson getGson() {
        return serviceManager.get(Gson.class);
    }

    /**
     * Returns the global {@link ImageFactory} for reading and writing images across all supported formats.
     *
     * @return the shared {@link ImageFactory} instance registered in the {@link #serviceManager}
     */
    public static @NotNull ImageFactory getImageFactory() {
        return serviceManager.get(ImageFactory.class);
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
     * @param <T> the entity type
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
     * Returns the global {@link SessionManager} that manages all active {@link JpaSession}
     * instances, including the H2 session for JSON-backed models bootstrapped by this class.
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
                .withLogLevel(Logging.Level.WARN)
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
            .withLogLevel(Logging.Level.WARN)
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
