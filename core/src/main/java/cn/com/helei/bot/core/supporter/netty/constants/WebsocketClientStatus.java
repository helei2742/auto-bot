package cn.com.helei.bot.core.supporter.netty.constants;


/**
 * WS客户端状态
 */
public enum WebsocketClientStatus {

    /**
     * 新建
     */
    NEW,

    /**
     * 正在启动
     */
    STARTING,

    /**
     * 正在运行
     */
    RUNNING,

    /**
     * 已暂停
     */
    STOP,

    /**
     * 已禁止使用
     */
    SHUTDOWN
}
