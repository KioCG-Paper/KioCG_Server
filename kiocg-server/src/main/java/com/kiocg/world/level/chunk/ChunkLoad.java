package com.kiocg.world.level.chunk;

/**
 * 单个区块的负载统计。
 */
public class ChunkLoad {
    // 采样窗口长度（tick），由 MinecraftServer 每 20 tick 调用一次 newTick()
    private static final int SAMPLE_TICKS = 20;
    // 等效窗口数量
    private static final int EQUIVALENT_WINDOWS = 15;
    // EMA 平滑系数
    private static final double ALPHA = 2.0 / (EQUIVALENT_WINDOWS + 1);
    // 单个具体统计的最大值，限制偶发尖峰对均值的影响
    private static final long MAX_MEASURE_NANOS = 1_000_000L;

    // 统计是否进行中
    private boolean started = false;
    // EMA 是否已经用第一个样本初始化
    private boolean initialized = false;
    // 当前的负载估计值，单位 ns/tick
    private double load;
    // 当前窗口内累计的耗时
    private long time;

    // 用于每个具体统计的计算
    private long nanos;
    // 统计计算深度, 防止嵌套的统计重复计算
    private int depth;

    /**
     * 结束上一个采样窗口，把窗口内的耗时并入 EMA
     */
    public void newTick() {
        if (!this.started) {
            this.started = true;
            return;
        }

        final double sample = (double) this.time / SAMPLE_TICKS;
        if (!this.initialized) {
            // 第一个样本直接作为初值，避免从 0 缓慢爬升
            this.load = sample;
            this.initialized = true;
        } else {
            this.load += (sample - this.load) * ALPHA;
        }
        this.time = 0L;
    }

    /**
     * 开始一个具体统计
     */
    public void startTicking() {
        if (!this.started || this.depth++ != 0) return;
        this.nanos = System.nanoTime();
    }

    /**
     * 结束一个具体统计
     * 将统计值计入当前负载窗口
     */
    public void endTickingAndCount() {
        if (!this.started || --this.depth != 0) return;
        this.time += Math.min(System.nanoTime() - this.nanos, MAX_MEASURE_NANOS);
    }

    /**
     * 清空统计 (当区块卸载时)
     */
    public void clear() {
        this.started = false;
        this.initialized = false;
        this.load = 0.0;
        this.time = 0L;
        this.nanos = 0L;
        this.depth = 0;
    }

    /**
     * @return 区块负载的指数移动平均值，单位 ns/tick
     */
    public long getAverage() {
        return (long) this.load;
    }
}
