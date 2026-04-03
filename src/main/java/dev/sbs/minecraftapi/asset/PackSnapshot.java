package dev.sbs.minecraftapi.asset;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.asset.model.BlockInfo;
import dev.sbs.minecraftapi.asset.model.ItemInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable snapshot of pack-specific asset data.
 * <p>
 * Contains all data needed to render with a specific texture pack stack,
 * including the full pack-specific asset context and pack-overridden block/item infos.
 *
 * @param assetContext the pack-specific asset context with resolved models, textures, overlays
 * @param blockInfos the pack-overridden block info entries
 * @param itemInfos the pack-overridden item info entries
 */
public record PackSnapshot(
    @NotNull AssetContext assetContext,
    @NotNull ConcurrentList<BlockInfo> blockInfos,
    @NotNull ConcurrentList<ItemInfo> itemInfos
) {}
