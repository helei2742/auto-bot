package cn.com.helei.bot.core.bot.constants;

/**
 * Bot Job 类型
 */
public enum BotJobType {
    /**
     * 注册
     */
    REGISTER,
    /**
     * 登录
     */
    LOGIN,
    /**
     * 查询奖励
     */
    QUERY_REWARD,
    /**
     * 定时任务
     */
    TIMED_TASK,
    /**
     * web socket连接任务
     */
    WEB_SOCKET_CONNECT,
}
