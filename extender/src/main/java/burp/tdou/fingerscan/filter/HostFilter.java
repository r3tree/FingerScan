package burp.tdou.fingerscan.filter;

import burp.tdou.common.log.Logger;
import burp.tdou.common.utils.StringUtils;
import burp.tdou.fingerscan.core.ScanRequest;
import burp.tdou.fingerscan.manager.WordlistManager;

import java.util.List;

/**
 * Host 过滤器
 * 实现黑白名单过滤逻辑，替代原来 BurpExtender 中的 hostAllowlistFilter 和 hostBlocklistFilter
 */
public class HostFilter implements ScanFilter {

    @Override
    public boolean accept(ScanRequest request) {
        String host = request.getHost();

        // 只对代理和重定向流量做 Host 过滤
        if (!request.isFromProxy() && !request.isFromRedirect()) {
            return true;
        }

        // 重定向流量需要额外检查配置开关
        if (request.isFromRedirect()) {
            // 此处留给外层 Config 配置判断，默认通过
            // 实际使用时应检查 Config.KEY_REDIRECT_TARGET_HOST_LIMIT
        }

        // 白名单过滤
        if (isBlockedByAllowlist(host)) {
            Logger.debug("HostFilter: allowlist blocked host: %s", host);
            return false;
        }

        // 黑名单过滤
        if (isBlockedByBlocklist(host)) {
            Logger.debug("HostFilter: blocklist blocked host: %s", host);
            return false;
        }

        return true;
    }

    private boolean isBlockedByAllowlist(String host) {
        List<String> allowlist = WordlistManager.getHostAllowlist();
        if (allowlist.isEmpty()) {
            return false;  // 白名单为空，不启用
        }
        for (String rule : allowlist) {
            if (matchHost(host, rule)) {
                return false;  // 命中白名单，放行
            }
        }
        return true;  // 未命中白名单，拒绝
    }

    private boolean isBlockedByBlocklist(String host) {
        List<String> blocklist = WordlistManager.getHostBlocklist();
        if (blocklist.isEmpty()) {
            return false;  // 黑名单为空，不启用
        }
        for (String rule : blocklist) {
            if (matchHost(host, rule)) {
                return true;  // 命中黑名单，拒绝
            }
        }
        return false;  // 未命中黑名单，放行
    }

    /**
     * 检测 Host 是否匹配规则（支持通配符 *）
     */
    static boolean matchHost(String host, String rule) {
        if (StringUtils.isEmpty(host)) {
            return StringUtils.isEmpty(rule);
        }
        if ("*".equals(rule)) {
            return true;
        }
        if (!rule.contains("*")) {
            return host.equals(rule);
        }

        String ruleValue = rule.replace("*", "");
        if (rule.startsWith("*") && rule.endsWith("*")) {
            return host.contains(ruleValue);
        } else if (rule.startsWith("*")) {
            return host.endsWith(ruleValue);
        } else if (rule.endsWith("*")) {
            return host.startsWith(ruleValue);
        } else {
            String[] parts = rule.split("\\*", 2);
            return parts.length == 2 && host.startsWith(parts[0]) && host.endsWith(parts[1]);
        }
    }
}
