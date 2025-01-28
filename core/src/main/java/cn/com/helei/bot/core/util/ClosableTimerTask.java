package cn.com.helei.bot.core.util;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

@Setter
@Getter
public class ClosableTimerTask {

    private volatile boolean isRunning = true;

    private Supplier<Boolean> task;

    public ClosableTimerTask(Supplier<Boolean> task) {
        this.task = task;
    }
}
