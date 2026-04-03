package dev.sbs.minecraftapi;

import dev.sbs.api.scheduler.Scheduler;
import dev.sbs.minecraftapi.asset.MinecraftAssetFactory;
import dev.sbs.minecraftapi.asset.MinecraftAssetOptions;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages the {@link MinecraftApi} lifecycle around the test plan execution.
 *
 * <p>On startup, eagerly triggers the {@link MinecraftApi} static initializer
 * (H2 session, Gson adapters, Feign clients), loads vanilla Minecraft assets,
 * and registers any texture packs found in the project hierarchy. On finish,
 * shuts down the session manager and scheduler so non-daemon threads do not
 * prevent the test JVM from exiting.
 */
public class TestLifecycleListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        System.out.println("[TestLifecycleListener] Initializing MinecraftApi");
        MinecraftApi.getSessionManager();

        // Load vanilla assets and discover texture packs
        String assetsDir = findDirectory("minecraft");
        if (assetsDir == null) {
            try {
                Path projectRoot = Path.of(System.getProperty("user.dir"));
                System.out.println("[TestLifecycleListener] Minecraft assets not found - downloading...");
                Path minecraftDir = MinecraftAssetFactory.downloadAndExtractAssets(null, projectRoot);
                assetsDir = minecraftDir.toAbsolutePath().toString();
            } catch (Exception e) {
                System.err.println("[TestLifecycleListener] Failed to download assets: " + e.getMessage());
            }
        }

        if (assetsDir != null && Files.isDirectory(Path.of(assetsDir))) {
            try {
                List<String> packDirs = discoverPackDirectories();
                MinecraftApi.loadAssets(MinecraftAssetOptions.builder()
                    .withAssetsDirectory(assetsDir)
                    .withTexturePackDirectories(packDirs).build());
                System.out.println("[TestLifecycleListener] Assets loaded from: " + assetsDir
                    + " with " + packDirs.size() + " texture pack directories");
            } catch (IOException e) {
                System.err.println("[TestLifecycleListener] Failed to load assets: " + e.getMessage());
            }
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        System.out.println("[TestLifecycleListener] testPlanExecutionFinished called");

        if (MinecraftApi.getSessionManager().isActive()) {
            MinecraftApi.getSessionManager().shutdown();
            System.out.println("[TestLifecycleListener] SessionManager shut down");
        }

        if (!MinecraftApi.getScheduler().isShutdown()) {
            MinecraftApi.getScheduler().shutdown();
            System.out.println("[TestLifecycleListener] Scheduler shut down");
        }

        Scheduler.leakedThreads().forEach(t -> System.out.printf(
            "[TestLifecycleListener] Remaining non-daemon: %s (state=%s, group=%s)%n",
            t.getName(), t.getState(), t.getThreadGroup()
        ));
    }

    private static List<String> discoverPackDirectories() {
        List<String> packDirs = new ArrayList<>();

        // Find texturepacks directory
        String texturePacksDir = findDirectory("texturepacks");
        if (texturePacksDir == null) {
            // Auto-extract texture pack ZIPs from test resources
            try {
                Path projectRoot = Path.of(System.getProperty("user.dir"));
                Path targetDir = projectRoot.resolve("texturepacks");
                Path resourcesDir = projectRoot.resolve("src").resolve("test").resolve("resources");

                if (Files.isDirectory(resourcesDir)) {
                    try (var stream = Files.list(resourcesDir)) {
                        var zips = stream.filter(p -> p.getFileName().toString().endsWith(".zip")).toList();
                        for (Path zip : zips) {
                            String zipName = zip.getFileName().toString();
                            String dirName = zipName.substring(0, zipName.length() - 4);
                            Path packPath = targetDir.resolve(dirName);

                            if (!Files.isDirectory(packPath)) {
                                Files.createDirectories(packPath);
                                extractZip(zip, packPath);
                                generateMetaIfMissing(packPath, dirName);
                            }
                        }
                    }

                    if (Files.isDirectory(targetDir))
                        texturePacksDir = targetDir.toAbsolutePath().toString();
                }
            } catch (IOException e) {
                System.err.println("[TestLifecycleListener] Failed to extract packs: " + e.getMessage());
            }
        }

        // Collect individual pack directories (each subdirectory with a meta.json)
        if (texturePacksDir != null) {
            try (var stream = Files.list(Path.of(texturePacksDir))) {
                stream.filter(Files::isDirectory)
                    .filter(p -> Files.exists(p.resolve("meta.json")))
                    .forEach(p -> packDirs.add(p.toAbsolutePath().toString()));
            } catch (IOException ignored) {
            }
        }

        return packDirs;
    }

    private static void generateMetaIfMissing(Path packPath, String dirName) throws IOException {
        if (!Files.exists(packPath.resolve("meta.json")) && Files.exists(packPath.resolve("pack.mcmeta"))) {
            String packId = dirName
                .replaceAll("\\s+\\d.*", "")
                .replace("+", "plus")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
            if (packId.isBlank()) packId = "texturepack";
            Files.writeString(packPath.resolve("meta.json"),
                """
                {
                  "id": "%s",
                  "name": "%s",
                  "version": "1.0.0",
                  "description": "Auto-extracted from test resources",
                  "authors": ["tests"]
                }
                """.formatted(packId, dirName));
        }
    }

    private static void extractZip(Path zipFile, Path targetDir) throws IOException {
        try (InputStream fis = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir))
                    throw new IOException("Zip entry outside target directory: " + entry.getName());
                if (entry.isDirectory())
                    Files.createDirectories(entryPath);
                else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath);
                }
                zis.closeEntry();
            }
        }
    }

    private static String findDirectory(String name) {
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
