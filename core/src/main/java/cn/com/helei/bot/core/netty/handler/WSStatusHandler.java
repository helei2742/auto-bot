package cn.com.helei.bot.core.netty.handler;

public interface WSStatusHandler {

    void onConnected();

    void onClosed();
}
