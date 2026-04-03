package dev.sbs.minecraftapi.asset.model;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.JpaModel;
import dev.sbs.api.persistence.type.GsonType;
import dev.sbs.minecraftapi.asset.selector.ItemModelSelector;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Information about a single registered item.
 */
@Getter
@Setter
@Entity
@Table(name = "item_info")
public class ItemInfo implements JpaModel {

    @Id
    @Column(name = "name", nullable = false)
    private @NotNull String name = "";

    @Column(name = "model")
    private @Nullable String model;

    @Column(name = "texture")
    private @Nullable String texture;

    private transient @Nullable ItemModelSelector selector;

    @Column(name = "layer_tints", nullable = false)
    private @NotNull ConcurrentMap<Integer, TintInfo> layerTints = Concurrent.newMap();

    // ================================================================
    // Inner types
    // ================================================================

    /**
     * Tint information for a single item layer.
     */
    @Getter
    @Setter
    @GsonType
    public static final class TintInfo {

        private @NotNull Kind kind = Kind.UNSPECIFIED;
        private int @Nullable [] defaultColor;

        /**
         * The kind of tint applied to an item layer.
         */
        public enum Kind {

            UNSPECIFIED,
            DYE,
            CONSTANT,
            UNKNOWN

        }
    }
}
