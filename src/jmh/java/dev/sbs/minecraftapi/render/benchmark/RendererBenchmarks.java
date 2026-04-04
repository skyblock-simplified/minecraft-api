package dev.sbs.minecraftapi.render.benchmark;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.minecraftapi.RendererApi;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.ByteTag;
import dev.sbs.minecraftapi.render.BlockRenderer;
import dev.sbs.minecraftapi.render.ItemRenderer;
import dev.sbs.minecraftapi.render.context.BlockRenderOptions;
import dev.sbs.minecraftapi.render.context.ItemRenderData;
import dev.sbs.minecraftapi.render.context.RenderContext;
import dev.sbs.minecraftapi.render.resolver.ResourceIdResult;
import org.openjdk.jmh.annotations.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for the RenderContext pipeline.
 * <p>
 * Requires a {@code minecraft/} assets directory and (for Hypixel benchmarks)
 * a {@code texturepacks/Hypixel+ 0.23.4 for 1.21.8} directory accessible from the project root.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class RendererBenchmarks {

    private RenderContext context;
    private BlockRenderOptions blockOptions;
    private BlockRenderOptions itemOptions;
    private CompoundTag diamondSwordNbt;
    private CompoundTag compassNbt;
    private RenderContext hypixelContext;
    private BlockRenderOptions hypixelItemOptions;
    private List<HypixelHeadSample> hypixelHeadSamples;

    private static final String[] HYPIXEL_PACK_IDS = {"hypixelplus"};

    private static final String[][] HYPIXEL_HEAD_DEFINITIONS = {
        {"ABICASE", "model", "BLUE_AQUA"},
        {"AGARIMOO_ARTIFACT"},
        {"ANITA_TALISMAN"},
        {"BAT_PERSON_RING"},
        {"BINGO_TALISMAN"},
        {"CANDY_ARTIFACT"}
    };

    @Setup(Level.Trial)
    public void setup() throws Exception {
        String assetsDirectory = locateDirectory("minecraft");
        RendererApi.loadAndWrap(assetsDirectory);
        context = RendererApi.getRenderContext();
        blockOptions = BlockRenderOptions.builder()
            .withSize(256)
            .withPerspectiveAmount(0.12f)
            .build();
        itemOptions = BlockRenderOptions.builder()
            .withSize(128)
            .build();

        diamondSwordNbt = new CompoundTag();
        diamondSwordNbt.put("id", "minecraft:diamond_sword");
        diamondSwordNbt.put("Count", new ByteTag((byte) 1));

        compassNbt = new CompoundTag();
        compassNbt.put("id", "minecraft:compass");
        compassNbt.put("Count", new ByteTag((byte) 1));

        String texturePackDirectory = locateDirectory("texturepacks");
        String hypixelPackPath = Path.of(texturePackDirectory, "Hypixel+ 0.23.4 for 1.21.8").toString();
        if (!new File(hypixelPackPath).isDirectory()) {
            throw new IllegalStateException("Hypixel+ texture pack not found at: " + hypixelPackPath);
        }

        RendererApi.loadAndWrap(assetsDirectory, List.of(hypixelPackPath));
        hypixelContext = RendererApi.getRenderContext();
        hypixelItemOptions = BlockRenderOptions.builder()
            .withSize(128)
            .withPackIds(Concurrent.newUnmodifiableList(HYPIXEL_PACK_IDS))
            .build();
        hypixelHeadSamples = createHypixelHeadSamples();
    }

    @TearDown(Level.Trial)
    public void cleanup() {
        if (context != null) context.close();
        if (hypixelContext != null) hypixelContext.close();
    }

    @Benchmark
    public int renderStoneBlock() {
        BufferedImage image = new BlockRenderer(context, "stone", blockOptions).render();
        return image.getWidth() * image.getHeight();
    }

    @Benchmark
    public int renderDiamondSwordItem() {
        BufferedImage image = new ItemRenderer(context, "diamond_sword", itemOptions).render();
        return image.getWidth() * image.getHeight();
    }

    @Benchmark
    public int computeDiamondSwordResourceId() {
        ResourceIdResult result = context.computeResourceId("diamond_sword", itemOptions);
        return result.getResourceId().hashCode();
    }

    @Benchmark
    public int renderDiamondSwordFromNbt() {
        BufferedImage image = ItemRenderer.fromNbt(context, diamondSwordNbt, itemOptions).render();
        return image.getWidth() * image.getHeight();
    }

    @Benchmark
    public int renderCompassFromNbt() {
        BufferedImage image = ItemRenderer.fromNbt(context, compassNbt, itemOptions).render();
        return image.getWidth() * image.getHeight();
    }

    @Benchmark
    public int renderHypixelPlayerHeads() {
        int hash = 17;
        for (HypixelHeadSample sample : hypixelHeadSamples) {
            BufferedImage image = new ItemRenderer(hypixelContext, "player_head", sample.itemData, hypixelItemOptions).render();
            hash = 31 * hash + image.getWidth() + image.getHeight();
        }
        return hash;
    }

    @Benchmark
    public int renderHypixelPlayerHeadsFromNbt() {
        int hash = 23;
        for (HypixelHeadSample sample : hypixelHeadSamples) {
            BufferedImage image = ItemRenderer.fromNbt(hypixelContext, sample.nbt, hypixelItemOptions).render();
            hash = 31 * hash + image.getWidth() + image.getHeight();
        }
        return hash;
    }

    @Benchmark
    public int computeHypixelPlayerHeadResourceIds() {
        int hash = 31;
        for (HypixelHeadSample sample : hypixelHeadSamples) {
            String itemId = "player_head";
            BlockRenderOptions options = hypixelItemOptions.mutate().withItemData(sample.itemData).build();
            ResourceIdResult result = hypixelContext.computeResourceId(itemId, options);
            hash = 31 * hash + result.getResourceId().hashCode();
        }
        return hash;
    }

    private static List<HypixelHeadSample> createHypixelHeadSamples() {
        List<HypixelHeadSample> samples = new ArrayList<>();
        for (String[] def : HYPIXEL_HEAD_DEFINITIONS) {
            String id = def[0];
            CompoundTag customData = new CompoundTag();
            customData.put("id", id);
            for (int i = 1; i < def.length - 1; i += 2) {
                customData.put(def[i], def[i + 1]);
            }

            CompoundTag components = new CompoundTag();
            components.put("minecraft:custom_data", customData);

            CompoundTag root = new CompoundTag();
            root.put("id", "minecraft:player_head");
            root.put("Count", new ByteTag((byte) 1));
            root.put("components", components);

            ItemRenderData itemData = new ItemRenderData(
                null, null, false, customData, null);
            samples.add(new HypixelHeadSample(itemData, root));
        }
        return samples;
    }

    private static String locateDirectory(String name) {
        File current = new File(System.getProperty("user.dir"));
        while (current != null) {
            File candidate = new File(current, name);
            if (candidate.isDirectory()) {
                return candidate.getAbsolutePath();
            }
            current = current.getParentFile();
        }
        throw new IllegalStateException("Unable to find '" + name + "' directory for benchmarks");
    }

    private record HypixelHeadSample(ItemRenderData itemData, CompoundTag nbt) {
    }
}
