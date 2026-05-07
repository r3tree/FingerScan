package burp.tdou.fingerscan.core.iconhash;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FaviconRegistry {

    private static final int MAX_HOSTS = 10000;
    private static final int MAX_PATHS_PER_HOST = 20;

    private final ConcurrentHashMap<String, Set<String>> registry = new ConcurrentHashMap<>();

    public void register(String host, String faviconPath) {
        if (host == null || faviconPath == null || host.isEmpty() || faviconPath.isEmpty()) {
            return;
        }
        if (registry.size() >= MAX_HOSTS && !registry.containsKey(host)) {
            return;
        }
        Set<String> paths = registry.computeIfAbsent(host,
                k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        if (paths.size() < MAX_PATHS_PER_HOST) {
            paths.add(faviconPath);
        }
    }

    public boolean isFavicon(String host, String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (isDefaultFaviconPath(path)) {
            return true;
        }
        if (host == null || host.isEmpty()) {
            return false;
        }
        Set<String> paths = registry.get(host);
        return paths != null && paths.contains(path);
    }

    public static boolean isDefaultFaviconPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.equals("/favicon.ico") || lower.endsWith("/favicon.ico");
    }

    public void clear() {
        registry.clear();
    }

    public int hostCount() {
        return registry.size();
    }
}
