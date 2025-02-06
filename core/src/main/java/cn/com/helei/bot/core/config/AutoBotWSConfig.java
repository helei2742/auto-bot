package cn.com.helei.bot.core.config;

import lombok.Data;


@Data
public class AutoBotWSConfig {

    /**
     * ws是否无限重试
     */
    private boolean wsUnlimitedRetry = false;

    /**
     * 连接url
     */
    private String wsBaseUrl;

    /**
     * 心跳间隔
     */
    private int heartBeatIntervalSecond = 30;

    /**
     * websocket 并发数量
     */
    private int wsConnectCount = 5;

    /**
     * 重连减少的间隔
     */
    private int reconnectCountDownSecond = 180;
}
