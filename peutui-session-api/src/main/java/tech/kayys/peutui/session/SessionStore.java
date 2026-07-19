package tech.kayys.peutui.session;

import java.util.List;
import java.util.Optional;

/**
 * Persistence strategy for {@link Session}s. Implementations decide *where*
 * sessions live - in memory, on the local filesystem as JSON, in a local
 * database, or in the cloud via {@code peutui-storage-api}'s
 * {@code StorageBackend} - without the rest of the library caring which.
 */
public interface SessionStore {

    Session save(Session session);

    Optional<Session> load(String sessionId);

    List<Session> listByProject(String projectId);

    void delete(String sessionId);
}
