package dev.sbs.minecraftapi.asset.model;

import dev.simplified.persistence.JpaModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Information about a single registered block.
 */
@Getter
@Setter
@Entity
@Table(name = "block_info")
public class BlockInfo implements JpaModel {

    @Id
    @Column(name = "name", nullable = false)
    private @NotNull String name = "";

    @Column(name = "block_state")
    private @Nullable String blockState;

    @Column(name = "model")
    private @Nullable String model;

    @Column(name = "texture")
    private @Nullable String texture;
}
