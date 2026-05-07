package burp.tdou.fingerscan.core.pipeline;

import burp.api.montoya.http.HttpService;
import burp.tdou.common.helper.DomainHelper;
import burp.tdou.common.log.Logger;
import burp.tdou.common.utils.*;
import burp.tdou.fingerscan.common.Config;
import burp.tdou.fingerscan.manager.WordlistManager;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 请求构建器
 * 将原 BurpExtender 中的 handleHeader + setupVariable + fillVariable +
 * updateContentLength 逻辑抽取为独立工具类。
 *
 * 职责：
 * 1. 基于原始请求头 + 配置替换头 + 移除头 → 组装新请求头
 * 2. 填充动态变量 ({{host}}, {{ip}}, {{random.ua}} 等)
 * 3. 更新 Content-Length
 */
public class RequestBuilder {

    /**
     * 使用原始代理请求头构建一个新路径的 GET 扫描请求
     *
     * @param originalHeaders 原始请求的 header 列表（首行是请求行）
     * @param newPath         新的请求路径
     * @param service         目标服务
     * @param replaceHeader   是否启用替换请求头
     * @param removeHeader    是否启用移除请求头
     * @return 构建后的请求字节，失败返回 null
     */
    public static byte[] buildScanRequest(List<String> originalHeaders, String newPath,
                                           HttpService service,
                                           boolean replaceHeader, boolean removeHeader) {
        List<String> configHeaders = replaceHeader ? WordlistManager.getHeader() : new ArrayList<>();
        List<String> removeHeaders = removeHeader ? WordlistManager.getRemoveHeaders() : new ArrayList<>();

        StringBuilder requestRaw = new StringBuilder();

        // 请求行：GET newPath HTTP/1.1
        requestRaw.append("GET ").append(newPath).append(" HTTP/1.1").append("\r\n");

        // 从原始请求头中复制（跳过首行请求行）
        for (int i = 1; i < originalHeaders.size(); i++) {
            String item = originalHeaders.get(i);
            String key = item.contains(": ") ? item.split(": ")[0] : item;

            // 移除指定的请求头
            if (removeHeaders.contains(key)) {
                continue;
            }
            // 扫描请求(GET)不需要 Content-Length
            if ("Content-Length".equalsIgnoreCase(key)) {
                continue;
            }

            // 检查是否有配置替换
            String matchItem = findAndRemoveConfigHeader(configHeaders, key, removeHeaders);
            if (matchItem != null) {
                requestRaw.append(matchItem).append("\r\n");
            } else {
                requestRaw.append(item).append("\r\n");
            }
        }

        // 追加剩余的配置头
        for (String item : configHeaders) {
            if (item.contains(": ")) {
                String key = item.split(": ")[0];
                if (!removeHeaders.contains(key)) {
                    requestRaw.append(item).append("\r\n");
                }
            }
        }
        requestRaw.append("\r\n");

        // 动态变量填充
        try {
            URL url = new URL(buildReqHost(service) + newPath);
            String result = fillAllVariables(service, url, requestRaw.toString());
            if (result == null) {
                return null;
            }
            return result.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            Logger.debug("RequestBuilder: variable fill failed: %s", e.getMessage());
            return requestRaw.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 对已有的完整请求数据包应用请求头替换/移除/动态变量
     * 用于非 Scan 来源的请求（保留原始请求方法和 body）
     *
     * @param originalHeaders 原始请求的 header 列表
     * @param originalBody    原始请求 body（可为 null）
     * @param service         目标服务
     * @param replaceHeader   是否启用替换请求头
     * @param removeHeader    是否启用移除请求头
     * @return 构建后的请求字节，失败返回 null
     */
    public static byte[] buildFullRequest(List<String> originalHeaders, byte[] originalBody,
                                           HttpService service,
                                           boolean replaceHeader, boolean removeHeader) {
        List<String> configHeaders = replaceHeader ? WordlistManager.getHeader() : new ArrayList<>();
        List<String> removeHeaders = removeHeader ? WordlistManager.getRemoveHeaders() : new ArrayList<>();

        StringBuilder requestRaw = new StringBuilder();

        // 保留原始请求行，但统一 HTTP 版本
        String reqLine = originalHeaders.get(0);
        if (reqLine.contains(" HTTP/")) {
            int start = reqLine.lastIndexOf(" HTTP/");
            reqLine = reqLine.substring(0, start) + " HTTP/1.1";
        }
        requestRaw.append(reqLine).append("\r\n");

        // 从原始请求头中复制
        for (int i = 1; i < originalHeaders.size(); i++) {
            String item = originalHeaders.get(i);
            String key = item.contains(": ") ? item.split(": ")[0] : item;

            if (removeHeaders.contains(key)) {
                continue;
            }

            String matchItem = findAndRemoveConfigHeader(configHeaders, key, removeHeaders);
            if (matchItem != null) {
                requestRaw.append(matchItem).append("\r\n");
            } else {
                requestRaw.append(item).append("\r\n");
            }
        }

        // 追加剩余配置头
        for (String item : configHeaders) {
            if (item.contains(": ")) {
                String key = item.split(": ")[0];
                if (!removeHeaders.contains(key)) {
                    requestRaw.append(item).append("\r\n");
                }
            }
        }
        requestRaw.append("\r\n");

        // 追加 body
        if (originalBody != null && originalBody.length > 0) {
            requestRaw.append(new String(originalBody));
        }

        // 动态变量填充
        try {
            String reqPath = extractPathFromReqLine(originalHeaders.get(0));
            URL url = new URL(buildReqHost(service) + reqPath);
            String result = fillAllVariables(service, url, requestRaw.toString());
            if (result == null) {
                return null;
            }
            return updateContentLength(result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.debug("RequestBuilder: build failed: %s", e.getMessage());
            return null;
        }
    }

    // ========== 动态变量填充 ==========

    /**
     * 填充所有动态变量
     */
    public static String fillAllVariables(HttpService service, URL url, String requestRaw) {
        String protocol = service.secure() ? "https" : "http";
        String host = service.host();
        int port = service.port();
        String hostWithPort = (port == 80 || port == 443) ? host : host + ":" + port;
        String domain = host;
        String timestamp = String.valueOf(DateUtils.getTimestamp());
        String randomIP = IPUtils.randomIPv4();
        String randomLocalIP = IPUtils.randomIPv4ForLocal();
        String randomUA = Utils.getRandomItem(WordlistManager.getUserAgent());
        String domainMain = DomainHelper.getDomain(domain, null);
        String domainName = DomainHelper.getDomainName(domain, null);
        String subdomain = getSubdomain(domain);
        String subdomains = getSubdomains(domain);
        String webroot = getWebrootByURL(url);

        try {
            requestRaw = fillVar(requestRaw, "protocol", protocol);
            requestRaw = fillVar(requestRaw, "host", hostWithPort);
            requestRaw = fillVar(requestRaw, "webroot", webroot);

            if (requestRaw.contains("{{ip}}")) {
                String ip = findIpByHost(domain);
                requestRaw = fillVar(requestRaw, "ip", ip);
            }

            requestRaw = fillVar(requestRaw, "domain", domain);
            requestRaw = fillVar(requestRaw, "domain.main", domainMain);
            requestRaw = fillVar(requestRaw, "domain.name", domainName);
            requestRaw = fillVar(requestRaw, "subdomain", subdomain);
            requestRaw = fillVar(requestRaw, "subdomains", subdomains);

            if (requestRaw.contains("{{subdomains.")) {
                if (StringUtils.isEmpty(subdomains)) {
                    return null;
                }
                String[] parts = subdomains.split("\\.");
                for (int i = 0; i < parts.length; i++) {
                    requestRaw = fillVar(requestRaw, "subdomains." + i, parts[i]);
                }
                if (requestRaw.contains("{{subdomains.")) {
                    return null;
                }
            }

            requestRaw = fillVar(requestRaw, "random.ip", randomIP);
            requestRaw = fillVar(requestRaw, "random.local-ip", randomLocalIP);
            requestRaw = fillVar(requestRaw, "random.ua", randomUA);
            requestRaw = fillVar(requestRaw, "timestamp", timestamp);

            if (requestRaw.contains("{{date.") || requestRaw.contains("{{time.")) {
                String currentDate = DateUtils.getCurrentDate("yyyy-MM-dd HH:mm:ss;yy-M-d H:m:s");
                String[] split = currentDate.split(";");
                String[] left = parseDT(split[0]);
                String[] right = parseDT(split[1]);
                requestRaw = fillVar(requestRaw, "date.yyyy", left[0]);
                requestRaw = fillVar(requestRaw, "date.MM", left[1]);
                requestRaw = fillVar(requestRaw, "date.dd", left[2]);
                requestRaw = fillVar(requestRaw, "time.HH", left[3]);
                requestRaw = fillVar(requestRaw, "time.mm", left[4]);
                requestRaw = fillVar(requestRaw, "time.ss", left[5]);
                requestRaw = fillVar(requestRaw, "date.yy", right[0]);
                requestRaw = fillVar(requestRaw, "date.M", right[1]);
                requestRaw = fillVar(requestRaw, "date.d", right[2]);
                requestRaw = fillVar(requestRaw, "time.H", right[3]);
                requestRaw = fillVar(requestRaw, "time.m", right[4]);
                requestRaw = fillVar(requestRaw, "time.s", right[5]);
            }

            return requestRaw;
        } catch (IllegalArgumentException e) {
            Logger.debug("fillAllVariables: %s", e.getMessage());
            return null;
        }
    }

    /**
     * 填充单个变量，值为空时抛异常（丢弃该请求）
     */
    private static String fillVar(String src, String name, String value) {
        if (StringUtils.isEmpty(src)) return src;
        String key = "{{" + name + "}}";
        if (!src.contains(key)) return src;
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException(key + " fill failed, value is empty.");
        }
        return src.replace(key, value);
    }

    // ========== Content-Length 更新 ==========

    /**
     * 更新 Content-Length 参数值
     */
    public static byte[] updateContentLength(byte[] rawBytes) {
        String temp = new String(rawBytes, StandardCharsets.US_ASCII);
        int bodyOffset = temp.indexOf("\r\n\r\n");
        if (bodyOffset == -1) return rawBytes;
        bodyOffset += 4;
        int bodySize = rawBytes.length - bodyOffset;
        if (bodySize <= 0) return rawBytes;

        String header = new String(rawBytes, 0, bodyOffset - 4);
        if (!header.contains("Content-Length")) {
            header += "\r\nContent-Length: " + bodySize;
        } else {
            header = header.replaceAll("Content-Length:.*", "Content-Length: " + bodySize);
        }
        String body = new String(rawBytes, bodyOffset, bodySize);
        return (header + "\r\n\r\n" + body).getBytes(StandardCharsets.UTF_8);
    }

    // ========== 辅助方法 ==========

    private static String findAndRemoveConfigHeader(List<String> configHeaders, String key,
                                                     List<String> removeHeaders) {
        String match = null;
        for (String configItem : configHeaders) {
            if (StringUtils.isNotEmpty(configItem) && configItem.contains(": ")) {
                String configKey = configItem.split(": ")[0];
                if (removeHeaders.contains(configKey)) continue;
                if (configKey.equals(key)) {
                    match = configItem;
                    break;
                }
            }
        }
        if (match != null) {
            configHeaders.remove(match);
        }
        return match;
    }

    public static String buildReqHost(HttpService service) {
        String protocol = service.secure() ? "https" : "http";
        String host = service.host();
        int port = service.port();
        if (Utils.isIgnorePort(port)) {
            return protocol + "://" + host;
        }
        return protocol + "://" + host + ":" + port;
    }

    private static String extractPathFromReqLine(String reqLine) {
        int start = reqLine.indexOf(' ');
        int end = reqLine.lastIndexOf(" HTTP/");
        if (start >= 0 && end > start) {
            return reqLine.substring(start + 1, end);
        }
        return "/";
    }

    private static String findIpByHost(String host) {
        if (IPUtils.hasIPv4(host)) return host;
        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (UnknownHostException e) {
            return "";
        }
    }

    private static String getSubdomain(String domain) {
        String subs = getSubdomains(domain);
        if (StringUtils.isEmpty(subs)) return null;
        return subs.contains(".") ? subs.substring(0, subs.indexOf(".")) : subs;
    }

    private static String getSubdomains(String domain) {
        if (IPUtils.hasIPv4(domain) || !domain.contains(".")) return null;
        String parsed = DomainHelper.getDomain(domain, null);
        if (StringUtils.isEmpty(parsed)) return null;
        int end = domain.lastIndexOf(parsed) - 1;
        return end < 0 ? null : domain.substring(0, end);
    }

    private static String getWebrootByURL(URL url) {
        if (url == null) return null;
        String path = url.getPath();
        if (StringUtils.isEmpty(path) || "/".equals(path)) return null;
        int end = path.indexOf("/", 1);
        return end < 0 ? null : path.substring(1, end);
    }

    private static String[] parseDT(String dt) {
        String[] result = new String[6];
        String[] parts = dt.split(" ");
        String[] d = parts[0].split("-");
        String[] t = parts[1].split(":");
        result[0] = d[0]; result[1] = d[1]; result[2] = d[2];
        result[3] = t[0]; result[4] = t[1]; result[5] = t[2];
        return result;
    }
}
