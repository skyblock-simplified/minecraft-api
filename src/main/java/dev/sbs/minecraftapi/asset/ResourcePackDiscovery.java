package dev.sbs.minecraftapi.asset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.asset.model.ResourcePack;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.util.StringUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Non-instantiable utility for discovering and validating resource packs from the filesystem.
 *
 * <p>
 * Scans directories for {@code meta.json} descriptors, resolves namespace roots, computes
 * content fingerprints, and returns fully populated {@link ResourcePack} entities ready for
 * JPA persistence.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourcePackDiscovery {

    /**
     * Discovers resource packs from each of the given directories. Each directory must contain
     * a valid {@code meta.json} and an {@code assets/minecraft} namespace root.
     *
     * @param directories directories to scan for resource packs
     * @return a list of discovered resource packs, deduplicated by ID
     * @throws IOException if any directory fails to load as a valid resource pack
     */
    public static @NotNull ConcurrentList<ResourcePack> discoverPacks(@NotNull Iterable<String> directories) throws IOException {
        ConcurrentMap<String, ResourcePack> seen = Concurrent.newMap();

        for (String dir : directories) {
            ResourcePack pack = discoverPack(dir);
            seen.putIfAbsent(pack.getId().toLowerCase(), pack);
        }

        return Concurrent.newList(seen.values());
    }

    /**
     * Discovers all resource packs under the specified root directory.
     * Directories without a {@code meta.json} file are silently ignored, as are
     * candidates that fail to load as valid packs.
     *
     * @param rootDir root directory containing one or more texture pack folders
     * @param recursive when true, searches all subdirectories; otherwise only immediate children
     * @return a list of discovered resource packs
     */
    public static @NotNull ConcurrentList<ResourcePack> discoverAllPacks(
        @NotNull String rootDir,
        boolean recursive
    ) {
        if (StringUtil.isEmpty(rootDir))
            return Concurrent.newList();

        Path fullRoot;

        try {
            fullRoot = Path.of(rootDir).toAbsolutePath().normalize();
        } catch (IllegalArgumentException | SecurityException e) {
            return Concurrent.newList();
        }

        if (!Files.isDirectory(fullRoot))
            return Concurrent.newList();

        // Collect candidate directories that contain meta.json
        List<String> candidateDirs = new ArrayList<>();

        if (Files.exists(fullRoot.resolve("meta.json")))
            candidateDirs.add(fullRoot.toString());

        try {
            List<Path> children;

            if (recursive) {
                try (Stream<Path> walk = Files.walk(fullRoot, FileVisitOption.FOLLOW_LINKS)) {
                    children = walk
                        .filter(p -> !p.equals(fullRoot))
                        .filter(Files::isDirectory)
                        .collect(Collectors.toList());
                }
            } else {
                children = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(fullRoot, Files::isDirectory)) {
                    stream.forEach(children::add);
                }
            }

            for (Path child : children) {
                try {
                    if (Files.exists(child.resolve("meta.json")))
                        candidateDirs.add(child.toString());
                } catch (SecurityException ignored) {
                    // Expected: not every candidate is accessible
                }
            }
        } catch (IOException | SecurityException ignored) {
            // Unable to enumerate directories; return whatever we found so far
        }

        ConcurrentMap<String, ResourcePack> results = Concurrent.newMap();

        for (String dir : candidateDirs) {
            try {
                ResourcePack pack = discoverPack(dir);
                results.putIfAbsent(pack.getId().toLowerCase(), pack);
            } catch (Exception ignored) {
                // Expected: not every candidate is a valid pack
            }
        }

        return Concurrent.newList(results.values());
    }

    /**
     * Discovers a single resource pack from the specified directory.
     *
     * @param directory path to the texture pack directory
     * @return the discovered resource pack
     * @throws IllegalArgumentException if directory is null or blank
     * @throws IOException if the directory does not exist or cannot be read
     * @throws IllegalStateException if the meta.json is missing or invalid
     */
    public static @NotNull ResourcePack discoverPack(@NotNull String directory) throws IOException {
        if (StringUtil.isEmpty(directory))
            throw new IllegalArgumentException("Directory path must not be null or blank");

        Path fullPath = Path.of(directory).toAbsolutePath().normalize();

        if (!Files.isDirectory(fullPath))
            throw new IOException("Texture pack directory not found: '%s'".formatted(directory));

        Path metaPath = fullPath.resolve("meta.json");

        if (!Files.exists(metaPath))
            throw new IOException("Texture pack meta.json file not found: '%s'".formatted(metaPath));

        ResourcePack.Meta meta = loadMeta(metaPath);
        if (meta.getId().isBlank())
            throw new IllegalStateException(
                "Texture pack '%s' is missing a valid 'id' field in meta.json".formatted(directory));

        Map<String, String> namespaceRoots = resolveNamespaceRoots(fullPath);
        String assetsPath = namespaceRoots.get("minecraft");
        if (assetsPath == null)
            throw new IOException(
                "Texture pack at '%s' does not contain an 'assets/minecraft' directory".formatted(fullPath));

        Integer packFormat = parsePackFormat(fullPath.resolve("pack.mcmeta"));
        boolean supportsCit = namespaceRoots.containsKey("cit")
            || Files.isDirectory(Path.of(assetsPath).resolve("optifine").resolve("cit"));

        Instant lastWriteTimeUtc = Files.getLastModifiedTime(fullPath).toInstant();
        long sizeBytes = namespaceRoots.values().stream()
            .mapToLong(ResourcePackDiscovery::calculateDirectorySize)
            .sum();

        String fingerprint = computeFingerprint(meta.getId(), meta.getVersion(), lastWriteTimeUtc, sizeBytes);

        ResourcePack pack = new ResourcePack();
        pack.setId(meta.getId());
        pack.setRootPath(fullPath.toString());
        pack.setAssetsPath(assetsPath);
        pack.setNamespaceRoots(Concurrent.newMap(namespaceRoots));
        pack.setMeta(meta);
        pack.setLastWriteTimeUtc(lastWriteTimeUtc.toEpochMilli());
        pack.setSizeBytes(sizeBytes);
        pack.setSupportsCit(supportsCit);
        pack.setPackFormat(packFormat);
        pack.setFingerprint(fingerprint);

        return pack;
    }

    // --- Private helpers ---

    private static @NotNull Map<String, String> resolveNamespaceRoots(@NotNull Path root) throws IOException {
        Path assetsRoot = root.resolve("assets");

        if (!Files.isDirectory(assetsRoot))
            throw new IOException("Texture pack at '%s' does not contain an 'assets' directory".formatted(root));

        Map<String, String> namespaces = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(assetsRoot, Files::isDirectory)) {
            for (Path directory : stream) {
                String name = directory.getFileName().toString();

                if (name.isBlank())
                    continue;

                Path normalizedPath = directory.toAbsolutePath().normalize();

                if (!namespaces.containsKey(name))
                    namespaces.put(name, normalizedPath.toString());
            }
        }

        if (!namespaces.containsKey("minecraft"))
            throw new IOException("Texture pack at '%s' does not contain an 'assets/minecraft' directory".formatted(root));

        // Normalize the minecraft path
        namespaces.compute("minecraft", (k, minecraftPath) -> Path.of(minecraftPath).toAbsolutePath().normalize().toString());

        return namespaces;
    }

    private static @Nullable Integer parsePackFormat(@NotNull Path packMcMetaPath) {
        try {
            String content = Files.readString(packMcMetaPath, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            JsonElement packElement = root.get("pack");

            if (packElement != null && packElement.isJsonObject()) {
                JsonElement formatElement = packElement.getAsJsonObject().get("pack_format");

                if (formatElement != null && formatElement.isJsonPrimitive()
                    && formatElement.getAsJsonPrimitive().isNumber()) {
                    return formatElement.getAsInt();
                }
            }
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            // Ignore malformed pack.mcmeta.
        }

        return null;
    }

    private static long calculateDirectorySize(@NotNull String path) {
        Path dir = Path.of(path);

        if (!Files.isDirectory(dir))
            return 0;

        long total = 0;

        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                try {
                    total += Files.size(file);
                } catch (IOException e) {
                    // Ignore files that disappear mid-enumeration.
                }
            }
        } catch (IOException e) {
            // Ignore enumeration failures.
        }

        return total;
    }

    private static @NotNull String computeFingerprint(
        @NotNull String id,
        @NotNull String version,
        @NotNull Instant lastWriteTimeUtc,
        long sizeBytes
    ) {
        String input = id + "|" + version + "|" + lastWriteTimeUtc + "|" + sizeBytes;
        return computeSha256(input);
    }

    public static @NotNull String computeSha256(@NotNull String input) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 algorithm not available", e);
        }
    }

    private static @NotNull ResourcePack.Meta loadMeta(@NotNull Path path) throws IOException {
        try (java.io.Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ResourcePack.Meta meta = MinecraftApi.getGson().fromJson(reader, ResourcePack.Meta.class);
            if (meta == null)
                throw new IllegalStateException("Failed to parse texture pack metadata from '%s'".formatted(path));
            return meta;
        }
    }
}
