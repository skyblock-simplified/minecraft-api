package dev.sbs.minecraftapi.render;


import dev.sbs.minecraftapi.render.context.HeadRenderOptions;
import dev.sbs.minecraftapi.render.context.RenderContext;
import dev.sbs.minecraftapi.render.engine.HeadEngine;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * Renderer that produces a 2D image from a Minecraft player head skin texture.
 *
 * <p>Renders both the base head layer and the overlay (hat) layer with correct depth sorting,
 * back-face culling, perspective projection, lighting, and optional FXAA anti-aliasing.
 */
@Getter
public final class HeadRenderer implements Renderer {

    private final @NotNull RenderContext context;
    private final @NotNull BufferedImage skin;
    private final @NotNull HeadRenderOptions options;

    public HeadRenderer(@NotNull RenderContext context, @NotNull BufferedImage skin,
                        @Nullable HeadRenderOptions options) {
        this.context = context;
        this.skin = skin;
        this.options = options != null ? options : HeadRenderOptions.DEFAULT;
    }

    @Override
    public @NotNull BufferedImage render() {
        return new HeadEngine(options, skin).render();
    }
}
