package burp.tdou.fingerscan.core.strategy;

import burp.api.montoya.http.HttpService;
import burp.tdou.common.log.Logger;
import burp.tdou.fingerscan.common.Config;
import burp.tdou.fingerscan.common.Constants;
import burp.tdou.fingerscan.core.ScanRequest;
import burp.tdou.fingerscan.core.ScanTask;
import burp.tdou.fingerscan.core.pipeline.RequestBuilder;
import burp.tdou.fingerscan.ui.tab.DataBoardTab;
import burp.tdou.fingerscan.ui.widget.payloadlist.PayloadItem;
import burp.tdou.fingerscan.ui.widget.payloadlist.PayloadRule;
import burp.tdou.fingerscan.ui.widget.payloadlist.ProcessingItem;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Payload Processing 策略
 * 对原始请求应用 Payload 变换规则（前缀、后缀、正则替换、条件检查）。
 *
 * 两种模式：
 * - Merge 模式：所有启用的合并规则依次应用到同一个请求上，产生 1 个任务
 * - Non-Merge 模式：每个独立规则单独应用，各自产生 1 个任务
 */
public class PayloadProcessingStrategy implements ScanStrategy {

    private DataBoardTab dataBoardTab;

    public void setDataBoardTab(DataBoardTab dataBoardTab) {
        this.dataBoardTab = dataBoardTab;
    }

    @Override
    public boolean shouldApply(ScanRequest request) {
        return dataBoardTab != null
                && dataBoardTab.hasActiveScan()
                && dataBoardTab.hasPayloadProcessing();
    }

