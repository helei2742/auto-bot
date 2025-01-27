package cn.com.helei.DepinBot.core.netty.handler;

public interface WSStatusHandler {

    void onConnected();

    void onClosed();
}
