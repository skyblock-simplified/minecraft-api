package dev.sbs.minecraftapi.asset;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class ColormapDiscoveryTest {

    private static final List<String> VERSIONS = List.of(
        "1.8.9", "1.13", "1.14", "1.16", "1.17", "1.19", "1.20", "1.21", "latest"
    );

    @Test
    void downloadAndFindColormaps() throws Exception {
        Path testRoot = Path.of(System.getProperty("user.dir"), "test");
        Files.createDirectories(testRoot);

        Map<String, String> results = new LinkedHashMap<>();

        for (String version : VERSIONS) {
            String versionId = version.equals("latest") ? null : version;

            System.out.println("\n--- Processing " + version + " ---");

            try {
                Path minecraftDir;

                // For "latest", RendererApi resolves null to the latest release.
                // Peek at the resolved directory name by checking existing extractions.
                Path versionDir = testRoot.resolve(version.equals("latest") ? "latest" : version);

                if (!version.equals("latest") && Files.isDirectory(versionDir.resolve("minecraft"))) {
                    System.out.println("Already extracted, skipping download.");
                    minecraftDir = versionDir.resolve("minecraft");
                } else {
                    // Use the actual version string as the output directory
                    if (version.equals("latest")) {
                        // Let RendererApi resolve "latest" and extract into a temp name,
                        // then we scan the result
                        versionDir = testRoot.resolve("latest");
                    }
                    minecraftDir = MinecraftAssetFactory.downloadAndExtractAssets(versionId, versionDir);
                }

                // Scan for colormap directories
                StringBuilder colormaps = new StringBuilder();
                try (Stream<Path> walk = Files.walk(minecraftDir)) {
                    walk.filter(Files::isDirectory)
                        .filter(p -> p.getFileName().toString().equals("colormap"))
                        .forEach(p -> {
                            String relative = minecraftDir.relativize(p).toString();
                            if (colormaps.length() > 0) colormaps.append(", ");
                            colormaps.append(relative);

                            // List files in the colormap directory
                            try (Stream<Path> files = Files.list(p)) {
                                files.filter(Files::isRegularFile)
                                    .forEach(f -> System.out.println("  " + f.getFileName()));
                            } catch (IOException e) {
                                System.err.println("  Error listing files: " + e.getMessage());
                            }
                        });
                }

                results.put(version, colormaps.length() > 0 ? colormaps.toString() : "(none)");
            } catch (Exception e) {
                results.put(version, "ERROR: " + e.getMessage());
                System.err.println("Failed: " + e.getMessage());
            }
        }

        // Print results table
        System.out.println("\n\n=== Colormap Directory Results ===");
        System.out.printf("%-25s | %s%n", "Version", "Colormap Path(s)");
        System.out.println("-".repeat(25) + "-+-" + "-".repeat(50));
        for (Map.Entry<String, String> entry : results.entrySet()) {
            System.out.printf("%-25s | %s%n", entry.getKey(), entry.getValue());
        }
    }

}
