package dev.sbs.minecraftapi.asset.selector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.sbs.minecraftapi.nbt.tags.Tag;
import dev.sbs.minecraftapi.nbt.tags.TagType;
import dev.sbs.minecraftapi.nbt.tags.array.ByteArrayTag;
import dev.sbs.minecraftapi.nbt.tags.array.IntArrayTag;
import dev.sbs.minecraftapi.nbt.tags.array.LongArrayTag;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.collection.ListTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.DoubleTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.FloatTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.NumericalTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A sealed interface representing an item model selector that resolves a model name from a context.
 *
 * <p>Implementations form a tree structure parsed from item model JSON definitions, supporting
 * conditional branching, selection by property, range dispatch, and optimized custom data lookups.
 */
public sealed interface ItemModelSelector
    permits ItemModelSelector.Model,
            ItemModelSelector.Special,
            ItemModelSelector.Condition,
            ItemModelSelector.Select,
            ItemModelSelector.RangeDispatch,
            ItemModelSelector.Optimized,
            ItemModelSelector.Empty {

    /**
     * Resolves this selector to a model name given the provided context.
     *
     * @param context the item model context
     * @return the resolved model name, or null if no model could be resolved
     */
    @Nullable String resolve(@NotNull Context context);

    // ---- Context types ----

    /**
     * Contextual data for item model resolution.
     *
     * @param itemData the item render data, or null if unavailable
     * @param displayContext the display context string (e.g. "gui", "thirdperson_righthand")
     */
    record Context(@Nullable ItemRenderData itemData, @NotNull String displayContext) {}

    /**
     * Item render data carrying optional tint, custom data, and profile information.
     *
     * @param layer0Tint the layer 0 tint color as an ARGB integer, or null
     * @param additionalLayerTints additional layer tints keyed by layer index, or null
     * @param disableDefaultLayer0Tint whether to disable the default layer 0 tint
     * @param customData the custom data NBT compound, or null
     * @param profile the profile NBT compound, or null
     */
    record ItemRenderData(
        @Nullable Integer layer0Tint,
        @Nullable Map<Integer, Integer> additionalLayerTints,
        boolean disableDefaultLayer0Tint,
        @Nullable CompoundTag customData,
        @Nullable CompoundTag profile
    ) {}

    // ---- Inner data containers ----

    /**
     * A case entry in a {@link Select} selector.
     *
     * @param when the list of matching values
     * @param selector the selector to resolve when matched
     */
    record SelectCase(@NotNull List<String> when, @Nullable ItemModelSelector selector) {}

    /**
     * A threshold entry in a {@link RangeDispatch} selector.
     *
     * @param threshold the threshold value
     * @param selector the selector to resolve when the value meets this threshold
     */
    record RangeDispatchEntry(double threshold, @Nullable ItemModelSelector selector) {}

    // ---- Implementations ----

    /**
     * A selector that resolves to a direct model name or a base model fallback.
     */
    @Getter
    final class Model implements ItemModelSelector {
        private final @Nullable String model;
        private final @Nullable String baseModel;

        public Model(@Nullable String model, @Nullable String baseModel) {
            this.model = isBlank(model) ? null : model.trim();
            this.baseModel = isBlank(baseModel) ? null : baseModel.trim();
        }

        @Override
        public @Nullable String resolve(Context context) {
            return model != null ? model : baseModel;
        }
    }

    /**
     * A selector for special model types that delegates to a nested selector or falls back to a base model.
     */
    @Getter
    final class Special implements ItemModelSelector {
        private final @Nullable String baseModel;
        private final @Nullable ItemModelSelector nested;

        public Special(@Nullable String baseModel, @Nullable ItemModelSelector nested) {
            this.baseModel = isBlank(baseModel) ? null : baseModel.trim();
            this.nested = nested;
        }

        @Override
        public @Nullable String resolve(Context context) {
            if (nested != null) {
                String result = nested.resolve(context);
                if (result != null) return result;
            }
            return baseModel;
        }
    }

    /**
     * A selector that evaluates a condition and delegates to an on-true or on-false branch.
     */
    @Getter
    @RequiredArgsConstructor
    final class Condition implements ItemModelSelector {

        private final @NotNull String property;
        private final @Nullable String predicate;
        private final @Nullable String component;
        private final @Nullable Map<String, String> valueProperties;
        private final @Nullable String valueLiteral;
        private final @Nullable ItemModelSelector onTrue;
        private final @Nullable ItemModelSelector onFalse;

        @Override
        public @Nullable String resolve(Context context) {
            return evaluateCondition(context)
                ? (onTrue != null ? onTrue.resolve(context) : null)
                : (onFalse != null ? onFalse.resolve(context) : null);
        }

        private boolean evaluateCondition(Context context) {
            if (property.equalsIgnoreCase("component")) {
                return evaluateComponentCondition(context);
            }

            if (property.equalsIgnoreCase("display_context")) {
                if (valueProperties != null && !valueProperties.isEmpty()) {
                    String expected = valueProperties.get("value");
                    if (expected == null) {
                        expected = valueProperties.get("equals");
                    }
                    if (expected != null) {
                        return expected.equalsIgnoreCase(context.displayContext());
                    }
                }

                if (!isBlank(valueLiteral)) {
                    return valueLiteral.equalsIgnoreCase(context.displayContext());
                }

                return false;
            }

            if (property.equalsIgnoreCase("selected")) {
                return false;
            }

            return false;
        }

        private boolean evaluateComponentCondition(Context context) {
            String pred = predicate != null ? predicate : "";
            if (pred.equalsIgnoreCase("custom_data")) {
                return evaluateCustomData(context);
            }
            return false;
        }

        private boolean evaluateCustomData(Context context) {
            CompoundTag customData = context.itemData() != null ? context.itemData().customData() : null;
            if (customData == null) {
                return false;
            }

            if (valueProperties != null && !valueProperties.isEmpty()) {
                for (Map.Entry<String, String> entry : valueProperties.entrySet()) {
                    if (!tryMatchCustomDataValue(customData, entry.getKey(), entry.getValue())) {
                        return false;
                    }
                }
                return true;
            }

            if (!isBlank(valueLiteral) && customData.containsType("id", TagType.STRING)) {
                StringTag idTag = customData.getTag("id");
                if (idTag.notEmpty())
                    return idTag.getValue().equalsIgnoreCase(valueLiteral);
            }

            return false;
        }

        static boolean tryMatchCustomDataValue(CompoundTag compound, String key, String expected) {
            if (!compound.containsKey(key))
                return false;

            Tag<?> tag = compound.getTag(key);

            if (isJsonStructure(expected))
                return tryMatchJsonStructure(tag, expected);

            return matchesPrimitiveValue(tag, expected);
        }

        private static boolean matchesPrimitiveValue(Tag<?> tag, String expected) {
            Boolean expectedBool = tryParseBoolean(expected);
            if (expectedBool != null) {
                if (tag instanceof NumericalTag<?> n) return (n.longValue() != 0) == expectedBool;
                if (tag instanceof StringTag s) {
                    Boolean parsed = tryParseBoolean(s.getValue());
                    return parsed != null && parsed.equals(expectedBool);
                }
                return false;
            }

            if (tag instanceof StringTag s) return s.getValue().equals(expected);
            if (tag instanceof NumericalTag<?> n) return n.toString().equals(expected);
            return false;
        }

        private static boolean isJsonStructure(String value) {
            if (isBlank(value)) return false;
            String trimmed = value.stripLeading();
            return trimmed.startsWith("{") || trimmed.startsWith("[");
        }

        private static boolean tryMatchJsonStructure(Tag<?> tag, String json) {
            try {
                JsonElement element = JsonParser.parseString(json);
                return matchTagWithJson(tag, element);
            } catch (JsonSyntaxException e) {
                return false;
            }
        }

        private static boolean matchTagWithJson(Tag<?> tag, JsonElement expected) {
            if (expected.isJsonObject()) {
                return tag instanceof CompoundTag compound && matchCompound(compound, expected.getAsJsonObject());
            }
            if (expected.isJsonArray()) {
                return matchArray(tag, expected.getAsJsonArray());
            }
            if (expected.isJsonPrimitive()) {
                var prim = expected.getAsJsonPrimitive();
                if (prim.isString()) {
                    return tag instanceof StringTag s && s.getValue().equals(prim.getAsString());
                }
                if (prim.isNumber()) {
                    return matchNumeric(tag, prim);
                }
                if (prim.isBoolean()) {
                    return matchesPrimitiveValue(tag, prim.getAsBoolean() ? "true" : "false");
                }
            }
            if (expected.isJsonNull()) {
                return false;
            }
            return false;
        }

        private static boolean matchCompound(CompoundTag compound, JsonObject expected) {
            for (Map.Entry<String, JsonElement> entry : expected.entrySet()) {
                if (!compound.containsKey(entry.getKey())) return false;
                if (!matchTagWithJson(compound.getTag(entry.getKey()), entry.getValue())) return false;
            }
            return true;
        }

        private static boolean matchArray(Tag<?> tag, JsonArray expected) {
            if (tag instanceof ListTag<?> list) return matchList(list, expected);
            if (tag instanceof ByteArrayTag byteArray) return matchPrimitiveArray(toDoubleList(byteArray), expected);
            if (tag instanceof IntArrayTag intArray) return matchPrimitiveArray(toDoubleList(intArray), expected);
            if (tag instanceof LongArrayTag longArray) return matchPrimitiveArray(toDoubleList(longArray), expected);
            return false;
        }

        private static List<Double> toDoubleList(ByteArrayTag array) {
            List<Double> result = new ArrayList<>(array.getValue().length);
            for (Byte v : array.getValue()) result.add((double) v);
            return result;
        }

        private static List<Double> toDoubleList(IntArrayTag array) {
            List<Double> result = new ArrayList<>(array.getValue().length);
            for (Integer v : array.getValue()) result.add((double) v);
            return result;
        }

        private static List<Double> toDoubleList(LongArrayTag array) {
            List<Double> result = new ArrayList<>(array.getValue().length);
            for (Long v : array.getValue()) result.add((double) v);
            return result;
        }

        private static boolean matchList(ListTag<?> list, JsonArray expected) {
            if (list.size() != expected.size()) return false;
            for (int i = 0; i < expected.size(); i++) {
                if (!matchTagWithJson(list.get(i), expected.get(i))) return false;
            }
            return true;
        }

        private static boolean matchPrimitiveArray(List<Double> actualValues, JsonArray expected) {
            if (actualValues.size() != expected.size()) return false;
            for (int i = 0; i < expected.size(); i++) {
                JsonElement elem = expected.get(i);
                if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isNumber()) return false;
                if (!numericEquals(actualValues.get(i), elem.getAsDouble())) return false;
            }
            return true;
        }

        private static boolean matchNumeric(Tag<?> tag, com.google.gson.JsonPrimitive expected) {
            if (!(tag instanceof NumericalTag<?> n)) return false;
            if (tag instanceof FloatTag || tag instanceof DoubleTag)
                return numericEquals(n.doubleValue(), expected.getAsDouble());
            try {
                return n.longValue() == expected.getAsLong();
            } catch (NumberFormatException ignored) {}
            return numericEquals(n.doubleValue(), expected.getAsDouble());
        }

        private static boolean numericEquals(double actual, double expected) {
            return Math.abs(actual - expected) < 1e-6;
        }

        private static @Nullable Boolean tryParseBoolean(String value) {
            if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
            if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
            return null;
        }
    }

    /**
     * A selector that evaluates a property against a list of cases and resolves the first match.
     */
    @Getter
    @RequiredArgsConstructor
    final class Select implements ItemModelSelector {

        private final @NotNull String property;
        private final @NotNull List<SelectCase> cases;
        private final @Nullable ItemModelSelector fallback;

        @Override
        public @Nullable String resolve(Context context) {
            for (SelectCase selectCase : cases) {
                if (matches(selectCase.when(), context)) {
                    String resolved = selectCase.selector() != null ? selectCase.selector().resolve(context) : null;
                    if (!isBlank(resolved)) {
                        return resolved;
                    }
                }
            }
            return fallback != null ? fallback.resolve(context) : null;
        }

        private boolean matches(List<String> when, Context context) {
            if (when.isEmpty()) return false;

            if (property.equalsIgnoreCase("display_context")) {
                return when.stream().anyMatch(v -> v.equalsIgnoreCase(context.displayContext()));
            }

            if (property.equalsIgnoreCase("component")) {
                for (String value : when) {
                    if (matchesComponentValue(value, context)) return true;
                }
                return false;
            }

            return false;
        }

        private static boolean matchesComponentValue(@Nullable String value, Context context) {
            if (isBlank(value)) return false;
            ItemRenderData itemData = context.itemData();
            if (itemData == null) return false;

            if ("minecraft:custom_data".equalsIgnoreCase(value) || "custom_data".equalsIgnoreCase(value)) {
                return itemData.customData() != null;
            }
            if ("minecraft:profile".equalsIgnoreCase(value) || "profile".equalsIgnoreCase(value)) {
                return itemData.profile() != null;
            }
            if ("minecraft:dyed_color".equalsIgnoreCase(value) || "dyed_color".equalsIgnoreCase(value)) {
                return itemData.layer0Tint() != null
                    || (itemData.additionalLayerTints() != null && !itemData.additionalLayerTints().isEmpty())
                    || itemData.disableDefaultLayer0Tint();
            }
            return false;
        }
    }

    /**
     * A selector that matches a numeric property value against threshold entries.
     */
    @Getter
    @RequiredArgsConstructor
    final class RangeDispatch implements ItemModelSelector {

        private final @NotNull String property;
        private final boolean normalize;
        private final @NotNull List<RangeDispatchEntry> entries;
        private final @Nullable ItemModelSelector fallback;

        @Override
        public @Nullable String resolve(Context context) {
            Double value = getPropertyValue(context);
            if (value == null) {
                return fallback != null ? fallback.resolve(context) : null;
            }

            RangeDispatchEntry matchedEntry = null;
            for (RangeDispatchEntry entry : entries) {
                if (value >= entry.threshold()) {
                    if (matchedEntry == null || entry.threshold() > matchedEntry.threshold()) {
                        matchedEntry = entry;
                    }
                }
            }

            if (matchedEntry != null && matchedEntry.selector() != null) {
                String resolved = matchedEntry.selector().resolve(context);
                if (!isBlank(resolved)) {
                    return resolved;
                }
            }

            return fallback != null ? fallback.resolve(context) : null;
        }

        private @Nullable Double getPropertyValue(Context context) {
            if ("count".equalsIgnoreCase(property)) {
                return 1.0;
            }
            return null;
        }
    }

    /**
     * An optimized selector for deeply nested conditional trees.
     *
     * <p>Pre-builds a lookup table for custom_data.id to model mappings to avoid stack overflow
     * and provide O(1) resolution for custom items.
     */
    @RequiredArgsConstructor
    final class Optimized implements ItemModelSelector {

        private final @NotNull Map<String, String> customDataIdToModel;
        private final @NotNull Map<String, ItemModelSelector> customDataIdToSelector;
        private final @NotNull List<CompositeMapping> compositeMappings;
        private final @Nullable ItemModelSelector fallbackSelector;

        /**
         * A composite mapping entry pairing expected custom data values with a model or selector.
         *
         * @param expectedValues the expected custom data key-value pairs
         * @param model the direct model name, or null
         * @param selector the selector to resolve, or null
         */
        record CompositeMapping(
            @NotNull Map<String, String> expectedValues,
            @Nullable String model,
            @Nullable ItemModelSelector selector
        ) {}

        @Override
        public @Nullable String resolve(Context context) {
            if (context.itemData() != null && context.itemData().customData() != null) {
                CompoundTag customData = context.itemData().customData();
                String customDataKey = null;

                if (customData.containsType("id", TagType.STRING)) {
                    StringTag idString = customData.getTag("id");
                    if (idString.notEmpty())
                        customDataKey = idString.getValue();
                }
                if (customDataKey == null && customData.containsType("model", TagType.STRING)) {
                    StringTag modelString = customData.getTag("model");
                    if (modelString.notEmpty())
                        customDataKey = modelString.getValue();
                }

                if (customDataKey != null) {
                    String model = customDataIdToModel.get(customDataKey);
                    if (model != null) return model;

                    ItemModelSelector selector = customDataIdToSelector.get(customDataKey);
                    if (selector != null) return selector.resolve(context);
                }
            }

            if (context.itemData() != null && context.itemData().customData() != null
                && !compositeMappings.isEmpty()) {
                CompoundTag compositeCustomData = context.itemData().customData();
                for (CompositeMapping mapping : compositeMappings) {
                    if (!matchesComposite(compositeCustomData, mapping.expectedValues())) continue;
                    if (mapping.selector() != null) return mapping.selector().resolve(context);
                    if (!isBlank(mapping.model())) return mapping.model();
                }
            }

            return fallbackSelector != null ? fallbackSelector.resolve(context) : null;
        }

        private static boolean matchesComposite(CompoundTag customData, Map<String, String> expected) {
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                if (!Condition.tryMatchCustomDataValue(customData, entry.getKey(), entry.getValue())) {
                    return false;
                }
            }
            return true;
        }

    }

    /**
     * A selector that always resolves to null.
     */
    final class Empty implements ItemModelSelector {
        @Override
        public @Nullable String resolve(Context context) {
            return null;
        }
    }

    // ---- Utility ----

    static boolean isBlank(@Nullable String s) {
        return s == null || s.isBlank();
    }
}