    @Override
    public List<ScanTask> generateTasks(ScanRequest request) {
        if (!shouldApply(request)) {
            return Collections.emptyList();
        }

        HttpService service = request.getService();
        if (service == null || request.getHttpReqResp() == null) {
            return Collections.emptyList();
        }

        byte[] originalRequest = request.getHttpReqResp().request() != null
                ? request.getHttpReqResp().request().toByteArray().getBytes() : null;
        if (originalRequest == null || originalRequest.length == 0) {
            return Collections.emptyList();
        }

        // 先应用请求头配置
        List<String> headers = parseHeaders(originalRequest);
        byte[] body = parseBody(originalRequest);
        boolean replaceHeader = dataBoardTab.hasReplaceHeader();
        boolean removeHeader = dataBoardTab.hasRemoveHeader();
        byte[] preparedRequest = RequestBuilder.buildFullRequest(
                headers, body, service, replaceHeader, removeHeader);
        if (preparedRequest == null) {
            preparedRequest = originalRequest;
        }

        // 获取已启用的处理规则
        ArrayList<ProcessingItem> allProcess = Config.getPayloadProcessList();
        if (allProcess == null || allProcess.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProcessingItem> enabledProcess = allProcess.stream()
                .filter(ProcessingItem::isEnabled)
                .collect(Collectors.toList());
        if (enabledProcess.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScanTask> tasks = new ArrayList<>();
        String dedupBase = RequestBuilder.buildReqHost(service) + request.getPath();

        // 1. 合并模式
        byte[] mergedRequest = buildMergedRequest(service, preparedRequest, enabledProcess);
        if (mergedRequest != null) {
            boolean changed = !Arrays.equals(preparedRequest, mergedRequest);
            String from = changed
                    ? request.getFrom() + "(" + ScanRequest.FROM_PROCESS + ")"
                    : request.getFrom();
            String dedupKey = dedupBase + ":merged";
            tasks.add(ScanTask.httpRequest(service, mergedRequest, dedupKey, from));
        }

        // 2. 非合并模式
        tasks.addAll(buildNonMergeTasks(service, preparedRequest, enabledProcess, dedupBase));

        Logger.debug("PayloadProcessingStrategy: generated %d tasks", tasks.size());
        return tasks;
    }

    private byte[] buildMergedRequest(HttpService service, byte[] reqBytes,
                                       List<ProcessingItem> processList) {
        List<ProcessingItem> mergeItems = processList.stream()
                .filter(ProcessingItem::isEnabledAndMerge)
                .collect(Collectors.toList());
        if (mergeItems.isEmpty()) {
            return reqBytes;
        }

        byte[] result = reqBytes;
        for (ProcessingItem item : mergeItems) {
            result = applyPayloadRules(service, result, item.getItems());
            if (result == null) {
                return reqBytes;
            }
        }
        return result;
    }

    private List<ScanTask> buildNonMergeTasks(HttpService service, byte[] reqBytes,
                                               List<ProcessingItem> processList,
                                               String dedupBase) {
        List<ScanTask> tasks = new ArrayList<>();

        for (ProcessingItem item : processList) {
            if (!item.isEnabledWithoutMerge()) continue;

            byte[] processed = applyPayloadRules(service, reqBytes, item.getItems());
            if (processed == null || Arrays.equals(reqBytes, processed)) continue;

            String from = ScanRequest.FROM_PROCESS + "(" + item.getName() + ")";
            String dedupKey = dedupBase + ":pp:" + item.getName();
            tasks.add(ScanTask.httpRequest(service, processed, dedupKey, from));
        }
        return tasks;
    }

    private byte[] applyPayloadRules(HttpService service, byte[] requestBytes,
                                      List<PayloadItem> items) {
        if (requestBytes == null || items == null || items.isEmpty()) {
            return null;
        }

        String reqStr = new String(requestBytes);
        int bodyOffset = reqStr.indexOf("\r\n\r\n");
        if (bodyOffset < 0) return null;

        String header = reqStr.substring(0, bodyOffset);
        String body = reqStr.substring(bodyOffset + 4);
        String url = extractUrl(header);
        String request = reqStr;

        for (PayloadItem item : items) {
            PayloadRule rule = item.getRule();
            try {
                switch (item.getScope()) {
                    case PayloadRule.SCOPE_URL:
                        String newUrl = rule.handleProcess(url);
                        String reqLine = header.substring(0, header.indexOf("\r\n"));
                        Matcher matcher = Constants.REGEX_REQ_LINE_URL.matcher(reqLine);
                        if (matcher.find()) {
                            header = header.substring(0, matcher.start(1))
                                    + newUrl + header.substring(matcher.end(1));
                            request = header + "\r\n\r\n" + body;
                        }
                        url = newUrl;
                        break;
                    case PayloadRule.SCOPE_HEADER:
                        header = rule.handleProcess(header);
                        request = header + "\r\n\r\n" + body;
                        break;
                    case PayloadRule.SCOPE_BODY:
                        body = rule.handleProcess(body);
                        request = header + "\r\n\r\n" + body;
                        break;
                    case PayloadRule.SCOPE_REQUEST:
                        request = rule.handleProcess(request);
                        break;
                }
            } catch (Exception e) {
                Logger.debug("PayloadProcessing rule error: %s", e.getMessage());
                return null;
            }
        }

        // 动态变量
        try {
            java.net.URL u = new java.net.URL(RequestBuilder.buildReqHost(service) + url);
            String filled = RequestBuilder.fillAllVariables(service, u, request);
            if (filled == null) return null;
            return RequestBuilder.updateContentLength(filled.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private String extractUrl(String header) {
        int end = header.indexOf("\r\n");
        if (end < 0) return "/";
        String reqLine = header.substring(0, end);
        Matcher matcher = Constants.REGEX_REQ_LINE_URL.matcher(reqLine);
        return (matcher.find() && matcher.groupCount() >= 1) ? matcher.group(1) : "/";
    }

    private List<String> parseHeaders(byte[] request) {
        String s = new String(request);
        int end = s.indexOf("\r\n\r\n");
        if (end < 0) end = s.length();
        String[] lines = s.substring(0, end).split("\r\n");
        List<String> headers = new ArrayList<>();
        Collections.addAll(headers, lines);
        return headers;
    }

    private byte[] parseBody(byte[] request) {
        String s = new String(request);
        int offset = s.indexOf("\r\n\r\n");
        if (offset < 0 || offset + 4 >= request.length) return new byte[0];
        return Arrays.copyOfRange(request, offset + 4, request.length);
    }

    @Override
    public String getName() {
        return "PayloadProcessing";
    }
}
