package burp.tdou.fingerscan.core.path;

import burp.tdou.common.log.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PathStore {

    private Connection connection;

    public PathStore(Connection connection) {
        this.connection = connection;
    }

    public void createTable() {
        if (connection == null) return;
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS collected_paths (" +
                "  path TEXT NOT NULL," +
                "  host TEXT NOT NULL," +
                "  hit_count INTEGER DEFAULT 1," +
                "  first_seen TEXT DEFAULT (datetime('now'))," +
                "  last_seen TEXT DEFAULT (datetime('now'))," +
                "  PRIMARY KEY (path, host)" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_collected_paths_path ON collected_paths(path)"
            );
        } catch (SQLException e) {
            Logger.error("PathStore createTable error: %s", e.getMessage());
        }
    }

    public void savePath(String path, String host) {
        if (connection == null || path == null || path.isEmpty()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO collected_paths (path, host) VALUES (?, ?) " +
                "ON CONFLICT(path, host) DO UPDATE SET hit_count = hit_count + 1, last_seen = datetime('now')")) {
            ps.setString(1, path);
            ps.setString(2, host);
            ps.executeUpdate();
        } catch (SQLException e) {
            Logger.debug("PathStore save error: %s", e.getMessage());
        }
    }

    public List<String[]> getAllPaths() {
        List<String[]> result = new ArrayList<>();
        if (connection == null) return result;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT path, GROUP_CONCAT(DISTINCT host) AS hosts, " +
                "COUNT(DISTINCT host) AS total_hits, MIN(first_seen) AS first_seen, MAX(last_seen) AS last_seen " +
                "FROM collected_paths GROUP BY path ORDER BY total_hits DESC")) {
            while (rs.next()) {
                result.add(new String[]{
                    rs.getString("path"),
                    rs.getString("hosts"),
                    String.valueOf(rs.getInt("total_hits")),
                    rs.getString("first_seen"),
                    rs.getString("last_seen")
                });
            }
        } catch (SQLException e) {
            Logger.debug("PathStore getAllPaths error: %s", e.getMessage());
        }
        return result;
    }

    public int getPathCount() {
        if (connection == null) return 0;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(DISTINCT path) FROM collected_paths")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    public List<String> getDistinctPaths() {
        List<String> result = new ArrayList<>();
        if (connection == null) return result;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT path FROM collected_paths ORDER BY path")) {
            while (rs.next()) {
                result.add(rs.getString("path"));
            }
        } catch (SQLException e) {
            Logger.debug("PathStore getDistinctPaths error: %s", e.getMessage());
        }
        return result;
    }

    public void deletePath(String path) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM collected_paths WHERE path = ?")) {
            ps.setString(1, path);
            ps.executeUpdate();
        } catch (SQLException e) {
            Logger.debug("PathStore delete error: %s", e.getMessage());
        }
    }

    public void clearAll() {
        if (connection == null) return;
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM collected_paths");
        } catch (SQLException e) {
            Logger.debug("PathStore clearAll error: %s", e.getMessage());
        }
    }

    public static String extractFirstPath(String fullPath) {
        if (fullPath == null || fullPath.isEmpty() || fullPath.equals("/")) {
            return null;
        }
        int q = fullPath.indexOf('?');
        if (q >= 0) fullPath = fullPath.substring(0, q);
        int f = fullPath.indexOf('#');
        if (f >= 0) fullPath = fullPath.substring(0, f);

        if (!fullPath.startsWith("/")) {
            fullPath = "/" + fullPath;
        }

        // Only collect directory-type paths (must have at least two segments)
        // /api/users/123 → /api
        // /robots.txt → skip (single segment, file-like)
        int second = fullPath.indexOf('/', 1);
        if (second > 0) {
            return fullPath.substring(0, second);
        }
        return null;
    }
}
