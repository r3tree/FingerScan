package burp.tdou.fingerscan.core.pipeline;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 超时主机追踪器
 * 记录已超时的请求主机，避免重复请求超时主机
 */
public class TimeoutTracker {

    private final Set<String> timedOutHosts = ConcurrentHashMap.newKeySet();

    /**
     * 标记主机超时
     *
     * @param reqHost 主机地址（格式：http://x.x.x.x 或 http://x.x.x.x:8080）
     */
    public void markTimeout(String reqHost) {
        if (reqHost != null) {
            timedOutHosts.add(reqHost);
        }
    }

    /**
     * 检查主机是否已超时
     *
     * @param reqHost 主机地址
     * @return true=已超时(应跳过)；false=正常
     */
    public boolean isTimedOut(String reqHost) {
        if (reqHost == null || timedOutHosts.isEmpty()) {
            return false;
        }
        return timedOutHosts.contains(reqHost);
    }

    /**
     * 清空所有超时记录
     */
    public void clear() {
        timedOutHosts.clear();
    }

    /**
     * 当前超时主机数量
     */
    public int size() {
        return timedOutHosts.size();
    }
}
