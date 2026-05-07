package burp.tdou.fingerscan.core.iconhash;

public class FaviconDetector {

    private static final String[] FAVICON_CONTENT_TYPES = {
        "image/x-icon", "image/vnd.microsoft.icon",
        "image/png", "image/svg+xml", "image/gif"
    };

    public static boolean isDefaultFaviconPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String lowerPath = path.toLowerCase();
        return lowerPath.equals("/favicon.ico") || lowerPath.endsWith("/favicon.ico");
    }

    public static boolean isFaviconContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return false;
        }
        String lower = contentType.toLowerCase();
        for (String type : FAVICON_CONTENT_TYPES) {
            if (lower.contains(type)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHtmlContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return false;
        }
        return contentType.toLowerCase().contains("text/html");
    }
}
