package dev.sbs.minecraftapi.asset.texture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.io.image.AnimatedImageData;
import dev.sbs.api.io.image.ImageFrame;
import dev.sbs.api.io.image.PixelBuffer;
import dev.sbs.minecraftapi.asset.AssetNamespace;
import dev.sbs.minecraftapi.asset.AssetNamespaceRegistry;
import dev.sbs.minecraftapi.asset.Namespace;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main texture loading, caching, and tinting subsystem for Minecraft asset rendering.
 * <p>
 * Resolves textures from multiple prioritized filesystem sources, handles animated texture
 * metadata (.mcmeta), applies tint colors, generates armor trim textures, and caches
 * results for reuse.
 */
public final class TextureRepository implements AutoCloseable {

    private final @NotNull List<TextureSource> sources;
    private final @NotNull AssetNamespaceRegistry assetNamespaces;
    private final @NotNull ConcurrentHashMap<String, BufferedImage> cache = new ConcurrentHashMap<>();
    private final @NotNull Map<String, String> embedded = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final @NotNull BufferedImage missingTexture;

    private final @NotNull ConcurrentHashMap<String, AnimatedImageData> animationCache = new ConcurrentHashMap<>();
    private final @NotNull ConcurrentHashMap<String, Boolean> interpolationFlags = new ConcurrentHashMap<>();

    private volatile @Nullable AnimationOverride activeAnimationOverride;
    private final @NotNull ReentrantLock animationOverrideLock = new ReentrantLock();
    private final @NotNull Map<Integer, Integer> trimPaletteLookup;
    private final int trimPaletteLength;

    @Getter private @NotNull BufferedImage grassColorMap;
    @Getter private @NotNull BufferedImage foliageColorMap;
    @Getter private @NotNull Optional<BufferedImage> dryFoliageColorMap = Optional.empty();
    private boolean disposed;

    /**
     * Constructs a new {@code TextureRepository} from an assets directory, overlay roots, and
     * asset namespace registry.
     *
     * @param assetsDirectory the Minecraft assets root directory (e.g. {@code minecraft/})
     * @param overlayRootsList the overlay roots providing additional search paths
     * @param assetNamespaces the asset namespace registry for multi-source resolution
     */
    public TextureRepository(@NotNull String assetsDirectory,
                             @NotNull List<OverlayRoot> overlayRootsList,
                             @NotNull AssetNamespaceRegistry assetNamespaces) {
        this(resolveTexturesRoot(assetsDirectory), null, extractOverlayPaths(overlayRootsList), assetNamespaces);
    }

    /**
     * Constructs a new {@code TextureRepository} with the given data root and overlays.
     *
     * @param dataRoot the primary asset root directory
     * @param embeddedTextureFile path to a JSON file containing base64-encoded embedded textures, or null
     * @param overlayRoots additional overlay root directories
     * @param assetNamespaces the asset namespace registry for multi-source resolution
     */
    public TextureRepository(@NotNull String dataRoot, @Nullable String embeddedTextureFile,
                             @NotNull Iterable<String> overlayRoots,
                             @NotNull AssetNamespaceRegistry assetNamespaces) {
        this.assetNamespaces = assetNamespaces;
        this.sources = buildSourceList(dataRoot, overlayRoots, assetNamespaces);
        this.missingTexture = createMissingTexture();

        int[] trimPaletteColors = tryLoadTrimPaletteColors();
        if (trimPaletteColors != null) {
            this.trimPaletteLength = trimPaletteColors.length;
            this.trimPaletteLookup = new HashMap<>(trimPaletteColors.length);
            for (int i = 0; i < trimPaletteColors.length; i++) {
                this.trimPaletteLookup.put(trimPaletteColors[i], i);
            }
        } else {
            this.trimPaletteLength = 0;
            this.trimPaletteLookup = new HashMap<>();
        }

        this.grassColorMap = missingTexture;
        this.foliageColorMap = missingTexture;

        String colormapRoot = findColormapRoot();
        if (colormapRoot != null) {
            Path grassPath = Path.of(colormapRoot, "colormap", "grass.png");
            if (Files.exists(grassPath)) {
                BufferedImage loaded = loadImageSafe(grassPath);
                if (loaded != null) grassColorMap = loaded;
            }

            Path foliagePath = Path.of(colormapRoot, "colormap", "foliage.png");
            if (Files.exists(foliagePath)) {
                BufferedImage loaded = loadImageSafe(foliagePath);
                if (loaded != null) foliageColorMap = loaded;
            }

            Path dryFoliagePath = Path.of(colormapRoot, "colormap", "dry_foliage.png");
            if (Files.exists(dryFoliagePath))
                dryFoliageColorMap = Optional.ofNullable(loadImageSafe(dryFoliagePath));
        }

        if (embeddedTextureFile != null && !embeddedTextureFile.isBlank() && Files.exists(Path.of(embeddedTextureFile))) {
            try (Reader reader = Files.newBufferedReader(Path.of(embeddedTextureFile))) {
                Gson gson = SimplifiedApi.getGson();
                Type listType = new TypeToken<List<TextureContentEntry>>() {}.getType();
                List<TextureContentEntry> entries = gson.fromJson(reader, listType);

                if (entries != null) {
                    for (TextureContentEntry entry : entries) {
                        if (entry.texture != null && !entry.texture.isBlank()) {
                            String key = normalizeTextureId(entry.name);
                            embedded.put(key, entry.texture);
                        }
                    }
                }
            } catch (IOException e) {
                // Silently ignore embedded texture file errors
            }
        }
    }

