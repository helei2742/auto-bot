package cn.com.helei.DepinBot.core;

import cn.com.helei.DepinBot.core.netty.base.AbstractWebSocketClientHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDepinWSClientHandler<Req, Resp> extends AbstractWebSocketClientHandler<Req, Resp> {


    /**
     * channel 空闲，向其发送心跳
     * @param ctx ctx
     */
    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        websocketClient
                .sendMessage(heartBeatMessage())
                .exceptionallyAsync(throwable -> {
                    // 心跳发送异常，打印日志
                    log.error("client[{}] 发送心跳异常", websocketClient.getName(), throwable);
                    return null;
                }, websocketClient.getCallbackInvoker());
    }

    @Override
    public String getRequestId(Req request) {
        return "";
    }

    @Override
    public String getResponseId(Resp response) {
        return "";
    }

    protected abstract Req heartBeatMessage();
}
