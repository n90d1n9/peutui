package tech.kayys.peutui.storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link StorageBackend} strategy backed by any JDBC {@link DataSource} -
 * SQLite, H2, Postgres, MySQL, etc. Uses a single generic key/value table so
 * it works identically regardless of which JDBC driver is wired in; the
 * host application owns the {@code DataSource} (and, in Quarkus, typically
 * gets one for free via {@code quarkus-jdbc-*} + Agroal).
 */
public final class LocalDatabaseStorageBackend implements StorageBackend {

    private static final String TABLE = "peutui_kv_store";

    private final DataSource dataSource;
    private final String describeLabel;

    public LocalDatabaseStorageBackend(DataSource dataSource, String describeLabel) {
        this.dataSource = dataSource;
        this.describeLabel = describeLabel;
        ensureSchema();
    }

    private void ensureSchema() {
        String ddl = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                "storage_key VARCHAR(1024) PRIMARY KEY, " +
                "storage_value BLOB, " +
                "updated_at BIGINT NOT NULL)";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        } catch (SQLException e) {
            throw new StorageException("Unable to initialize storage schema", e);
        }
    }

    @Override
    public void put(String key, byte[] value) {
        String upsert = "MERGE INTO " + TABLE
                + " (storage_key, storage_value, updated_at) KEY(storage_key) VALUES (?, ?, ?)";
        String fallback = "INSERT INTO " + TABLE + " (storage_key, storage_value, updated_at) VALUES (?, ?, ?) " +
                "ON CONFLICT (storage_key) DO UPDATE SET storage_value = EXCLUDED.storage_value, updated_at = EXCLUDED.updated_at";
        try (Connection conn = dataSource.getConnection()) {
            try {
                executeUpsert(conn, upsert, key, value);
            } catch (SQLException firstAttemptFailed) {
                executeUpsert(conn, fallback, key, value);
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to write key: " + key, e);
        }
    }

    private void executeUpsert(Connection conn, String sql, String key, byte[] value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setBytes(2, value);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<byte[]> get(String key) {
        String sql = "SELECT storage_value FROM " + TABLE + " WHERE storage_key = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getBytes(1));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to read key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        String sql = "DELETE FROM " + TABLE + " WHERE storage_key = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to delete key: " + key, e);
        }
    }

    @Override
    public List<String> listKeys(String prefix) {
        String sql = "SELECT storage_key FROM " + TABLE + " WHERE storage_key LIKE ? ORDER BY storage_key";
        List<String> keys = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, escapeLike(prefix) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    keys.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to list keys with prefix: " + prefix, e);
        }
        return keys;
    }

    @Override
    public boolean exists(String key) {
        String sql = "SELECT 1 FROM " + TABLE + " WHERE storage_key = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to check existence of key: " + key, e);
        }
    }

    @Override
    public String describe() {
        return "local-database:" + describeLabel;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
