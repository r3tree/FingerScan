package burp.tdou.fingerscan.core.pipeline;

import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.tdou.common.log.Logger;

/**
 * 重试策略
 * 封装请求重试逻辑，支持可配置的重试次数和间隔
 */
public class RetryPolicy {

    private final RequestExecutor executor;
    private final TimeoutTracker timeoutTracker;

    public RetryPolicy(RequestExecutor executor, TimeoutTracker timeoutTracker) {
        this.executor = executor;
        this.timeoutTracker = timeoutTracker;
    }

    /**
     * 执行请求并在失败时重试
     *
     * @param service       目标服务
     * @param requestBytes  请求数据
     * @param retryCount    最大重试次数（0=不重试）
     * @param retryInterval 重试间隔(ms)
     * @param reqHost       请求主机标识（用于超时追踪）
     * @return 请求响应对象
     * @throws InterruptedException 线程被中断时抛出
     */
    public HttpRequestResponse executeWithRetry(HttpService service, byte[] requestBytes,
                                                  int retryCount, int retryInterval,
                                                  String reqHost) throws InterruptedException {

        HttpRequestResponse result = executor.send(service, requestBytes);

        // 检查是否有有效响应
        if (hasValidResponse(result)) {
            return result;
        }

        // 没有重试机会，标记超时
        if (retryCount <= 0) {
            timeoutTracker.markTimeout(reqHost);
            return result;
        }

        // 重试循环
        for (int i = 0; i < retryCount; i++) {
            // 线程中断检查
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Retry interrupted");
            }

            Logger.debug("Retry request host: %s, attempt: %d/%d", reqHost, i + 1, retryCount);

            // 重试间隔
            if (retryInterval > 0) {
                Thread.sleep(retryInterval);
            }

            result = executor.send(service, requestBytes);
            if (hasValidResponse(result)) {
                return result;
            }
        }

        // 所有重试都失败，标记超时
        timeoutTracker.markTimeout(reqHost);
        return result;
    }

    private boolean hasValidResponse(HttpRequestResponse reqResp) {
        if (reqResp == null) return false;
        return reqResp.response() != null && reqResp.response().toByteArray().length() > 0;
    }
}
