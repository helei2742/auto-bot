package cn.com.helei.bot.core.supporter.netty.handler;

public interface WSStatusHandler {

    void onConnected();

    void onClosed();
}
