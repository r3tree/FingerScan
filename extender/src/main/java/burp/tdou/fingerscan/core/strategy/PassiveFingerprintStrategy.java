package burp.tdou.fingerscan.core.strategy;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.tdou.fingerscan.core.ScanRequest;
import burp.tdou.fingerscan.core.ScanTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 被动指纹识别策略
 * 对已有的 request/response 数据做指纹匹配，不发送新的 HTTP 请求
 */
public class PassiveFingerprintStrategy implements ScanStrategy {

    @Override
    public boolean shouldApply(ScanRequest request) {
        return request.hasResponse();
    }

    @Override
    public List<ScanTask> generateTasks(ScanRequest request) {
        if (!shouldApply(request)) {
            return Collections.emptyList();
        }

        String dedupKey = "passive:" + request.getHost() + request.getPath();
        HttpRequestResponse reqResp = request.getHttpReqResp();

        List<ScanTask> tasks = new ArrayList<>();
        tasks.add(ScanTask.passiveMatch(reqResp, request.getService(), dedupKey, request.getFrom()));
        return tasks;
    }

    @Override
    public String getName() {
        return "PassiveFingerprint";
    }
}
