package burp.tdou.fingerscan.core.strategy;

import burp.api.montoya.http.HttpService;
import burp.tdou.common.log.Logger;
import burp.tdou.common.utils.StringUtils;
import burp.tdou.common.utils.Utils;
import burp.tdou.fingerscan.common.Config;
import burp.tdou.fingerscan.core.ScanRequest;
import burp.tdou.fingerscan.core.ScanTask;
import burp.tdou.fingerscan.core.pipeline.RequestBuilder;
import burp.tdou.fingerscan.core.rule.RuleEngine;
import burp.tdou.fingerscan.ui.tab.DataBoardTab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 递归目录扫描策略
 * 将请求路径分解为多级目录，与规则引擎的扫描路径组合，生成扫描任务。
 * 使用代理原始请求头 + 配置头替换/移除 + 动态变量填充。
 */
public class RecursiveDirectoryScanStrategy implements ScanStrategy {

    private final RuleEngine ruleEngine;
    private DataBoardTab dataBoardTab;

    public RecursiveDirectoryScanStrategy(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * 设置 DataBoardTab 引用（用于读取 UI 开关状态）
     */
    public void setDataBoardTab(DataBoardTab dataBoardTab) {
        this.dataBoardTab = dataBoardTab;
    }

    @Override
    public boolean shouldApply(ScanRequest request) {
        return dataBoardTab != null && dataBoardTab.hasActiveScan();
    }

    @Override
    public List<ScanTask> generateTasks(ScanRequest request) {
        List<String> scanPaths = ruleEngine.getScanPaths();
        if (scanPaths.isEmpty()) {
            return Collections.emptyList();
        }

        HttpService service = request.getService();
        if (service == null) {
            return Collections.emptyList();
        }

        // 获取原始请求头
        List<String> originalHeaders = getOriginalHeaders(request);

        String urlPath = request.getPath();
        List<String> basePaths = getUrlPathDict(urlPath);

        // 读取 UI 开关状态
        boolean replaceHeader = dataBoardTab != null && dataBoardTab.hasReplaceHeader();
        boolean removeHeader = dataBoardTab != null && dataBoardTab.hasRemoveHeader();

        List<ScanTask> tasks = new ArrayList<>();

        // 基础扫描：直接使用规则路径
        for (String scanPath : scanPaths) {
            String fullPath = normalizePath(scanPath);
            ScanTask task = buildScanTask(service, fullPath, originalHeaders, replaceHeader, removeHeader);
            if (task != null) {
                tasks.add(task);
            }
        }

        // 递归扫描：basePath × scanPath
        for (String basePath : basePaths) {
            if ("/".equals(basePath)) {
                continue;
            }
            for (String scanPath : scanPaths) {
                String fullPath = basePath + normalizePath(scanPath);
                ScanTask task = buildScanTask(service, fullPath, originalHeaders, replaceHeader, removeHeader);
                if (task != null) {
                    tasks.add(task);
                }
            }
        }

        Logger.debug("RecursiveStrategy: generated %d tasks for path %s", tasks.size(), urlPath);
        return tasks;
    }

    /**
     * 从 ScanRequest 中提取原始请求头列表
     */
    private List<String> getOriginalHeaders(ScanRequest request) {
        if (request.getHttpReqResp() != null && request.getHttpReqResp().request() != null) {
            try {
                byte[] reqBytes = request.getHttpReqResp().request().toByteArray().getBytes();
                // 手动解析请求头（不依赖 IExtensionHelpers）
                String reqStr = new String(reqBytes);
                int headerEnd = reqStr.indexOf("\r\n\r\n");
                if (headerEnd < 0) headerEnd = reqStr.length();
                String headerPart = reqStr.substring(0, headerEnd);
                String[] lines = headerPart.split("\r\n");
                List<String> headers = new ArrayList<>();
                for (String line : lines) {
                    headers.add(line);
                }
                if (!headers.isEmpty()) {
                    return headers;
                }
            } catch (Exception e) {
                Logger.debug("RecursiveStrategy: failed to parse original headers: %s", e.getMessage());
            }
        }

        // 降级：构建最小请求头
        HttpService service = request.getService();
        List<String> fallback = new ArrayList<>();
        fallback.add("GET / HTTP/1.1");
        String host = service.host();
        int port = service.port();
        if (!Utils.isIgnorePort(port)) {
            host = host + ":" + port;
        }
        fallback.add("Host: " + host);
        fallback.add("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        fallback.add("Accept: */*");
        fallback.add("Connection: close");
        return fallback;
    }

    /**
     * 使用 RequestBuilder 构建带原始请求头的扫描任务
     */
    private ScanTask buildScanTask(HttpService service, String path,
                                    List<String> originalHeaders,
                                    boolean replaceHeader, boolean removeHeader) {
        byte[] requestBytes = RequestBuilder.buildScanRequest(
                originalHeaders, path, service, replaceHeader, removeHeader);

        if (requestBytes == null) {
            return null;
        }

        // 生成去重键
        String dedupKey = RequestBuilder.buildReqHost(service) + path;

        return ScanTask.httpRequest(service, requestBytes, dedupKey, ScanRequest.FROM_SCAN);
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "/";
        return path.startsWith("/") ? path : "/" + path;
    }

    /**
     * 使用 '/' 分割 URL 路径，生成各级目录列表
     */
    private List<String> getUrlPathDict(String urlPath) {
        String direct = Config.get(Config.KEY_SCAN_LEVEL_DIRECT);
        int scanLevel = Config.getInt(Config.KEY_SCAN_LEVEL);
        List<String> result = new ArrayList<>();
        result.add("/");

        if (StringUtils.isEmpty(urlPath) || "/".equals(urlPath)) {
            return result;
        }
        if (Config.DIRECT_LEFT.equals(direct) && scanLevel <= 1) {
            return result;
        }
        if (!urlPath.endsWith("/")) {
            urlPath = urlPath.substring(0, urlPath.lastIndexOf("/") + 1);
        }
        String[] segments = urlPath.split("/");
        if (segments.length == 0) {
            return result;
        }
        if (Config.DIRECT_RIGHT.equals(direct) && scanLevel < segments.length) {
            result.remove("/");
        }

        StringBuilder sb = new StringBuilder("/");
        for (String segment : segments) {
            if (StringUtils.isNotEmpty(segment)) {
                sb.append(segment).append("/");
                int level = StringUtils.countMatches(sb.toString(), "/");
                if (Config.DIRECT_LEFT.equals(direct) && level > scanLevel) {
                    continue;
                } else if (Config.DIRECT_RIGHT.equals(direct)) {
                    level = segments.length - level;
                    if (level >= scanLevel) {
                        continue;
                    }
                }
                String path = sb.toString();
                if (path.endsWith("/") && path.length() > 1) {
                    path = path.substring(0, path.length() - 1);
                }
                result.add(path);
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return "RecursiveDirectory";
    }
}
