package dev.sbs.minecraftapi.schema;

import dev.sbs.minecraftapi.MinecraftApi;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

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

                // Use Hibernate 7 SchemaManagementTool SPI to export DDL
                StandardServiceRegistry serviceRegistry = session.getServiceRegistry();
                SchemaManagementTool tool = serviceRegistry.getService(SchemaManagementTool.class);

                @SuppressWarnings("unchecked")
                Map<String, Object> configValues = new HashMap<>((Map<String, Object>) (Map<?, ?>) session.getProperties());

                ExecutionOptions executionOptions = new ExecutionOptions() {
                    @Override
                    public Map<String, Object> getConfigurationValues() {
                        return configValues;
                    }

                    @Override
                    public boolean shouldManageNamespaces() {
                        return false;
                    }

                    @Override
                    public ExceptionHandler getExceptionHandler() {
                        return ExceptionHandlerHaltImpl.INSTANCE;
                    }
                };

                SourceDescriptor sourceDescriptor = new SourceDescriptor() {
                    @Override
                    public SourceType getSourceType() {
                        return SourceType.METADATA;
                    }

                    @Override
                    public ScriptSourceInput getScriptSourceInput() {
                        return null;
                    }
                };

                ScriptTargetOutput scriptOutput = new ScriptTargetOutputToFile(sqlFile.toFile(), "UTF-8");
                TargetDescriptor targetDescriptor = new TargetDescriptor() {
                    @Override
                    public EnumSet<TargetType> getTargetTypes() {
                        return EnumSet.of(TargetType.SCRIPT);
                    }

                    @Override
                    public ScriptTargetOutput getScriptTargetOutput() {
                        return scriptOutput;
                    }
                };

                tool.getSchemaCreator(configValues).doCreation(
                    metadata,
                    executionOptions,
                    ContributableMatcher.ALL,
                    sourceDescriptor,
                    targetDescriptor
                );

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
