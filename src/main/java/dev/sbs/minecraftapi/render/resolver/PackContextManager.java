package dev.sbs.minecraftapi.render.resolver;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.asset.context.AssetContext;
import dev.sbs.minecraftapi.asset.context.PackContext;
import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.ItemInfo;
import dev.sbs.minecraftapi.asset.model.ResourcePack;
import dev.sbs.minecraftapi.asset.namespace.AssetNamespace;
import dev.sbs.minecraftapi.asset.namespace.AssetNamespaceRegistry;
import dev.sbs.minecraftapi.asset.namespace.Namespace;
import dev.sbs.minecraftapi.asset.texture.TexturePackStack;
import dev.sbs.minecraftapi.asset.texture.TextureReference;
import dev.sbs.minecraftapi.nbt.tags.Tag;
import dev.sbs.minecraftapi.nbt.tags.array.ByteArrayTag;
import dev.sbs.minecraftapi.nbt.tags.array.IntArrayTag;
import dev.sbs.minecraftapi.nbt.tags.array.LongArrayTag;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.collection.ListTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.NumericalTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.ItemRenderData;
import dev.sbs.minecraftapi.render.context.RenderContext;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Resource pack system for resolving textures across pack stacks and overlays.
 *
 * <p>Contains the pack-aware fingerprinting, texture pack stack management, overlay/namespace
 * resolution, and the {@link PackContext} that ties all pack information together.
 */
@UtilityClass
public final class PackContextManager {

    private static final String RENDERER_VERSION = "1.0";

    // ----------------------------------------------------------------
    // Renderer resolution
    // ----------------------------------------------------------------

    public static RenderContext resolveContext(@NotNull RenderContext context, @Nullable ConcurrentList<String> packIds) {
        if (!context.hasResourcePacks() || packIds == null || packIds.isEmpty())
            return context;

        if (!context.getPackAssetContext().getPackIds().isEmpty()
            && packSequencesEqual(packIds, context.getPackAssetContext().getPackIds()))
            return context;

        return getContextForPackStack(context, packIds);
    }

    public static BlockRenderOptions forwardOptions(@NotNull RenderContext context, BlockRenderOptions options) {
        if (!context.hasResourcePacks() || options.getPackIds() == null || options.getPackIds().isEmpty())
            return options;

        return options.mutate().withPackIds(null).build();
    }

    private static boolean packSequencesEqual(@NotNull ConcurrentList<String> candidate, @NotNull ConcurrentList<String> baseline) {
        if (candidate.size() != baseline.size())
            return false;

        for (int i = 0; i < candidate.size(); i++) {
            if (!candidate.get(i).equalsIgnoreCase(baseline.get(i)))
                return false;
        }

        return true;
    }

    private static RenderContext getContextForPackStack(@NotNull RenderContext context, ConcurrentList<String> packIds) {
        if (!context.hasResourcePacks()) {
            throw new IllegalStateException(
                "No resource packs are loaded and cannot resolve pack combinations.");
        }

        if (context.getAssetsDirectory() == null || context.getAssetsDirectory().isBlank()) {
            throw new IllegalStateException(
                "Texture pack rendering requires a context created from Minecraft assets.");
        }

        TexturePackStack packStack = TexturePackStack.buildPackStack(packIds);
        return context.getPackContextCache().computeIfAbsent(packStack.getFingerprint(),
            k -> createPackContext(context, packStack));
    }

    private static RenderContext createPackContext(RenderContext context,
                                                    TexturePackStack packStack) {
        try {
            AssetContext packAssetContext = MinecraftApi.getAssetFactory().loadPackContext(
                packStack.getPacks()
                    .stream()
                    .map(ResourcePack::getId)
                    .collect(Concurrent.toList())
            );
            return new RenderContext(packAssetContext);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to create pack context", e);
        }
    }

    // ----------------------------------------------------------------
    // Resource ID computation
    // ----------------------------------------------------------------

