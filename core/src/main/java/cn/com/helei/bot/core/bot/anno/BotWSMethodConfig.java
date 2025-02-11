package cn.com.helei.bot.core.bot.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BotWSMethodConfig {
    /**
     * 是否刷新ws连
     *
     * @return boolean
     */
    boolean isRefreshWSConnection() default false;


    boolean wsUnlimitedRetry() default false;

    /**
     * 重连次数
     */
    int reconnectLimit() default 3;

    /**
     * 心跳间隔
     */
    int heartBeatIntervalSecond() default  30;

    /**
     * websocket 并发数量
     */
    int wsConnectCount() default 50;

    /**
     * 重连减少的间隔
     */
    int reconnectCountDownSecond() default 180;
}
