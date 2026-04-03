package dev.sbs.minecraftapi.render;

import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.ItemRenderData;
import dev.sbs.minecraftapi.render.context.RenderContext;
import dev.sbs.minecraftapi.render.resolver.ItemModelResolver;
import dev.sbs.minecraftapi.render.resolver.PackContextManager;
import dev.sbs.minecraftapi.render.resolver.RenderedResource;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Renderer that produces a 2D image from a Minecraft item model.
 */
@Getter
public final class ItemRenderer implements Renderer {

    private final @NotNull RenderContext context;
    private final @NotNull String itemName;
    private final @NotNull BlockRenderOptions options;

    public ItemRenderer(@NotNull RenderContext context, @NotNull String itemName,
                        @Nullable BlockRenderOptions options) {
        this.context = context;
        this.itemName = itemName;
        this.options = options != null ? options : BlockRenderOptions.DEFAULT;
    }

    public ItemRenderer(@NotNull RenderContext context, @NotNull String itemName,
                        @NotNull ItemRenderData itemData, @Nullable BlockRenderOptions options) {
        this.context = context;
        this.itemName = itemName;
        BlockRenderOptions baseOptions = options != null ? options : BlockRenderOptions.DEFAULT;
        this.options = baseOptions.mutate().withItemData(itemData).build();
    }

    @Override
    public @NotNull BufferedImage render() {
        RenderContext ctx = PackContextManager.resolveContext(context, options.getPackIds());
        BlockRenderOptions forwardedOptions = PackContextManager.forwardOptions(context, options);
        return ctx.renderItem(itemName, forwardedOptions);
    }

    /**
     * Renders the image and returns both the rendered image and a computed resource ID.
     *
     * @return a rendered resource containing the image and resource ID
     */
    public @NotNull RenderedResource renderWithResourceId() {
        RenderContext ctx = PackContextManager.resolveContext(context, options.getPackIds());
        BlockRenderOptions forwardedOptions = PackContextManager.forwardOptions(context, options);
        return ctx.renderItemWithResourceId(itemName, forwardedOptions);
    }

    /**
     * Creates an item renderer from an NBT compound, extracting the item ID and render data.
     *
     * @param context the render context
     * @param compound the NBT compound
     * @param options optional render options
     * @return a configured item renderer
     */
    public static @NotNull ItemRenderer fromNbt(@NotNull RenderContext context, @NotNull CompoundTag compound,
                                                @Nullable BlockRenderOptions options) {
        if (compound == null) throw new IllegalArgumentException("compound cannot be null");
        String itemId = tryGetItemId(compound);
        if (itemId == null) throw new IllegalArgumentException("SNBT item payload did not contain an item id.");
        String normalizedItemId = ItemModelResolver.normalizeItemTextureKey(itemId);

        ItemRenderData itemData = ItemRenderData.fromComponents(compound);
        if (itemData != null)
            return new ItemRenderer(context, normalizedItemId, itemData, options);
        return new ItemRenderer(context, normalizedItemId, options);
    }

    private static final String[] NESTED_KEYS = {"item", "Item", "stack", "Stack"};

    private static @Nullable String tryGetItemId(@NotNull CompoundTag compound) {
        if (compound.containsPath("id")) {
            StringTag tag = compound.getPath("id");
            if (tag.notEmpty())
                return tag.getValue();
        }

        for (String key : NESTED_KEYS) {
            String path = key + ".id";
            if (compound.containsPath(path)) {
                StringTag tag = compound.getPath(path);
                if (tag.notEmpty())
                    return tag.getValue();
            }
        }

        return null;
    }
}
