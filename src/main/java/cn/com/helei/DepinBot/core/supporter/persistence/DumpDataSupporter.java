package cn.com.helei.DepinBot.core.supporter.persistence;

import cn.com.helei.DepinBot.core.util.DiscardingBlockingQueue;
import cn.com.helei.DepinBot.core.util.FileUtil;
import cn.com.helei.DepinBot.core.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DumpDataSupporter {

    private final ConcurrentMap<String, DiscardingBlockingQueue<String>> updateQueueDumpPathMap = new ConcurrentHashMap<>();

    /**
     * 执行的线程池
     */
    private final ExecutorService executorService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final long intervalSecond = 10;

    public DumpDataSupporter() {
        this.executorService = Executors.newThreadPerTaskExecutor(new NamedThreadFactory("persistence-"));
    }


    /**
     * 绑定更新队列
     *
     * @param dumpPath dumpPath
     * @param queue    queue
     */
    public void bindUpdateQueue(String dumpPath, DiscardingBlockingQueue<String> queue) {
        updateQueueDumpPathMap.compute(dumpPath, (k, v) -> {
            if (v == null) {
                v = queue;
            }

            return v;
        });
    }

    /**
     * 开启dump任务
     */
    public void startDumpTask() {
        if (running.compareAndSet(false, true)) {
            executorService.execute(() -> {
                while (running.get()) {
                    try {
                        TimeUnit.SECONDS.sleep(intervalSecond);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        Integer successCount = dumpAllQueue().get();
                        log.debug("dump执行完毕，成功[{}],共[{}]", successCount, updateQueueDumpPathMap.size());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else {
            log.warn("dump 线程已启动，无需程序开启");
        }
    }


    /**
     * dumpAllQueue
     *
     * @return success count
     */
    private CompletableFuture<Integer> dumpAllQueue() {
        log.debug("开始启动dump任务");

        List<CompletableFuture<Boolean>> futures = updateQueueDumpPathMap
                .entrySet()
                .stream()
                .map(e ->
                        dumpQueue(e.getKey(), e.getValue())
                                .exceptionallyAsync(throwable -> {
                                    log.error("保存[{}]发生异常", e.getKey(), throwable);
                                    return null;
                                }, executorService)
                )
                .toList();

        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(unused -> {
                    int count = 0;
                    for (CompletableFuture<Boolean> future : futures) {
                        try {
                            if (future.get()) {
                                count++;
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return count;
                }, executorService);
    }


    /**
     * dump
     *
     * @param dumpPath dumpPath
     * @param queue    queue
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> dumpQueue(String dumpPath, DiscardingBlockingQueue<String> queue) {
        return CompletableFuture.supplyAsync(() -> {
            String dump = null;

            // 循环取到最新的dump数据
            while (!queue.isEmpty()) {
                try {
                    dump = queue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException("take data from DiscardingBlockingQueue error", e);
                }
            }

            if (dump != null) {
                // 保存
                try {
                    FileUtil.saveJSONStringContext(Path.of(dumpPath), dump);
                    return true;
                } catch (Exception e) {
                    log.error("保存[{}]发生异常", dumpPath, e);
                }
            }

            return false;
        }, executorService);
    }
}
