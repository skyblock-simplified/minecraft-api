package dev.sbs.minecraftapi.schema;

import dev.sbs.minecraftapi.MinecraftApi;
import org.hibernate.boot.Metadata;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;

/**
 * Exports H2 DDL from all embedded JPA sessions and creates a persistent H2 file
 * database for IntelliJ JPA column resolution.
 *
 * <p>Run via Gradle: {@code ./gradlew :minecraft-api:generateSchema}</p>
 *
 * <p>IntelliJ JDBC connection string:
 * {@code jdbc:h2:file:$PROJECT_DIR$/minecraft-api/.schema/<name>;ACCESS_MODE_DATA=r}
 * with user {@code sa} and an empty password.</p>
 */
public final class SchemaExporter {

    public static void main(String[] args) {
        Path outputDir = Path.of(args.length > 0 ? args[0] : ".schema").normalize().toAbsolutePath();

        // Triggers MinecraftApi static init, which builds all H2 sessions.
        // Shutdown is handled by JVM shutdown hooks in SessionManager and Scheduler.
        MinecraftApi.getSessionManager()
            .getSessions()
            .stream()
            .filter(session -> session.getConfig().getDriver().isEmbedded())
            .forEach(session -> {
                Metadata metadata = session.getMetadata();
                String baseName = session.getConfig().getSchema();

                outputDir.toFile().mkdirs();

                // Export DDL to a temporary SQL file (keeps quoted identifiers for reserved keywords)
                Path sqlFile = outputDir.resolve(baseName + "-schema.sql");

                try {
                    Files.deleteIfExists(sqlFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                new SchemaExport()
                    .setOutputFile(sqlFile.toString())
                    .setDelimiter(";")
                    .setFormat(true)
                    .createOnly(EnumSet.of(TargetType.SCRIPT), metadata);

                // Create a persistent H2 file database from the DDL
                Path dbFile = outputDir.resolve(baseName);
                String jdbcUrl = "jdbc:h2:file:" + dbFile;

                try {
                    Files.deleteIfExists(Path.of(dbFile + ".mv.db"));
                    Files.deleteIfExists(Path.of(dbFile + ".trace.db"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try (
                    Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
                    Statement stmt = conn.createStatement()
                ) {
                    String ddl = Files.readString(sqlFile);

                    for (String sql : ddl.split(";")) {
                        sql = sql.trim();

                        if (!sql.isEmpty())
                            stmt.execute(sql);
                    }
                } catch (SQLException | IOException e) {
                    throw new RuntimeException(e);
                }

                // DDL file was only needed to seed the H2 file database
                try {
                    Files.deleteIfExists(sqlFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                System.out.printf("Exported %d entities to %s%n", session.getModels().size(), dbFile);
            });
    }

}
