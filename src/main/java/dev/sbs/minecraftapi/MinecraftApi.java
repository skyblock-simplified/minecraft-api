package dev.sbs.minecraftapi;

import com.google.gson.Gson;
import dev.sbs.minecraftapi.asset.MinecraftAssetFactory;
import dev.sbs.minecraftapi.asset.MinecraftAssetOptions;
import dev.sbs.minecraftapi.asset.context.AssetContext;
import dev.sbs.minecraftapi.asset.context.VanillaContext;
import dev.sbs.minecraftapi.asset.texture.TextureReference;
import dev.sbs.minecraftapi.client.MinecraftClients;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelContract;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelForumContract;
import dev.sbs.minecraftapi.client.mojang.request.MinecraftServerPing;
import dev.sbs.minecraftapi.client.mojang.request.MojangContract;
import dev.sbs.minecraftapi.client.mojang.response.MojangMultiUsername;
import dev.sbs.minecraftapi.client.sbs.request.SbsContract;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockEmojis;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockImages;
import dev.sbs.minecraftapi.client.sbs.response.SkyBlockItems;
import dev.sbs.minecraftapi.nbt.NbtFactory;
import dev.sbs.minecraftapi.persistence.SkyBlockFactory;
import dev.sbs.minecraftapi.persistence.model.Item;
import dev.sbs.minecraftapi.skyblock.common.NbtContent;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.client.Client;
import dev.simplified.client.Proxy;
import dev.simplified.client.request.Contract;
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
 * Non-instantiable central service locator that bootstraps and exposes the core infrastructure
 * shared across all Minecraft- and Hypixel-related modules: managers, serialization, scheduling,
 * persistence, Feign clients, and an H2-backed JPA session for JSON-sourced game models.
 * <p>
 * All bootstrapping happens in a single static initializer block:
 * <ul>
 *     <li>Configures a {@link Gson} instance with the {@link JpaExclusionStrategy} and
 *         Minecraft/Hypixel type adapters for {@link NbtContent}, {@link MojangMultiUsername},
 *         {@link SkyBlockDate.RealTime}, {@link SkyBlockDate.SkyBlockTime},
 *         {@link SkyBlockEmojis}, {@link SkyBlockImages}, {@link TextureReference}, and
 *         {@link SkyBlockItems}.</li>
 *     <li>Registers a {@link Scheduler} for asynchronous and recurring tasks.</li>
 *     <li>Registers a {@link SessionManager} for managing JPA database sessions.</li>
 *     <li>Registers an {@link ImageFactory} for reading and writing images.</li>
 *     <li>Registers an {@link NbtFactory} for reading and writing Minecraft NBT data.</li>
 *     <li>Registers a {@link Client} for each of {@link SbsContract}, {@link HypixelContract},
 *         and {@link HypixelForumContract}, plus a {@link Proxy} for {@link MojangContract} and
 *         a {@link MinecraftServerPing} utility.</li>
 *     <li>Connects an H2 in-memory JPA session that loads JSON model files from the
 *         {@code skyblock/} classpath resource directory.</li>
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
 * MinecraftApi.getClient(HypixelContract.class);
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
     * a {@link Client} for each registered {@link Contract}, a {@link Proxy} for
     * {@link MojangContract}, and {@link MinecraftServerPing}. Uses
     * {@link Manager.Mode#UPDATE} mode, allowing both registration and replacement of services.
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
        Gson gson = gsonSettings.create();
        serviceManager.add(Gson.class, gson);

        // Provide Core Services
        serviceManager.add(Scheduler.class, new Scheduler());
        serviceManager.add(SessionManager.class, new SessionManager());
        serviceManager.add(ImageFactory.class, new ImageFactory());
        serviceManager.add(NbtFactory.class, new NbtFactory());

        // Provide Api Clients (keyed by contract class)
        registerContract(SbsContract.class, Client.create(MinecraftClients.sbsOptions(gson)));
        registerContract(
            HypixelContract.class,
            Client.create(MinecraftClients.hypixelOptions(gson, keyManager.getSupplier("HYPIXEL_API_KEY")))
        );
        registerContract(HypixelForumContract.class, Client.create(MinecraftClients.hypixelForumOptions(gson)));
        registerContract(MojangContract.class, MinecraftClients.mojangProxy(gson));
        serviceManager.add(MinecraftServerPing.class, new MinecraftServerPing());

        // Provide Json Persistence (H2-backed JPA session)
        connectSkyBlockSession();
    }

    /**
     * Stores the given client or proxy under its contract class as the service-manager key.
     * <p>
     * The {@link ServiceManager} signature requires {@code Class<T>} to match the value type, but
     * we want to use the {@link Contract} interface as the lookup key for both {@link Client} and
     * {@link Proxy} values. The cast is unchecked but consistent: every registration goes through
     * this method, and every lookup goes through {@link #getClient(Class)} or
     * {@link #getProxy(Class)}, both of which apply the inverse cast.
     *
     * @param contractClass the contract interface class used as the key
     * @param instance the client or proxy instance to register
     * @param <T> the contract type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <T extends Contract> void registerContract(@NotNull Class<T> contractClass, @NotNull Object instance) {
        serviceManager.add((Class) contractClass, instance);
    }

    /**
     * Retrieves the registered {@link Client} for the given contract class.
     * <p>
     * Throws {@link ClassCastException} at the call site if the contract is registered as a
     * {@link Proxy} (e.g. {@link MojangContract}); use {@link #getProxy(Class)} for those.
     *
     * @param contractClass the contract interface class to look up
     * @param <T> the contract type
     * @return the registered client
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T extends Contract> @NotNull Client<T> getClient(@NotNull Class<T> contractClass) {
        return (Client<T>) serviceManager.get((Class) contractClass);
    }

    /**
     * Retrieves the registered {@link Proxy} for the given contract class.
     * <p>
     * Throws {@link ClassCastException} at the call site if the contract is registered as a
     * direct {@link Client}; use {@link #getClient(Class)} for those.
     *
     * @param contractClass the contract interface class to look up
     * @param <T> the contract type
     * @return the registered proxy
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T extends Contract> @NotNull Proxy<T> getProxy(@NotNull Class<T> contractClass) {
        return (Proxy<T>) serviceManager.get((Class) contractClass);
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
     * Returns the {@link Proxy} that manages a pool of Mojang {@link Client} instances with
     * optional IPv6 rotation to avoid Mojang API rate limits.
     * <p>
     * The pool's IPv6 rotation prefix is configured via {@link #setInet6NetworkPrefix(String)}
     * at startup; without it, the proxy still serves requests from a single client bound to the
     * system default local address.
     *
     * @return the shared Mojang proxy instance
     */
    public static @NotNull Proxy<MojangContract> getMojangProxy() {
        return getProxy(MojangContract.class);
    }

    /**
     * Replaces the registered Mojang {@link Proxy} with one that rotates outbound source
     * addresses across the given IPv6 CIDR prefix.
     * <p>
     * Constructs a fresh proxy via {@link MinecraftClients#mojangProxy(Gson, String)} and stores
     * it under {@link MojangContract} in the {@link #serviceManager}, replacing any previously
     * registered Mojang proxy. Intended to be called once at application startup once the
     * runtime IPv6 prefix is known.
     *
     * @param cidrPrefix an IPv6 network prefix in CIDR notation (e.g. {@code "2000:444:33ff::/48"})
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void setInet6NetworkPrefix(@NotNull String cidrPrefix) {
        Proxy<MojangContract> proxy = MinecraftClients.mojangProxy(getGson(), cidrPrefix);
        serviceManager.put((Class) MojangContract.class, proxy);
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
