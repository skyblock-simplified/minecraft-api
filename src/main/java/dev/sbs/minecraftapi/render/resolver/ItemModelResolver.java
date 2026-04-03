package dev.sbs.minecraftapi.render.resolver;

import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.asset.model.ItemInfo;
import dev.sbs.minecraftapi.asset.selector.ItemModelSelector;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.ItemRenderData;
import dev.sbs.minecraftapi.render.context.RenderContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves item names to block model instances by evaluating model selectors, Firmament custom
 * items, inventory model variants, and block fallback candidates.
 */
public final class ItemModelResolver {

    private ItemModelResolver() {
    }

    // ----------------------------------------------------------------
    // Item model resolution
    // ----------------------------------------------------------------

    public record ResolveResult(
        @Nullable BlockModel model,
        List<String> candidates,
        @Nullable String resolvedModelName
    ) {}

    public static ResolveResult resolveItemModel(RenderContext context, String itemName,
                                           @Nullable ItemInfo itemInfo,
                                           BlockRenderOptions options) {
        String dynamicModel = null;
        if (itemInfo != null && itemInfo.getSelector() != null) {
            dynamicModel = itemInfo.getSelector().resolve(
                toSelectorContext(options.getItemData(), options.isUseGuiTransform() ? "gui" : "none"));
        }

        String firmamentModel = tryGetFirmamentModel(options.getItemData());

        String primaryModel = itemInfo != null ? itemInfo.getModel() : null;
        String fallbackModel;
        if (firmamentModel != null && !firmamentModel.isBlank()) {
            fallbackModel = firmamentModel;
        } else if (dynamicModel != null && !dynamicModel.isBlank()) {
            fallbackModel = dynamicModel;
        } else if (primaryModel != null && !primaryModel.isBlank()) {
            fallbackModel = primaryModel;
        } else {
            String blockModel = context.getBlockModel(itemName);
            if (blockModel != null && !blockModel.isBlank()) {
                fallbackModel = blockModel;
            } else {
                fallbackModel = itemName;
            }
        }

        List<String> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        appendCandidates(candidates, seen, firmamentModel, itemName, false);
        appendCandidates(candidates, seen, dynamicModel, itemName, false);
        appendCandidates(candidates, seen, primaryModel, itemName, true);
        appendCandidates(candidates, seen, fallbackModel, itemName, true);
        appendCandidates(candidates, seen, itemName, itemName, true);

        if (candidates.isEmpty()) {
            candidates.add(itemName);
        }

        BlockModel model = null;
        String resolvedModelName = null;
        for (String candidate : candidates) {
            try {
                model = context.resolveModel(candidate);
                resolvedModelName = candidate;
                String normalizedName = PackContextManager.normalizeModelIdentifier(resolvedModelName);
                if (!model.getName().equalsIgnoreCase(normalizedName)) {
                    model = new BlockModel(
                        normalizedName, model.getParentChain(), model.getTextures(),
                        model.getDisplay(), model.getElements());
                }
                break;
            } catch (Exception ignored) {
            }
        }

        return new ResolveResult(model, candidates, resolvedModelName);
    }

    private static void appendCandidates(List<String> candidates, Set<String> seen,
                                          @Nullable String primary, String itemName,
                                          boolean includeItemNameFallback) {
        if (primary == null || primary.isBlank()) return;

        if (!includeItemNameFallback) {
            for (String candidate : enumerateCandidateNames(primary)) {
                if (seen.add(candidate.toLowerCase())) {
                    candidates.add(candidate);
                }
            }
            return;
        }

        for (String candidate : buildModelCandidates(primary, itemName)) {
            if (seen.add(candidate.toLowerCase())) {
                candidates.add(candidate);
            }
        }
    }

    // ----------------------------------------------------------------
    // Candidate name generation
    // ----------------------------------------------------------------