    /**
     * Retrieves a texture by its identifier, returning the missing texture placeholder if not found.
     *
     * @param textureId the texture resource identifier
     * @return the resolved texture, or the missing texture placeholder
     */
    public @NotNull BufferedImage getTexture(@Nullable String textureId) {
        if (textureId == null || textureId.isBlank())
            return missingTexture;

        String normalized = normalizeTextureId(textureId);
        AnimationOverride overrideContext = activeAnimationOverride;
        if (overrideContext != null) {
            ImageFrame overrideFrame = overrideContext.tryGetFrame(normalized);
            if (overrideFrame != null)
                return overrideFrame.getImage();
        }

        return cache.computeIfAbsent(normalized, this::loadTextureInternal);
    }

    /**
     * Attempts to retrieve a texture by its identifier.
     *
     * @param textureId the texture resource identifier
     * @return the resolved texture, or null if not found
     */
    public @Nullable BufferedImage tryGetTexture(@Nullable String textureId) {
        BufferedImage texture = getTexture(textureId);
        return texture != missingTexture ? texture : null;
    }

    /**
     * Retrieves a texture with an applied tint color.
     *
     * @param textureId the texture resource identifier
     * @param tintArgb the tint color as a packed ARGB integer
     * @param strengthMultiplier the tint strength multiplier
     * @param blend the blend factor between 0.0 (original) and 1.0 (fully tinted)
     * @return the tinted texture, or the original texture if the tint is fully transparent
     */
    public @NotNull BufferedImage getTintedTexture(@NotNull String textureId, int tintArgb, float strengthMultiplier, float blend) {
        int tintA = (tintArgb >> 24) & 0xFF;
        if (tintA == 0)
            return getTexture(textureId);

        String normalized = normalizeTextureId(textureId);
        String strengthKey = formatFloat(strengthMultiplier);
        String blendKey = formatFloat(blend);
        String hexColor = String.format("%08X", tintArgb);
        String cacheKey = normalized + "_" + hexColor + "_" + strengthKey + "_" + blendKey;
        AnimationOverride override_ = activeAnimationOverride;
        if (override_ != null) {
            String overrideSuffix = override_.getCacheKeySuffix();
            if (overrideSuffix != null && !overrideSuffix.isEmpty())
                cacheKey += "|anim:" + overrideSuffix;
        }

        String finalCacheKey = cacheKey;
        return cache.computeIfAbsent(finalCacheKey, key -> {
            BufferedImage original = getTexture(textureId);
            if (original == missingTexture)
                return missingTexture;

            BufferedImage tinted = PixelBuffer.wrap(original).toBufferedImage();
            int tintR = (tintArgb >> 16) & 0xFF;
            int tintG = (tintArgb >> 8) & 0xFF;
            int tintB = tintArgb & 0xFF;

            float tintVecR = Math.min(tintR / 255f * strengthMultiplier, 1f);
            float tintVecG = Math.min(tintG / 255f * strengthMultiplier, 1f);
            float tintVecB = Math.min(tintB / 255f * strengthMultiplier, 1f);
            float tintVecA = tintA / 255f;

            float clampedBlend = Math.max(0f, Math.min(blend, 1f));

            for (int y = 0; y < tinted.getHeight(); y++) {
                for (int x = 0; x < tinted.getWidth(); x++) {
                    int pixel = tinted.getRGB(x, y);
                    float pixA = ((pixel >> 24) & 0xFF) / 255f;
                    float pixR = ((pixel >> 16) & 0xFF) / 255f;
                    float pixG = ((pixel >> 8) & 0xFF) / 255f;
                    float pixB = (pixel & 0xFF) / 255f;

                    float tintedR = pixR * tintVecR;
                    float tintedG = pixG * tintVecG;
                    float tintedB = pixB * tintVecB;
                    float tintedA = pixA * tintVecA;

                    tintedR = Math.max(0f, Math.min(tintedR, 1f));
                    tintedG = Math.max(0f, Math.min(tintedG, 1f));
                    tintedB = Math.max(0f, Math.min(tintedB, 1f));
                    tintedA = Math.max(0f, Math.min(tintedA, 1f));

                    float finalR, finalG, finalB, finalA;
                    if (clampedBlend >= 0.999f) {
                        finalR = tintedR;
                        finalG = tintedG;
                        finalB = tintedB;
                        finalA = tintedA;
                    } else {
                        finalR = pixR + (tintedR - pixR) * clampedBlend;
                        finalG = pixG + (tintedG - pixG) * clampedBlend;
                        finalB = pixB + (tintedB - pixB) * clampedBlend;
                        finalA = pixA + (tintedA - pixA) * clampedBlend;
                    }

                    int outA = Math.round(finalA * 255f) & 0xFF;
                    int outR = Math.round(finalR * 255f) & 0xFF;
                    int outG = Math.round(finalG * 255f) & 0xFF;
                    int outB = Math.round(finalB * 255f) & 0xFF;
                    tinted.setRGB(x, y, (outA << 24) | (outR << 16) | (outG << 8) | outB);
                }
            }

            return tinted;
        });
    }