    public static ResourceIdResult computeResourceId(RenderContext context, String target,
                                                       BlockRenderOptions options,
                                                       @Nullable ItemModelResolution preResolvedItem) {
        context.ensureNotDisposed();

        String normalizedTarget = target.trim();
        String lookupTarget = normalizedTarget;
        int namespaceSeparator = lookupTarget.indexOf(':');
        if (namespaceSeparator >= 0) {
            lookupTarget = lookupTarget.substring(namespaceSeparator + 1);
        }

        String[] modelPath = {null};
        String[] primaryModelIdentifier = {null};
        Set<String> resolvedTextures = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        String[] variantKey = {null};

        ItemInfo itemInfo = null;
        boolean hasItemRegistry = context.hasItemData();
        boolean hasItemInfo = false;
        if (hasItemRegistry) {
            itemInfo = context.getItemInfo(lookupTarget);
            hasItemInfo = itemInfo != null;
        }

        if (preResolvedItem != null && preResolvedItem.getLookupTarget().equalsIgnoreCase(lookupTarget)) {
            if (preResolvedItem.getItemInfo() != null) {
                itemInfo = preResolvedItem.getItemInfo();
                hasItemInfo = true;
            }
        }

        boolean shouldTreatAsItem = hasItemRegistry && (hasItemInfo || options.getItemData() != null);

        String finalLookupTarget = lookupTarget;
        ItemInfo finalItemInfo = itemInfo;

        if (shouldTreatAsItem) {
            processItem(context, normalizedTarget, finalLookupTarget, finalItemInfo, options,
                preResolvedItem, modelPath, primaryModelIdentifier, resolvedTextures, variantKey);
        } else {
            String blockModelPath = context.getBlockModel(lookupTarget);
            if (blockModelPath != null && !blockModelPath.isBlank()) {
                modelPath[0] = blockModelPath;
                BlockModel model = context.resolveModel(blockModelPath);
                modelPath[0] = model.getName();
                for (String tex : collectResolvedTextures(model)) {
                    resolvedTextures.add(tex);
                }
                variantKey[0] = "block:" + normalizedTarget + ":" + model.getName() + ":"
                    + joinTextures(resolvedTextures);
            } else if (hasItemRegistry) {
                processItem(context, normalizedTarget, finalLookupTarget, finalItemInfo, options,
                    preResolvedItem, modelPath, primaryModelIdentifier, resolvedTextures, variantKey);
            } else {
                variantKey[0] = "literal:" + normalizedTarget;
            }
        }

        if (variantKey[0] == null)
            variantKey[0] = "literal:" + normalizedTarget;

        String sourcePackId = determineSourcePackId(context, modelPath[0], resolvedTextures);

        String descriptor = RENDERER_VERSION + "|" + context.getPackAssetContext().getPackStackHash() + "|" + variantKey[0];
        String resourceId = computeResourceIdHash(descriptor);
        ConcurrentList<String> texturesList = resolvedTextures.isEmpty() ? Concurrent.newUnmodifiableList() : Concurrent.newList(resolvedTextures);

        ResourceIdResult result = new ResourceIdResult(resourceId, sourcePackId, context.getPackAssetContext().getPackStackHash());
        result.setModel(modelPath[0]);
        result.setTextures(texturesList);
        return result;
    }

