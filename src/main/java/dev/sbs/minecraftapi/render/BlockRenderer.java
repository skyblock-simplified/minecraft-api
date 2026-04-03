package dev.sbs.minecraftapi.render;


import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.RenderContext;
import dev.sbs.minecraftapi.render.resolver.PackContextManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Renderer that produces a 2D image from a 3D Minecraft block model.
 */
@Getter
public final class BlockRenderer implements Renderer {

    private final @NotNull RenderContext context;
    private final @NotNull String blockName;
    private final @NotNull BlockRenderOptions options;

    public BlockRenderer(@NotNull RenderContext context, @NotNull String blockName,
                         @Nullable BlockRenderOptions options) {
        this.context = context;
        this.blockName = blockName;
        this.options = options != null ? options : BlockRenderOptions.DEFAULT;
    }

    @Override
    public @NotNull BufferedImage render() {
        if (blockName.isBlank())
            throw new IllegalArgumentException("blockName cannot be blank");
        RenderContext ctx = PackContextManager.resolveContext(context, options.getPackIds());
        BlockRenderOptions forwardedOptions = PackContextManager.forwardOptions(context, options);
        return ctx.renderBlock(blockName, forwardedOptions);
    }
}
