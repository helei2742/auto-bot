package cn.com.helei.bot.core.supporter.netty;

import cn.com.helei.bot.core.dto.ConnectStatusInfo;
import cn.com.helei.bot.core.supporter.netty.base.AbstractWebSocketClientHandler;
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
        BaseBotWSClient<Req, Resp> botWSClient = getBotWSClient();


        Req heartbeatMessage = botWSClient.getHeartbeatMessage();
        if (heartbeatMessage != null) {
            botWSClient
                    .sendMessage(heartbeatMessage)
                    .whenCompleteAsync((unused, throwable) -> {
                        ConnectStatusInfo connectStatusInfo = botWSClient
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
                    }, botWSClient.getCallbackInvoker());
        }
    }


    @Override
    public Object getRequestId(Req request) {
        return getBotWSClient().getRequestId(request);
    }

    @Override
    public Object getResponseId(Resp response) {
        return getBotWSClient().getResponseId(response);
    }


    @Override
    protected void handleResponseMessage(Object id, Resp response) {
        getBotWSClient().whenAccountReceiveResponse(id, response);
    }

    @Override
    protected void handleOtherMessage(Resp message) {
        getBotWSClient().whenAccountReceiveMessage(message);
    }

    private BaseBotWSClient<Req, Resp> getBotWSClient() {
        return (BaseBotWSClient<Req, Resp>) websocketClient;
    }
}
