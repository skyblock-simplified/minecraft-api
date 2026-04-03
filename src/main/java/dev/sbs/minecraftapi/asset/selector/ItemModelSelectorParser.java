package dev.sbs.minecraftapi.asset.selector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

/**
 * Static parser for constructing an {@link ItemModelSelector} tree from a Gson {@link JsonElement}.
 */
public final class ItemModelSelectorParser {

    private static final int MAX_RECURSION_DEPTH = 10_000;

    private ItemModelSelectorParser() {}

    /**
     * Parses an item model selector from the root JSON element of an item model definition.
     *
     * @param root the root JSON element
     * @return the parsed selector, or null if the element could not be parsed
     */
    public static @Nullable ItemModelSelector parseFromRoot(JsonElement root) {
        if (!root.isJsonObject()) return null;
        JsonObject obj = root.getAsJsonObject();

        if (obj.has("model")) {
            JsonElement modelElement = obj.get("model");
            ItemModelSelector optimized = tryOptimizeCustomDataSelector(modelElement);
            if (optimized != null) return optimized;

            ItemModelSelector selector = parse(modelElement, 0);
            if (selector != null) return selector;
        }

        if (obj.has("components") && obj.get("components").isJsonObject()) {
            JsonObject components = obj.getAsJsonObject("components");
            if (components.has("minecraft:model")) {
                ItemModelSelector selector = parse(components.get("minecraft:model"), 0);
                if (selector != null) return selector;
            }
        }

        if (obj.has("type") && obj.get("type").isJsonPrimitive()
            && obj.getAsJsonPrimitive("type").isString()) {
            ItemModelSelector selector = parse(root, 0);
            if (selector != null) return selector;
        }

        if (obj.has("cases") || obj.has("on_true") || obj.has("on_false")) {
            ItemModelSelector selector = parse(root, 0);
            if (selector != null) return selector;
        }

        return null;
    }

