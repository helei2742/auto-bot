package cn.com.helei.DepinBot.core.util;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class ClosableTimerTask {

    private volatile boolean isRunning = true;

    public abstract boolean run();
}
