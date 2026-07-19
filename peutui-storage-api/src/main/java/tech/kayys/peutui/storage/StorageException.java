package tech.kayys.peutui.storage;

/**
 * Unchecked wrapper for any lower-level failure (SQL, HTTP, IO) surfaced by a
 * {@link StorageBackend}.
 */
public final class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