    /**
     * Retrieves a texture with an applied tint color using default strength and full blend.
     *
     * @param textureId the texture resource identifier
     * @param tintArgb the tint color as a packed ARGB integer
     * @return the tinted texture
     */
    public @NotNull BufferedImage getTintedTexture(@NotNull String textureId, int tintArgb) {
        return getTintedTexture(textureId, tintArgb, 1f, 1f);
    }

    /**
     * Registers a texture in the cache under the given identifier.
     *
     * @param textureId the texture resource identifier
     * @param image the image to register
     * @param overwrite whether to overwrite an existing cached entry
     */
    public void registerTexture(@NotNull String textureId, @NotNull BufferedImage image, boolean overwrite) {
        if (textureId == null || textureId.isBlank())
            throw new IllegalArgumentException("textureId must not be null or blank");
        Objects.requireNonNull(image, "image must not be null");

        String normalized = normalizeTextureId(textureId);
        if (!overwrite && cache.containsKey(normalized))
            return;

        cache.put(normalized, PixelBuffer.wrap(image).toBufferedImage());
    }

    /**
     * Registers a texture in the cache under the given identifier, overwriting any existing entry.
     *
     * @param textureId the texture resource identifier
     * @param image the image to register
     */
    public void registerTexture(@NotNull String textureId, @NotNull BufferedImage image) {
        registerTexture(textureId, image, true);
    }

    private BufferedImage loadTextureInternal(String normalized) {
        Namespace parsed = Namespace.of(normalized);
        List<String> logicalPaths = enumerateLogicalPaths(parsed.path());

        // Iterate sources in reverse order (High Priority -> Low Priority)
        for (int i = sources.size() - 1; i >= 0; i--) {
            TextureSource source = sources.get(i);
            for (String logicalPath : logicalPaths) {
                String candidate = source.tryResolve(parsed.name(), logicalPath);
                if (candidate != null) {
                    BufferedImage loadedTexture = loadImageSafe(Path.of(candidate));
                    if (loadedTexture != null)
                        return processAnimatedTexture(normalized, candidate, loadedTexture);
                }
            }
        }

        String dataUri = embedded.get(normalized);
        if (dataUri != null) {
            BufferedImage image = tryDecodeDataUri(dataUri);
            if (image != null)
                return image;
        }

        int shortKeyIndex = normalized.lastIndexOf('/');
        if (shortKeyIndex >= 0) {
            String shortKey = normalized.substring(shortKeyIndex + 1);
            dataUri = embedded.get(shortKey);
            if (dataUri != null) {
                BufferedImage image = tryDecodeDataUri(dataUri);
                if (image != null)
                    return image;
            }
        }

        BufferedImage generated = tryGenerateArmorTrimTexture(normalized);
        if (generated != null)
            return generated;

        return missingTexture;
    }


