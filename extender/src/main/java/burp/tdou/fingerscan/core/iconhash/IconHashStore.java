package burp.tdou.fingerscan.core.iconhash;

import burp.tdou.common.log.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IconHashStore {

    private final String dbPath;
    private Connection connection;

    public IconHashStore(String workDir) {
        this.dbPath = workDir + "icon_hash.db";
    }

    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.setAutoCommit(true);
            createTables();
            Logger.debug("IconHashStore: initialized at %s", dbPath);
        } catch (Exception e) {
            Logger.error("IconHashStore init failed: %s", e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS icons (" +
                "  murmur_hash TEXT PRIMARY KEY," +
                "  md5 TEXT," +
                "  data BLOB," +
                "  content_type TEXT," +
                "  size INTEGER," +
                "  first_seen TEXT DEFAULT (datetime('now'))," +
                "  match_result TEXT," +
                "  remark TEXT DEFAULT ''" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS icon_sources (" +
                "  murmur_hash TEXT NOT NULL," +
                "  host TEXT NOT NULL," +
                "  path TEXT NOT NULL," +
                "  seen_at TEXT DEFAULT (datetime('now'))," +
                "  PRIMARY KEY (murmur_hash, host, path)," +
                "  FOREIGN KEY (murmur_hash) REFERENCES icons(murmur_hash)" +
                ")"
            );
            migrateAddRemark(stmt);
        }
    }

    private void migrateAddRemark(Statement stmt) {
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(icons)");
            boolean hasRemark = false;
            while (rs.next()) {
                if ("remark".equals(rs.getString("name"))) {
                    hasRemark = true;
                    break;
                }
            }
            if (!hasRemark) {
                stmt.executeUpdate("ALTER TABLE icons ADD COLUMN remark TEXT DEFAULT ''");
                Logger.debug("IconHashStore: migrated - added remark column");
            }
        } catch (SQLException e) {
            Logger.debug("IconHashStore migrate remark: %s", e.getMessage());
        }
    }

    public void saveIcon(String murmurHash, String md5, byte[] data,
                         String contentType, String matchResult,
                         String host, String path) {
        if (connection == null) return;

        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR IGNORE INTO icons (murmur_hash, md5, data, content_type, size, match_result) " +
                    "VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, murmurHash);
                ps.setString(2, md5);
                ps.setBytes(3, data);
                ps.setString(4, contentType);
                ps.setInt(5, data != null ? data.length : 0);
                ps.setString(6, matchResult);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR IGNORE INTO icon_sources (murmur_hash, host, path) VALUES (?, ?, ?)")) {
                ps.setString(1, murmurHash);
                ps.setString(2, host);
                ps.setString(3, path);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            Logger.debug("IconHashStore save error: %s", e.getMessage());
        }
    }

    public boolean hasIcon(String murmurHash) {
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM icons WHERE murmur_hash = ?")) {
            ps.setString(1, murmurHash);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public int getIconCount() {
        if (connection == null) return 0;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM icons")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    public byte[] getIconData(String murmurHash) {
        if (connection == null) return null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT data FROM icons WHERE murmur_hash = ?")) {
            ps.setString(1, murmurHash);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getBytes("data") : null;
        } catch (SQLException e) {
            return null;
        }
    }

    public List<String[]> getAllIcons() {
        List<String[]> result = new ArrayList<>();
        if (connection == null) return result;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT i.murmur_hash, i.md5, i.content_type, i.size, i.first_seen, i.match_result, " +
                "(SELECT COUNT(*) FROM icon_sources s WHERE s.murmur_hash = i.murmur_hash) AS source_count, " +
                "i.remark " +
                "FROM icons i ORDER BY i.first_seen DESC")) {
            while (rs.next()) {
                String remark = rs.getString("remark");
                result.add(new String[]{
                    rs.getString("murmur_hash"),
                    rs.getString("md5"),
                    rs.getString("content_type"),
                    String.valueOf(rs.getInt("size")),
                    rs.getString("first_seen"),
                    rs.getString("match_result"),
                    String.valueOf(rs.getInt("source_count")),
                    remark != null ? remark : ""
                });
            }
        } catch (SQLException e) {
            Logger.debug("IconHashStore getAllIcons error: %s", e.getMessage());
        }
        return result;
    }

    public void updateMatchResult(String murmurHash, String matchResult) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE icons SET match_result = ? WHERE murmur_hash = ?")) {
            ps.setString(1, matchResult != null ? matchResult : "");
            ps.setString(2, murmurHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            Logger.debug("IconHashStore updateMatchResult error: %s", e.getMessage());
        }
    }

    public void updateRemark(String murmurHash, String remark) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE icons SET remark = ? WHERE murmur_hash = ?")) {
            ps.setString(1, remark != null ? remark : "");
            ps.setString(2, murmurHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            Logger.debug("IconHashStore updateRemark error: %s", e.getMessage());
        }
    }

    public List<String[]> getIconSources(String murmurHash) {
        List<String[]> result = new ArrayList<>();
        if (connection == null) return result;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT host, path, seen_at FROM icon_sources WHERE murmur_hash = ? ORDER BY seen_at DESC")) {
            ps.setString(1, murmurHash);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new String[]{
                    rs.getString("host"),
                    rs.getString("path"),
                    rs.getString("seen_at")
                });
            }
        } catch (SQLException e) {
            Logger.debug("IconHashStore getIconSources error: %s", e.getMessage());
        }
        return result;
    }

    public void deleteIcon(String murmurHash) {
        if (connection == null) return;
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM icon_sources WHERE murmur_hash = ?")) {
                ps.setString(1, murmurHash);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM icons WHERE murmur_hash = ?")) {
                ps.setString(1, murmurHash);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            Logger.debug("IconHashStore delete error: %s", e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                Logger.debug("IconHashStore: closed");
            } catch (SQLException e) {
                Logger.error("IconHashStore close error: %s", e.getMessage());
            }
        }
    }
}
