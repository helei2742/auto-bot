package cn.com.helei.DepinBot.core;

import cn.com.helei.DepinBot.core.dto.account.ConnectStatusInfo;
import cn.com.helei.DepinBot.core.netty.base.AbstractWebSocketClientHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseDepinWSClientHandler<Req, Resp> extends AbstractWebSocketClientHandler<Req, Resp> {

    /**
     * channel 空闲，向其发送心跳
     * @param ctx ctx
     */
    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        BaseDepinWSClient<Req, Resp> depinWSClient = getDepinWSClient();

        depinWSClient
                .sendMessage(depinWSClient.getHeartbeatMessage(depinWSClient))
                .whenCompleteAsync((unused, throwable) -> {
                    ConnectStatusInfo connectStatusInfo = depinWSClient
                            .getAccountContext()
                            .getConnectStatusInfo();

                    if (throwable != null) {
                        log.error("client[{}] 发送心跳异常", websocketClient.getName(), throwable);
                        // 发送心跳失败，记录次数
                        connectStatusInfo.getErrorHeartBeatCount().getAndIncrement();
                    }

                    // 心跳计数
                    connectStatusInfo
                            .getHeartBeatCount()
                            .getAndIncrement();
                }, depinWSClient.getCallbackInvoker());
    }


    @Override
    public String getRequestId(Req request) {
        return "";
    }

    @Override
    public String getResponseId(Resp response) {
        return "";
    }


    @Override
    protected void handleResponseMessage(String id, Resp response) {
        BaseDepinWSClient<Req, Resp> depinWSClient = getDepinWSClient();

        depinWSClient.whenAccountReceiveResponse(depinWSClient, id, response);
    }

    @Override
    protected void handleOtherMessage(Resp message) {
        BaseDepinWSClient<Req, Resp> depinWSClient = getDepinWSClient();

        depinWSClient.whenAccountReceiveMessage(depinWSClient, message);
    }

    private BaseDepinWSClient<Req, Resp> getDepinWSClient() {
        return (BaseDepinWSClient<Req, Resp>) websocketClient;
    }
}
