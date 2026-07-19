package tech.kayys.peutui.quarkus;

import tech.kayys.peutui.storage.CloudStorageBackend;
import tech.kayys.peutui.storage.LocalDatabaseStorageBackend;
import tech.kayys.peutui.storage.LocalFileStorageBackend;
import tech.kayys.peutui.storage.StorageBackend;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.net.URI;
import java.nio.file.Path;

/**
 * Produces the two {@link StorageBackend} beans (session and settings
 * storage, independently configurable) by translating
 * {@link PeutuiConfig.StorageConfig} into the matching backend
 * implementation - this is the concrete "local file / local database /
 * cloud" strategy switch described in the module's Javadoc.
 */
@ApplicationScoped
public final class StorageBackendProducer {

    @Inject
    PeutuiConfig config;

    @Inject
    Instance<DataSource> dataSource;

    @Produces
    @ApplicationScoped
    @SessionStorage
    public StorageBackend sessionStorageBackend() {
        return build(config.storage(), "session-store");
    }

    @Produces
    @ApplicationScoped
    @SettingsStorage
    public StorageBackend settingsStorageBackend() {
        return build(config.settingsStorage(), "settings-store");
    }

    private StorageBackend build(PeutuiConfig.StorageConfig storageConfig, String label) {
        return switch (storageConfig.mode()) {
            case LOCAL_FILE ->
                new LocalFileStorageBackend(Path.of(storageConfig.localFile().rootPath()).resolve(label));
            case LOCAL_DATABASE -> {
                if (dataSource.isUnsatisfied()) {
                    throw new IllegalStateException(
                            "peutui.storage.mode=local-database requires a javax.sql.DataSource bean " +
                                    "(add quarkus-jdbc-* + configure quarkus.datasource.*)");
                }
                yield new LocalDatabaseStorageBackend(dataSource.get(), storageConfig.database().label() + "-" + label);
            }
            case CLOUD -> {
                String baseUrl = storageConfig.cloud().baseUrl()
                        .orElseThrow(() -> new IllegalStateException(
                                "peutui.storage.cloud.base-url is required when storage mode is 'cloud'"));
                String authHeader = storageConfig.cloud().authorizationHeader().orElse(null);
                yield new CloudStorageBackend(URI.create(baseUrl), () -> authHeader);
            }
        };
    }
}