    private List<String> enumerateLogicalPaths(String pathWithinNamespace) {
        if (pathWithinNamespace == null || pathWithinNamespace.isBlank())
            return Collections.emptyList();

        List<String> results = new ArrayList<>();
        results.add(pathWithinNamespace);

        String[] segments = pathWithinNamespace.split("/");
        List<String> nonEmptySegments = new ArrayList<>();
        for (String seg : segments) {
            if (!seg.isEmpty())
                nonEmptySegments.add(seg);
        }
        String[] workingSegments = nonEmptySegments.toArray(new String[0]);

        if (workingSegments.length > 1 && workingSegments[0].equalsIgnoreCase("textures")) {
            String[] stripped = new String[workingSegments.length - 1];
            System.arraycopy(workingSegments, 1, stripped, 0, stripped.length);
            workingSegments = stripped;
            if (workingSegments.length > 0)
                results.add(String.join("/", workingSegments));
        }

        if (workingSegments.length > 1) {
            String first = workingSegments[0];
            StringBuilder remainder = new StringBuilder();
            for (int i = 1; i < workingSegments.length; i++) {
                if (i > 1) remainder.append('/');
                remainder.append(workingSegments[i]);
            }
            for (String variant : enumerateFolderCandidates(first)) {
                if (!variant.equalsIgnoreCase(first))
                    results.add(variant + "/" + remainder);
            }
        }

        if (workingSegments.length > 0)
            results.add(workingSegments[workingSegments.length - 1]);

        return results;
    }

    private List<String> enumerateCandidatePaths(String normalized) {
        List<String> results = new ArrayList<>();
        Namespace parsed = Namespace.of(normalized);
        for (String logicalPath : enumerateLogicalPaths(parsed.path())) {
            for (int i = sources.size() - 1; i >= 0; i--) {
                TextureSource source = sources.get(i);
                String candidate = source.tryResolve(parsed.name(), logicalPath);
                if (candidate != null)
                    results.add(candidate);
            }
        }
        return results;
    }

    private static List<TextureSource> buildSourceList(String primaryRoot,
                                                       @NotNull Iterable<String> overlayRoots,
                                                       @NotNull AssetNamespaceRegistry assetNamespaces) {
        List<TextureSource> sources = new ArrayList<>();

        if (!assetNamespaces.getSources().isEmpty()) {
            for (String sourceId : assetNamespaces.getSources())
                sources.add(new RegistryTextureSource(sourceId, assetNamespaces));
        } else {
            LinkedHashSet<String> orderedRoots = new LinkedHashSet<>();

            tryAddDirectory(primaryRoot, orderedRoots);
            for (String overlay : overlayRoots)
                tryAddDirectory(overlay, orderedRoots);

            for (String root : orderedRoots)
                sources.add(new DirectoryTextureSource(root));
        }

        return Collections.unmodifiableList(sources);
    }

    private static void tryAddDirectory(@Nullable String candidate, LinkedHashSet<String> orderedRoots) {
        if (candidate == null || candidate.isBlank()) return;
        Path fullPath = Path.of(candidate).toAbsolutePath().normalize();
        if (!Files.isDirectory(fullPath)) return;
        String fullPathStr = fullPath.toString();
        orderedRoots.add(fullPathStr);

        Path texturesSub = fullPath.resolve("textures");
        if (Files.isDirectory(texturesSub))
            orderedRoots.add(texturesSub.toString());
    }

    private static @NotNull String resolveTexturesRoot(@NotNull String assetsDirectory) {
        Path textures = Path.of(assetsDirectory, "textures");
        return Files.isDirectory(textures) ? textures.toString() : assetsDirectory;
    }

    /**
     * Extracts unique overlay paths from the given overlay roots.
     *
     * @param overlayRoots the overlay roots to extract paths from
     * @return a list of unique overlay paths
     */
    public static @NotNull List<String> extractOverlayPaths(@NotNull List<OverlayRoot> overlayRoots) {
        List<String> overlayPaths = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (OverlayRoot root : overlayRoots) {
            if (seen.add(root.path().toLowerCase()))
                overlayPaths.add(root.path());
        }
        return overlayPaths;
    }

