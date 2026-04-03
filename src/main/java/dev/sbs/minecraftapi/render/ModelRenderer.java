package dev.sbs.minecraftapi.render;


import dev.sbs.minecraftapi.asset.model.BlockModel;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.RenderContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Renderer that produces a 2D image from a pre-resolved {@link BlockModel}.
 */
@Getter
@RequiredArgsConstructor
public final class ModelRenderer implements Renderer {

    private final @NotNull RenderContext context;
    private final @NotNull BlockModel model;
    private final @NotNull BlockRenderOptions options;
    private final @Nullable String blockName;

    @Override
    public @NotNull BufferedImage render() {
        return context.renderModel(model, options, blockName);
    }
}
