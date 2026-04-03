package dev.sbs.minecraftapi.render;


import dev.sbs.minecraftapi.render.context.BlockFaceRenderOptions;
import dev.sbs.minecraftapi.render.context.RenderContext;
import dev.sbs.minecraftapi.render.resolver.PackContextManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Renderer that produces a flat 2D image of a single block face texture.
 */
@Getter
public final class BlockFaceRenderer implements Renderer {

    private final @NotNull RenderContext context;
    private final @NotNull String blockName;
    private final @NotNull BlockFaceRenderOptions options;

    public BlockFaceRenderer(@NotNull RenderContext context, @NotNull String blockName,
                             @Nullable BlockFaceRenderOptions options) {
        this.context = context;
        this.blockName = blockName;
        this.options = options != null ? options : BlockFaceRenderOptions.DEFAULT;
    }

    @Override
    public @NotNull BufferedImage render() {
        if (blockName.isBlank())
            throw new IllegalArgumentException("blockName cannot be blank");
        RenderContext ctx = PackContextManager.resolveContext(context, options.getPackIds());
        return ctx.renderBlockFace(blockName, options);
    }
}