    @Nullable
    private String findColormapRoot() {
        for (int i = sources.size() - 1; i >= 0; i--) {
            String path = sources.get(i).tryResolve("minecraft", "colormap/grass");
            if (path != null) {
                Path resolved = Path.of(path);
                Path parent = resolved.getParent();
                if (parent != null) {
                    Path grandparent = parent.getParent();
                    if (grandparent != null)
                        return grandparent.toString();
                }
            }
        }
        return null;
    }

    /**
     * An abstract texture source that resolves relative texture paths to absolute filesystem paths.
     */
    private static abstract class TextureSource {
        @Nullable
        abstract String tryResolve(String namespaceName, String relativePath);
    }

    @RequiredArgsConstructor
    private static final class RegistryTextureSource extends TextureSource {

        private final String sourceId;
        private final AssetNamespaceRegistry registry;

        @Override
        @Nullable
        String tryResolve(String namespaceName, String relativePath) {
            List<AssetNamespace> roots = registry.getRoots(namespaceName, sourceId);
            if (roots.isEmpty() && !namespaceName.equalsIgnoreCase("minecraft"))
                roots = registry.getRoots("minecraft", sourceId);

            String withExtension = relativePath.replace('/', java.io.File.separatorChar) + ".png";
            for (AssetNamespace root : roots) {
                Path candidate = Path.of(root.path(), withExtension);
                if (Files.exists(candidate))
                    return candidate.toString();
            }

            return null;
        }
    }

    @RequiredArgsConstructor
    private static final class DirectoryTextureSource extends TextureSource {

        private final String root;

        @Override
        @Nullable
        String tryResolve(String namespaceName, String relativePath) {
            String withExtension = relativePath.replace('/', java.io.File.separatorChar) + ".png";
            Path candidate = Path.of(root, withExtension);
            if (Files.exists(candidate))
                return candidate.toString();
            return null;
        }
    }

    private static List<String> enumerateFolderCandidates(String folder) {
        List<String> candidates = new ArrayList<>();
        candidates.add(folder);

        if (folder.equalsIgnoreCase("block"))
            candidates.add("blocks");
        else if (folder.equalsIgnoreCase("blocks"))
            candidates.add("block");
        else if (folder.equalsIgnoreCase("item"))
            candidates.add("items");
        else if (folder.equalsIgnoreCase("items"))
            candidates.add("item");

        return candidates;
    }

