package burp.tdou.fingerscan.core.pipeline;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 去重过滤器
 * 基于请求 ID 进行全局去重，替代原来分散在各处的去重逻辑
 */
public class DeduplicateFilter {

    private final Set<String> seen;

    public DeduplicateFilter() {
        this(16384);
    }

    public DeduplicateFilter(int initialCapacity) {
        this.seen = ConcurrentHashMap.newKeySet(initialCapacity);
    }

    /**
     * 检查是否重复。如果不重复，自动标记为已处理。
     *
     * @param key 去重键
     * @return true=重复(应跳过)；false=首次出现(已自动标记)
     */
    public boolean isDuplicate(String key) {
        if (key == null) {
            return false;
        }
        return !seen.add(key);
    }

    /**
     * 移除已记录的去重键（任务取消时调用）
     */
    public void remove(String key) {
        if (key != null) {
            seen.remove(key);
        }
    }

    /**
     * 清空所有去重记录
     */
    public void clear() {
        seen.clear();
    }

    /**
     * 当前去重记录数量
     */
    public int size() {
        return seen.size();
    }
}
