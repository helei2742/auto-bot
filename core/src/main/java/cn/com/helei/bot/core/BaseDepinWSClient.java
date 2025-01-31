package cn.com.helei.bot.core;

import cn.com.helei.bot.core.dto.account.AccountContext;
import cn.com.helei.bot.core.constants.ConnectStatus;
import cn.com.helei.bot.core.netty.base.AbstractWebsocketClient;
import cn.com.helei.bot.core.netty.constants.WebsocketClientStatus;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;


@Slf4j
@Getter
public abstract class BaseDepinWSClient<Req, Resp> extends AbstractWebsocketClient<Req, Resp> {


    /**
     * client对应的账号
     */
    private final AccountContext accountContext;

    public BaseDepinWSClient(
            AccountContext accountContext,
            BaseDepinWSClientHandler<Req, Resp> handler
    ) {
        super(accountContext.getConnectUrl(), handler);

        DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
        accountContext.getWSHeaders().forEach(httpHeaders::add);
        super.setHeaders(httpHeaders);

        super.setName(accountContext.getName());
        super.setProxy(accountContext.getProxy());
        super.setClientStatusChangeHandler(this::whenClientStatusChange);

        this.accountContext = accountContext;

        updateClientStatus(WebsocketClientStatus.NEW);
    }


    public abstract Req getHeartbeatMessage(BaseDepinWSClient<Req, Resp> wsClient);

    public abstract void whenAccountReceiveResponse(BaseDepinWSClient<Req, Resp> wsClient, Object id, Resp response) ;

    public abstract void whenAccountReceiveMessage(BaseDepinWSClient<Req, Resp> wsClient, Resp message);

    public abstract Object getRequestId(Req request);

    public abstract Object getResponseId(Resp response);

    /**
     * ws客户端状态改变，同步更新账户状态
     *
     * @param newClientStatus 最新的客户端状态
     */
    public void whenClientStatusChange(WebsocketClientStatus newClientStatus) {
        accountContext.getConnectStatusInfo().setConnectStatus(
                switch (newClientStatus) {
                    case NEW -> {
                        accountContext.getConnectStatusInfo().setStartDateTime(LocalDateTime.now());
                        accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                        yield ConnectStatus.NEW;
                    }
                    case STARTING -> {
                        accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                        yield ConnectStatus.STARTING;
                    }
                    case RUNNING -> {
                        accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                        yield ConnectStatus.RUNNING;
                    }
                    case STOP, SHUTDOWN -> {
                        accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                        accountContext.setUsable(false);
                        yield ConnectStatus.STOP;
                    }
                }
        );
    }

}
