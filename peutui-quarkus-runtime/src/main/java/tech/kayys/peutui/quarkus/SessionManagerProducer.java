package tech.kayys.peutui.quarkus;

import tech.kayys.peutui.project.ProjectRegistry;
import tech.kayys.peutui.session.MultiSessionManager;
import tech.kayys.peutui.session.Session;
import tech.kayys.peutui.session.SessionManager;
import tech.kayys.peutui.session.SessionStore;
import tech.kayys.peutui.session.SingleSessionManager;
import tech.kayys.peutui.session.StorageBackedSessionStore;
import tech.kayys.peutui.storage.StorageBackend;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Produces the {@link SessionStore} (always storage-backed, per configured
 * mode) and the {@link SessionManager} strategy (single vs multi).
 */
@ApplicationScoped
public final class SessionManagerProducer {

    @Inject
    PeutuiConfig config;

    @Inject
    ProjectRegistry projectRegistry;

    @Inject
    @SessionStorage
    StorageBackend sessionStorageBackend;

    @Produces
    @ApplicationScoped
    public SessionStore sessionStore() {
        return new StorageBackedSessionStore(sessionStorageBackend);
    }

    @Produces
    @ApplicationScoped
    public SessionManager sessionManager(SessionStore store) {
        String projectId = projectRegistry.active().id();
        return switch (config.session().mode()) {
            case SINGLE -> new SingleSessionManager(store,
                    Session.create(projectId, config.agent().defaultAgentId().orElse("default")));
            case MULTI -> {
                MultiSessionManager manager = new MultiSessionManager(store);
                manager.startNew(projectId, config.agent().defaultAgentId().orElse("default"));
                yield manager;
            }
        };
    }
}
