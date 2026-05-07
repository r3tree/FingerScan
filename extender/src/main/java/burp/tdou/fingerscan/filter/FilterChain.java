package burp.tdou.fingerscan.filter;

import burp.tdou.common.log.Logger;
import burp.tdou.fingerscan.core.ScanRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 过滤器链
 * 按注册顺序依次执行过滤器，任一过滤器拒绝则整体拒绝
 */
public class FilterChain implements ScanFilter {

    private final List<ScanFilter> filters = new ArrayList<>();

    /**
     * 添加过滤器
     */
    public FilterChain add(ScanFilter filter) {
        if (filter != null) {
            filters.add(filter);
        }
        return this;
    }

    /**
     * 移除过滤器
     */
    public void remove(ScanFilter filter) {
        filters.remove(filter);
    }

    @Override
    public boolean accept(ScanRequest request) {
        for (ScanFilter filter : filters) {
            if (!filter.accept(request)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 过滤器数量
     */
    public int size() {
        return filters.size();
    }
}
