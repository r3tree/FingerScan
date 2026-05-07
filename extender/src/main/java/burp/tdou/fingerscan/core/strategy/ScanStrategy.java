package burp.tdou.fingerscan.core.strategy;

import burp.tdou.fingerscan.core.ScanRequest;
import burp.tdou.fingerscan.core.ScanTask;

import java.util.List;

/**
 * 扫描策略接口
 * 每种扫描逻辑（被动指纹、递归目录、Payload处理）实现此接口
 */
public interface ScanStrategy {

    /**
     * 当前策略是否应该对这个请求生效
     */
    boolean shouldApply(ScanRequest request);

    /**
     * 根据请求生成需要执行的扫描任务列表
     */
    List<ScanTask> generateTasks(ScanRequest request);

    /**
     * 策略名称（用于日志和调试）
     */
    String getName();
}
