package burp.tdou.fingerscan.filter;

import burp.tdou.common.log.Logger;
import burp.tdou.common.utils.StringUtils;
import burp.tdou.fingerscan.common.Config;
import burp.tdou.fingerscan.core.ScanRequest;

/**
 * 请求方法过滤器
 * 检查请求方法是否在允许列表中（仅对代理流量生效）
 */
public class MethodFilter implements ScanFilter {

    @Override
    public boolean accept(ScanRequest request) {
        // 只对代理流量做方法过滤
        if (!request.isFromProxy()) {
            return true;
        }

        String method = request.getMethod();
        if (StringUtils.isEmpty(method)) {
            return true;
        }

        String includeMethod = Config.get(Config.KEY_INCLUDE_METHOD);
        if (StringUtils.isEmpty(includeMethod)) {
            return true;  // 未配置，不过滤
        }

        String[] allowed = includeMethod.split("\\|");
        for (String item : allowed) {
            if (method.equals(item)) {
                return true;
            }
        }

        Logger.debug("MethodFilter: blocked method %s for host %s", method, request.getHost());
        return false;
    }
}
