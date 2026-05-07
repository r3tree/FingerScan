package burp.tdou.fingerscan.filter;

import burp.tdou.fingerscan.core.ScanRequest;

/**
 * 扫描请求过滤器接口
 */
public interface ScanFilter {

    /**
     * 检测请求是否应该被接受（通过过滤）
     *
     * @param request 扫描请求
     * @return true=接受(继续处理)；false=拒绝(丢弃)
     */
    boolean accept(ScanRequest request);
}