    /**
     * Normalizes a texture identifier by stripping the {@code minecraft:} prefix, trimming leading
     * slashes, converting backslashes to forward slashes, and lowering case.
     *
     * @param textureId the raw texture identifier
     * @return the normalized identifier
     */
    static String normalizeTextureId(String textureId) {
        String normalized = textureId.trim();

        if (normalized.length() >= 10 && normalized.substring(0, 10).equalsIgnoreCase("minecraft:"))
            normalized = normalized.substring(10);

        while (normalized.startsWith("/"))
            normalized = normalized.substring(1);

        normalized = normalized.replace('\\', '/');
        return normalized.toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static BufferedImage tryDecodeDataUri(String dataUri) {
        String prefix = "data:image/png;base64,";
        if (dataUri.length() > prefix.length()
                && dataUri.substring(0, prefix.length()).equalsIgnoreCase(prefix)) {
            String base64 = dataUri.substring(prefix.length());
            try {
                byte[] bytes = Base64.getDecoder().decode(base64);
                return ImageIO.read(new ByteArrayInputStream(bytes));
            } catch (IOException | IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private static BufferedImage createMissingTexture() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int magenta = (0xFF << 24) | (0xFF << 16) | (0x00 << 8) | 0xFF; // ARGB
        int black = (0xFF << 24) | (0x00 << 16) | (0x00 << 8) | 0x00;   // ARGB

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean isMagenta = (x / 8 + y / 8) % 2 == 0;
                image.setRGB(x, y, isMagenta ? magenta : black);
            }
        }

        return image;
    }

    private BufferedImage processAnimatedTexture(String normalizedKey, String texturePath,
                                                 BufferedImage spriteSheet) {
        AnimatedImageData animation = tryBuildAnimation(texturePath, spriteSheet, normalizedKey);
        if (animation == null || animation.getFrames().isEmpty())
            return spriteSheet;

        animationCache.put(normalizedKey, animation);
        return PixelBuffer.wrap(animation.getFrames().getFirst().getImage()).toBufferedImage();
    }

    @Nullable
    private AnimatedImageData tryBuildAnimation(String texturePath, BufferedImage spriteSheet,
                                                String normalizedKey) {
        Path metadataPath = Path.of(texturePath + ".mcmeta");
        if (!Files.exists(metadataPath))
            return null;

        try (Reader reader = Files.newBufferedReader(metadataPath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (!root.has("animation"))
                return null;
            JsonElement animElem = root.get("animation");
            if (!animElem.isJsonObject())
                return null;
            JsonObject animationElement = animElem.getAsJsonObject();

            float defaultFrameTime = 1f;
            if (animationElement.has("frametime")) {
                JsonElement ftElem = animationElement.get("frametime");
                if (ftElem.isJsonPrimitive() && ftElem.getAsJsonPrimitive().isNumber())
                    defaultFrameTime = Math.max(ftElem.getAsFloat(), 1f);
            }

            List<AnimationFrameDescriptor> explicitFrames = extractFrameSequence(animationElement, defaultFrameTime);
            int frameWidth = clamp(
                    getOptionalPositiveInt(animationElement, "width", spriteSheet.getWidth()),
                    1, spriteSheet.getWidth());
            int frameHeightValue = clamp(
                    getOptionalPositiveInt(animationElement, "height", frameWidth),
                    1, spriteSheet.getHeight());

            int framesPerRow = Math.max(1, spriteSheet.getWidth() / frameWidth);
            int framesPerColumn = Math.max(1, spriteSheet.getHeight() / frameHeightValue);
            int maximumFrameIndex = Math.max(framesPerRow * framesPerColumn - 1, 0);

            List<AnimationFrameDescriptor> sequence = !explicitFrames.isEmpty()
                    ? explicitFrames
                    : buildSequentialFrames(maximumFrameIndex + 1, defaultFrameTime);

            AnimatedImageData.Builder builder = AnimatedImageData.builder()
                .withWidth(frameWidth)
                .withHeight(frameHeightValue);

            for (AnimationFrameDescriptor descriptor : sequence) {
                if (descriptor.index < 0)
                    continue;

                int normalizedIndex = maximumFrameIndex > 0
                        ? descriptor.index % (maximumFrameIndex + 1)
                        : 0;
                int column = normalizedIndex % framesPerRow;
                int row = normalizedIndex / framesPerRow;
                int x = column * frameWidth;
                int y = row * frameHeightValue;

                if (x + frameWidth > spriteSheet.getWidth() || y + frameHeightValue > spriteSheet.getHeight())
                    continue;

                BufferedImage frameImage = spriteSheet.getSubimage(x, y, frameWidth, frameHeightValue);
                BufferedImage frameCopy = PixelBuffer.wrap(frameImage).toBufferedImage();
                int durationMs = Math.max(50, (int) (descriptor.frameTime * 50));
                builder.withFrame(ImageFrame.of(frameCopy, durationMs));
            }

            boolean interpolate = animationElement.has("interpolate")
                    && animationElement.get("interpolate").isJsonPrimitive()
                    && animationElement.get("interpolate").getAsBoolean();
            interpolationFlags.put(normalizedKey, interpolate);

            AnimatedImageData animation = builder.build();
            return animation.getFrames().isEmpty() ? null : animation;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<AnimationFrameDescriptor> extractFrameSequence(JsonObject animationElement,
                                                                       float defaultFrameTime) {
        if (!animationElement.has("frames"))
            return Collections.emptyList();
        JsonElement framesElem = animationElement.get("frames");
        if (!framesElem.isJsonArray())
            return Collections.emptyList();

        JsonArray framesArray = framesElem.getAsJsonArray();
        List<AnimationFrameDescriptor> frames = new ArrayList<>();
        for (JsonElement entry : framesArray) {
            if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isNumber()) {
                frames.add(new AnimationFrameDescriptor(entry.getAsInt(), defaultFrameTime));
            } else if (entry.isJsonObject()) {
                JsonObject obj = entry.getAsJsonObject();
                int index = -1;
                if (obj.has("index") && obj.get("index").isJsonPrimitive()
                        && obj.get("index").getAsJsonPrimitive().isNumber())
                    index = obj.get("index").getAsInt();
                if (index < 0)
                    continue;

                float frameTime = defaultFrameTime;
                if (obj.has("time") && obj.get("time").isJsonPrimitive()
                        && obj.get("time").getAsJsonPrimitive().isNumber())
                    frameTime = Math.max(obj.get("time").getAsInt(), 1);

                frames.add(new AnimationFrameDescriptor(index, frameTime));
            }
        }

        return frames;
    }

    private static List<AnimationFrameDescriptor> buildSequentialFrames(int frameCount, float defaultFrameTime) {
        if (frameCount <= 0)
            return Collections.emptyList();

        List<AnimationFrameDescriptor> frames = new ArrayList<>(frameCount);
        for (int i = 0; i < frameCount; i++)
            frames.add(new AnimationFrameDescriptor(i, defaultFrameTime));

        return frames;
    }

    @Nullable
    private int[] tryLoadTrimPaletteColors() {
        for (String candidate : enumerateCandidatePaths("trims/color_palettes/trim_palette")) {
            if (!Files.exists(Path.of(candidate))) continue;

            try {
                BufferedImage image = ImageIO.read(Path.of(candidate).toFile());
                if (image == null || image.getHeight() <= 0) continue;

                int width = image.getWidth();
                int[] colors = new int[width];
                for (int x = 0; x < width; x++)
                    colors[x] = image.getRGB(x, 0);
                return colors;
            } catch (IOException e) {
                // continue to next candidate
            }
        }

        return null;
    }

    @Nullable
    private BufferedImage tryGenerateArmorTrimTexture(String normalized) {
        if (!normalized.toLowerCase(Locale.ROOT).startsWith("trims/items/"))
            return null;

        String fileName = normalized;
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0)
            fileName = normalized.substring(lastSlash + 1);
        if (fileName.isBlank())
            return null;

        int trimMarkerIndex = fileName.toLowerCase(Locale.ROOT).indexOf("_trim_");
        if (trimMarkerIndex < 0)
            return null;

        String baseOverlayName = fileName.substring(0, trimMarkerIndex + "_trim".length());
        String materialToken = fileName.substring(trimMarkerIndex + "_trim_".length());
        if (baseOverlayName.isBlank() || materialToken.isBlank())
            return null;

        String baseOverlayId = "trims/items/" + baseOverlayName;
        BufferedImage overlayBase = getTexture(baseOverlayId);
        if (overlayBase == missingTexture)
            return null;

        if (trimPaletteLength == 0 || trimPaletteLookup.isEmpty())
            return null;

        BufferedImage materialPalette = resolveArmorTrimPalette(materialToken);
        if (materialPalette == null || materialPalette == missingTexture || materialPalette.getHeight() == 0)
            return null;

        int paletteWidth = materialPalette.getWidth();
        if (paletteWidth == 0)
            return null;

        int[] materialPaletteRow = new int[paletteWidth];
        for (int x = 0; x < paletteWidth; x++)
            materialPaletteRow[x] = materialPalette.getRGB(x, 0);

        BufferedImage tinted = PixelBuffer.wrap(overlayBase).toBufferedImage();

        for (int y = 0; y < overlayBase.getHeight(); y++) {
            for (int x = 0; x < overlayBase.getWidth(); x++) {
                int sourcePixel = overlayBase.getRGB(x, y);
                int sourceA = (sourcePixel >> 24) & 0xFF;
                if (sourceA == 0)
                    continue;

                Integer paletteIndex = trimPaletteLookup.get(sourcePixel);
                if (paletteIndex == null) {
                    tinted.setRGB(x, y, sourcePixel);
                    continue;
                }

                int clampedIndex = clamp(paletteIndex, 0, materialPaletteRow.length - 1);
                int replacement = materialPaletteRow[clampedIndex];
                int repR = (replacement >> 16) & 0xFF;
                int repG = (replacement >> 8) & 0xFF;
                int repB = replacement & 0xFF;
                tinted.setRGB(x, y, (sourceA << 24) | (repR << 16) | (repG << 8) | repB);
            }
        }

        return tinted;
    }

    @Nullable
    private BufferedImage resolveArmorTrimPalette(String materialToken) {
        for (String candidate : enumerateArmorTrimPaletteCandidates(materialToken)) {
            BufferedImage palette = getTexture("trims/color_palettes/" + candidate);
            if (palette != missingTexture)
                return palette;
        }
        return null;
    }

    private static List<String> enumerateArmorTrimPaletteCandidates(String materialToken) {
        if (materialToken == null || materialToken.isBlank())
            return Collections.emptyList();

        String normalizedMaterial = materialToken.trim();
        if (normalizedMaterial.isEmpty())
            return Collections.emptyList();

        List<String> candidates = new ArrayList<>();
        if (normalizedMaterial.toLowerCase(Locale.ROOT).endsWith("_darker")) {
            candidates.add(normalizedMaterial);
            if (normalizedMaterial.length() > 7)
                candidates.add(normalizedMaterial.substring(0, normalizedMaterial.length() - 7));
            return candidates;
        }

        candidates.add(normalizedMaterial);
        return candidates;
    }

    private static int getOptionalPositiveInt(JsonObject element, String propertyName, int defaultValue) {
        if (element.has(propertyName)) {
            JsonElement prop = element.get(propertyName);
            if (prop.isJsonPrimitive() && prop.getAsJsonPrimitive().isNumber()) {
                int value = prop.getAsInt();
                return value > 0 ? value : defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Attempts to retrieve the animation data for the given texture identifier.
     *
     * @param textureId the texture resource identifier
     * @return the animation data, or null if the texture is not animated
     */
    @Nullable
    AnimatedImageData tryGetAnimation(String textureId) {
        String normalized = normalizeTextureId(textureId);
        return animationCache.get(normalized);
    }

    /**
     * Returns whether interpolation is enabled for the given texture animation.
     *
     * @param textureId the texture resource identifier
     * @return true if interpolation is enabled, false otherwise
     */
    boolean isInterpolationEnabled(String textureId) {
        String normalized = normalizeTextureId(textureId);
        return interpolationFlags.getOrDefault(normalized, false);
    }

    /**
     * Begins an animation override scope that temporarily overrides specific texture frames.
     * The returned {@link AutoCloseable} must be closed to restore the previous state.
     *
     * @param frames a map of normalized texture identifiers to their override frames
     * @return a scope that restores the previous animation override state when closed
     */
    AutoCloseable beginAnimationOverride(@Nullable Map<String, ImageFrame> frames) {
        if (frames == null || frames.isEmpty())
            return () -> {};

        animationOverrideLock.lock();
        AnimationOverride previous = activeAnimationOverride;
        String cacheKey = buildOverrideCacheKey(frames);
        activeAnimationOverride = new AnimationOverride(frames, cacheKey);
        return new AnimationOverrideScope(this, previous);
    }

    private static String buildOverrideCacheKey(Map<String, ImageFrame> frames) {
        StringBuilder sb = new StringBuilder();
        frames.entrySet().stream()
                .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getKey(), b.getKey()))
                .forEach(e -> {
                    if (!sb.isEmpty()) sb.append('|');
                    sb.append(e.getKey()).append(':').append(e.getValue().getDelayMs());
                });
        return sb.toString();
    }

    // --- Inner types ---

    @RequiredArgsConstructor
    private static final class AnimationFrameDescriptor {

        final int index;
        final float frameTime;
    }


    @RequiredArgsConstructor
    private static final class AnimationOverride {

        private final Map<String, ImageFrame> frames;
        @Getter private final String cacheKeySuffix;

        @Nullable
        ImageFrame tryGetFrame(String normalizedTextureId) {
            return frames.get(normalizedTextureId);
        }
    }

    @RequiredArgsConstructor
    private static final class AnimationOverrideScope implements AutoCloseable {

        private final TextureRepository owner;
        @Nullable private final AnimationOverride previous;
        private boolean disposed;

        @Override
        public void close() {
            if (disposed)
                return;

            owner.activeAnimationOverride = previous;
            owner.animationOverrideLock.unlock();
            disposed = true;
        }
    }


    @Override
    public void close() {
        if (disposed) return;
        disposed = true;
        activeAnimationOverride = null;

        animationCache.clear();
        cache.clear();
    }

    // --- Private utility types ---

    private static final class TextureContentEntry {
        String name;
        @Nullable String texture;
    }

    @Nullable
    private static BufferedImage loadImageSafe(Path path) {
        try {
            return ImageIO.read(path.toFile());
        } catch (IOException e) {
            return null;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static String formatFloat(float value) {
        String formatted = String.format(Locale.ROOT, "%.3f", value);
        if (formatted.contains(".")) {
            formatted = formatted.replaceAll("0+$", "");
            formatted = formatted.replaceAll("\\.$", "");
        }
        return formatted;
    }
}
