package dev.sbs.minecraftapi.render;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.asset.context.AssetContext;
import dev.sbs.minecraftapi.render.context.RenderContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared utilities for integration tests that require the minecraft assets directory.
 *
 * <p>Asset loading, texture pack extraction, and JPA session setup are handled once
 * at test plan startup by {@link dev.sbs.minecraftapi.TestLifecycleListener}. This
 * base class provides convenience methods for creating render contexts.
 */
public class IntegrationTestBase {

    protected static String getAssetsDirectory() {
        String found = findDirectory("minecraft");
        assumeTrue(found != null && Files.isDirectory(Path.of(found)),
            "Minecraft assets directory not found - skipping integration test");
        return found;
    }

    protected static String getTexturesDirectory() {
        String assets = getAssetsDirectory();
        String textures = Path.of(assets, "textures").toString();
        assumeTrue(Files.isDirectory(Path.of(textures)),
            "Textures directory not found - skipping integration test");
        return textures;
    }

    protected static String getTexturePacksDirectory() {
        String found = findDirectory("texturepacks");
        assumeTrue(found != null, "Texture packs directory not found - skipping integration test");
        return found;
    }

    /**
     * Creates a render context using the already-loaded asset context.
     */
    protected static RenderContext createRenderContext(String assetsDirectory) throws IOException {
        return createRenderContext(assetsDirectory, null);
    }

    /**
     * Creates a render context with optional pack IDs.
     */
    protected static RenderContext createRenderContext(String assetsDirectory, ConcurrentList<String> packIds) throws IOException {
        if (packIds != null && !packIds.isEmpty())
            return new RenderContext(MinecraftApi.getAssetFactory().loadPackContext(packIds));
        return new RenderContext(MinecraftApi.getServiceManager().get(AssetContext.class));
    }

    /**
     * Registers additional pack directories in the JPA session, preserving any
     * pre-existing packs from the {@code texturepacks/} directory.
     *
     * @param additionalPackDirs pack root directories to add
     */
    protected static void registerPacks(List<String> additionalPackDirs) throws IOException {
        List<String> allDirs = new ArrayList<>(additionalPackDirs);

        // Include pre-existing packs so they aren't lost during rescan
        String texturePacksDir = findDirectory("texturepacks");
        if (texturePacksDir != null) {
            try (var stream = Files.list(Path.of(texturePacksDir))) {
                stream.filter(Files::isDirectory)
                    .filter(p -> Files.exists(p.resolve("meta.json")))
                    .map(p -> p.toAbsolutePath().toString())
                    .forEach(allDirs::add);
            }
        }

        MinecraftApi.getAssetFactory().rescanResourcePacks(allDirs);
    }

    protected static String findDirectory(String name) {
        File current = new File(System.getProperty("user.dir"));
        while (current != null) {
            File candidate = new File(current, name);
            if (candidate.isDirectory())
                return candidate.getAbsolutePath();
            current = current.getParentFile();
        }
        return null;
    }
}
