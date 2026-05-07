package burp.tdou.fingerscan.core.pipeline;

/**
 * QPS 限速器
 * 滑动窗口 + 预约占位算法
 *
 * 核心思路：每个线程在锁内「预约」一个时间槽（记录计划发送时间而非实际发送时间），
 * 释放锁后再 sleep 到预约时间。这样既不持锁 sleep，又能精确限速。
 */
public class QpsLimiter {

    private volatile long[] accessTime;
    private volatile int index;
    private volatile int limit;
    private volatile int delay;

    public QpsLimiter(int limit, int delay) {
        update(limit, delay);
    }

    /**
     * 获取许可（阻塞式）
     */
    public void acquire() throws InterruptedException {
        int currentDelay = delay;
        if (currentDelay > 0) {
            Thread.sleep(currentDelay);
        }

        long sleepUntil;

        synchronized (this) {
            if (accessTime == null || limit <= 0) {
                return;
            }

            long now = System.currentTimeMillis();
            long oldest = accessTime[index];

            // 计算这个槽位最早可用的时间
            long earliest = oldest + 1000;

            if (now >= earliest) {
                // 槽位已过期，立即可用
                accessTime[index] = now;
                sleepUntil = 0;
            } else {
                // 槽位还在窗口内，预约到 earliest 时间点
                accessTime[index] = earliest;
                sleepUntil = earliest;
            }
            index = (index + 1) % limit;
        }

        // 锁外等待到预约时间
        if (sleepUntil > 0) {
            long now = System.currentTimeMillis();
            if (sleepUntil > now) {
                Thread.sleep(sleepUntil - now);
            }
        }
    }

    /**
     * 更新限速参数
     */
    public synchronized void update(int limit, int delay) {
        this.delay = Math.max(0, delay);
        this.limit = limit;
        if (limit > 0 && limit <= 9999) {
            this.accessTime = new long[limit];
            this.index = 0;
        } else {
            this.accessTime = null;
        }
    }

    public int getLimit() { return limit; }
    public int getDelay() { return delay; }
}
