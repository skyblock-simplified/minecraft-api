package dev.sbs.minecraftapi.render;


import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.RenderContext;
import dev.sbs.minecraftapi.render.resolver.PackContextManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Renderer that produces a 2D image from a flat texture ID sprite.
 */
@Getter
public final class GuiItemRenderer implements Renderer {

    private final @NotNull RenderContext context;
    private final @NotNull String textureId;
    private final @NotNull BlockRenderOptions options;

    public GuiItemRenderer(@NotNull RenderContext context, @NotNull String textureId,
                           @Nullable BlockRenderOptions options) {
        this.context = context;
        this.textureId = textureId;
        this.options = options != null ? options : BlockRenderOptions.DEFAULT;
    }

    @Override
    public @NotNull BufferedImage render() {
        if (textureId.isBlank())
            throw new IllegalArgumentException("textureId cannot be blank");
        RenderContext ctx = PackContextManager.resolveContext(context, options.getPackIds());
        BlockRenderOptions forwardedOptions = PackContextManager.forwardOptions(context, options);
        return ctx.renderGuiItemFromTextureId(textureId, forwardedOptions);
    }
}
