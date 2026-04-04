package dev.sbs.minecraftapi.asset.namespace;

import org.jetbrains.annotations.NotNull;

/**
 * A parsed Minecraft resource identifier split into its namespace name and relative path.
 */
public record Namespace(@NotNull String name, @NotNull String path) {

    /** The default namespace name used when no explicit namespace is specified. */
    public static final String DEFAULT_NAME = "minecraft";

    /**
     * Parses a resource identifier into its namespace and path components.
     *
     * <p>Leading slashes are stripped and backslashes are normalized to forward slashes.
     * If no colon delimiter is present, the namespace defaults to {@value #DEFAULT_NAME}.
     *
     * @param resourceId the raw resource identifier
     * @return the parsed namespace
     */
    public static @NotNull Namespace of(@NotNull String resourceId) {
        String sanitized = resourceId;
        while (sanitized.startsWith("/"))
            sanitized = sanitized.substring(1);
        sanitized = sanitized.replace('\\', '/');
        int colonIndex = sanitized.indexOf(':');
        if (colonIndex >= 0)
            return new Namespace(sanitized.substring(0, colonIndex), sanitized.substring(colonIndex + 1));
        return new Namespace(DEFAULT_NAME, sanitized);
    }
}
