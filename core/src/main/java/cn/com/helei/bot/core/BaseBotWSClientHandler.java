package cn.com.helei.bot.core;

import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.netty.base.AbstractWebSocketClientHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseBotWSClientHandler<Req, Resp> extends AbstractWebSocketClientHandler<Req, Resp> {

    /**
     * channel 空闲，向其发送心跳
     * @param ctx ctx
     */
    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        BaseBotWSClient<Req, Resp> depinWSClient = getDepinWSClient();


        Req heartbeatMessage = depinWSClient.getHeartbeatMessage(depinWSClient);
        if (heartbeatMessage != null) {
            depinWSClient
                    .sendMessage(heartbeatMessage)
                    .whenCompleteAsync((unused, throwable) -> {
                        ConnectStatusInfo connectStatusInfo = depinWSClient
                                .getAccountContext()
                                .getConnectStatusInfo();

                        if (throwable != null) {
                            log.error("client[{}] 发送心跳异常", websocketClient.getName(), throwable);
                            // 发送心跳失败，记录次数
                            connectStatusInfo.getErrorHeartBeat().getAndIncrement();
                        }

                        // 心跳计数
                        connectStatusInfo.getHeartBeat()
                                .getAndIncrement();
                    }, depinWSClient.getCallbackInvoker());
        }
    }


    @Override
    public Object getRequestId(Req request) {
        return getDepinWSClient().getRequestId(request);
    }

    @Override
    public Object getResponseId(Resp response) {
        return getDepinWSClient().getResponseId(response);
    }


    @Override
    protected void handleResponseMessage(Object id, Resp response) {
        BaseBotWSClient<Req, Resp> depinWSClient = getDepinWSClient();

        depinWSClient.whenAccountReceiveResponse(depinWSClient, id, response);
    }

    @Override
    protected void handleOtherMessage(Resp message) {
        BaseBotWSClient<Req, Resp> depinWSClient = getDepinWSClient();

        depinWSClient.whenAccountReceiveMessage(depinWSClient, message);
    }

    private BaseBotWSClient<Req, Resp> getDepinWSClient() {
        return (BaseBotWSClient<Req, Resp>) websocketClient;
    }
}
