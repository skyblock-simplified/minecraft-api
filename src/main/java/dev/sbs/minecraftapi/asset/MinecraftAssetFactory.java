package dev.sbs.minecraftapi.asset;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.math.Vector3f;
import dev.sbs.api.persistence.JpaConfig;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.RepositoryFactory;
import dev.sbs.api.persistence.source.Source;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.asset.model.BlockInfo;
import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.BlockModel.Element;
import dev.sbs.minecraftapi.asset.model.BlockModel.Face;
import dev.sbs.minecraftapi.asset.model.BlockModel.FaceData;
import dev.sbs.minecraftapi.asset.model.BlockModel.Rotation;
import dev.sbs.minecraftapi.asset.model.BlockModel.Transform;
import dev.sbs.minecraftapi.asset.model.ItemInfo;
import dev.sbs.minecraftapi.asset.model.ItemInfo.TintInfo;
import dev.sbs.minecraftapi.asset.model.ResourcePack;
import dev.sbs.minecraftapi.asset.model.TextureReference;
import dev.sbs.minecraftapi.asset.selector.ItemModelSelector;
import dev.sbs.minecraftapi.asset.selector.ItemModelSelectorParser;
import dev.sbs.minecraftapi.asset.texture.OverlayRoot;
import dev.sbs.minecraftapi.asset.texture.pack.TexturePackStack;
import dev.sbs.minecraftapi.client.mojang.response.PistonManifest;
import dev.sbs.minecraftapi.client.mojang.response.PistonMetadata;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Self-contained factory that downloads, extracts, and loads Minecraft assets into JPA models.
 *
 * <p>Handles the full lifecycle from Piston manifest resolution through JAR download, extraction,
 * JSON parsing, model inheritance resolution, and H2 persistence. Requires only a
 * {@link MinecraftAssetOptions} instance to configure version, directory, and texture pack paths.
 *
 * <p>All model definitions are eagerly resolved at initialization time, computing parent
 * inheritance chains so that consumers always receive fully-merged {@link BlockModel} instances.
 *
 * <p>Use the static {@link #initialize(MinecraftAssetOptions)} entry point to create a new factory.
 */
public final class MinecraftAssetFactory implements RepositoryFactory {

    private static final Gson GSON = MinecraftApi.getGson();

    private static final String[] PREFERRED_VARIANT_KEYS = {
        "", "inventory", "normal", "facing=north", "north=true",
        "axis=y", "half=lower", "type=bottom", "part=base"
    };

    private static final String[] TEXTURE_PREFERENCE_ORDER = {
        "all", "layer0", "texture", "side", "top", "bottom",
        "front", "back", "north", "south", "east", "west", "up", "down", "particle"
    };

    // --- Loaded data (RepositoryFactory contract) ---

    private final @NotNull ConcurrentList<Class<JpaModel>> models;
    private final @NotNull ConcurrentMap<Class<?>, Source<?>> sources;
    private final @NotNull ConcurrentMap<Class<?>, Consumer<?>> peeks;
    private final @NotNull AtomicReference<ConcurrentList<ResourcePack>> resourcePacksRef;

    // --- State ---

    @Getter private final @NotNull MinecraftAssetOptions options;
    @Getter private final @NotNull String assetsDirectory;
    @Getter private final @NotNull Map<String, BlockModel> resolvedModels;
    @Getter private final @NotNull AssetNamespaceRegistry assetNamespaces;
    @Getter private final @NotNull List<OverlayRoot> overlayRoots;
    @Getter private final @NotNull List<String> overlayPaths;
    @Setter private @Nullable JpaConfig assetJpaConfig;

    private MinecraftAssetFactory(
        @NotNull MinecraftAssetOptions options,
        @NotNull String assetsDirectory,
        @NotNull Map<String, BlockModel> resolvedModels,
        @NotNull ConcurrentList<BlockInfo> blockInfos,
        @NotNull ConcurrentList<ItemInfo> itemInfos,
        @NotNull ConcurrentList<ResourcePack> resourcePacks,
        @NotNull AssetNamespaceRegistry assetNamespaces,
        @NotNull List<OverlayRoot> overlayRoots,
        @NotNull List<String> overlayPaths
    ) {
        this.options = options;
        this.assetsDirectory = assetsDirectory;
        this.resolvedModels = resolvedModels;
        this.assetNamespaces = assetNamespaces;
        this.overlayRoots = overlayRoots;
        this.overlayPaths = overlayPaths;
        this.models = RepositoryFactory.resolveModels(BlockInfo.class);
        this.resourcePacksRef = new AtomicReference<>(resourcePacks);

        ConcurrentMap<Class<?>, Source<?>> srcMap = Concurrent.newMap();
        srcMap.put(BlockInfo.class, (Source<BlockInfo>) repo -> blockInfos);
        srcMap.put(ItemInfo.class, (Source<ItemInfo>) repo -> itemInfos);
        srcMap.put(BlockModel.class, (Source<BlockModel>) repo -> Concurrent.newList(resolvedModels.values()));
        srcMap.put(ResourcePack.class, (Source<ResourcePack>) repo -> this.resourcePacksRef.get());
        this.sources = srcMap.toUnmodifiableMap();

        ConcurrentMap<Class<?>, Consumer<?>> peekMap = Concurrent.newMap();
        peekMap.put(ItemInfo.class, (Consumer<ItemInfo>) itemInfo -> {
            ItemInfo original = itemInfos.findFirstOrNull(ItemInfo::getName, itemInfo.getName());
            if (original != null)
                itemInfo.setSelector(original.getSelector());
        });
        this.peeks = peekMap.toUnmodifiableMap();
    }

    // ================================================================
    // Public API
    // ================================================================

    /**
     * Loads assets from the given options, returning a fully initialized factory.
     *
     * <p>When {@link MinecraftAssetOptions#getAssetsDirectory()} is set, assets are loaded from that
     * pre-existing directory. Otherwise, the version is resolved (defaulting to latest) and assets
     * are downloaded and cached under {@link MinecraftAssetOptions#getCacheRoot()}.
     *
     * @param options the asset loading configuration
     * @return a fully loaded and resolved factory
     * @throws IOException if asset loading fails
     */
    public static @NotNull MinecraftAssetFactory initialize(@NotNull MinecraftAssetOptions options) throws IOException {
        String assetsDir;

        if (options.getAssetsDirectory() != null) {
            assetsDir = options.getAssetsDirectory();
        } else {
            String versionId = options.getVersionId();
            String resolvedVersion = versionId != null ? versionId : "latest";
            Path versionDir = options.getCacheRoot().resolve(resolvedVersion);

            if (Files.isDirectory(versionDir.resolve("minecraft")))
                assetsDir = versionDir.resolve("minecraft").toString();
            else
                assetsDir = downloadAndExtractAssets(versionId, versionDir).toString();
        }

        List<OverlayRoot> overlayRoots = OverlayRoot.discoverFromAssetsDirectory(assetsDir);
        AssetNamespaceRegistry namespaces = AssetNamespaceRegistry.buildFromRoots(assetsDir, overlayRoots);
        List<String> overlayPaths = OverlayRoot.extractUniquePaths(overlayRoots);

        Map<String, BlockModel> rawDefs = loadModelDefinitions(assetsDir, overlayPaths, namespaces);
        Map<String, BlockModel> resolved = resolveAllModels(rawDefs);
        ConcurrentList<BlockInfo> blockInfos = loadBlockInfos(assetsDir, rawDefs, overlayPaths, namespaces);
        ConcurrentList<ItemInfo> itemInfos = loadItemInfos(assetsDir, rawDefs, overlayPaths, namespaces);
        ConcurrentList<ResourcePack> resourcePacks = discoverResourcePacks(options.getTexturePackDirectories());

        return new MinecraftAssetFactory(
            options, assetsDir, resolved, blockInfos, itemInfos, resourcePacks,
            namespaces, overlayRoots, overlayPaths
        );
    }

    /**
     * Updates the resource packs held by this factory. The next time the H2 session
     * is reconnected, the new packs will be loaded into the ResourcePack repository.
     *
     * @param newPacks the updated list of discovered resource packs
     */
    public void updateResourcePacks(@NotNull ConcurrentList<ResourcePack> newPacks) {
        this.resourcePacksRef.set(newPacks);
    }

    /**
     * Loads a pack-specific snapshot of asset data by reading from disk with
     * the given pack stack's overlays applied.
     * <p>
     * The H2 database is not modified - it always contains vanilla data. The returned
     * snapshot is an immutable, self-contained set of pack-overridden models and textures
     * suitable for caching on a per-pack-stack basis.
     *
     * @param packIds the ordered list of pack identifiers
     * @return an immutable snapshot of pack-specific data
     * @throws IOException if asset loading fails
     */
    public @NotNull PackSnapshot loadPackSnapshot(@NotNull List<String> packIds) throws IOException {
        TexturePackStack packStack = TexturePackStack.buildPackStack(packIds);

        List<OverlayRoot> packOverlays = new ArrayList<>(overlayRoots);
        packOverlays.addAll(packStack.getOverlayRoots());
        AssetNamespaceRegistry packNamespaces = AssetNamespaceRegistry.buildFromRoots(
            assetsDirectory != null ? java.nio.file.Path.of(assetsDirectory).toAbsolutePath().toString() : "",
            packOverlays);
        List<String> packOverlayPaths = OverlayRoot.extractUniquePaths(packOverlays);

        Map<String, BlockModel> rawDefs = loadModelDefinitions(
            assetsDirectory, packOverlayPaths, packNamespaces);
        Map<String, BlockModel> resolved = resolveAllModels(rawDefs);
        ConcurrentList<BlockInfo> blockInfos = loadBlockInfos(
            assetsDirectory, rawDefs, packOverlayPaths, packNamespaces);
        ConcurrentList<ItemInfo> itemInfos = loadItemInfos(
            assetsDirectory, rawDefs, packOverlayPaths, packNamespaces);

        AssetContext packContext = AssetContext.create(assetsDirectory, overlayRoots, resolved, packStack);

        return new PackSnapshot(packContext, blockInfos, itemInfos);
    }

    /**
     * Rescans the configured texture pack directories and repopulates the ResourcePack
     * repository in the H2 assets session.
     *
     * @throws IOException if scanning fails
     */
    public void rescanResourcePacks() throws IOException {
        rescanResourcePacks(options.getTexturePackDirectories());
    }

    /**
     * Rescans the given directories for resource packs and repopulates the ResourcePack
     * repository in the H2 assets session.
     *
     * @param texturePackDirs directories to scan for resource packs
     * @throws IOException if scanning fails
     */
    public void rescanResourcePacks(@NotNull Iterable<String> texturePackDirs) throws IOException {
        ConcurrentList<ResourcePack> packs = discoverResourcePacks(texturePackDirs);
        updateResourcePacks(packs);
        if (assetJpaConfig != null) {
            MinecraftApi.getSessionManager().shutdown(assetJpaConfig);
            MinecraftApi.getSessionManager().connect(assetJpaConfig);
        }
    }

    // ================================================================
    // RepositoryFactory contract
    // ================================================================

    @Override
    public @NotNull ConcurrentList<Class<JpaModel>> getModels() {
        return this.models;
    }

    @Override
    public @NotNull ConcurrentMap<Class<?>, Source<?>> getSources() {
        return this.sources;
    }

    @Override
    public @NotNull ConcurrentMap<Class<?>, Consumer<?>> getPeeks() {
        return this.peeks;
    }

    // ================================================================
    // Model resolution (absorbed from BlockModelResolver)
    // ================================================================

    /**
     * Eagerly resolves all model definitions by computing parent inheritance chains.
     *
     * <p>Each raw definition is walked through its parent chain, merging textures, display
     * transforms, and elements from ancestors. Models with circular inheritance or missing
     * parents fall back to their unresolved definition.
     *
     * @param rawDefinitions the raw model name to definition map
     * @return a new map of model name to fully resolved {@link BlockModel}
     */
    public static @NotNull Map<String, BlockModel> resolveAllModels(@NotNull Map<String, BlockModel> rawDefinitions) {
        ConcurrentHashMap<String, BlockModel> definitions = new ConcurrentHashMap<>();

        for (Map.Entry<String, BlockModel> entry : rawDefinitions.entrySet())
            definitions.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());

        ConcurrentHashMap<String, BlockModel> cache = new ConcurrentHashMap<>();
        Map<String, BlockModel> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (String key : rawDefinitions.keySet()) {
            String normalized = BlockModel.normalizeName(key);
            String normalizedLower = normalized.toLowerCase(Locale.ROOT);

            if (!cache.containsKey(normalizedLower)) {
                try {
                    BlockModel resolved = resolveModelInternal(normalized, new HashSet<>(), definitions, cache);
                    result.put(key, resolved);
                } catch (Exception e) {
                    result.put(key, rawDefinitions.get(key));
                }
            } else {
                result.put(key, cache.get(normalizedLower));
            }
        }

        return result;
    }

    private static @NotNull BlockModel resolveModelInternal(
        @NotNull String name,
        @NotNull Set<String> stack,
        @NotNull ConcurrentHashMap<String, BlockModel> definitions,
        @NotNull ConcurrentHashMap<String, BlockModel> cache
    ) {
        if (stack.contains(name))
            throw new IllegalStateException("Detected circular model inheritance involving '%s'".formatted(name));

        BlockModel cached = cache.get(name.toLowerCase(Locale.ROOT));
        if (cached != null)
            return cached;

        BlockModel definition = definitions.get(name.toLowerCase(Locale.ROOT));
        if (definition == null)
            throw new java.util.NoSuchElementException("Model '%s' was not found in the loaded definitions".formatted(name));

        stack.add(name);

        ConcurrentList<String> parentChain = Concurrent.newList();
        Map<String, TextureReference> textures = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Transform> display = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ConcurrentList<Element> elements = null;

        String parentName = definition.getParent();
        if (parentName != null && !parentName.isBlank()) {
            BlockModel parent = resolveModelInternal(BlockModel.normalizeName(parentName), stack, definitions, cache);
            parentChain.addAll(parent.getParentChain());
            parentChain.add(parent.getName());

            for (Map.Entry<String, TextureReference> kvp : parent.getTextures().entrySet())
                textures.putIfAbsent(kvp.getKey(), kvp.getValue());

            for (Map.Entry<String, Transform> kvp : parent.getDisplay().entrySet())
                display.put(kvp.getKey(), cloneTransform(kvp.getValue()));

            if (!parent.getElements().isEmpty()) {
                elements = Concurrent.newList();

                for (Element element : parent.getElements())
                    elements.add(cloneElement(element));
            }
        }

        Map<String, TextureReference> defTextures = definition.getTextures();
        if (!defTextures.isEmpty())
            textures.putAll(defTextures);

        Map<String, Transform> defDisplay = definition.getDisplay();
        if (!defDisplay.isEmpty()) {
            for (Map.Entry<String, Transform> kvp : defDisplay.entrySet())
                display.put(kvp.getKey(), cloneTransform(kvp.getValue()));
        }

        List<Element> defElements = definition.getElements();
        if (!defElements.isEmpty()) {
            elements = Concurrent.newList();

            for (Element element : defElements) {
                if (isValidElement(element))
                    elements.add(cloneElement(element));
            }
        }

        stack.remove(name);

        BlockModel resolved = new BlockModel(
            name,
            parentChain.toUnmodifiableList(),
            Concurrent.newUnmodifiableMap(textures),
            Concurrent.newUnmodifiableMap(display),
            Concurrent.newUnmodifiableList(elements)
        );

        cache.putIfAbsent(name.toLowerCase(Locale.ROOT), resolved);
        return resolved;
    }

    private static @NotNull Transform cloneTransform(@NotNull Transform source) {
        return GSON.fromJson(GSON.toJson(source), Transform.class);
    }

    private static boolean isValidElement(@NotNull Element element) {
        Vector3f from = element.getFrom();
        Vector3f to = element.getTo();
        return !from.equals(Vector3f.ZERO) || !to.equals(Vector3f.ZERO);
    }

    private static @NotNull Element cloneElement(@NotNull Element element) {
        Map<Face, FaceData> faces = new HashMap<>();
        for (Map.Entry<Face, FaceData> pair : element.getFaces().entrySet()) {
            FaceData original = pair.getValue();
            faces.put(pair.getKey(), new FaceData(
                original.getTexture(),
                original.getUv(),
                original.getRotation(),
                original.getTintIndex(),
                original.getCullFace()
            ));
        }

        return new Element(
            element.getFrom(),
            element.getTo(),
            element.getRotation() != null
                ? new Rotation(
                    element.getRotation().getAngle(),
                    element.getRotation().getOrigin(),
                    element.getRotation().getAxis(),
                    element.getRotation().isRescale()
                )
                : null,
            faces,
            element.isShade()
        );
    }

    // ================================================================
    // Download and extraction
    // ================================================================

    /**
     * Downloads and extracts Minecraft assets for the specified version.
     *
     * @param versionId the Minecraft version, or null for the latest release
     * @param outputDir the directory where the {@code minecraft/} assets folder will be created
     * @return the path to the extracted {@code minecraft/} directory
     * @throws IOException if any step fails
     */
    public static @NotNull Path downloadAndExtractAssets(@Nullable String versionId, @NotNull Path outputDir) throws IOException {
        PistonManifest manifest = MinecraftApi.getMojangProxy().getEndpoint().getVersionManifest();
        String resolvedVersion = versionId != null ? versionId : manifest.getLatest().getRelease();
        PistonManifest.Entry entry = manifest.getVersions().findFirstOrNull(PistonManifest.Entry::getVersion, resolvedVersion);

        if (entry == null)
            throw new IOException("Minecraft version '%s' not found in manifest".formatted(resolvedVersion));

        PistonMetadata metadata = MinecraftApi.getMojangProxy().getEndpoint().getVersionMetadata(entry);
        Path jarPath = downloadClientJar(metadata.getDownloads().getClient());

        try {
            return extractAssets(jarPath, outputDir);
        } finally {
            Files.deleteIfExists(jarPath);
        }
    }

    private static @NotNull Path downloadClientJar(@NotNull PistonMetadata.Downloads.Entry entry) throws IOException {
        Path tempFile = Files.createTempFile("minecraft-client-", ".jar");

        try (InputStream in = MinecraftApi.getMojangProxy().getEndpoint().downloadClientJar(entry);
             OutputStream out = Files.newOutputStream(tempFile)) {
            in.transferTo(out);
        }

        String actualSha1 = computeSha1(tempFile);

        if (!actualSha1.equalsIgnoreCase(entry.getSha1())) {
            Files.deleteIfExists(tempFile);
            throw new IOException("SHA-1 hash mismatch for client JAR: expected '%s', got '%s'"
                .formatted(entry.getSha1(), actualSha1));
        }

        return tempFile;
    }

    private static @NotNull Path extractAssets(@NotNull Path jarPath, @NotNull Path outputDir) throws IOException {
        Path minecraftDir = outputDir.resolve("minecraft");
        Files.createDirectories(minecraftDir);

        try (FileSystem jarFs = FileSystems.newFileSystem(jarPath, (ClassLoader) null)) {
            Path jarAssetsRoot = jarFs.getPath("assets", "minecraft");

            if (!Files.isDirectory(jarAssetsRoot))
                throw new IOException("Client JAR does not contain assets/minecraft/ directory");

            Files.walkFileTree(jarAssetsRoot, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(minecraftDir.resolve(jarAssetsRoot.relativize(dir).toString()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, minecraftDir.resolve(jarAssetsRoot.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return minecraftDir;
    }

    private static @NotNull String computeSha1(@NotNull Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) > 0)
                    digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm not available", e);
        }
    }

    // ================================================================
    // Resource pack discovery
    // ================================================================

    private static @NotNull ConcurrentList<ResourcePack> discoverResourcePacks(@NotNull Iterable<String> directories) {
        List<String> dirs = new ArrayList<>();
        directories.forEach(dirs::add);
        if (dirs.isEmpty())
            return Concurrent.newList();

        try {
            return ResourcePackDiscovery.discoverPacks(dirs);
        } catch (IOException e) {
            return Concurrent.newList();
        }
    }

    // ================================================================
    // Model definitions loading
    // ================================================================

    /**
     * Loads all model definitions from the asset tree.
     *
     * @param assetsRoot the root assets directory
     * @param overlayPaths optional overlay root directories
     * @param namespaces optional asset namespace registry
     * @return a mutable map of model key to definition
     * @throws IOException if an I/O error occurs
     */
    public static @NotNull Map<String, BlockModel> loadModelDefinitions(
        @NotNull String assetsRoot,
        @NotNull List<String> overlayPaths,
        @NotNull AssetNamespaceRegistry namespaces
    ) throws IOException {
        List<AssetNamespace> roots = resolveNamespaceRoots(assetsRoot, overlayPaths, namespaces, true);
        Map<String, BlockModel> definitions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        boolean hasAnyModels = false;

        for (AssetNamespace root : roots) {
            for (Path directory : enumerateModelDirectories(root.path())) {
                hasAnyModels = true;
                for (Path file : walkJsonFiles(directory)) {
                    String key = normalizeModelName(directory.relativize(file).toString(), root.name());
                    if (key == null || key.isBlank())
                        continue;

                    BlockModel definition = GSON.fromJson(Files.readString(file), BlockModel.class);
                    if (definition == null)
                        definition = new BlockModel();
                    definition.setName(key);
                    definitions.put(key, definition);
                }
            }
        }

        if (!hasAnyModels)
            throw new IOException("Models directory not found at '%s'".formatted(Path.of(assetsRoot, "models")));

        // Builtins
        definitions.putIfAbsent("builtin/generated", new BlockModel());
        definitions.putIfAbsent("builtin/entity", new BlockModel());
        BlockModel missing = GSON.fromJson("{\"textures\":{\"particle\":\"minecraft:block/missingno\"}}", BlockModel.class);
        definitions.putIfAbsent("builtin/missing", missing);

        return definitions;
    }

    // ================================================================
    // Block info loading
    // ================================================================

    /**
     * Loads block info entries from the asset tree's blockstates/ directory.
     *
     * @param assetsRoot the root assets directory
     * @param models the loaded model definitions
     * @param overlayPaths optional overlay root directories
     * @param namespaces optional asset namespace registry
     * @return a list of block info entries
     * @throws IOException if an I/O error occurs
     */
    public static @NotNull ConcurrentList<BlockInfo> loadBlockInfos(
        @NotNull String assetsRoot,
        @NotNull Map<String, BlockModel> models,
        @NotNull List<String> overlayPaths,
        @NotNull AssetNamespaceRegistry namespaces
    ) throws IOException {
        List<String> roots = resolveRootPaths(assetsRoot, overlayPaths, namespaces);
        Map<String, BlockInfo> entries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        boolean hasAnyBlockstates = false;

        for (String root : roots) {
            for (Path directory : enumerateBlockstateDirectories(root)) {
                hasAnyBlockstates = true;
                for (Path file : walkJsonFiles(directory)) {
                    String blockName = stripJsonExtension(directory.relativize(file).toString());
                    JsonObject rootElement = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                    String modelReference = resolveDefaultModel(blockName, rootElement, models);
                    String textureReference = resolveTexture(modelReference, models);

                    BlockInfo info = new BlockInfo();
                    info.setName(blockName);
                    info.setBlockState(blockName);
                    info.setModel(modelReference);
                    info.setTexture(textureReference);
                    entries.put(blockName, info);
                }
            }
        }

        if (!hasAnyBlockstates)
            throw new IOException("Blockstates directory not found at '%s'".formatted(Path.of(assetsRoot, "blockstates")));

        return Concurrent.newList(entries.values());
    }

    // ================================================================
    // Item info loading
    // ================================================================

    /**
     * Loads item info entries from the asset tree's models and items directories.
     *
     * @param assetsRoot the root assets directory
     * @param models the loaded model definitions
     * @param overlayPaths optional overlay root directories
     * @param namespaces optional asset namespace registry
     * @return a list of item info entries
     * @throws IOException if an I/O error occurs
     */
    public static @NotNull ConcurrentList<ItemInfo> loadItemInfos(
        @NotNull String assetsRoot,
        @NotNull Map<String, BlockModel> models,
        @NotNull List<String> overlayPaths,
        @NotNull AssetNamespaceRegistry namespaces
    ) throws IOException {
        Map<String, ItemInfo> entries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // Phase 1: items derived from model definitions with item/ prefix
        for (Map.Entry<String, BlockModel> entry : models.entrySet()) {
            String key = entry.getKey();
            if (!key.toLowerCase(Locale.ROOT).startsWith("item/"))
                continue;

            String itemName = key.substring(5);
            if (itemName.isBlank() || isTemplateItem(itemName))
                continue;

            String texture = resolvePrimaryTexture(entry.getValue(), models);
            ItemInfo info = entries.computeIfAbsent(itemName, k -> { ItemInfo i = new ItemInfo(); i.setName(k); return i; });
            info.setModel(key);
            if (StringUtil.isEmpty(info.getTexture()) && StringUtil.isNotEmpty(texture))
                info.setTexture(texture);
        }

        // Phase 2: item definitions from items/ directory
        List<AssetNamespace> nsRoots = resolveNamespaceRoots(assetsRoot, overlayPaths, namespaces, true);
        for (AssetNamespace nsRoot : nsRoots) {
            Path itemsRoot = Path.of(nsRoot.path(), "items");
            if (!Files.isDirectory(itemsRoot))
                continue;

            for (Path file : walkJsonFiles(itemsRoot)) {
                String itemName = stripJsonExtension(itemsRoot.relativize(file).toString());
                if (StringUtil.isEmpty(itemName) || isTemplateItem(itemName))
                    continue;

                try {
                    JsonObject rootElement = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                    Map<Integer, TintInfo> tintMap = new HashMap<>();
                    extractTintInfo(rootElement, tintMap);
                    ItemModelSelector selector = ItemModelSelectorParser.parseFromRoot(rootElement);
                    String modelReference = resolveModelFromItemDefinition(rootElement);

                    ItemInfo info = entries.computeIfAbsent(itemName, k -> { ItemInfo i = new ItemInfo(); i.setName(k); return i; });

                    if (StringUtil.isNotEmpty(modelReference)) {
                        info.setModel(modelReference);
                        if (StringUtil.isEmpty(info.getTexture())) {
                            String normalized = normalizeModelReference(modelReference);
                            if (StringUtil.isNotEmpty(normalized)) {
                                BlockModel def = caseInsensitiveGet(models, normalized);
                                if (def != null) {
                                    String texture = resolvePrimaryTexture(def, models);
                                    if (StringUtil.isNotEmpty(texture))
                                        info.setTexture(texture);
                                }
                            }
                        }
                    }

                    if (selector != null)
                        info.setSelector(selector);

                    if (!tintMap.isEmpty())
                        tintMap.forEach((k, v) -> info.getLayerTints().put(k, v));

                } catch (JsonSyntaxException | IllegalStateException ignored) {
                }
            }
        }

        return Concurrent.newList(entries.values());
    }

    // ================================================================
    // Namespace root resolution
    // ================================================================

    private static @NotNull List<AssetNamespace> resolveNamespaceRoots(
        @NotNull String primaryRoot,
        @Nullable List<String> overlayPaths,
        @Nullable AssetNamespaceRegistry namespaces,
        boolean includeAllNamespaces
    ) {
        if (namespaces != null) {
            List<AssetNamespace> resolved = includeAllNamespaces
                ? namespaces.getRoots()
                : namespaces.resolveRoots("minecraft");

            List<AssetNamespace> deduped = deduplicateRoots(resolved);
            if (!deduped.isEmpty())
                return deduped;
        }

        Set<String> dedupe = new HashSet<>();
        List<AssetNamespace> results = new ArrayList<>();
        tryAddNamespaceRoot(primaryRoot, "minecraft", dedupe, results);
        if (overlayPaths != null)
            for (String overlay : overlayPaths)
                tryAddNamespaceRoot(overlay, "minecraft", dedupe, results);

        return results;
    }

    private static @NotNull List<String> resolveRootPaths(
        @NotNull String primaryRoot,
        @Nullable List<String> overlayPaths,
        @Nullable AssetNamespaceRegistry namespaces
    ) {
        List<AssetNamespace> nsRoots = resolveNamespaceRoots(primaryRoot, overlayPaths, namespaces, false);
        Set<String> seen = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (AssetNamespace root : nsRoots) {
            if (seen.add(root.path().toLowerCase(Locale.ROOT)))
                result.add(root.path());
        }
        return result;
    }

    private static void tryAddNamespaceRoot(@Nullable String candidate, @NotNull String namespace,
                                             @NotNull Set<String> dedupe, @NotNull List<AssetNamespace> results) {
        if (candidate == null || candidate.isBlank())
            return;

        Path fullPath = Path.of(candidate).toAbsolutePath().normalize();
        if (!Files.isDirectory(fullPath))
            return;

        if (dedupe.add(fullPath.toString().toLowerCase(Locale.ROOT)))
            results.add(new AssetNamespace(namespace, fullPath.toString(), "external", false));
    }

    private static @NotNull List<AssetNamespace> deduplicateRoots(@NotNull List<AssetNamespace> roots) {
        List<AssetNamespace> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (AssetNamespace root : roots) {
            if (root == null || StringUtil.isEmpty(root.path()))
                continue;

            Path fullPath = Path.of(root.path()).toAbsolutePath().normalize();
            if (!Files.isDirectory(fullPath))
                continue;

            String ns = StringUtil.isEmpty(root.name()) ? "minecraft" : root.name();
            String identity = ns.toLowerCase(Locale.ROOT) + "|" + fullPath.toString().toLowerCase(Locale.ROOT);
            if (seen.add(identity))
                results.add(new AssetNamespace(ns, fullPath.toString(), root.sourceId(), root.vanilla()));
        }
        return results;
    }

    // ================================================================
    // Directory enumeration
    // ================================================================

    private static @NotNull List<Path> enumerateModelDirectories(@NotNull String root) {
        List<Path> dirs = new ArrayList<>();
        Path modelsRoot = Path.of(root, "models");
        if (Files.isDirectory(modelsRoot))
            dirs.add(modelsRoot);

        Path blockEntityModels = Path.of(root, "blockentities", "blockModels");
        if (Files.isDirectory(blockEntityModels))
            dirs.add(blockEntityModels);

        return dirs;
    }

    private static @NotNull List<Path> enumerateBlockstateDirectories(@NotNull String root) {
        List<Path> dirs = new ArrayList<>();
        Path blockstatesRoot = Path.of(root, "blockstates");
        if (Files.isDirectory(blockstatesRoot))
            dirs.add(blockstatesRoot);

        Path blockEntityStates = Path.of(root, "blockentities", "blockStates");
        if (Files.isDirectory(blockEntityStates))
            dirs.add(blockEntityStates);

        return dirs;
    }

    private static @NotNull List<Path> walkJsonFiles(@NotNull Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                if (file.toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    files.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    // ================================================================
    // Model name normalization
    // ================================================================

    private static @Nullable String normalizeModelName(@Nullable String relativePath, @Nullable String namespaceName) {
        if (relativePath == null || relativePath.isBlank())
            return null;

        String normalized = relativePath.replace('\\', '/').trim();
        if (normalized.toLowerCase(Locale.ROOT).endsWith(".json"))
            normalized = normalized.substring(0, normalized.length() - 5);

        while (!normalized.isEmpty() && (normalized.charAt(0) == '.' || normalized.charAt(0) == '/'))
            normalized = normalized.substring(1);

        if (normalized.toLowerCase(Locale.ROOT).startsWith("block/"))
            normalized = normalized.substring(6);
        else if (normalized.toLowerCase(Locale.ROOT).startsWith("blocks/"))
            normalized = normalized.substring(7);

        while (!normalized.isEmpty() && normalized.charAt(0) == '/')
            normalized = normalized.substring(1);

        if (namespaceName != null && !namespaceName.isBlank()
            && !namespaceName.equalsIgnoreCase("minecraft")
            && !normalized.toLowerCase(Locale.ROOT).startsWith(namespaceName.toLowerCase(Locale.ROOT) + ":"))
            normalized = namespaceName + ":" + normalized;

        return normalized;
    }

    private static @NotNull String normalizeModelReference(@Nullable String reference) {
        if (reference == null || reference.isBlank())
            return "";

        String normalized = reference.trim();
        if (normalized.toLowerCase(Locale.ROOT).startsWith("minecraft:"))
            normalized = normalized.substring(10);
        normalized = normalized.replace('\\', '/');
        while (!normalized.isEmpty() && normalized.charAt(0) == '/')
            normalized = normalized.substring(1);

        if (normalized.toLowerCase(Locale.ROOT).startsWith("block/"))
            normalized = normalized.substring(6);
        else if (normalized.toLowerCase(Locale.ROOT).startsWith("blocks/"))
            normalized = normalized.substring(7);

        return normalized;
    }

    private static @NotNull String stripJsonExtension(@NotNull String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        if (normalized.toLowerCase(Locale.ROOT).endsWith(".json"))
            normalized = normalized.substring(0, normalized.length() - 5);
        while (!normalized.isEmpty() && normalized.charAt(0) == '/')
            normalized = normalized.substring(1);
        while (!normalized.isEmpty() && normalized.charAt(normalized.length() - 1) == '/')
            normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    // ================================================================
    // Blockstate to model resolution
    // ================================================================

    private static @Nullable String resolveDefaultModel(
        @NotNull String blockName, @NotNull JsonObject root, @NotNull Map<String, BlockModel> models
    ) {
        if (root.has("variants") && root.get("variants").isJsonObject()) {
            String resolved = resolveFromVariants(root.getAsJsonObject("variants"), models);
            if (resolved != null && !resolved.isBlank())
                return resolved;
        }

        if (root.has("multipart") && root.get("multipart").isJsonArray()) {
            String resolved = resolveFromMultipart(blockName, root.getAsJsonArray("multipart"), models);
            if (resolved != null && !resolved.isBlank())
                return resolved;
        }

        String candidate = "minecraft:block/" + blockName;
        String normalized = normalizeModelReference(candidate);
        if (StringUtil.isNotEmpty(normalized) && caseInsensitiveContains(models, normalized))
            return candidate;

        normalized = normalizeModelReference(blockName);
        if (StringUtil.isNotEmpty(normalized) && caseInsensitiveContains(models, normalized))
            return formatModelReference(null, normalized);

        return null;
    }

    private static @Nullable String resolveFromVariants(@NotNull JsonObject variants, @NotNull Map<String, BlockModel> models) {
        for (String key : PREFERRED_VARIANT_KEYS) {
            if (variants.has(key)) {
                String modelRef = extractModelReference(variants.get(key));
                String normalized = normalizeModelReference(modelRef);
                if (StringUtil.isNotEmpty(normalized) && caseInsensitiveContains(models, normalized))
                    return formatModelReference(modelRef, normalized);
            }
        }

        for (Map.Entry<String, JsonElement> property : variants.entrySet()) {
            String modelRef = extractModelReference(property.getValue());
            String normalized = normalizeModelReference(modelRef);
            if (StringUtil.isNotEmpty(normalized) && caseInsensitiveContains(models, normalized))
                return formatModelReference(modelRef, normalized);
        }

        return null;
    }

    private static @Nullable String resolveFromMultipart(
        @NotNull String blockName, @NotNull JsonArray multipart, @NotNull Map<String, BlockModel> models
    ) {
        for (String candidate : new String[]{
            "minecraft:block/" + blockName + "_inventory", "minecraft:block/" + blockName + "_item",
            "minecraft:block/" + blockName, "minecraft:block/" + blockName + "_post",
            "minecraft:block/" + blockName + "_center", "minecraft:block/" + blockName + "_side",
            "minecraft:block/" + blockName + "_floor", "minecraft:block/" + blockName + "_top",
            "minecraft:block/" + blockName + "_bottom"
        }) {
            String normalized = normalizeModelReference(candidate);
            if (StringUtil.isNotEmpty(normalized) && caseInsensitiveContains(models, normalized))
                return formatModelReference(candidate, normalized);
        }

        for (JsonElement part : multipart) {
            if (!part.isJsonObject())
                continue;
            JsonObject partObj = part.getAsJsonObject();
            if (!partObj.has("apply"))
                continue;
            String modelRef = extractModelReference(partObj.get("apply"));
            String normalized = normalizeModelReference(modelRef);
            if (StringUtil.isNotEmpty(normalized) && caseInsensitiveContains(models, normalized))
                return formatModelReference(modelRef, normalized);
        }

        return null;
    }

    // ================================================================
    // Model reference helpers
    // ================================================================

    private static @NotNull String formatModelReference(@Nullable String original, @NotNull String normalized) {
        if (original != null && !original.isBlank())
            return original;
        if (normalized.toLowerCase(Locale.ROOT).startsWith("item/"))
            return "minecraft:" + normalized;
        if (normalized.toLowerCase(Locale.ROOT).startsWith("builtin/"))
            return normalized;
        return "minecraft:block/" + normalized;
    }

    private static @Nullable String extractModelReference(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull())
            return null;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("model")) {
                JsonElement modelProperty = obj.get("model");
                if (modelProperty.isJsonPrimitive() && modelProperty.getAsJsonPrimitive().isString())
                    return modelProperty.getAsString();
                String nested = extractModelReference(modelProperty);
                if (nested != null && !nested.isBlank())
                    return nested;
            }
            if (obj.has("base")) {
                JsonElement baseProperty = obj.get("base");
                if (baseProperty.isJsonPrimitive() && baseProperty.getAsJsonPrimitive().isString())
                    return baseProperty.getAsString();
            }
        } else if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                String candidate = extractModelReference(entry);
                if (candidate != null && !candidate.isBlank())
                    return candidate;
            }
        }

        return null;
    }

    // ================================================================
    // Tint info extraction
    // ================================================================

    private static void extractTintInfo(@NotNull JsonElement element, @NotNull Map<Integer, TintInfo> target) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("tints") && obj.get("tints").isJsonArray()) {
                JsonArray tintsArray = obj.getAsJsonArray("tints");
                int index = 0;
                for (JsonElement tintElement : tintsArray) {
                    if (!target.containsKey(index)) {
                        TintInfo tintInfo = createTintInfo(tintElement);
                        if (tintInfo != null)
                            target.put(index, tintInfo);
                    }
                    index++;
                }
            }

            for (Map.Entry<String, JsonElement> property : obj.entrySet()) {
                if (!property.getKey().equalsIgnoreCase("tints"))
                    extractTintInfo(property.getValue(), target);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray())
                extractTintInfo(entry, target);
        }
    }

    private static @Nullable TintInfo createTintInfo(@NotNull JsonElement tintElement) {
        if (!tintElement.isJsonObject())
            return null;

        JsonObject obj = tintElement.getAsJsonObject();
        TintInfo tintInfo = new TintInfo();

        if (obj.has("type") && obj.get("type").isJsonPrimitive() && obj.getAsJsonPrimitive("type").isString()) {
            String type = obj.get("type").getAsString();
            switch (type) {
                case "minecraft:dye" -> tintInfo.setKind(TintInfo.Kind.DYE);
                case "minecraft:constant" -> tintInfo.setKind(TintInfo.Kind.CONSTANT);
                case null -> tintInfo.setKind(TintInfo.Kind.UNSPECIFIED);
                default -> tintInfo.setKind(TintInfo.Kind.UNKNOWN);
            }
        }

        int[] defaultColor = switch (tintInfo.getKind()) {
            case DYE -> tryReadColor(obj, "default");
            case CONSTANT -> tryReadColor(obj, "value");
            default -> {
                int[] color = tryReadColor(obj, "default");
                yield color != null ? color : tryReadColor(obj, "value");
            }
        };

        tintInfo.setDefaultColor(defaultColor);
        return tintInfo;
    }

    private static int @Nullable [] tryReadColor(@NotNull JsonObject element, @NotNull String propertyName) {
        if (!element.has(propertyName))
            return null;

        JsonElement property = element.get(propertyName);

        if (property.isJsonPrimitive() && property.getAsJsonPrimitive().isNumber())
            return convertIntToColor(property.getAsInt());

        if (property.isJsonPrimitive() && property.getAsJsonPrimitive().isString())
            return parseColorString(property.getAsString());

        return null;
    }

    private static int @Nullable [] parseColorString(@Nullable String value) {
        if (StringUtil.isEmpty(value))
            return null;

        String text = value.trim();
        if (text.startsWith("#"))
            text = text.substring(1);
        else if (text.toLowerCase(Locale.ROOT).startsWith("0x"))
            text = text.substring(2);

        try {
            return convertIntToColor((int) Long.parseUnsignedLong(text, 16));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int @NotNull [] convertIntToColor(int argb) {
        long raw = Integer.toUnsignedLong(argb);
        int a = (int) ((raw >> 24) & 0xFF);
        int r = (int) ((raw >> 16) & 0xFF);
        int g = (int) ((raw >> 8) & 0xFF);
        int b = (int) (raw & 0xFF);
        if (a == 0 && raw != 0)
            a = 0xFF;
        return new int[]{ r, g, b, a == 0 ? 0xFF : a };
    }

    // ================================================================
    // Item definition model resolution
    // ================================================================

    private static @Nullable String resolveModelFromItemDefinition(@NotNull JsonObject root) {
        if (root.has("model")) {
            String reference = extractModelReference(root.get("model"));
            if (StringUtil.isNotEmpty(reference))
                return reference;
        }

        if (root.has("components") && root.get("components").isJsonObject()) {
            JsonObject components = root.getAsJsonObject("components");
            if (components.has("minecraft:model")) {
                String reference = extractModelReference(components.get("minecraft:model"));
                if (StringUtil.isNotEmpty(reference))
                    return reference;
            }
        }

        return null;
    }

    // ================================================================
    // Texture resolution
    // ================================================================

    private static @Nullable String resolveTexture(@Nullable String modelReference, @NotNull Map<String, BlockModel> models) {
        String normalized = normalizeModelReference(modelReference);
        if (StringUtil.isEmpty(normalized))
            return null;

        BlockModel definition = caseInsensitiveGet(models, normalized);
        return definition != null ? resolvePrimaryTexture(definition, models) : null;
    }

    private static @Nullable String resolvePrimaryTexture(@NotNull BlockModel definition, @NotNull Map<String, BlockModel> models) {
        return resolvePrimaryTexture(definition, models, new HashSet<>());
    }

    private static @Nullable String resolvePrimaryTexture(
        @NotNull BlockModel definition, @NotNull Map<String, BlockModel> models, @NotNull Set<String> visited
    ) {
        Map<String, TextureReference> textures = definition.getTextures();
        if (!textures.isEmpty()) {
            for (String key : TEXTURE_PREFERENCE_ORDER) {
                TextureReference ref = caseInsensitiveGet(textures, key);
                if (ref != null && !ref.sprite().isBlank())
                    return ref.sprite();
            }
            for (TextureReference ref : textures.values()) {
                if (ref != null && !ref.sprite().isBlank())
                    return ref.sprite();
            }
        }

        String parent = definition.getParent();
        if (parent != null && !parent.isBlank()) {
            String parentKey = normalizeModelReference(parent);
            if (!parentKey.isBlank() && visited.add(parentKey.toLowerCase(Locale.ROOT))) {
                BlockModel parentDef = caseInsensitiveGet(models, parentKey);
                if (parentDef != null)
                    return resolvePrimaryTexture(parentDef, models, visited);
            }
        }

        return null;
    }

    // ================================================================
    // Template detection
    // ================================================================

    private static boolean isTemplateItem(@NotNull String itemName) {
        if (itemName.toLowerCase(Locale.ROOT).startsWith("template_"))
            return true;
        return itemName.equalsIgnoreCase("generated")
            || itemName.equalsIgnoreCase("handheld")
            || itemName.equalsIgnoreCase("handheld_rod")
            || itemName.equalsIgnoreCase("handheld_mace");
    }

    // ================================================================
    // Case-insensitive map helpers
    // ================================================================

    private static <V> boolean caseInsensitiveContains(@NotNull Map<String, V> map, @NotNull String key) {
        if (map instanceof TreeMap)
            return map.containsKey(key);
        for (String k : map.keySet()) {
            if (k.equalsIgnoreCase(key))
                return true;
        }
        return false;
    }

    private static <V> @Nullable V caseInsensitiveGet(@NotNull Map<String, V> map, @NotNull String key) {
        V direct = map.get(key);
        if (direct != null)
            return direct;
        for (Map.Entry<String, V> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key))
                return entry.getValue();
        }
        return null;
    }
}
