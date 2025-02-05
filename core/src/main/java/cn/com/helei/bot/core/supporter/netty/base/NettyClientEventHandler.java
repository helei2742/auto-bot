package cn.com.helei.bot.core.supporter.netty.base;


import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 * netty事件处理器
 */
public interface NettyClientEventHandler {

    /**
     * 激活事件
     * @param ctx ctx
     */
    default void activeHandler(ChannelHandlerContext ctx){}

    /**
     * 关闭事件
     * @param channel channel
     */
    default void closeHandler(Channel channel){}

    /**
     * 异常事件
     * @param ctx ctx
     * @param cause cause
     */
    void exceptionHandler(ChannelHandlerContext ctx, Throwable cause);
}