    /**
     * Parses a selector from the given JSON element at the specified recursion depth.
     *
     * @param element the JSON element to parse
     * @param depth the current recursion depth
     * @return the parsed selector, or null if parsing fails or depth is exceeded
     */
    public static @Nullable ItemModelSelector parse(@Nullable JsonElement element, int depth) {
        if (element == null || depth > MAX_RECURSION_DEPTH) return null;

        while (true) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
                return new ItemModelSelector.Model(element.getAsString(), null);

            if (!element.isJsonObject()) return null;
            JsonObject obj = element.getAsJsonObject();

            if (!obj.has("type") && !obj.has("cases") && !obj.has("entries") && !obj.has("model")) {
                if (obj.has("on_false")) {
                    element = obj.get("on_false");
                    depth++;
                    continue;
                }
                if (obj.has("on_true")) {
                    element = obj.get("on_true");
                    depth++;
                    continue;
                }
            }

            String type = determineSelectorType(obj);
            return switch (type) {
                case "model" -> new ItemModelSelector.Model(getString(obj, "model"), getString(obj, "base"));
                case "special" -> new ItemModelSelector.Special(
                    getString(obj, "base"),
                    parse(obj.has("model") ? obj.get("model") : null, depth + 1)
                );
                case "condition" -> parseCondition(obj, depth + 1);
                case "select" -> parseSelect(obj, depth + 1);
                case "range_dispatch" -> parseRangeDispatch(obj, depth + 1);
                case "composite" -> parseComposite(obj, depth + 1);
                case "empty" -> new ItemModelSelector.Empty();
                default -> createFallbackSelector(obj);
            };
        }
    }

    private static String determineSelectorType(JsonObject obj) {
        if (obj.has("type") && obj.get("type").isJsonPrimitive()
            && obj.getAsJsonPrimitive("type").isString())
            return normalizeType(obj.get("type").getAsString());

        if (obj.has("cases") && obj.get("cases").isJsonArray()) return "select";
        if (obj.has("entries") && obj.get("entries").isJsonArray()) return "range_dispatch";
        if (obj.has("models") && obj.get("models").isJsonArray()) return "composite";

        if ((obj.has("on_true") || obj.has("on_false")) && obj.has("property")) return "condition";

        if (obj.has("model") && obj.get("model").isJsonObject())
            return determineSelectorType(obj.getAsJsonObject("model"));

        return "model";
    }

    private static @NotNull ItemModelSelector parseCondition(JsonObject obj, int depth) {
        String property = getString(obj, "property");
        if (property == null) property = "";
        String predicate = getString(obj, "predicate");
        String component = getString(obj, "component");

        Map<String, String> valueProperties = null;
        String valueLiteral = null;
        if (obj.has("value")) {
            JsonElement valueElement = obj.get("value");
            valueProperties = parseStringMap(valueElement);
            if (valueProperties == null && valueElement.isJsonPrimitive()
                && valueElement.getAsJsonPrimitive().isString()) {
                valueLiteral = valueElement.getAsString();
            } else if (valueProperties == null) {
                valueLiteral = valueElement.toString();
            }
        }

        ItemModelSelector onTrue = obj.has("on_true") ? parse(obj.get("on_true"), depth + 1) : null;
        ItemModelSelector onFalse = obj.has("on_false") ? parse(obj.get("on_false"), depth + 1) : null;

        if (onTrue == null && onFalse != null)
            return onFalse;

        return new ItemModelSelector.Condition(property, predicate, component, valueProperties, valueLiteral, onTrue, onFalse);
    }

    private static @Nullable ItemModelSelector createFallbackSelector(JsonObject obj) {
        String directModel = getString(obj, "model");
        if (directModel == null) directModel = getString(obj, "base");
        return isBlank(directModel) ? null : new ItemModelSelector.Model(directModel, null);
    }

    private static @Nullable ItemModelSelector parseComposite(JsonObject obj, int depth) {
        if (!obj.has("models") || !obj.get("models").isJsonArray()) return null;
        JsonArray modelsArray = obj.getAsJsonArray("models");

        for (JsonElement modelElement : modelsArray) {
            ItemModelSelector parsed = parse(modelElement, depth);
            if (parsed != null) return parsed;
        }

        return null;
    }

    private static @NotNull ItemModelSelector parseSelect(JsonObject obj, int depth) {
        String property = getString(obj, "property");
        if (property == null) property = "";

        List<ItemModelSelector.SelectCase> cases = new ArrayList<>();
        if (obj.has("cases") && obj.get("cases").isJsonArray()) {
            for (JsonElement caseElement : obj.getAsJsonArray("cases")) {
                if (!caseElement.isJsonObject()) continue;
                JsonObject caseObj = caseElement.getAsJsonObject();
                List<String> whenValues = parseWhen(caseObj.has("when") ? caseObj.get("when") : null);
                ItemModelSelector selector = caseObj.has("model")
                    ? parse(caseObj.get("model"), depth + 1) : null;
                cases.add(new ItemModelSelector.SelectCase(whenValues, selector));
            }
        }

        ItemModelSelector fallback = obj.has("fallback") ? parse(obj.get("fallback"), depth + 1) : null;
        return new ItemModelSelector.Select(property, cases, fallback);
    }

    private static @NotNull ItemModelSelector parseRangeDispatch(JsonObject obj, int depth) {
        String property = getString(obj, "property");
        if (property == null) property = "";
        boolean normalize = obj.has("normalize") && obj.get("normalize").isJsonPrimitive()
            && obj.getAsJsonPrimitive("normalize").getAsBoolean();

        List<ItemModelSelector.RangeDispatchEntry> entries = new ArrayList<>();
        if (obj.has("entries") && obj.get("entries").isJsonArray()) {
            for (JsonElement entryElement : obj.getAsJsonArray("entries")) {
                if (!entryElement.isJsonObject()) continue;
                JsonObject entryObj = entryElement.getAsJsonObject();
                if (entryObj.has("threshold") && entryObj.get("threshold").isJsonPrimitive()) {
                    double threshold = entryObj.get("threshold").getAsDouble();
                    ItemModelSelector selector = entryObj.has("model")
                        ? parse(entryObj.get("model"), depth + 1) : null;
                    entries.add(new ItemModelSelector.RangeDispatchEntry(threshold, selector));
                }
            }
        }

        ItemModelSelector fallback = obj.has("fallback") ? parse(obj.get("fallback"), depth + 1) : null;
        return new ItemModelSelector.RangeDispatch(property, normalize, entries, fallback);
    }

    private static @Nullable Map<String, String> parseStringMap(JsonElement element) {
        if (!element.isJsonObject()) return null;

        Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            JsonElement val = entry.getValue();
            String value;
            if (val.isJsonPrimitive()) {
                var prim = val.getAsJsonPrimitive();
                if (prim.isString()) {
                    value = prim.getAsString();
                } else if (prim.isNumber()) {
                    try {
                        value = String.valueOf(prim.getAsLong());
                    } catch (NumberFormatException e) {
                        value = String.valueOf(prim.getAsDouble());
                    }
                } else if (prim.isBoolean()) {
                    value = String.valueOf(prim.getAsBoolean());
                } else {
                    value = val.toString();
                }
            } else if (val.isJsonNull()) {
                value = "null";
            } else {
                value = val.toString();
            }

            if (!isBlank(entry.getKey()) && !isBlank(value))
                map.put(entry.getKey(), value);
        }

        return map.isEmpty() ? null : map;
    }

    private static List<String> parseWhen(@Nullable JsonElement element) {
        if (element == null) return Collections.emptyList();

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            return isBlank(value) ? Collections.emptyList() : List.of(value);
        }

        if (element.isJsonArray()) {
            List<String> values = new ArrayList<>();
            for (JsonElement entry : element.getAsJsonArray()) {
                if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                    String value = entry.getAsString();
                    if (!isBlank(value)) values.add(value);
                }
            }
            return values;
        }

        return Collections.emptyList();
    }

    private static @Nullable String getString(JsonObject obj, String propertyName) {
        if (obj.has(propertyName) && obj.get(propertyName).isJsonPrimitive()
            && obj.getAsJsonPrimitive(propertyName).isString())
            return obj.get(propertyName).getAsString();
        return null;
    }

    private static String normalizeType(@Nullable String value) {
        if (isBlank(value)) return "model";
        String type = value.trim();
        if (type.toLowerCase().startsWith("minecraft:"))
            type = type.substring(10);
        return type.toLowerCase();
    }

    // ---- Optimization ----

    private static @Nullable ItemModelSelector tryOptimizeCustomDataSelector(JsonElement element) {
        int[] estimatedDepth = new int[1];
        if (!isDeepCustomDataConditional(element, estimatedDepth)) return null;

        Map<String, String> modelMappings = new HashMap<>();
        Map<String, ItemModelSelector> selectorMappings = new HashMap<>();
        List<ItemModelSelector.Optimized.CompositeMapping> compositeMappings = new ArrayList<>();
        ExtractionResult result = extractCustomDataMappings(
            element, modelMappings, selectorMappings, compositeMappings, 0, 100_000
        );

        if (result.encounteredUnsupportedCondition()
            || (modelMappings.isEmpty() && selectorMappings.isEmpty()))
            return null;

        ItemModelSelector fallbackSelector = null;
        if (result.fallbackModel() != null) {
            JsonElement fallbackModel = result.fallbackModel();
            if (fallbackModel.isJsonPrimitive() && fallbackModel.getAsJsonPrimitive().isString()) {
                String modelStr = fallbackModel.getAsString();
                if (!isBlank(modelStr))
                    fallbackSelector = new ItemModelSelector.Model(modelStr, null);
            } else if (fallbackModel.isJsonObject()) {
                fallbackSelector = parse(fallbackModel, 0);
            }
        }

        return new ItemModelSelector.Optimized(modelMappings, selectorMappings, compositeMappings, fallbackSelector);
    }

    private record ExtractionResult(
        @Nullable JsonElement fallbackModel,
        boolean encounteredUnsupportedCondition
    ) {}

    private static boolean isDeepCustomDataConditional(JsonElement element, int[] estimatedDepth) {
        estimatedDepth[0] = 0;
        if (!element.isJsonObject()) return false;

        JsonElement current = element;
        if (current.isJsonObject() && current.getAsJsonObject().has("fallback"))
            current = current.getAsJsonObject().get("fallback");

        int customDataCount = 0;
        int depth = 0;

        for (int i = 0; i < 20; i++) {
            if (!current.isJsonObject()) break;
            JsonObject obj = current.getAsJsonObject();

            if (hasStringValue(obj, "type", "condition")) {
                if (hasStringValue(obj, "property", "component")
                    && hasStringValue(obj, "predicate", "custom_data"))
                    customDataCount++;
                depth++;

                if (obj.has("on_false")) {
                    current = obj.get("on_false");
                    continue;
                }
            }
            break;
        }

        if (customDataCount >= 15) {
            estimatedDepth[0] = depth * 400;
            return true;
        }

        return false;
    }

    private static boolean hasStringValue(JsonObject obj, String key, String expected) {
        return obj.has(key) && obj.get(key).isJsonPrimitive()
            && obj.getAsJsonPrimitive(key).isString()
            && expected.equals(obj.get(key).getAsString());
    }

    private static ExtractionResult extractCustomDataMappings(
        JsonElement root,
        Map<String, String> modelMappings,
        Map<String, ItemModelSelector> selectorMappings,
        List<ItemModelSelector.Optimized.CompositeMapping> compositeMappings,
        int startDepth,
        int maxDepth
    ) {
        JsonElement startElement = root;
        if (root.isJsonObject() && root.getAsJsonObject().has("fallback"))
            startElement = root.getAsJsonObject().get("fallback");

        Queue<ElementWithDepth> queue = new LinkedList<>();
        queue.add(new ElementWithDepth(startElement, startDepth));
        JsonElement fallbackModel = null;
        boolean encounteredUnsupportedCondition = false;

        while (!queue.isEmpty()) {
            ElementWithDepth item = queue.poll();
            JsonElement current = item.element();
            int depth = item.depth();

            if (depth > maxDepth) continue;

            if (current.isJsonPrimitive() && current.getAsJsonPrimitive().isString()) {
                fallbackModel = current;
                continue;
            }

            if (!current.isJsonObject()) continue;
            JsonObject obj = current.getAsJsonObject();

            if (hasStringValue(obj, "type", "condition")
                && hasStringValue(obj, "property", "component")
                && hasStringValue(obj, "predicate", "custom_data")) {

                String customDataId = null;
                Map<String, String> compositeExpectedValues = null;
                boolean supportedKeyFound = false;

                if (obj.has("value")) {
                    JsonElement valueEl = obj.get("value");
                    if (valueEl.isJsonPrimitive() && valueEl.getAsJsonPrimitive().isString()) {
                        customDataId = valueEl.getAsString();
                        supportedKeyFound = !isBlank(customDataId);
                    } else if (valueEl.isJsonObject()) {
                        JsonObject valueObj = valueEl.getAsJsonObject();
                        if (valueObj.has("id") && valueObj.get("id").isJsonPrimitive()
                            && valueObj.getAsJsonPrimitive("id").isString()) {
                            customDataId = valueObj.get("id").getAsString();
                            supportedKeyFound = true;
                        } else if (valueObj.has("model") && valueObj.get("model").isJsonPrimitive()
                            && valueObj.getAsJsonPrimitive("model").isString()) {
                            customDataId = valueObj.get("model").getAsString();
                            supportedKeyFound = true;
                        }

                        Map<String, String> extracted = null;
                        for (Map.Entry<String, JsonElement> prop : valueObj.entrySet()) {
                            if (prop.getValue().isJsonPrimitive()
                                && prop.getValue().getAsJsonPrimitive().isString()) {
                                if (extracted == null) extracted = new HashMap<>();
                                extracted.put(prop.getKey(), prop.getValue().getAsString());
                            }
                        }

                        if (extracted != null) {
                            if (!isBlank(customDataId)) {
                                extracted.remove("id");
                                extracted.remove("model");
                            }
                            if (!extracted.isEmpty()) {
                                compositeExpectedValues = extracted;
                                supportedKeyFound = true;
                            }
                        }
                    }
                }

                if (!supportedKeyFound)
                    encounteredUnsupportedCondition = true;

                ItemModelSelector selector = null;
                String model = null;

                if (obj.has("on_true")) {
                    model = extractModelFromElement(obj.get("on_true"));
                    if (isBlank(model))
                        selector = parse(obj.get("on_true"), 0);
                }

                if (!isBlank(customDataId)) {
                    if (!isBlank(model))
                        modelMappings.put(customDataId, model);
                    else if (selector != null)
                        selectorMappings.put(customDataId, selector);
                }

                if (compositeExpectedValues != null && (!isBlank(model) || selector != null)) {
                    compositeMappings.add(new ItemModelSelector.Optimized.CompositeMapping(
                        compositeExpectedValues,
                        isBlank(model) ? null : model,
                        selector
                    ));
                }

                if (obj.has("on_false"))
                    queue.add(new ElementWithDepth(obj.get("on_false"), depth + 1));
            } else {
                fallbackModel = current;
            }
        }

        return new ExtractionResult(fallbackModel, encounteredUnsupportedCondition);
    }

    private record ElementWithDepth(JsonElement element, int depth) {}

    private static @Nullable String extractModelFromElement(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
            return element.getAsString();

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("model") && obj.get("model").isJsonPrimitive()
                && obj.getAsJsonPrimitive("model").isString())
                return obj.get("model").getAsString();
            if (obj.has("type") && obj.get("type").isJsonPrimitive()
                && obj.getAsJsonPrimitive("type").isString()) {
                String type = obj.get("type").getAsString();
                if ("model".equals(type) && obj.has("model")
                    && obj.get("model").isJsonPrimitive()
                    && obj.getAsJsonPrimitive("model").isString())
                    return obj.get("model").getAsString();
            }
        }

        return null;
    }

    private static boolean isBlank(@Nullable String s) {
        return ItemModelSelector.isBlank(s);
    }
}
