package burp.tdou.fingerscan.filter;

import burp.tdou.common.log.Logger;
import burp.tdou.common.utils.StringUtils;
import burp.tdou.fingerscan.common.Config;
import burp.tdou.fingerscan.core.ScanRequest;
import burp.tdou.fingerscan.core.iconhash.FaviconDetector;
import burp.tdou.fingerscan.core.iconhash.FaviconRegistry;

public class SuffixFilter implements ScanFilter {

    private final FaviconRegistry faviconRegistry;

    public SuffixFilter(FaviconRegistry faviconRegistry) {
        this.faviconRegistry = faviconRegistry;
    }

    @Override
    public boolean accept(ScanRequest request) {
        String path = request.getPath();
        if (StringUtils.isEmpty(path) || "/".equals(path)) {
            return true;
        }

        if (FaviconDetector.isDefaultFaviconPath(path)
                || faviconRegistry.isFavicon(request.getHost(), path)) {
            return true;
        }

        String suffix = Config.get(Config.KEY_EXCLUDE_SUFFIX);
        if (StringUtils.isEmpty(suffix)) {
            return true;
        }

        String lowerPath = path.toLowerCase();
        String lowerSuffix = suffix.toLowerCase();

        // 单个后缀
        if (!lowerSuffix.contains("|")) {
            if (lowerPath.endsWith("." + lowerSuffix)) {
                Logger.debug("SuffixFilter: blocked path %s", path);
                return false;
            }
            return true;
        }

        // 多个后缀
        String[] suffixes = lowerSuffix.split("\\|");
        for (String item : suffixes) {
            if (lowerPath.endsWith("." + item)) {
                Logger.debug("SuffixFilter: blocked path %s (suffix: %s)", path, item);
                return false;
            }
        }
        return true;
    }
}
