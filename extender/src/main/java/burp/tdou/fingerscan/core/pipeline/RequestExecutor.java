package burp.tdou.fingerscan.core.pipeline;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.tdou.common.log.Logger;
import burp.tdou.fingerscan.common.HttpReqRespAdapter;

/**
 * 请求执行器
 * 封装 Burp 的 HTTP 请求发送，集中管理请求执行
 */
public class RequestExecutor {

    private final MontoyaApi api;

    public RequestExecutor(MontoyaApi api) {
        this.api = api;
    }

    /**
     * 发送 HTTP 请求
     *
     * @param service     目标服务
     * @param requestBytes 请求数据
     * @return 请求响应对象（永不返回 null）
     */
    public HttpRequestResponse send(HttpService service, byte[] requestBytes) {
        try {
            HttpRequest request = HttpRequest.httpRequest(service, ByteArray.byteArray(requestBytes));
            HttpRequestResponse reqResp = api.http().sendRequest(request);
            if (reqResp.response() != null && reqResp.response().toByteArray().length() > 0) {
                return reqResp;
            }
            // 响应为空视为失败
            return HttpReqRespAdapter.from(service, requestBytes);
        } catch (Exception e) {
            Logger.debug("RequestExecutor send error, host: %s, error: %s",
                    service.host(), e.getMessage());
            return HttpReqRespAdapter.from(service, requestBytes);
        }
    }
}