    private static void processItem(RenderContext context, String normalizedTarget,
                                     String lookupTarget,
                                     @Nullable ItemInfo itemInfo,
                                     BlockRenderOptions options,
                                     @Nullable ItemModelResolution preResolvedItem,
                                     String[] modelPath, String[] primaryModelIdentifier,
                                     Set<String> resolvedTextures, String[] variantKey) {
        if (!context.hasItemData()) {
            variantKey[0] = "literal:" + normalizedTarget;
            return;
        }

        String referenceModel = null;
        String itemModel = context.getItemModel(lookupTarget);
        if (itemModel != null && !itemModel.isBlank())
            referenceModel = normalizeModelIdentifier(itemModel);

        BlockModel effectiveModel = null;
        List<String> modelCandidates;
        String resolvedModelName = null;
        String effectiveModelIdentifier;

        if (preResolvedItem != null && preResolvedItem.getLookupTarget().equalsIgnoreCase(lookupTarget)) {
            effectiveModel = preResolvedItem.getModel();
            modelCandidates = preResolvedItem.getModelCandidates();
            resolvedModelName = preResolvedItem.getResolvedModelName();
            if (preResolvedItem.getItemInfo() != null) {
                itemInfo = preResolvedItem.getItemInfo();
            }
        } else {
            // Resolve the item model from scratch (same as ItemModelResolver.resolveItemModel)
            String dynamicModel = null;
            if (itemInfo != null && itemInfo.getSelector() != null) {
                dynamicModel = itemInfo.getSelector().resolve(
                    ItemModelResolver.toSelectorContext(options.getItemData(),
                        options.isUseGuiTransform() ? "gui" : "none"));
            }
            String firmamentModel = ItemModelResolver.tryGetFirmamentModel(options.getItemData());
            String primaryModel = itemInfo != null ? itemInfo.getModel() : null;
            String fallbackModel;
            if (firmamentModel != null && !firmamentModel.isBlank()) {
                fallbackModel = firmamentModel;
            } else if (dynamicModel != null && !dynamicModel.isBlank()) {
                fallbackModel = dynamicModel;
            } else if (primaryModel != null && !primaryModel.isBlank()) {
                fallbackModel = primaryModel;
            } else {
                String blockModel = context.getBlockModel(lookupTarget);
                fallbackModel = (blockModel != null && !blockModel.isBlank()) ? blockModel : lookupTarget;
            }

            List<String> candidates = ItemModelResolver.buildModelCandidates(fallbackModel, lookupTarget);
            Set<String> seen = new java.util.HashSet<>();
            modelCandidates = new java.util.ArrayList<>();
            for (String c : candidates) {
                if (seen.add(c.toLowerCase())) modelCandidates.add(c);
            }

            for (String candidate : modelCandidates) {
                BlockModel resolved = ItemModelResolver.resolveModelOrNull(context, candidate);
                if (resolved != null) {
                    effectiveModel = resolved;
                    resolvedModelName = candidate;
                    break;
                }
            }
        }

        effectiveModelIdentifier = resolvedModelName != null ? resolvedModelName
            : (effectiveModel != null ? effectiveModel.getName() : null);

        if (effectiveModel == null && resolvedModelName != null && !resolvedModelName.isBlank()) {
            effectiveModel = ItemModelResolver.resolveModelOrNull(context, resolvedModelName);
            if (effectiveModel != null && (effectiveModelIdentifier == null || effectiveModelIdentifier.isBlank())) {
                effectiveModelIdentifier = resolvedModelName;
            }
        }

        if (effectiveModel == null && modelCandidates != null) {
            for (String candidate : modelCandidates) {
                BlockModel candidateModel = ItemModelResolver.resolveModelOrNull(context, candidate);
                if (candidateModel != null) {
                    effectiveModel = candidateModel;
                    effectiveModelIdentifier = (candidate == null || candidate.isBlank())
                        ? candidateModel.getName() : candidate;
                    break;
                }
            }
        }

        if (effectiveModel != null) {
            String identifier = normalizeModelIdentifier(
                effectiveModelIdentifier != null ? effectiveModelIdentifier : effectiveModel.getName());
            primaryModelIdentifier[0] = identifier;
            modelPath[0] = identifier;
            referenceModel = identifier;
            for (String tex : collectResolvedTextures(effectiveModel)) {
                resolvedTextures.add(tex);
            }
        } else if (itemInfo != null && itemInfo.getTexture() != null) {
            resolvedTextures.add(itemInfo.getTexture());
        }

        if (resolvedTextures.isEmpty() && referenceModel != null) {
            resolvedTextures.add(referenceModel);
        }

        String itemDataKey = options.getItemData() != null ? buildItemRenderDataKey(options.getItemData()) : "";
        if (modelPath[0] == null) modelPath[0] = referenceModel != null ? referenceModel : normalizedTarget;
        variantKey[0] = "item:" + normalizedTarget + ":" + modelPath[0] + ":"
            + joinTextures(resolvedTextures) + ":" + itemDataKey;
    }

    private static Collection<String> collectResolvedTextures(BlockModel model) {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (TextureReference ref : model.getTextures().values()) {
            String resolved = model.resolveTexture(ref.sprite());

            if (StringUtil.isNotEmpty(resolved))
                set.add(resolved);
        }

        return set;
    }

    private static String joinTextures(Collection<String> textures) {
        if (textures.isEmpty()) return "";
        List<String> sorted = new ArrayList<>(textures);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return String.join(",", sorted);
    }

    static String normalizeModelIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return identifier;
        String trimmed = identifier.trim();
        if (trimmed.indexOf(':') >= 0) return trimmed;
        while (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        return trimmed.isBlank() ? "minecraft:" : "minecraft:" + trimmed;
    }