    static List<String> buildModelCandidates(String primaryName, String itemName) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String candidate : enumerateCandidateNames(primaryName)) {
            if (seen.add(candidate.toLowerCase())) result.add(candidate);
        }

        if (!primaryName.equalsIgnoreCase(itemName)) {
            for (String candidate : enumerateCandidateNames(itemName)) {
                if (seen.add(candidate.toLowerCase())) result.add(candidate);
            }
        }

        return result;
    }

    private static List<String> enumerateCandidateNames(String name) {
        if (name == null || name.isBlank()) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        result.add(name);
        result.addAll(generateInventoryVariants(name));
        return result;
    }

    private static final String[][] INVENTORY_MODEL_SUFFIXES = {
        {"_fence", "_fence_inventory"},
        {"_wall", "_wall_inventory"},
        {"_button", "_button_inventory"}
    };

    private static List<String> generateInventoryVariants(String name) {
        String[] split = splitModelName(name);
        String prefix = split[0];
        String baseName = split[1];
        if (baseName == null || baseName.isBlank()) return Collections.emptyList();

        List<String> variants = new ArrayList<>();

        if (!baseName.toLowerCase().endsWith("_inventory")) {
            variants.add(prefix + baseName + "_inventory");

            for (String[] suffixPair : INVENTORY_MODEL_SUFFIXES) {
                if (baseName.toLowerCase().endsWith(suffixPair[0].toLowerCase())) {
                    String replaced = baseName.substring(0, baseName.length() - suffixPair[0].length()) + suffixPair[1];
                    variants.add(prefix + replaced);
                }
            }
        }

        return variants;
    }

    private static String[] splitModelName(String name) {
        if (name == null || name.isBlank()) return new String[]{"", ""};
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            return new String[]{name.substring(0, lastSlash + 1), name.substring(lastSlash + 1)};
        }
        return new String[]{"", name};
    }

    // ----------------------------------------------------------------
    // Normalization
    // ----------------------------------------------------------------

    /**
     * Strips the {@code minecraft:} namespace prefix and normalizes slashes.
     *
     * @param itemName the raw item name
     * @return the normalized key
     */
    public static String normalizeItemTextureKey(String itemName) {
        String normalized = itemName.trim();
        if (normalized.toLowerCase().startsWith("minecraft:")) {
            normalized = normalized.substring(10);
        }
        return normalized.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
    }

    public static List<String> normalizeToBlockCandidates(@Nullable String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();

        String normalized = value.trim().replace('\\', '/');
        if (normalized.startsWith("#")) return Collections.emptyList();
        if (normalized.toLowerCase().startsWith("minecraft:")) normalized = normalized.substring(10);
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.toLowerCase().startsWith("textures/")) normalized = normalized.substring(9);
        if (normalized.toLowerCase().startsWith("models/")) normalized = normalized.substring(7);
        if (normalized.toLowerCase().startsWith("block/")) normalized = normalized.substring(6);
        else if (normalized.toLowerCase().startsWith("blocks/")) normalized = normalized.substring(7);
        if (normalized.toLowerCase().startsWith("item/") || normalized.toLowerCase().startsWith("items/")) {
            return Collections.emptyList();
        }
        if (normalized.toLowerCase().startsWith("builtin/")) return Collections.emptyList();

        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.isBlank()) return Collections.emptyList();

        return List.of(normalized);
    }

    // ----------------------------------------------------------------
    // Model type detection
    // ----------------------------------------------------------------

    public static boolean isBillboardModel(BlockModel model) {
        if (model.getTextures().containsKey("cross")) return true;

        for (String parent : model.getParentChain()) {
            if (parentIndicatesBillboard(parent)) return true;
        }

        return parentIndicatesBillboard(model.getName());
    }

    private static boolean parentIndicatesBillboard(String value) {
        if (value == null) return false;
        String lower = value.toLowerCase();
        return lower.contains("cross") || lower.contains("tinted_cross")
            || lower.contains("seagrass") || lower.contains("item/generated")
            || lower.contains("builtin/generated");
    }

    public static boolean isGuiTexture(String textureId) {
        if (textureId == null || textureId.isBlank()) return false;
        String lower = textureId.replace('\\', '/').toLowerCase();
        return lower.contains("/item/") || lower.contains(":item/")
            || lower.contains("/items/") || lower.contains(":items/")
            || lower.contains("textures/item/");
    }

    // ----------------------------------------------------------------
    // Firmament
    // ----------------------------------------------------------------

    static @Nullable String tryGetFirmamentModel(@Nullable ItemRenderData itemData) {
        if (itemData == null || itemData.getCustomData() == null) return null;
        if (!itemData.getCustomData().containsKey("id")) return null;
        StringTag idTag = itemData.getCustomData().getTag("id");
        if (idTag.isEmpty()) return null;
        String encodedId = encodeFirmamentId(idTag.getValue());
        return "firmskyblock:item/" + encodedId;
    }

    public static String encodeFirmamentId(String skyblockId) {
        String lowercaseId = skyblockId.toLowerCase();
        StringBuilder result = new StringBuilder(lowercaseId.length());

        for (char c : lowercaseId.toCharArray()) {
            if (c == ':') {
                result.append("___");
            } else if (c == ';') {
                result.append("__");
            } else if (isValidResourceLocationChar(c)) {
                result.append(c);
            } else {
                result.append(String.format("__%04X", (int) c));
            }
        }

        return result.toString();
    }

    private static boolean isValidResourceLocationChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/';
    }

    // ----------------------------------------------------------------
    // Selector context
    // ----------------------------------------------------------------

    static ItemModelSelector.Context toSelectorContext(
        @Nullable ItemRenderData itemData, String displayContext
    ) {
        if (itemData == null) {
            return new ItemModelSelector.Context(null, displayContext);
        }

        Integer layer0Tint = null;
        if (itemData.getLayer0Tint() != null) {
            int[] rgb = itemData.getLayer0Tint();
            layer0Tint = (0xFF << 24) | ((rgb[0] & 0xFF) << 16) | ((rgb[1] & 0xFF) << 8) | (rgb[2] & 0xFF);
        }

        Map<Integer, Integer> additionalTints = null;
        if (itemData.getAdditionalLayerTints() != null && !itemData.getAdditionalLayerTints().isEmpty()) {
            additionalTints = new HashMap<>();
            for (Map.Entry<Integer, int[]> entry : itemData.getAdditionalLayerTints().entrySet()) {
                int[] rgb = entry.getValue();
                int packed = (0xFF << 24) | ((rgb[0] & 0xFF) << 16) | ((rgb[1] & 0xFF) << 8) | (rgb[2] & 0xFF);
                additionalTints.put(entry.getKey(), packed);
            }
        }

        ItemModelSelector.ItemRenderData selectorData = new ItemModelSelector.ItemRenderData(
            layer0Tint, additionalTints, itemData.isDisableDefaultLayer0Tint(),
            itemData.getCustomData(), itemData.getProfile()
        );

        return new ItemModelSelector.Context(selectorData, displayContext);
    }

    // ----------------------------------------------------------------
    // Utility
    // ----------------------------------------------------------------

    public static @Nullable BlockModel resolveModelOrNull(RenderContext context, String name) {
        try {
            return context.resolveModel(name);
        } catch (Exception ignored) {
            return null;
        }
    }
}
