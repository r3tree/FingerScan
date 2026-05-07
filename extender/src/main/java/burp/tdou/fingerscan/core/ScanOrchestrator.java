package burp.tdou.fingerscan.core;

import burp.tdou.common.log.Logger;
import burp.tdou.fingerscan.core.pipeline.RequestPipeline;
import burp.tdou.fingerscan.core.strategy.ScanStrategy;
import burp.tdou.fingerscan.filter.FilterChain;

import java.util.ArrayList;
import java.util.List;

/**
 * 扫描编排器 — 唯一的扫描入口
 *
 * 所有来源（代理、手动发送、导入URL、重定向）的请求都通过此类进入。
 * 编排器负责：
 * 1. 全局前置过滤（黑白名单、方法过滤、后缀过滤）
 * 2. 调用注册的 ScanStrategy 生成扫描任务
 * 3. 将任务提交到统一的 RequestPipeline
 */
public class ScanOrchestrator {

    private final List<ScanStrategy> strategies = new ArrayList<>();
    private final RequestPipeline pipeline;
    private final FilterChain filterChain;

    public ScanOrchestrator(RequestPipeline pipeline, FilterChain filterChain) {
        this.pipeline = pipeline;
        this.filterChain = filterChain;
    }

    /**
     * 注册扫描策略
     */
    public ScanOrchestrator registerStrategy(ScanStrategy strategy) {
        if (strategy != null) {
            strategies.add(strategy);
            Logger.debug("ScanOrchestrator: registered strategy: %s", strategy.getName());
        }
        return this;
    }

    /**
     * 唯一入口：提交扫描请求
     *
     * @param request 统一扫描请求
     */
    public void submit(ScanRequest request) {
        if (request == null) {
            return;
        }

        // 管道已关闭
        if (pipeline.isShutdown()) {
            Logger.debug("ScanOrchestrator: pipeline is shutdown, ignoring request");
            return;
        }

        // 全局前置过滤
        if (!filterChain.accept(request)) {
            Logger.debug("ScanOrchestrator: request filtered by chain, host: %s", request.getHost());
            return;
        }

        // 每个 Strategy 生成自己的任务
        for (ScanStrategy strategy : strategies) {
            try {
                if (strategy.shouldApply(request)) {
                    List<ScanTask> tasks = strategy.generateTasks(request);
                    if (tasks != null) {
                        for (ScanTask task : tasks) {
                            // 管道关闭时停止提交
                            if (pipeline.isShutdown()) {
                                Logger.debug("ScanOrchestrator: pipeline shutdown during task submission");
                                return;
                            }
                            pipeline.submit(task);
                        }
                    }
                    Logger.debug("ScanOrchestrator: strategy %s generated %d tasks",
                            strategy.getName(), tasks != null ? tasks.size() : 0);
                }
            } catch (Exception e) {
                Logger.error("ScanOrchestrator: strategy %s error: %s",
                        strategy.getName(), e.getMessage());
            }
        }
    }

    /**
     * 获取请求管道
     */
    public RequestPipeline getPipeline() {
        return pipeline;
    }

    /**
     * 获取过滤器链
     */
    public FilterChain getFilterChain() {
        return filterChain;
    }

    /**
     * 停止所有扫描任务
     */
    public RequestPipeline.StopResult stopAll() {
        return pipeline.stopAll();
    }

    /**
     * 完全关闭（插件卸载时调用）
     */
    public RequestPipeline.ShutdownResult shutdown() {
        return pipeline.shutdown();
    }

    /**
     * 清空历史
     */
    public void clearHistory() {
        pipeline.clearHistory();
    }

    /**
     * 注册的策略数量
     */
    public int getStrategyCount() {
        return strategies.size();
    }
}
