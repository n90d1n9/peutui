package tech.kayys.peutui.storage;

import javax.sql.DataSource;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Strategy factory that centralizes construction of a {@link StorageBackend}
 * for a given {@link StorageMode}, so host applications (and the Quarkus
 * CDI producers in {@code peutui-quarkus-runtime}) have one place to look
 * when wiring "which storage strategy is active" from configuration.
 */
public final class StorageBackendFactory {

    private StorageBackendFactory() {
    }

    public static StorageBackend localFile(Path rootDir) {
        return new LocalFileStorageBackend(rootDir);
    }

    public static StorageBackend localDatabase(DataSource dataSource, String describeLabel) {
        return new LocalDatabaseStorageBackend(dataSource, describeLabel);
    }

    public static StorageBackend cloud(URI baseUrl, Supplier<String> authorizationHeader) {
        return new CloudStorageBackend(baseUrl, authorizationHeader);
    }

    /**
     * One-shot creation from an already-decided {@link StorageMode} plus every
     * possible dependency; unused ones are ignored.
     */
    public static StorageBackend create(StorageMode mode, Path localFileRoot, DataSource dataSource,
            String databaseLabel, URI cloudBaseUrl, Supplier<String> cloudAuthHeader) {
        return switch (mode) {
            case LOCAL_FILE -> localFile(localFileRoot);
            case LOCAL_DATABASE -> localDatabase(dataSource, databaseLabel);
            case CLOUD -> cloud(cloudBaseUrl, cloudAuthHeader);
        };
    }
}
