package cn.com.helei.DepinBot.core.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final String namePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public NamedThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        // 使用指定的名称前缀和自动编号来创建线程名称
        Thread thread = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
        // 设置为守护线程（可选）
        thread.setDaemon(false);
        return thread;
    }
}
