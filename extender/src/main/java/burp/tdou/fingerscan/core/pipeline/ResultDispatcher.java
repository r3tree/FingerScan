package burp.tdou.fingerscan.core.pipeline;

import burp.tdou.common.log.Logger;
import burp.tdou.fingerscan.core.ScanResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 结果分发器
 * 将扫描结果分发给所有注册的消费者（观察者模式）
 * 使用 CopyOnWriteArrayList 保证线程安全
 */
public class ResultDispatcher {

    /**
     * 结果消费者接口
     */
    public interface ScanResultConsumer {
        /**
         * 接收扫描结果
         *
         * @param result 扫描结果
         */
        void onResult(ScanResult result);
    }

    private final List<ScanResultConsumer> consumers = new CopyOnWriteArrayList<>();

    /**
     * 注册结果消费者
     */
    public void register(ScanResultConsumer consumer) {
        if (consumer != null) {
            consumers.add(consumer);
        }
    }

    /**
     * 移除结果消费者
     */
    public void unregister(ScanResultConsumer consumer) {
        consumers.remove(consumer);
    }

    /**
     * 分发扫描结果给所有消费者
     */
    public void dispatch(ScanResult result) {
        if (result == null) {
            return;
        }
        for (ScanResultConsumer consumer : consumers) {
            try {
                consumer.onResult(result);
            } catch (Exception e) {
                Logger.error("ResultDispatcher: consumer error: %s", e.getMessage());
            }
        }
    }

    /**
     * 清空所有消费者
     */
    public void clear() {
        consumers.clear();
    }

    /**
     * 当前消费者数量
     */
    public int getConsumerCount() {
        return consumers.size();
    }
}
