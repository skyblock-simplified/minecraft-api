package dev.sbs.minecraftapi.asset.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.type.GsonType;
import dev.sbs.api.util.StringUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * A fully resolved and registered resource pack with filesystem paths, namespace mappings,
 * content fingerprint, and CIT support status.
 */
@Getter
@Setter
@Entity
@Table(name = "resource_pack")
public class ResourcePack implements JpaModel {

    @Id
    @Column(name = "id", nullable = false)
    private @NotNull String id = "";

    @Column(name = "root_path", nullable = false)
    private @NotNull String rootPath = "";

    @Column(name = "assets_path", nullable = false)
    private @NotNull String assetsPath = "";

    @Column(name = "namespace_roots", nullable = false)
    private @NotNull ConcurrentMap<String, String> namespaceRoots = Concurrent.newMap();

    @Column(name = "meta", nullable = false)
    private @NotNull Meta meta = new Meta();

    @Column(name = "last_write_time_utc", nullable = false)
    private long lastWriteTimeUtc;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "supports_cit", nullable = false)
    private boolean supportsCit;

    @Column(name = "pack_format")
    private @Nullable Integer packFormat;

    @Column(name = "fingerprint", nullable = false)
    private @NotNull String fingerprint = "";

    /** The human-readable display name, delegated from the pack metadata. */
    public @NotNull String getDisplayName() {
        return meta.getName();
    }

    /** The last modification time of the pack directory. */
    public @NotNull Instant getLastWriteTime() {
        return Instant.ofEpochMilli(lastWriteTimeUtc);
    }

    /**
     * Looks up the resolved filesystem path for the given namespace.
     *
     * @param namespace the namespace name to resolve
     * @return an optional containing the path if the namespace exists, or empty otherwise
     */
    public @NotNull Optional<String> getNamespacePath(@NotNull String namespace) {
        String resolved = namespaceRoots.get(namespace.toLowerCase());
        return Optional.ofNullable(resolved);
    }

    /**
     * Enumerates all unique overlay root paths for this pack, prioritizing well-known namespaces
     * ({@code minecraft}, {@code firmskyblock}, {@code cittofirmgenerated}, {@code cit}) before
     * any remaining namespaces.
     *
     * @return an unmodifiable list of unique overlay root paths in priority order
     */
    public @NotNull List<String> enumerateOverlayRootPaths() {
        SequencedMap<String, String> emitted = new LinkedHashMap<>();
        String[] prioritized = { "minecraft", "firmskyblock", "cittofirmgenerated", "cit" };

        for (String namespaceName : prioritized) {
            String namespacePath = namespaceRoots.get(namespaceName.toLowerCase());
            if (namespacePath != null)
                emitted.putIfAbsent(namespacePath.toLowerCase(), namespacePath);
        }

        for (String namespacePath : namespaceRoots.values())
            emitted.putIfAbsent(namespacePath.toLowerCase(), namespacePath);

        return Collections.unmodifiableList(new ArrayList<>(emitted.values()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourcePack that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Metadata describing a resource pack - its identity, version, authorship, and download URL.
     */
    @Getter
    @Setter
    @GsonType
    public static class Meta {

        /** The unique pack identifier. */
        @SerializedName("id")
        private @NotNull String id = "";

        @SerializedName("name")
        private @Nullable String name;

        @SerializedName("version")
        private @Nullable String version;

        /** A human-readable description of the pack. */
        @SerializedName("description")
        private @NotNull String description = "";

        /** The list of author names. */
        @SerializedName("authors")
        private @NotNull ConcurrentList<String> authors = Concurrent.newList();

        /** The optional download URL. */
        @SerializedName("downloadUrl")
        private @NotNull Optional<String> downloadUrl = Optional.empty();

        /** The display name, defaulting to the pack identifier when absent. */
        public @NotNull String getName() {
            return StringUtil.isNotEmpty(this.name) ? this.name : this.getId();
        }

        /** The semantic version string, defaulting to {@code "0.0.0"} when absent. */
        public @NotNull String getVersion() {
            return StringUtil.isNotEmpty(this.version) ? this.version : "0.0.0";
        }
    }
}