    private static String buildItemRenderDataKey(ItemRenderData data) {
        StringBuilder builder = new StringBuilder();
        if (data.getLayer0Tint() != null) {
            builder.append("l0=");
            builder.append(String.format("%02X%02X%02X", data.getLayer0Tint()[0],
                data.getLayer0Tint()[1], data.getLayer0Tint()[2]));
        } else {
            builder.append("l0=none");
        }

        builder.append(";disable=").append(data.isDisableDefaultLayer0Tint() ? '1' : '0');

        if (data.getAdditionalLayerTints() != null && !data.getAdditionalLayerTints().isEmpty()) {
            data.getAdditionalLayerTints().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    builder.append(";l").append(e.getKey()).append('=');
                    int[] c = e.getValue();
                    builder.append(String.format("%02X%02X%02X", c[0], c[1], c[2]));
                });
        }

        if (data.getCustomData() != null) {
            builder.append(";custom=").append(buildCustomDataKey(data.getCustomData()));
        } else {
            builder.append(";custom=none");
        }

        if (data.getProfile() != null) {
            builder.append(";profile=").append(buildProfileKey(data.getProfile()));
        } else {
            builder.append(";profile=none");
        }

        return builder.toString();
    }

    private static String buildProfileKey(CompoundTag profile) {
        Tag<?> propertiesTag = profile.get("properties");
        if (propertiesTag instanceof ListTag<?> properties) {
            for (Tag<?> entry : properties) {
                if (!(entry instanceof CompoundTag propertyCompound)) continue;
                Tag<?> nameTag = propertyCompound.get("name");
                if (nameTag instanceof StringTag nameValue
                    && "textures".equalsIgnoreCase(nameValue.getValue())) {
                    Tag<?> valueTag = propertyCompound.get("value");
                    if (valueTag instanceof StringTag valueString
                        && valueString.notEmpty()) {
                        return sha256Hex(valueString.getValue());
                    }
                }
            }
        }

        if (profile.containsKey("id")) {
            String formatted = formatNbtValue(profile.get("id"));
            if (formatted != null && !formatted.isBlank()) return formatted;
        }

        return "none";
    }

    private static String buildCustomDataKey(CompoundTag compound) {
        List<String> segments = new ArrayList<>();
        List<Map.Entry<String, Tag<?>>> entries = new ArrayList<>(compound.entrySet());
        entries.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));
        for (Map.Entry<String, Tag<?>> pair : entries) {
            String key = pair.getKey() != null ? pair.getKey() : "";
            String value = formatNbtValue(pair.getValue());
            segments.add(key + "=" + value);
        }
        return segments.isEmpty() ? "empty" : String.join("|", segments);
    }

    private static String formatNbtValue(Tag<?> tag) {
        if (tag instanceof StringTag s) return s.getValue();
        if (tag instanceof NumericalTag<?> n) return n.toString();
        if (tag instanceof CompoundTag compound) return "{" + buildCustomDataKey(compound) + "}";
        if (tag instanceof ListTag<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(formatNbtValue(list.get(i)));
            }
            sb.append(']');
            return sb.toString();
        }
        if (tag instanceof IntArrayTag intArray) {
            StringBuilder sb = new StringBuilder("[");
            Integer[] values = intArray.getValue();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(values[i]);
            }
            sb.append(']');
            return sb.toString();
        }
        if (tag instanceof LongArrayTag longArray) {
            StringBuilder sb = new StringBuilder("[");
            Long[] values = longArray.getValue();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(values[i]);
            }
            sb.append(']');
            return sb.toString();
        }
        if (tag instanceof ByteArrayTag byteArray) {
            StringBuilder sb = new StringBuilder("[");
            Byte[] values = byteArray.getValue();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(values[i]);
            }
            sb.append(']');
            return sb.toString();
        }
        return "";
    }

    private static String determineSourcePackId(RenderContext context, @Nullable String modelPath, Collection<String> textureIds) {
        AssetNamespaceRegistry assetNamespaces = context.getPackAssetContext().getAssetNamespaces();
        if (assetNamespaces == null)
            return AssetContext.VANILLA_PACK_ID;

        for (String textureId : textureIds) {
            if (textureId == null || textureId.isBlank()) continue;

            Namespace parsed = Namespace.of(textureId);
            String namespace = parsed.name();
            String path = parsed.path();

            if (!path.startsWith("textures/"))
                path = "textures/" + path;

            String[] pathVariants = { path, path.replace("blocks/", "block/").replace("items/", "item/") };

            // Check roots from highest priority (last) to lowest (first)
            List<AssetNamespace> roots = assetNamespaces.resolveRoots(namespace);
            for (int i = roots.size() - 1; i >= 0; i--) {
                AssetNamespace root = roots.get(i);

                for (String variant : pathVariants) {
                    String withExtension = variant.replace('/', File.separatorChar) + ".png";
                    Path candidate = Path.of(root.path(), withExtension);

                    if (Files.exists(candidate)) {
                        if (!root.vanilla())
                            return root.sourceId();

                        // Found in vanilla - stop checking this texture
                        break;
                    }
                }
            }
        }

        return AssetContext.VANILLA_PACK_ID;
    }

    // ----------------------------------------------------------------
    // Hash computation
    // ----------------------------------------------------------------

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String computeResourceIdHash(String input) {
        return encodeBase32(sha256(input));
    }

    private static String sha256Hex(String input) {
        return HexFormat.of().withUpperCase().formatHex(sha256(input));
    }

    private static String encodeBase32(byte[] bytes) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        int outputLength = (bytes.length * 8 + 4) / 5;
        StringBuilder builder = new StringBuilder(outputLength);
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0b11111;
                bitsLeft -= 5;
                builder.append(alphabet.charAt(index));
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0b11111;
            builder.append(alphabet.charAt(index));
        }

        return builder.toString();
    }

}
