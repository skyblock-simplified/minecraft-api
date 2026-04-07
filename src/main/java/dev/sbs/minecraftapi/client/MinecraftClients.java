package dev.sbs.minecraftapi.client;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.Gson;
import dev.sbs.minecraftapi.client.hypixel.exception.HypixelApiException;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelContract;
import dev.sbs.minecraftapi.client.hypixel.request.HypixelForumContract;
import dev.sbs.minecraftapi.client.hypixel.response.forum.HypixelForum;
import dev.sbs.minecraftapi.client.mojang.MojangDomain;
import dev.sbs.minecraftapi.client.mojang.exception.MojangApiException;
import dev.sbs.minecraftapi.client.mojang.request.MojangContract;
import dev.sbs.minecraftapi.client.sbs.exception.SbsApiException;
import dev.sbs.minecraftapi.client.sbs.request.SbsContract;
import dev.simplified.client.ClientOptions;
import dev.simplified.client.Proxy;
import dev.simplified.client.codec.RssTreeTransformers;
import dev.simplified.client.codec.XmlDecoder;
import dev.simplified.client.codec.XmlEncoder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Static factory holder that produces {@link ClientOptions} for every Minecraft/Hypixel API the
 * project talks to, plus the {@link Proxy} variants for APIs that need pooled IPv6 rotation.
 * <p>
 * Each factory takes the configured {@link Gson} instance (and any other runtime-supplied inputs
 * such as the Hypixel API key supplier) as parameters rather than reaching back into
 * {@link dev.sbs.minecraftapi.MinecraftApi MinecraftApi} statically. This avoids a circular
 * class-load dependency between {@code MinecraftApi}'s static initializer and the options it is
 * about to register.
 * <p>
 * Hypixel, SBS, and HypixelForum are addressed as direct {@code Client<*Contract>} instances.
 * Mojang is wrapped in a {@code Proxy<MojangContract>} so that callers can spread requests across
 * a pool of clients with rotating IPv6 source addresses to avoid per-IP rate limits.
 *
 * @see ClientOptions
 * @see Proxy
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MinecraftClients {

    /** Shared Jackson {@link XmlMapper} reused by every Hypixel-forum client constructed from these options. */
    private static final @NotNull XmlMapper FORUM_XML_MAPPER = XmlDecoder.defaultXmlMapper();

    /**
     * Builds the {@link ClientOptions} for the Hypixel Public API v2.
     * <p>
     * The {@code apiKeySupplier} is invoked at request time on every outbound call; returning
     * {@link Optional#empty()} omits the {@code API-Key} header for that request.
     *
     * @param gson the configured {@link Gson} instance
     * @param apiKeySupplier the dynamic supplier of the {@code API-Key} header value
     * @return immutable Hypixel client options
     */
    public static @NotNull ClientOptions<HypixelContract> hypixelOptions(@NotNull Gson gson, @NotNull Supplier<Optional<String>> apiKeySupplier) {
        return ClientOptions.builder(HypixelContract.class, gson)
            .withErrorDecoder((methodKey, response) -> { throw new HypixelApiException(methodKey, response); })
            .withDynamicHeader("API-Key", apiKeySupplier)
            .build();
    }

    /**
     * Builds the {@link ClientOptions} for the SBS Public API.
     *
     * @param gson the configured {@link Gson} instance
     * @return immutable SBS client options
     */
    public static @NotNull ClientOptions<SbsContract> sbsOptions(@NotNull Gson gson) {
        return ClientOptions.builder(SbsContract.class, gson)
            .withErrorDecoder((methodKey, response) -> { throw new SbsApiException(methodKey, response); })
            .build();
    }

    /**
     * Builds the {@link ClientOptions} for the Hypixel forum RSS feeds.
     * <p>
     * The XML codec pair routes responses through Jackson's {@link XmlMapper#readTree} into a
     * Gson tree before binding, applying {@link RssTreeTransformers#ATOM_SELF_LINK_COLLISION_FIX}
     * to fold the {@code <link>} / {@code <atom:link>} namespace collision so the DTO exposes
     * a scalar {@code link} field. The encoder is wired symmetrically via ROME's
     * {@link com.rometools.rome.io.SyndFeedOutput SyndFeedOutput} for completeness; today every
     * forum endpoint is read-only.
     *
     * @param gson the configured {@link Gson} instance
     * @return immutable Hypixel forum client options
     */
    public static @NotNull ClientOptions<HypixelForumContract> hypixelForumOptions(@NotNull Gson gson) {
        return ClientOptions.builder(HypixelForumContract.class, gson)
            .withDecoderFactory(g -> new XmlDecoder(g, FORUM_XML_MAPPER, RssTreeTransformers.ATOM_SELF_LINK_COLLISION_FIX))
            .withEncoderFactory(g -> XmlEncoder.of(HypixelForum.class, HypixelForum::toSyndFeed))
            .build();
    }

    /**
     * Builds the base {@link ClientOptions} for the Mojang/Minecraft contract.
     * <p>
     * Used as the seed for {@link #mojangProxy(Gson)} and {@link #mojangProxy(Gson, String)};
     * generally not registered as a single client because Mojang traffic should flow through the
     * proxy pool to support IPv6 rotation.
     *
     * @param gson the configured {@link Gson} instance
     * @return immutable Mojang client options
     */
    public static @NotNull ClientOptions<MojangContract> mojangOptions(@NotNull Gson gson) {
        return ClientOptions.builder(MojangContract.class, gson)
            .withErrorDecoder((methodKey, response) -> { throw new MojangApiException(methodKey, response); })
            .build();
    }

    /**
     * Builds a {@link Proxy} for the Mojang/Minecraft contract with no IPv6 source-address
     * rotation. All clients in the pool share the system default local address.
     * <p>
     * Availability is gated on
     * {@link MojangDomain#MINECRAFT_SERVICES MojangDomain.MINECRAFT_SERVICES} - the bucket
     * most callers care about - rather than the type-level default, because Mojang's contract
     * routes individual operations to several different domains.
     *
     * @param gson the configured {@link Gson} instance
     * @return a Mojang proxy without IPv6 rotation
     */
    public static @NotNull Proxy<MojangContract> mojangProxy(@NotNull Gson gson) {
        return Proxy.builder(mojangOptions(gson))
            .withAvailability(client -> !client.isRateLimited(MojangDomain.MINECRAFT_SERVICES))
            .build();
    }

    /**
     * Builds a {@link Proxy} for the Mojang/Minecraft contract with IPv6 source-address rotation
     * across the given CIDR network prefix.
     * <p>
     * Each new client added to the pool gets a fresh randomized IPv6 local address derived from
     * the CIDR prefix. Combined with the per-domain availability gate, this lets callers spread
     * Mojang traffic across many source addresses to avoid per-IP rate limits.
     *
     * @param gson the configured {@link Gson} instance
     * @param cidrPrefix an IPv6 network prefix in CIDR notation (e.g. {@code "2000:444:33ff::/48"})
     * @return a Mojang proxy with IPv6 rotation
     * @see Proxy.Builder#withInet6Rotation(String)
     */
    public static @NotNull Proxy<MojangContract> mojangProxy(@NotNull Gson gson, @NotNull String cidrPrefix) {
        return Proxy.builder(mojangOptions(gson))
            .withInet6Rotation(cidrPrefix)
            .withAvailability(client -> !client.isRateLimited(MojangDomain.MINECRAFT_SERVICES))
            .build();
    }

}
